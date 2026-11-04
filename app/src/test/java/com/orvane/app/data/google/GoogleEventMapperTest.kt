package com.orvane.app.data.google

import com.orvane.app.data.db.CalendarEventEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * G4c: local event → Google REST payload mapping.
 */
class GoogleEventMapperTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun event(allDay: Boolean = false) = CalendarEventEntity(
        id = "local-1",
        title = "Pay rent",
        note = "via bank",
        location = "Home",
        start = 1_699_999_200_000, // 2023-11-14T22:00Z → 2023-11-15 03:30 IST
        end = 1_700_002_800_000,
        allDay = allDay,
    )

    @Test
    fun timedEvent_mapsToRfc3339InDeviceZone() {
        val g = GoogleEventMapper.from(event(), zone)
        assertEquals("Pay rent", g.summary)
        assertEquals("via bank", g.description)
        assertEquals("Home", g.location)
        assertEquals("2023-11-15T03:30:00+05:30", g.start?.dateTime)
        assertEquals("2023-11-15T04:30:00+05:30", g.end?.dateTime)
        assertNull(g.start?.date)
    }

    @Test
    fun allDayEvent_mapsToDateOnly() {
        val g = GoogleEventMapper.from(event(allDay = true), zone)
        assertNull(g.start?.dateTime)
        assertEquals("2023-11-14", g.start?.date)
        assertEquals("2023-11-14", g.end?.date)
    }

    @Test
    fun blankNote_omitted() {
        val g = GoogleEventMapper.from(event().copy(note = ""), zone)
        assertNull(g.description)
    }

    @Test
    fun payload_serializesWithoutNulls() {
        val json = Json { encodeDefaults = false }
        val body = json.encodeToString(GoogleEventMapper.from(event(), zone))
        assertTrue(body.contains("\"summary\":\"Pay rent\""))
        assertTrue(body.contains("\"dateTime\":\"2023-11-15T03:30:00+05:30\""))
        assertTrue(!body.contains("null"))
    }
}
