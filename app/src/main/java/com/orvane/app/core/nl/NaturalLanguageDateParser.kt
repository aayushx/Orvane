package com.orvane.app.core.nl

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Natural-language quick-add parser.
 * Understands dates ("today", "tomorrow", "tonight", "eod", "friday", "next monday",
 * "in 3 days / 2 weeks / 4 hours", "aug 15", "15 aug", "2026-08-15", "8/15"),
 * times ("5pm", "9:30am", "17:00", "noon", "midnight", "at 5pm"),
 * priorities ("!high" / "!!!" / "!3", "!medium" / "!!" / "!2", "!low" / "!" / "!1")
 * and tags ("#work").
 *
 * Matched tokens are removed from the remaining title. Date and time tokens may
 * appear in any order; they are merged at the end.
 */
object NaturalLanguageDateParser {

    data class Parsed(
        val title: String,
        val due: ZonedDateTime?,
        val isAllDay: Boolean,
        val priority: Int,
        val tags: List<String>,
    )

    const val NO_PRIORITY = 0
    const val PRIORITY_LOW = 1
    const val PRIORITY_MEDIUM = 2
    const val PRIORITY_HIGH = 3

    private val MONTHS = mapOf(
        "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4, "may" to 5, "jun" to 6, "june" to 6, "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8, "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10, "nov" to 11, "november" to 11, "dec" to 12, "december" to 12,
    )

    private val WEEKDAYS = mapOf(
        "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "weds" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY,
    )

    private val PRIORITIES: List<Pair<String, Int>> = listOf(
        "!high" to PRIORITY_HIGH,
        "!medium" to PRIORITY_MEDIUM,
        "!low" to PRIORITY_LOW,
        "!!!" to PRIORITY_HIGH,
        "!!" to PRIORITY_MEDIUM,
        "!hi" to PRIORITY_HIGH,
        "!med" to PRIORITY_MEDIUM,
        "!lo" to PRIORITY_LOW,
        "!1" to PRIORITY_LOW,
        "!2" to PRIORITY_MEDIUM,
        "!3" to PRIORITY_HIGH,
        "!h" to PRIORITY_HIGH,
        "!m" to PRIORITY_MEDIUM,
        "!l" to PRIORITY_LOW,
        "!" to PRIORITY_LOW,
    ).sortedByDescending { it.first.length }

    private val AM_PM_TIME = Regex("^([0-9]{1,2})(?::([0-9]{1,2}))?(am|pm)$")
    private val H24_TIME = Regex("^([01]?[0-9]|2[0-3]):([0-5][0-9])$")
    private val ISO_DATE = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$")
    private val SLASH_DATE = Regex("^(\\d{1,4})[/.](\\d{1,2})(?:[/.](\\d{2,4}))?$")
    private val DAY_NUM = Regex("^([0-9]{1,2})(?:st|nd|rd|th)?$")
    private val TAG_TOKEN = Regex("(?<=^|\\s)#([A-Za-z0-9_\\-]+)")
    private val YEAR_4 = Regex("^\\d{4}$")

    fun parse(raw: String, now: ZonedDateTime = ZonedDateTime.now()): Parsed {
        var priority = NO_PRIORITY
        val tags = mutableListOf<String>()

        var text = raw

        for ((token, p) in PRIORITIES) {
            if (priority != NO_PRIORITY) break
            val r = Regex("(?<=^|\\s|,)" + Regex.escape(token) + "(?=\\s|,|\\.|!|:|$)", RegexOption.IGNORE_CASE)
            if (r.containsMatchIn(text)) {
                priority = p
                text = r.replace(text, " ")
            }
        }

        text = TAG_TOKEN.replace(text) { m -> tags.add(m.groupValues[1]); " " }

        var pendingDate: LocalDate? = null
        var pendingTime: LocalTime? = null
        var hasTime = false

        fun applyDate(d: LocalDate?) {
            if (d != null) pendingDate = d
        }

        fun applyTime(t: LocalTime?) {
            if (t != null) {
                pendingTime = t
                hasTime = true
            }
        }

        fun applyRelative(absolute: ZonedDateTime) {
            pendingDate = absolute.toLocalDate()
            pendingTime = absolute.toLocalTime()
            hasTime = true
        }

        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val titleTokens = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i].trimEnd(',', '.', ';', ':')
            val lower = token.lowercase()
            var consumed = 1

            // "at"/"on" followed by time or date
            if ((lower == "at" || lower == "on") && tokens.size > i + 1) {
                val next = tokens[i + 1].trimEnd(',', '.', ';', ':')
                val nextLower = next.lowercase()
                val time = tryParseTime(nextLower)
                if (time != null) {
                    applyTime(time); i += 2; continue
                }
                var extra = 0
                val date = tryParseDateToken(nextLower, tokens.drop(i + 2).map { it.trimEnd(',', '.', ';', ':') }, now) { e -> extra = e }
                if (date != null) {
                    applyDate(date); i += 2 + extra; continue
                }
                titleTokens.add(tokens[i]); i += 1; continue
            }

            when {
                lower == "in" -> {
                    val nTok = tokens.getOrNull(i + 1)?.trimEnd(',', '.')
                    val unit = tokens.getOrNull(i + 2)?.trimEnd(',', '.', ';', ':')?.lowercase()
                    val n = nTok?.toIntOrNull()
                    if (n != null && unit != null && n > 0) {
                        val ok = when {
                            unit.startsWith("hour") -> { applyRelative(now.plusHours(n.toLong())); true }
                            unit.startsWith("minute") -> { applyRelative(now.plusMinutes(n.toLong())); true }
                            unit.startsWith("day") -> { applyDate(now.toLocalDate().plusDays(n.toLong())); true }
                            unit.startsWith("week") -> { applyDate(now.toLocalDate().plusWeeks(n.toLong())); true }
                            else -> false
                        }
                        if (ok) { i += 3; continue }
                    }
                    titleTokens.add(tokens[i]); i += 1
                }
                lower == "today" || lower == "tod" -> { applyDate(now.toLocalDate()); i += 1 }
                lower == "tomorrow" || lower == "tmrw" || lower == "tmr" || lower == "tom" -> {
                    applyDate(now.toLocalDate().plusDays(1)); i += 1
                }
                lower == "tonight" -> { applyDate(now.toLocalDate()); applyTime(LocalTime.of(20, 0)); i += 1 }
                lower == "eod" -> { applyDate(now.toLocalDate()); applyTime(LocalTime.of(18, 0)); i += 1 }
                lower == "noon" -> { applyTime(LocalTime.NOON); i += 1 }
                lower == "midnight" -> { applyTime(LocalTime.MIDNIGHT); i += 1 }
                (lower == "next" || lower == "this") -> {
                    val next = tokens.getOrNull(i + 1)?.trimEnd(',', '.', ';', ':')?.lowercase()
                    when {
                        next == "week" -> { applyDate(now.toLocalDate().plusWeeks(1)); i += 2 }
                        next == "month" -> { applyDate(now.toLocalDate().plusMonths(1)); i += 2 }
                        next != null && WEEKDAYS.containsKey(next) -> {
                            applyDate(now.toLocalDate().nextWeekday(WEEKDAYS[next]!!, true)); i += 2
                        }
                        next == null -> { titleTokens.add(tokens[i]); i += 1 }
                        else -> {
                            // "next" as part of title
                            titleTokens.add(tokens[i]); i += 1
                        }
                    }
                }
                else -> {
                    val time = tryParseTime(lower)
                    if (time != null) { applyTime(time); i += 1; continue }
                    var extra = 0
                    val date = tryParseDateToken(lower, tokens.drop(i + 1).map { it.trimEnd(',', '.', ';', ':') }, now) { e -> extra = e }
                    if (date != null) {
                        applyDate(date); i += consumed + extra; continue
                    }
                    titleTokens.add(tokens[i]); i += 1
                }
            }
        }

        val due: ZonedDateTime? = when {
            pendingDate != null && hasTime && pendingTime != null ->
                pendingDate!!.atTime(pendingTime).atZone(now.zone)
            pendingDate != null ->
                pendingDate!!.atStartOfDay(now.zone)
            hasTime && pendingTime != null ->
                now.toLocalDate().atTime(pendingTime).atZone(now.zone)
            else -> null
        }

        val allDay = due != null && !hasTime

        val finalTitle = titleTokens.joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return Parsed(title = finalTitle, due = due, isAllDay = allDay, priority = priority, tags = tags)
    }

    private fun tryParseDateToken(
        tokenLower: String,
        following: List<String>,
        now: ZonedDateTime,
        onExtra: (Int) -> Unit,
    ): LocalDate? {
        // plain weekday: "friday" (or "fri day"? no)
        WEEKDAYS[tokenLower]?.let { dow ->
            return now.toLocalDate().nextWeekday(dow, false)
        }
        ISO_DATE.matchEntire(tokenLower)?.let { m ->
            val y = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            val d = m.groupValues[3].toInt()
            return try { LocalDate.of(y, mo, d) } catch (e: Exception) { null }
        }
        SLASH_DATE.matchEntire(tokenLower)?.let { m ->
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            val yearTok = m.groupValues[3]
            return try {
                if (yearTok.isNotEmpty()) {
                    var year = yearTok.toInt()
                    if (year < 100) year += 2000
                    val md = if (a > 12) b to a else a to b
                    LocalDate.of(year, md.first, md.second)
                } else {
                    val md = if (a > 12) b to a else a to b
                    LocalDate.of(now.year, md.first, md.second).let {
                        if (it < now.toLocalDate()) it.plusYears(1) else it
                    }
                }
            } catch (e: Exception) { null }
        }

        // "aug 15 [2026]"
        val month = MONTHS[tokenLower]
        if (month != null) {
            val dayTok = following.getOrNull(0)?.lowercase()?.let { it.trimEnd(',') }
            val dayDigit = dayTok?.let { DAY_NUM.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
            if (dayDigit != null && dayDigit in 1..31) {
                var consumedCount = 1
                var year: Int? = null
                val yearTok = following.getOrNull(1)?.lowercase()
                if (yearTok != null && YEAR_4.matches(yearTok)) {
                    year = yearTok.toInt(); consumedCount = 2
                }
                onExtra(consumedCount)
                return try {
                    val y = year ?: now.year
                    val d = LocalDate.of(y, month, dayDigit)
                    if (year == null && d < now.toLocalDate()) d.plusYears(1) else d
                } catch (e: Exception) { null }
            }
            return null
        }

        // "15 aug [2026]"
        val dayDigit = DAY_NUM.matchEntire(tokenLower)?.groupValues?.get(1)?.toIntOrNull()
        if (dayDigit != null && dayDigit in 1..31 && following.isNotEmpty()) {
            val monthTok = following.getOrNull(0)?.lowercase()?.trimEnd(',')
            val m = monthTok?.let { MONTHS[it] }
            if (m != null) {
                var consumedCount = 1
                var year: Int? = null
                val yearTok = following.getOrNull(1)?.lowercase()
                if (yearTok != null && YEAR_4.matches(yearTok)) {
                    year = yearTok.toInt(); consumedCount = 2
                }
                onExtra(consumedCount)
                return try {
                    val y = year ?: now.year
                    val d = LocalDate.of(y, m, dayDigit)
                    if (year == null && d < now.toLocalDate()) d.plusYears(1) else d
                } catch (e: Exception) { null }
            }
        }
        return null
    }

    private fun tryParseTime(tokenLower: String): LocalTime? {
        val t = tokenLower.trim().removeSuffix(".")
        if (t.isEmpty()) return null
        if (t == "noon") return LocalTime.NOON
        if (t == "midnight") return LocalTime.MIDNIGHT
        AM_PM_TIME.matchEntire(t)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return null
            val mins = m.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: 0
            val isPm = m.groupValues[3].equals("pm", true)
            return amPmToTime(h, mins, isPm)
        }
        H24_TIME.matchEntire(t)?.let { m ->
            return try { LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()) } catch (e: Exception) { null }
        }
        return null
    }

    private fun amPmToTime(h: Int, minute: Int, pm: Boolean): LocalTime {
        val dayHour = if (h == 0) 12 else h
        val hour24 = when {
            pm && dayHour < 12 -> dayHour + 12
            !pm && dayHour == 12 -> 0
            else -> dayHour
        }
        return LocalTime.of(hour24, minute)
    }

    /** Next date whose day-of-week is [dow], at least 1 day ahead. */
    private fun LocalDate.plainNext(dow: DayOfWeek): LocalDate {
        var d = this.plusDays(1)
        while (d.dayOfWeek != dow) d = d.plusDays(1)
        return d
    }

    /**
     * Plain weekday: the next occurrence (1..7 days ahead).
     * "next <weekday>": if the plain occurrence falls within the current week
     * (today..Sunday), jump a full week ahead instead.
     */
    private fun LocalDate.nextWeekday(dow: DayOfWeek, wantNextWeek: Boolean): LocalDate {
        val p = plainNext(dow)
        if (!wantNextWeek) return p
        val endOfWeek = this.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))
        return if (!p.isAfter(endOfWeek)) p.plusWeeks(1) else p
    }
}
