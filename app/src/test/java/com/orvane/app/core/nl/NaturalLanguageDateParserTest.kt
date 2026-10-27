package com.orvane.app.core.nl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NaturalLanguageDateParserTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    // Fixed reference: Thursday 2026-10-22 10:30
    private val now: ZonedDateTime = LocalDateTime.of(2026, 10, 22, 10, 30).atZone(zone)

    private fun parse(raw: String) = NaturalLanguageDateParser.parse(raw, now)

    @Test
    fun `acceptance scenario - submit report tomorrow 5pm !High #work`() {
        val p = parse("Submit report tomorrow 5pm !High #work")
        assertEquals("Submit report", p.title)
        assertEquals(listOf("work"), p.tags)
        assertEquals(NaturalLanguageDateParser.PRIORITY_HIGH, p.priority)
        assertFalse(p.isAllDay)
        val due = p.due!!
        assertEquals(LocalDate.of(2026, 10, 23), due.toLocalDate())
        assertEquals(17, due.hour)
        assertEquals(0, due.minute)
    }

    @Test
    fun `today and tomorrow basic dates`() {
        assertEquals(LocalDate.of(2026, 10, 22), parse("call mom today").due!!.toLocalDate())
        assertTrue(parse("call mom today").isAllDay)
        assertEquals(LocalDate.of(2026, 10, 23), parse("gym tomorrow").due!!.toLocalDate())
    }

    @Test
    fun `tonight maps to today 8pm`() {
        val p = parse("watch movie tonight")
        assertEquals(LocalDate.of(2026, 10, 22), p.due!!.toLocalDate())
        assertEquals(20, p.due!!.hour)
        assertFalse(p.isAllDay)
    }

    @Test
    fun `weekday plain maps within 7 days`() {
        // today is Thursday; "friday" -> tomorrow
        assertEquals(LocalDate.of(2026, 10, 23), parse("party friday").due!!.toLocalDate())
        // "monday" -> next monday
        assertEquals(LocalDate.of(2026, 10, 26), parse("standup monday").due!!.toLocalDate())
    }

    @Test
    fun `next monday skips a week ahead`() {
        assertEquals(LocalDate.of(2026, 10, 26), parse("meeting next monday").due!!.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 30), parse("trip next friday").due!!.toLocalDate())
    }

    @Test
    fun `in 3 days and in 2 weeks`() {
        assertEquals(LocalDate.of(2026, 10, 25), parse("review in 3 days").due!!.toLocalDate())
        assertTrue(parse("review in 3 days").isAllDay)
        assertEquals(LocalDate.of(2026, 11, 5), parse("sprint in 2 weeks").due!!.toLocalDate())
    }

    @Test
    fun `in 4 hours is timed`() {
        val p = parse("check oven in 4 hours")
        assertEquals(14, p.due!!.toLocalDateTime().hour)
        assertFalse(p.isAllDay)
    }

    @Test
    fun `explicit date formats`() {
        assertEquals(LocalDate.of(2026, 11, 1), parse("taxes 2026-11-01").due!!.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 3), parse("zoom 11/3").due!!.toLocalDate())
        assertEquals(LocalDate.of(2027, 8, 15), parse("zoom 15/8").due!!.toLocalDate())
        assertEquals(LocalDate.of(2027, 8, 15), parse("birth aug 15").due!!.toLocalDate())
        assertEquals(LocalDate.of(2027, 8, 15), parse("birth 15 aug 2027").due!!.toLocalDate())
    }

    @Test
    fun `time formats`() {
        assertEquals(17, parse("gym 5pm").due!!.hour)
        assertEquals(30, parse("gym 5:30pm").due!!.minute)
        assertEquals(5, parse("gym 05:30").due!!.hour)
        assertEquals(0, parse("dinner 12am").due!!.hour)
        assertEquals(12, parse("lunch noon").due!!.hour)
        assertEquals(9, parse("call 9:30am").due!!.hour)
        assertEquals(17, parse("call at 5pm").due!!.hour)
        assertEquals(LocalDate.of(2026, 10, 22), parse("quick 5pm").due!!.toLocalDate())
    }

    @Test
    fun `priorities`() {
        assertEquals(3, parse("task !High").priority)
        assertEquals(3, parse("task !!!").priority)
        assertEquals(2, parse("task !!").priority)
        assertEquals(2, parse("task !medium").priority)
        assertEquals(1, parse("task !1").priority)
        assertEquals(1, parse("task !").priority)
        assertEquals(0, parse("task none").priority)
    }

    @Test
    fun `multiple tags and title cleanup`() {
        val p = parse("Plan #work #urgent-retro tomorrow 8:30am !medium")
        assertEquals("Plan", p.title)
        assertEquals(listOf("work", "urgent-retro"), p.tags)
        assertEquals(2, p.priority)
        assertEquals(LocalDate.of(2026, 10, 23), p.due!!.toLocalDate())
        assertEquals(8, p.due!!.hour)
        assertEquals(30, p.due!!.minute)
    }

    @Test
    fun `no matches leaves title intact`() {
        val p = parse("just some text")
        assertEquals("just some text", p.title)
        assertTrue(p.due == null)
        assertTrue(p.tags.isEmpty())
        assertEquals(0, p.priority)
    }

    @Test
    fun `token order independence`() {
        val a = parse("5pm tomorrow title")
        val b = parse("title tomorrow 5pm")
        assertEquals(b.due!!.toLocalDateTime(), a.due!!.toLocalDateTime())
        assertTrue(a.title.contains("title"))
    }
}
