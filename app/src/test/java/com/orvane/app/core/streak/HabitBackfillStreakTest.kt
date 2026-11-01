package com.orvane.app.core.streak

import com.orvane.app.data.db.CheckStatus
import com.orvane.app.data.db.HabitCheckEntity
import com.orvane.app.data.db.HabitEntity
import com.orvane.app.data.db.HabitFreqType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * v1.2.0 / E8: backfill proves recording a PAST day through the weekly bars
 * recomputes streaks correctly through the existing StreakCalculator logic
 * (no schema change — checks are already stored per date).
 */
class HabitBackfillStreakTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 31)

    private fun habit(createdAt: LocalDate) = HabitEntity(
        id = "h1",
        name = "Backfill test",
        freqType = HabitFreqType.DAILY,
        createdAt = createdAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    private fun check(epoch: Long) =
        HabitCheckEntity(habitId = "h1", epochDay = epoch, status = CheckStatus.CHECKED, createdAt = 0)

    private fun d(day: LocalDate): Long = day.toEpochDay()

    @Test
    fun `gap at yesterday keeps streak at one`() {
        val h = habit(today.minusDays(30))
        val stats = StreakCalculator.compute(h, listOf(check(d(today))), today)
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.checkedLast7)
    }

    @Test
    fun `backfilling yesterday extends the current streak`() {
        val h = habit(today.minusDays(30))
        val before = StreakCalculator.compute(h, listOf(check(d(today))), today)
        // Backfill: the user taps yesterday's bar in the weekly chart...
        val after = StreakCalculator.compute(
            h,
            listOf(check(d(today)), check(d(today.minusDays(1)))),
            today,
        )
        assertEquals(before.currentStreak + 1, after.currentStreak)
        assertEquals(before.checkedLast7 + 1, after.checkedLast7)
        assertEquals(before.totalChecked + 1, after.totalChecked)
    }

    @Test
    fun `backfilling a full past week extends the streak further`() {
        val h = habit(today.minusDays(30))
        var checks = listOf(check(d(today)))
        var stats = StreakCalculator.compute(h, checks, today)
        assertEquals(1, stats.currentStreak)
        // Backfill 6 consecutive days (yesterday .. six days back).
        for (back in 1..6) {
            checks = checks + check(d(today.minusDays(back.toLong())))
            stats = StreakCalculator.compute(h, checks, today)
        }
        assertEquals(7, stats.currentStreak)
    }

    @Test
    fun `backfilled skip is streak-neutral`() {
        val h = habit(today.minusDays(30))
        val withSkip = listOf(
            check(d(today)),
            check(d(today.minusDays(1))),
            HabitCheckEntity(
                habitId = "h1",
                epochDay = d(today.minusDays(2)),
                status = CheckStatus.SKIPPED,
                createdAt = 0,
            ),
        )
        // SKIP neither extends nor breaks: the run through it stays at the
        // two checked days (d-1 and today), and the skip is counted
        // separately.
        val stats = StreakCalculator.compute(h, withSkip, today)
        assertEquals(2, stats.currentStreak)
        assertEquals(1, stats.totalSkipped)
    }
}
