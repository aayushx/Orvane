package com.orvane.app.core.google

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G4: pure parsing of the Google Calendar REST v3 event payloads.
 */
class GoogleCalendarModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun eventsPage_parsesItemsAndSyncToken() {
        val body = """
            {
              "kind": "calendar#events",
              "nextSyncToken": "CJkS0a2vCBEQ…",
              "items": [
                {
                  "id": "evt1",
                  "status": "confirmed",
                  "summary": "Standup",
                  "start": {"dateTime": "2026-11-04T10:00:00Z"},
                  "end": {"dateTime": "2026-11-04T10:30:00Z"}
                },
                {
                  "id": "evt2",
                  "status": "cancelled",
                  "summary": "Deleted thing",
                  "start": {"dateTime": "2026-11-05T09:00:00Z"},
                  "end": {"dateTime": "2026-11-05T10:00:00Z"}
                }
              ]
            }
        """.trimIndent()
        val page = json.decodeFromString<GoogleCalendarModels.EventsPage>(body)
        assertEquals(2, page.items.size)
        assertEquals("evt1", page.items[0].id)
        assertEquals("Standup", page.items[0].summary)
        assertEquals("cancelled", page.items[1].status)
        assertEquals("CJkS0a2vCBEQ…", page.nextSyncToken)
    }

    @Test
    fun parseMillis_rfc3339dateTime() {
        val dt = GoogleCalendarModels.DateTime(dateTime = "2026-11-04T10:00:00Z")
        assertEquals(
            java.time.ZonedDateTime.of(2026, 11, 4, 10, 0, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            GoogleCalendarModels.parseMillis(dt),
        )
    }

    @Test
    fun parseMillis_dateOnly_isStartOfDayUtc() {
        val dt = GoogleCalendarModels.DateTime(date = "2026-11-04")
        assertEquals(
            java.time.ZonedDateTime.of(2026, 11, 4, 0, 0, 0, 0, java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            GoogleCalendarModels.parseMillis(dt),
        )
    }

    @Test
    fun parseMillis_nullOrGarbage_isNull() {
        assertNull(GoogleCalendarModels.parseMillis(null))
        assertNull(GoogleCalendarModels.parseMillis(GoogleCalendarModels.DateTime()))
        assertNull(GoogleCalendarModels.parseMillis(GoogleCalendarModels.DateTime(dateTime = "not-a-date")))
    }

    @Test
    fun isAllDay_onlyWhenDateOnly() {
        assertTrue(GoogleCalendarModels.isAllDay(GoogleCalendarModels.DateTime(date = "2026-11-04")))
        assertFalse(GoogleCalendarModels.isAllDay(GoogleCalendarModels.DateTime(dateTime = "2026-11-04T10:00:00Z")))
        assertFalse(GoogleCalendarModels.isAllDay(null))
    }
}
