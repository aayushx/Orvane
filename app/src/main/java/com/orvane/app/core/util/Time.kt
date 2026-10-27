package com.orvane.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Date/time helpers shared across the app. All persistence uses epoch millis UTC;
 * user-facing computations use device-local time.
 */
object Time {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun tomorrowEpochDay(): Long = LocalDate.now().plusDays(1).toEpochDay()

    fun startOfDayEpoch(day: LocalDate = LocalDate.now()): Long =
        day.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun startOfTomorrow(): Long = startOfDayEpoch(LocalDate.now().plusDays(1))

    fun startOfToday(): Long = startOfDayEpoch(LocalDate.now())

    fun endOfToday(): Long = startOfDayEpoch(LocalDate.now().plusDays(1))

    fun at(day: LocalDate, hour: Int, minute: Int): Long =
        day.atTime(LocalTime.of(hour, minute)).atZone(zone()).toInstant().toEpochMilli()

    fun atTime(day: Long, hour: Int, minute: Int): Long =
        Instant.ofEpochMilli(day).atZone(zone()).toLocalDate().let { at(it, hour, minute) }

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDate()

    fun toLocalTime(epochMillis: Long): LocalTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalTime()

    fun dayRange(epochDay: Long): Pair<Long, Long> {
        val d = LocalDate.ofEpochDay(epochDay)
        return startOfDayEpoch(d) to startOfDayEpoch(d.plusDays(1))
    }

    fun epochDayOf(millis: Long): Long = toLocalDate(millis).toEpochDay()

    fun zdt(epochMillis: Long): ZonedDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone())
}
