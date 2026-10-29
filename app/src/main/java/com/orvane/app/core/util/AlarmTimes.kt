package com.orvane.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Alarm-time math shared by the schedulers and unit tests. */
object AlarmTimes {

    /**
     * Next occurrence of a wall-clock HH:MM: now if it is exactly now or still
     * ahead today, otherwise tomorrow at the same time. The anchor date comes
     * from [nowMillis] (not the wall clock) so the function is pure —
     * deterministic for tests and schedulers alike.
     */
    fun nextOccurrence(hour: Int, minute: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        var t = today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        if (t < nowMillis) {
            t += 24L * 60 * 60 * 1000
        }
        return t
    }

    /** Midnight of the given day in [zone], for due-day buckets. */
    fun startOfDayEpoch(date: LocalDate, zone: ZoneId): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** epoch millis of now's day-start. */
    fun startOfToday(zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDayEpoch(LocalDate.now(zone), zone)

    /** Convenience: epoch millis for a wall-clock instant. */
    fun at(startOfDayEpochMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long =
        Instant.ofEpochMilli(startOfDayEpochMillis).atZone(zone).toLocalDate()
            .atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}
