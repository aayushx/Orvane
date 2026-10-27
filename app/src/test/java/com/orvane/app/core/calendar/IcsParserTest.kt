package com.orvane.app.core.calendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsParserTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun `parses simple timed event`() {
        val ics = """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Team sync
            DTSTART:20261025T100000Z
            DTEND:20261025T110000Z
            LOCATION:Zoom
            DESCRIPTION:Weekly\, all hands
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()
        val events = IcsParser.parse(ics, zone)
        assertEquals(1, events.size)
        val e = events[0]
        assertEquals("Team sync", e.title)
        assertEquals("Weekly, all hands", e.note)
        assertEquals("Zoom", e.location)
        assertEquals(false, e.allDay)
        assertEquals(10, e.start.hour)
        assertEquals(11, e.end.hour)
    }

    @Test
    fun `parses all day date-only event`() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:Holiday
            DTSTART;VALUE=DATE:20261225
            END:VEVENT
        """.trimIndent()
        val events = IcsParser.parse(ics, zone)
        val e = events.single()
        assertEquals("Holiday", e.title)
        assertTrue(e.allDay)
        assertEquals(LocalDate.of(2026, 12, 25), e.start.toLocalDate())
        assertEquals(LocalDate.of(2026, 12, 26), e.end.toLocalDate())
    }

    @Test
    fun `unfolds folded summary lines`() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:Long title that
              continues here
            DTSTART:20261025T090000
            END:VEVENT
        """.trimIndent()
        val e = IcsParser.parse(ics, zone).single()
        assertEquals("Long title that continues here", e.title)
    }

    @Test
    fun `floating datetime interpreted in local zone`() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:Floating
            DTSTART:20261025T090000
            DTEND:20261025T100000
            END:VEVENT
        """.trimIndent()
        val e = IcsParser.parse(ics, zone).single()
        assertEquals(LocalDateTime.of(2026, 10, 25, 9, 0), e.start.toLocalDateTime())
        assertEquals(zone, e.start.zone)
    }

    @Test
    fun `default end is one hour when DTEND missing`() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:Quick note
            DTSTART:20261025T140000
            END:VEVENT
        """.trimIndent()
        val e = IcsParser.parse(ics, zone).single()
        assertEquals(e.start.plusHours(1), e.end)
    }

    @Test
    fun `bad dtstart skips event`() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:Broken
            DTSTART:not-a-date
            END:VEVENT
        """.trimIndent()
        assertEquals(0, IcsParser.parse(ics, zone).size)
    }

    @Test
    fun `parses multiple events`() {
        val ics = """
            BEGIN:VEVENT
            SUMMARY:First
            DTSTART:20261025T090000
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Second
            DTSTART:20261026T090000
            END:VEVENT
        """.trimIndent()
        val events = IcsParser.parse(ics, zone)
        assertEquals(2, events.size)
        assertEquals("First", events[0].title)
        assertEquals("Second", events[1].title)
    }
}
