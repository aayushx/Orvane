package com.tasky.app.core.streak

import com.tasky.app.data.db.CheckStatus
import com.tasky.app.data.db.HabitCheckEntity
import com.tasky.app.data.db.HabitEntity
import com.tasky.app.data.db.HabitFreqType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 25) // a Sunday

    private fun habit(
        freqType: Int,
        weekdays: List<String> = emptyList(),
        timesPerWeek: Int = 3,
        timesPerMonth: Int = 10,
        createdAt: LocalDate = LocalDate.of(2026, 9, 1),
    ) = HabitEntity(
        id = "h1",
        name = "Test habit",
        freqType = freqType,
        weekdays = weekdays,
        timesPerWeek = timesPerWeek,
        timesPerMonth = timesPerMonth,
        createdAt = createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    private fun check(epochDay: Long, status: Int = CheckStatus.CHECKED) =
        HabitCheckEntity(habitId = "h1", epochDay = epochDay, status = status, createdAt = 0)

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day).toEpochDay()

    @Test
    fun `daily streak continues when today is unchecked`() {
        // Fri 23 + Sat 24 checked, Sunday 25 not yet.
        val checks = listOf(check(d(10, 23)), check(d(10, 24)))
        val stats = StreakCalculator.compute(habit(HabitFreqType.DAILY), checks, today)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
        assertEquals(2, stats.checkedLast7)
    }

    @Test
    fun `daily streak broken by a missed day`() {
        val checks = listOf(
            check(d(10, 19)), // Mon
            // Tue 20 missed
            check(d(10, 21)),
            check(d(10, 22)),
            check(d(10, 23)),
            check(d(10, 24)),
        )
        val stats = StreakCalculator.compute(habit(HabitFreqType.DAILY), checks, today)
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
    }

    @Test
    fun `skipped day preserves the streak`() {
        val checks = listOf(
            check(d(10, 22), CheckStatus.CHECKED), // Thu
            check(d(10, 23), CheckStatus.SKIPPED), // Fri
            check(d(10, 24), CheckStatus.CHECKED), // Sat
        )
        val stats = StreakCalculator.compute(habit(HabitFreqType.DAILY), checks, today)
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
        assertEquals(1, stats.totalSkipped)
    }

    @Test
    fun `weekday habit ignores non-scheduled days`() {
        // Mondays in Oct 2026: 5, 12, 19 — all checked; other days must not matter.
        val checks = listOf(check(d(10, 5)), check(d(10, 12)), check(d(10, 19)))
        val stats = StreakCalculator.compute(
            habit(HabitFreqType.WEEKDAYS, weekdays = listOf("MO"), createdAt = LocalDate.of(2026, 10, 5)),
            checks,
            today,
        )
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(100, stats.completionPercent)
    }

    @Test
    fun `weekday habit breaks when a scheduled day is missed`() {
        val checks = listOf(check(d(10, 5)), check(d(10, 12)))
        // Monday Oct 19 missed; today is Sunday Oct 25.
        val stats = StreakCalculator.compute(
            habit(HabitFreqType.WEEKDAYS, weekdays = listOf("MO")),
            checks,
            today,
        )
        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun `weekly count habit with met weeks`() {
        // Week Oct 19-25: 3 checks (met). Week Oct 12-18: only 2 (missed, resets). Week Oct 5-11: met.
        val checks = listOf(
            check(d(10, 21)), check(d(10, 22)), check(d(10, 23)),
            check(d(10, 13)), check(d(10, 14)),
            check(d(10, 6)), check(d(10, 7)), check(d(10, 8)),
        )
        val stats = StreakCalculator.compute(
            habit(HabitFreqType.WEEK_COUNT, timesPerWeek = 3, createdAt = LocalDate.of(2026, 10, 1)),
            checks,
            today,
        )
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.longestStreak)
        assertEquals(8, stats.checkedLast30)
    }

    @Test
    fun `weekly count current in-progress week keeps streak`() {
        // Prior week met; this week only 1 of 3 so far — streak must not reset.
        val checks = listOf(
            check(d(10, 13)), check(d(10, 14)), check(d(10, 15)),
            check(d(10, 21)),
        )
        val stats = StreakCalculator.compute(
            habit(HabitFreqType.WEEK_COUNT, timesPerWeek = 3, createdAt = LocalDate.of(2026, 10, 1)),
            checks,
            today,
        )
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.longestStreak)
    }

    @Test
    fun `monthly count habit counts met months`() {
        val checks = listOf(
            check(d(9, 10)), check(d(9, 11)),
            check(d(10, 3)), check(d(10, 4)),
        )
        val stats = StreakCalculator.compute(
            habit(HabitFreqType.MONTH_COUNT, timesPerMonth = 2, createdAt = LocalDate.of(2026, 9, 1)),
            checks,
            today,
        )
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun `no checks at all yields zeros`() {
        val stats = StreakCalculator.compute(habit(HabitFreqType.DAILY), emptyList(), today)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.longestStreak)
        assertEquals(0, stats.completionPercent)
        assertEquals(0, stats.checkedLast7)
        assertEquals(0, stats.checkedLast30)
        assertEquals(0, stats.checkedLast90)
    }
}
