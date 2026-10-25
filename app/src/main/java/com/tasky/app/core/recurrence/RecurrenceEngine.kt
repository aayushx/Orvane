package com.tasky.app.core.recurrence

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Recurrence engine. A rule is stored as a compact string:
 *
 *   FREQ=DAILY;INTERVAL=2
 *   FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE
 *   FREQ=WEEKLY;INTERVAL=2
 *   FREQ=MONTHLY;INTERVAL=1
 *   FREQ=MONTHLY;INTERVAL=1;BYMONTHDAY=15
 *   FREQ=MONTHLY;INTERVAL=1;BYDAY=2FR   (2nd Friday)
 *   FREQ=YEARLY;INTERVAL=1
 *   COMPLETION_AFTER_DAYS=3
 *   COMPLETION_AFTER_WEEKS=2
 *   COMPLETION_AFTER_MONTHS=1
 *
 * Optional COUNT / UNTIL(epoch millis) may appear on standard rules.
 */
object RecurrenceEngine {

    private const val FREQ_COMPLETION = "COMPLETION"

    data class Recurrence(
        val freq: String,
        val interval: Int = 1,
        val weekdays: List<DayOfWeek> = emptyList(),
        val byMonthDay: Int? = null,
        val nthWeekday: Nth? = null,
        val count: Int? = null,
        val until: Long? = null, // epoch millis
    ) {
        data class Nth(val nth: Int) // 1..4 = nth, 5 = last
    }

    fun parse(rule: String?): Recurrence? {
        if (rule.isNullOrBlank()) return null
        val map = rule.split(";").mapNotNull { p ->
            val kv = p.split("=", limit = 2)
            if (kv.size != 2) null else kv[0].trim().uppercase() to kv[1].trim()
        }.toMap()

        return when {
            map.containsKey("FREQ") -> {
                val freq = map["FREQ"]!!.uppercase()
                val interval = map["INTERVAL"]?.toIntOrNull() ?: 1
                val weekdays = map["BYDAY"]
                    ?.split(",")
                    ?.mapNotNull { dowOf(it) }
                    .orEmpty()
                val byMonthDay = map["BYMONTHDAY"]?.toIntOrNull()
                val nth = map["BYDAY"]?.let { parseNth(it) }
                val count = map["COUNT"]?.toIntOrNull()
                val until = map["UNTIL"]?.toLongOrNull()
                if (freq !in listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) return null
                if (interval < 1) return null
                Recurrence(
                    freq = freq,
                    interval = interval,
                    weekdays = if (freq == "WEEKLY") weekdays else emptyList(),
                    byMonthDay = if (freq == "MONTHLY") byMonthDay else null,
                    nthWeekday = if (freq == "MONTHLY") nth else null,
                    count = count,
                    until = until,
                )
            }
            map.containsKey("COMPLETION_AFTER_DAYS") -> Recurrence(
                freq = FREQ_COMPLETION,
                interval = map["COMPLETION_AFTER_DAYS"]?.toIntOrNull() ?: 1,
            )
            map.containsKey("COMPLETION_AFTER_WEEKS") -> Recurrence(
                freq = FREQ_COMPLETION,
                interval = (map["COMPLETION_AFTER_WEEKS"]?.toIntOrNull() ?: 1) * 7,
            )
            map.containsKey("COMPLETION_AFTER_MONTHS") -> Recurrence(
                freq = FREQ_COMPLETION,
                interval = -(map["COMPLETION_AFTER_MONTHS"]?.toIntOrNull() ?: 1), // negative marks months
            )
            else -> null
        }
    }

    /**
     * The next occurrence strictly after [currentDue] (calendar-based rules),
     * or after [completedAt] (completion-based rules).
     * Returns null when the rule is exhausted or invalid.
     */
    fun nextDue(
        rule: String?,
        currentDue: ZonedDateTime?,
        completedAt: ZonedDateTime,
    ): ZonedDateTime? {
        val r = parse(rule) ?: return null

        if (r.freq == FREQ_COMPLETION) {
            val base = completedAt
            val keepTime = currentDue?.toLocalTime() ?: LocalTime.of(9, 0)
            val nextDate: LocalDate? = if (r.interval > 0) {
                base.toLocalDate().plusDays(r.interval.toLong())
            } else {
                base.toLocalDate().plusMonths((-r.interval).toLong())
            }
            return nextDate?.atTime(keepTime)?.atZone(base.zone)
        }

        val current = currentDue ?: completedAt
        val anchor = current.toLocalDate()
        val anchorTime = current.toLocalTime()
        val zone = current.zone

        val next: ZonedDateTime? = when (r.freq) {
            "DAILY" -> anchor.plusDays(r.interval.toLong()).atTime(anchorTime).atZone(zone)
            "WEEKLY" -> {
                if (r.weekdays.isEmpty()) {
                    anchor.plusWeeks(r.interval.toLong()).atTime(anchorTime).atZone(zone)
                } else {
                    val anchorWeek = Math.floorDiv(anchor.toEpochDay(), 7)
                    var candidate: LocalDate? = null
                    var d = anchor.plusDays(1)
                    var guard = 0
                    while (guard < 400) {
                        val weekIndex = Math.floorDiv(d.toEpochDay(), 7)
                        if (r.weekdays.contains(d.dayOfWeek) &&
                            Math.floorMod(weekIndex - anchorWeek, r.interval.toLong()) == 0L
                        ) {
                            candidate = d
                            break
                        }
                        d = d.plusDays(1)
                        guard += 1
                    }
                    candidate?.atTime(anchorTime)?.atZone(zone)
                }
            }
            "MONTHLY" -> monthlyNext(r, anchor, anchorTime, zone)
            "YEARLY" -> anchor.plusYears(r.interval.toLong()).atTime(anchorTime).atZone(zone)
            else -> null
        } ?: return null

        val finalNext: ZonedDateTime = next ?: return null
        r.until?.let { until ->
            if (finalNext.toInstant().toEpochMilli() > until) return null
        }
        if (r.count != null && r.count <= 0) return null
        return finalNext
    }

    private fun monthlyNext(
        r: Recurrence,
        anchor: LocalDate,
        anchorTime: LocalTime,
        zone: java.time.ZoneId,
    ): ZonedDateTime? {
        var month = anchor.monthValue
        var year = anchor.year
        var guard = 0
        var result: LocalDate? = null
        while (guard < 120 && result == null) {
            month += r.interval
            while (month > 12) { month -= 12; year += 1 }
            val len = LocalDate.of(year, month, 1).lengthOfMonth()
            val cand: LocalDate? = when {
                r.byMonthDay != null && r.byMonthDay <= len -> LocalDate.of(year, month, r.byMonthDay)
                r.nthWeekday != null -> {
                    val dow = r.weekdays.firstOrNull() ?: anchor.dayOfWeek
                    nthWeekdayOf(year, month, dow, r.nthWeekday.nth)
                }
                anchor.dayOfMonth <= len -> LocalDate.of(year, month, anchor.dayOfMonth)
                else -> null
            }
            if (cand != null && cand.isAfter(anchor)) result = cand
            guard += 1
        }
        return result?.atTime(anchorTime)?.atZone(zone)
    }

    /** Generate occurrences strictly after [after]. */
    fun occurrences(rule: String?, after: ZonedDateTime, limit: Int = 10): List<ZonedDateTime> {
        val r = parse(rule) ?: return emptyList()
        val out = mutableListOf<ZonedDateTime>()
        var current = after
        guard@ for (iter in 0 until (limit * 40 + 400)) {
            val next = nextDue(rule, current, after) ?: break
            if (r.until != null && next.toInstant().toEpochMilli() > r.until) break
            if (r.count != null && out.size >= r.count) break
            out.add(next)
            current = next
            if (out.size >= limit) break@guard
        }
        return out
    }

    /** Human-friendly description for the UI. */
    fun describe(rule: String?): String {
        val r = parse(rule) ?: return "Never"
        if (r.freq == FREQ_COMPLETION) {
            return when {
                r.interval > 0 && r.interval >= 14 && r.interval % 7 == 0 -> "${r.interval / 7} week${if (r.interval == 7) "" else "s"} after completion"

                r.interval > 0 -> "${if (r.interval == 1) "1 day" else "${r.interval} days"} after completion"
                else -> {
                    val months = -r.interval
                    "${if (months == 1) "1 month" else "${months} months"} after completion"
                }
            }
        }
        return when (r.freq) {
            "DAILY" -> if (r.interval == 1) "Every day" else "Every ${r.interval} days"
            "WEEKLY" -> when {
                r.weekdays.isNotEmpty() -> {
                    val days = listOf(
                        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
                    ).filter { r.weekdays.contains(it) }.joinToString(", ") { dowName(it) }
                    if (r.interval == 1) "Weekly on $days" else "Every ${r.interval} weeks on $days"
                }
                r.interval == 1 -> "Every week"
                else -> "Every ${r.interval} weeks"
            }
            "MONTHLY" -> when {
                r.byMonthDay != null && r.interval == 1 -> "Monthly on day ${r.byMonthDay}"
                r.nthWeekday != null && r.interval == 1 ->
                    "Monthly on the ${nthName(r.nthWeekday.nth, r.weekdays.firstOrNull())}"
                r.interval == 1 -> "Every month"
                else -> "Every ${r.interval} months"
            }
            "YEARLY" -> "Yearly"
            else -> "Repeats"
        }
    }

    private fun parseNth(byday: String): Recurrence.Nth? {
        val digits = byday.takeWhile { it.isDigit() || it == '-' }
        if (digits.isEmpty()) return null
        val n = digits.toIntOrNull() ?: return null
        return when {
            n in 1..4 -> Recurrence.Nth(n)
            n == -1 || n == 5 -> Recurrence.Nth(5)
            else -> null
        }
    }

    private fun dowOf(s: String): DayOfWeek? = when (s.trim().uppercase().takeLast(2)) {
        "MO" -> DayOfWeek.MONDAY
        "TU" -> DayOfWeek.TUESDAY
        "WE" -> DayOfWeek.WEDNESDAY
        "TH" -> DayOfWeek.THURSDAY
        "FR" -> DayOfWeek.FRIDAY
        "SA" -> DayOfWeek.SATURDAY
        "SU" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun dowName(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }

    private fun nthName(n: Int, dow: DayOfWeek?): String {
        val word = when (n) {
            1 -> "first"
            2 -> "second"
            3 -> "third"
            4 -> "fourth"
            else -> "last"
        }
        return "$word ${dow?.let { dowName(it) } ?: "day"}"
    }

    private fun nthWeekdayOf(year: Int, month: Int, dow: DayOfWeek, nth: Int): LocalDate {
        val first = LocalDate.of(year, month, 1)
        return if (nth == 5) {
            first.with(TemporalAdjusters.lastInMonth(dow))
        } else {
            first.with(TemporalAdjusters.nextOrSame(dow)).plusWeeks((nth - 1).toLong())
        }
    }
}
