package com.tasky.app.core.streak

import com.tasky.app.data.db.CheckStatus
import com.tasky.app.data.db.HabitCheckEntity
import com.tasky.app.data.db.HabitEntity
import com.tasky.app.data.db.HabitFreqType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Computes habit streaks and completion statistics.
 *
 * Semantics:
 *  - CHECKED extends a streak; SKIPPED neither extends nor breaks it.
 *  - A scheduled day (or met period) with no record breaks a streak historically.
 *  - "Current" streak: consecutive met periods ending today; an unchecked today is
 *    treated as still-in-progress and does not break the streak.
 */
object StreakCalculator {

    data class Stats(
        val currentStreak: Int,
        val longestStreak: Int,
        val totalChecked: Int,
        val totalSkipped: Int,
        val totalScheduled: Int,
        val completionPercent: Int,          // 0..100 since creation
        val checkedLast7: Int,
        val checkedLast30: Int,
        val checkedLast90: Int,
        val checkedAllTime: Int,
    )

    fun compute(habit: HabitEntity, checks: List<HabitCheckEntity>, today: LocalDate): Stats {
        val checked: Set<Long> = checks.filter { it.status == CheckStatus.CHECKED }
            .map { it.epochDay }.toSet()
        val skipped: Set<Long> = checks.filter { it.status == CheckStatus.SKIPPED }
            .map { it.epochDay }.toSet()

        val created: LocalDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val recordStart = (checked + skipped).minOrNull()?.let { LocalDate.ofEpochDay(it) }
        val startDate = listOfNotNull(created, recordStart).minOrNull() ?: today

        return when (habit.freqType) {
            HabitFreqType.WEEK_COUNT,
            HabitFreqType.MONTH_COUNT,
            -> computePeriod(habit, checked, startDate, today)
            else -> computeDaily(habit, checked, skipped, startDate, today)
        }
    }

    /** Whether a given day is a scheduled check-in day for day-based habits. */
    fun isScheduledDay(habit: HabitEntity, day: LocalDate): Boolean = when (habit.freqType) {
        HabitFreqType.WEEKDAYS -> habit.weekdays.contains(dowCode(day))
        else -> true
    }

    private fun computeDaily(
        habit: HabitEntity,
        checked: Set<Long>,
        skipped: Set<Long>,
        startDate: LocalDate,
        today: LocalDate,
    ): Stats {
        var longest = 0
        var run = 0
        var scheduledCount = 0
        var d = startDate
        while (!d.isAfter(today)) {
            val epoch = d.toEpochDay()
            if (isScheduledDay(habit, d)) {
                scheduledCount += 1
                when {
                    checked.contains(epoch) -> { run += 1; if (run > longest) longest = run }
                    skipped.contains(epoch) -> { /* skip preserves the run */ }
                    else -> run = 0
                }
            }
            d = d.plusDays(1)
        }

        // Current streak: walk back from today; an unchecked today is "in progress".
        var current = 0
        var cursor = today
        if (isScheduledDay(habit, cursor) && !checked.contains(cursor.toEpochDay())) {
            cursor = cursor.minusDays(1)
        }
        while (!cursor.isBefore(startDate)) {
            val epoch = cursor.toEpochDay()
            if (isScheduledDay(habit, cursor)) {
                when {
                    checked.contains(epoch) -> current += 1
                    skipped.contains(epoch) -> { /* pass; streak preserved */ }
                    else -> break
                }
            }
            cursor = cursor.minusDays(1)
        }

        val last7 = countCheckedInWindow(checked, today, 6)
        val last30 = countCheckedInWindow(checked, today, 29)
        val last90 = countCheckedInWindow(checked, today, 89)

        val percent = if (scheduledCount > 0) {
            (100L * checked.size / scheduledCount).toInt().coerceIn(0, 100)
        } else 0

        return Stats(
            currentStreak = current,
            longestStreak = longest,
            totalChecked = checked.size,
            totalSkipped = skipped.size,
            totalScheduled = scheduledCount,
            completionPercent = percent,
            checkedLast7 = last7,
            checkedLast30 = last30,
            checkedLast90 = last90,
            checkedAllTime = checked.size,
        )
    }

    private fun countCheckedInWindow(checked: Set<Long>, today: LocalDate, daysBack: Int): Int =
        (today.minusDays(daysBack.toLong()).toEpochDay()..today.toEpochDay()).count { checked.contains(it) }

    /**
     * For "X times per week" / "X times per month" habits. A period is met when the
     * number of checked days in it >= target.
     */
    private fun computePeriod(
        habit: HabitEntity,
        checked: Set<Long>,
        startDate: LocalDate,
        today: LocalDate,
    ): Stats {
        val weekly = habit.freqType == HabitFreqType.WEEK_COUNT
        val target = (if (weekly) habit.timesPerWeek else habit.timesPerMonth).coerceAtLeast(1)

        fun periodKey(d: LocalDate): LocalDate = if (weekly) d.with(DayOfWeek.MONDAY) else d.withDayOfMonth(1)
        fun step(p: LocalDate): LocalDate = if (weekly) p.plusDays(7) else p.plusMonths(1)
        fun periodEnd(p: LocalDate): LocalDate = step(p).minusDays(1)

        fun countInPeriod(periodStart: LocalDate): Int {
            val end = minOf(periodEnd(periodStart), today)
            if (periodStart.isAfter(today)) return 0
            return (periodStart.toEpochDay()..end.toEpochDay()).count { checked.contains(it) }
        }

        val thisPeriod = periodKey(today)
        val startPeriod = periodKey(startDate)

        var longest = 0
        var run = 0
        var periodsElapsed = 0
        var currentStreak = 0

        var p = startPeriod
        while (!p.isAfter(thisPeriod)) {
            val count = countInPeriod(p)
            val isCurrent = p == thisPeriod
            // Count the current in-progress period toward slots too (operator sees progress)
            if (count >= target) {
                run += 1
                if (run > longest) longest = run
                currentStreak += 1
            } else if (!isCurrent) {
                // period over and unmet: resets both runs
                run = 0
                currentStreak = 0
            } else {
                // current unmet: freeze history, don't reset anything
            }
            periodsElapsed += 1
            p = step(p)
        }

        val last7 = countCheckedInWindow(checked, today, 6)
        val last30 = countCheckedInWindow(checked, today, 29)
        val last90 = countCheckedInWindow(checked, today, 89)

        val slots = (periodsElapsed.toLong() * target)
        val percent = if (slots > 0) (100L * checked.size / slots).toInt().coerceIn(0, 100) else 0

        return Stats(
            currentStreak = currentStreak,
            longestStreak = longest,
            totalChecked = checked.size,
            totalSkipped = 0,
            totalScheduled = slots.toInt(),
            completionPercent = percent,
            checkedLast7 = last7,
            checkedLast30 = last30,
            checkedLast90 = last90,
            checkedAllTime = checked.size,
        )
    }

    private fun dowCode(day: LocalDate): String = when (day.dayOfWeek) {
        DayOfWeek.MONDAY -> "MO"
        DayOfWeek.TUESDAY -> "TU"
        DayOfWeek.WEDNESDAY -> "WE"
        DayOfWeek.THURSDAY -> "TH"
        DayOfWeek.FRIDAY -> "FR"
        DayOfWeek.SATURDAY -> "SA"
        else -> "SU"
    }
}
