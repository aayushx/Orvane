package com.tasky.app.ui.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** User-facing date/time text formatting (system zone, English labels). */
object Format {

    val zone: ZoneId get() = ZoneId.systemDefault()

    fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun timeOf(millis: Long): LocalTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()

    fun startOfDay(day: LocalDate): Long = day.atStartOfDay(zone).toInstant().toEpochMilli()

    fun millisOf(day: LocalDate, time: LocalTime?): Long =
        day.atTime(time ?: LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()

    private val shortDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val tinyDate = DateTimeFormatter.ofPattern("MMM d")
    private val withYear = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val time12h = DateTimeFormatter.ofPattern("h:mm a")
    private val weekdayFull = DateTimeFormatter.ofPattern("EEEE")

    /** Short label like "Today", "Tomorrow", "Wed, Oct 28". */
    fun dueLabel(millis: Long?): String {
        if (millis == null) return ""
        val now = LocalDate.now(zone)
        val d = dateOf(millis)
        return when {
            d == now -> "Today"
            d == now.plusDays(1) -> "Tomorrow"
            d == now.minusDays(1) -> "Yesterday"
            d.year == now.year -> d.format(tinyDate)
            else -> d.format(withYear)
        }
    }

    fun weekdayName(day: LocalDate): String = day.format(weekdayFull)

    /** e.g. "Today 5:00 PM", "Oct 28", "Overdue: Today". */
    fun dueChip(millis: Long, allDay: Boolean): String {
        val base = dueLabel(millis)
        return if (allDay) base else "$base ${timeOf(millis).format(time12h)}"
    }

    fun timeLabel(millis: Long): String = timeOf(millis).format(time12h)

    fun dateLabel(millis: Long): String = dateOf(millis).format(shortDate)

    fun priorityName(priority: Int): String = when (priority) {
        1 -> "Low"
        2 -> "Medium"
        3 -> "High"
        else -> "None"
    }

    fun repeatName(rule: String?): String {
        if (rule == null) return ""
        val tokens = rule.split(";").mapNotNull { p ->
            val i = p.indexOf('=')
            if (i > 0) p.substring(0, i) to p.substring(i + 1) else null
        }.toMap()
        val freq = tokens["FREQ"] ?: return ""
        val every = tokens["INTERVAL"]?.toIntOrNull() ?: 1
        val n = if (every > 1) "every $every " else ""
        return when (freq) {
            "DAILY" -> "${n}day${if (every > 1) "s" else ""}"
            "WEEKLY" -> "${n}week${if (every > 1) "s" else ""}"
            "MONTHLY" -> "${n}month${if (every > 1) "s" else ""}"
            "YEARLY" -> "${n}year${if (every > 1) "s" else ""}"
            "COMPLETION_AFTER_DAYS" -> "days after completion"
            "COMPLETION_AFTER_WEEKS" -> "weeks after completion"
            "COMPLETION_AFTER_MONTHS" -> "months after completion"
            else -> freq.lowercase()
        }
    }

    /** Trashed N days ago style for trash list rows. */
    fun daysRemaining(deletedAt: Long, retentionDays: Long): Long {
        val elapsed = (System.currentTimeMillis() - deletedAt) / (24 * 3600_000L)
        return (retentionDays - elapsed).coerceAtLeast(0)
    }

    fun dowCode(day: LocalDate): String = when (day.dayOfWeek) {
        DayOfWeek.MONDAY -> "MO"
        DayOfWeek.TUESDAY -> "TU"
        DayOfWeek.WEDNESDAY -> "WE"
        DayOfWeek.THURSDAY -> "TH"
        DayOfWeek.FRIDAY -> "FR"
        DayOfWeek.SATURDAY -> "SA"
        else -> "SU"
    }
}
