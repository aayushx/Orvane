package com.orvane.app.data.google

import com.orvane.app.core.google.GoogleCalendarModels
import com.orvane.app.data.db.CalendarEventEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * G4c: maps local calendar events to Google Calendar REST payloads (push
 * direction). Pure and unit-testable; all times are RFC3339 in the device
 * zone (all-day events use date-only values).
 */
object GoogleEventMapper {

    fun from(e: CalendarEventEntity, zone: ZoneId = ZoneId.systemDefault()): GoogleCalendarModels.Event =
        GoogleCalendarModels.Event(
            summary = e.title,
            description = e.note.ifBlank { null },
            location = e.location,
            start = toDateTime(e.start, e.allDay, zone),
            end = toDateTime(e.end, e.allDay, zone),
        )

    private fun toDateTime(millis: Long, allDay: Boolean, zone: ZoneId): GoogleCalendarModels.DateTime =
        if (allDay) {
            GoogleCalendarModels.DateTime(
                date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().toString(),
            )
        } else {
            GoogleCalendarModels.DateTime(
                dateTime = Instant.ofEpochMilli(millis).atZone(zone)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )
        }

    /** RFC3339 for a date-only remote event (kept in UTC, matching Google's convention). */
    fun allDayDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** Local date of an all-day event millis (device zone). */
    fun localDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
