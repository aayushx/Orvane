package com.tasky.app.core.recurrence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceEngineTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(d: Int, m: Int, y: Int, hour: Int = 9): ZonedDateTime =
        LocalDateTime.of(y, m, d, hour, 0).atZone(zone)

    @Test
    fun `daily recurrence advances one day`() {
        val next = RecurrenceEngine.nextDue("FREQ=DAILY", at(10, 10, 2026), at(10, 10, 2026))
        assertEquals(LocalDate.of(2026, 10, 11), next!!.toLocalDate())
        assertEquals(9, next.hour)
    }

    @Test
    fun `every 3 days`() {
        val next = RecurrenceEngine.nextDue("FREQ=DAILY;INTERVAL=3", at(10, 10, 2026), at(10, 10, 2026))
        assertEquals(LocalDate.of(2026, 10, 13), next!!.toLocalDate())
    }

    @Test
    fun `weekly with weekdays MO WE - from Wednesday`() {
        // Wed Oct 14 2026 → next MO WE is Fri? no. Wednesday+1 = Thu, Fri, Sat, Sun, Mon(19) → MO
        val next = RecurrenceEngine.nextDue(
            "FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE",
            at(14, 10, 2026),
            at(14, 10, 2026),
        )
        assertEquals(LocalDate.of(2026, 10, 19), next!!.toLocalDate())
        assertEquals(DayOfWeek.MONDAY, next.dayOfWeek)
    }

    @Test
    fun `daily recurring acceptance - reappears next day after completion`() {
        val due = at(25, 10, 2026, 8)
        val next = RecurrenceEngine.nextDue("FREQ=DAILY;INTERVAL=1", due, LocalDateTime.of(2026, 10, 25, 12, 0).atZone(zone))
        assertEquals(LocalDate.of(2026, 10, 26), next!!.toLocalDate())
        assertEquals(8, next.hour) // keeps the original time
    }

    @Test
    fun `interval 2 weekly from Monday stays on Monday`() {
        val next = RecurrenceEngine.nextDue("FREQ=WEEKLY;INTERVAL=2", at(5, 10, 2026), at(5, 10, 2026))
        assertEquals(LocalDate.of(2026, 10, 19), next!!.toLocalDate())
    }

    @Test
    fun `monthly plain day`() {
        val next = RecurrenceEngine.nextDue("FREQ=MONTHLY", at(31, 1, 2026), at(31, 1, 2026))
        // Jan 31 + 1 month in a clamped way -> next month that has 31 after Jan 31 = March
        assertEquals(LocalDate.of(2026, 3, 31), next!!.toLocalDate())
    }

    @Test
    fun `monthly on day 15`() {
        val next = RecurrenceEngine.nextDue("FREQ=MONTHLY;BYMONTHDAY=15", at(15, 10, 2026), at(15, 10, 2026))
        assertEquals(LocalDate.of(2026, 11, 15), next!!.toLocalDate())
    }

    @Test
    fun `monthly 2nd Friday`() {
        val next = RecurrenceEngine.nextDue("FREQ=MONTHLY;BYDAY=2FR", at(9, 10, 2026), at(9, 10, 2026))
        // Oct 9 is the 2nd Friday of October 2026 → next: November 2nd Friday = Nov 13
        assertEquals(LocalDate.of(2026, 11, 13), next!!.toLocalDate())
    }

    @Test
    fun `yearly keeps month-day`() {
        val next = RecurrenceEngine.nextDue("FREQ=YEARLY", at(1, 3, 2026), at(1, 3, 2026))
        assertEquals(LocalDate.of(2027, 3, 1), next!!.toLocalDate())
    }

    @Test
    fun `completion based 3 days after completion`() {
        val next = RecurrenceEngine.nextDue(
            "COMPLETION_AFTER_DAYS=3",
            at(1, 10, 2026),
            LocalDateTime.of(2026, 10, 25, 18, 0).atZone(zone),
        )
        assertEquals(LocalDate.of(2026, 10, 28), next!!.toLocalDate())
        assertEquals(9, next.hour) // default keepTime when due null
    }

    @Test
    fun `completion based 2 weeks`() {
        val next = RecurrenceEngine.nextDue(
            "COMPLETION_AFTER_WEEKS=2",
            at(1, 10, 2026),
            LocalDateTime.of(2026, 10, 25, 18, 0).atZone(zone),
        )
        assertEquals(LocalDate.of(2026, 11, 8), next!!.toLocalDate())
    }

    @Test
    fun `until caps occurrences`() {
        val rule = "FREQ=DAILY;UNTIL=1760256000000" // 2026-10-12 (UTC) roughly
        val next = RecurrenceEngine.nextDue(rule, at(11, 10, 2026), at(11, 10, 2026))
        assertNull(next)
    }

    @Test
    fun `occurrences generator`() {
        val after = at(25, 10, 2026)
        val list = RecurrenceEngine.occurrences("FREQ=WEEKLY", after, limit = 4)
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 8),
                LocalDate.of(2026, 11, 15),
                LocalDate.of(2026, 11, 22),
            ),
            list.map { it.toLocalDate() },
        )
    }

    @Test
    fun `describe strings`() {
        assertEquals("Every day", RecurrenceEngine.describe("FREQ=DAILY"))
        assertEquals("Weekly on Mon, Wed", RecurrenceEngine.describe("FREQ=WEEKLY;BYDAY=MO,WE"))
        assertEquals("Monthly on day 15", RecurrenceEngine.describe("FREQ=MONTHLY;BYMONTHDAY=15"))
        assertEquals("Yearly", RecurrenceEngine.describe("FREQ=YEARLY"))
        assertEquals("3 days after completion", RecurrenceEngine.describe("COMPLETION_AFTER_DAYS=3"))
        assertEquals("Never", RecurrenceEngine.describe(null))
    }

    @Test
    fun `invalid rules return null`() {
        assertNull(RecurrenceEngine.parse(null))
        assertNull(RecurrenceEngine.parse(""))
        assertNull(RecurrenceEngine.parse("FREQ=WHAT"))
        assertNull(RecurrenceEngine.nextDue("garbage", null, at(1, 1, 2026)))
    }
}
