package com.orvane.app.core.util

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** Round 2 / B1: next-alarm computation for custom nag cutoff times. */
class AlarmTimesTest {

    private val zone = ZoneId.of("UTC") // fixed offset, deterministic
    private fun now(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, 28, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun expected(day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun occurrenceLaterTodayIsKept() {
        // Cutoff 23:47, now 14:00 → fires today at 23:47 (a fully custom HH:MM).
        assertEquals(expected(28, 23, 47), AlarmTimes.nextOccurrence(23, 47, now(14), zone))
    }

    @Test
    fun occurrenceEarlierTodayRollsToTomorrow() {
        // Cutoff 21:00, now 22:30 → fires tomorrow at 21:00.
        assertEquals(expected(29, 21, 0), AlarmTimes.nextOccurrence(21, 0, now(22, 30), zone))
    }

    @Test
    fun occurrenceExactlyNowIsNotSkipped() {
        // Now 14:30 == cutoff 14:30 → still fires now (alarm fires; receiver will reschedule).
        assertEquals(expected(28, 14, 30), AlarmTimes.nextOccurrence(14, 30, now(14, 30), zone))
    }

    @Test
    fun customMinutesPreserved() {
        // Cutoff 05:19 with non-zero minutes keeps 19 minutes.
        assertEquals(expected(28, 5, 19), AlarmTimes.nextOccurrence(5, 19, now(2), zone))
    }
}
