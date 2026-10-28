package com.orvane.app.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** C1: the greeting period calculation is genuinely new logic — pin the buckets. */
class GreetingTest {

    @Test
    fun hourBucketsMapToPeriods() {
        assertEquals(GreetingPeriod.NIGHT, greetingPeriod(0))
        assertEquals(GreetingPeriod.NIGHT, greetingPeriod(4))
        assertEquals(GreetingPeriod.MORNING, greetingPeriod(5))
        assertEquals(GreetingPeriod.MORNING, greetingPeriod(11))
        assertEquals(GreetingPeriod.AFTERNOON, greetingPeriod(12))
        assertEquals(GreetingPeriod.AFTERNOON, greetingPeriod(16))
        assertEquals(GreetingPeriod.EVENING, greetingPeriod(17))
        assertEquals(GreetingPeriod.EVENING, greetingPeriod(21))
        assertEquals(GreetingPeriod.NIGHT, greetingPeriod(22))
        assertEquals(GreetingPeriod.NIGHT, greetingPeriod(23))
    }

    @Test
    fun nameAppendedOnlyWhenPresent() {
        assertEquals("Good morning", greetingText(GreetingPeriod.MORNING, null))
        assertEquals("Good morning", greetingText(GreetingPeriod.MORNING, "  "))
        assertEquals("Good morning, Ada", greetingText(GreetingPeriod.MORNING, "Ada"))
        assertEquals("Working late, Ada", greetingText(GreetingPeriod.NIGHT, "Ada"))
    }

    @Test
    fun dayLabelRendersHumanForm() {
        assertEquals("Tuesday, October 29", dayLabel(LocalDate.of(2024, 10, 29)))
    }
}
