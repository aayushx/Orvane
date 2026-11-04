package com.orvane.app.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * G1: pure block helpers — end-time computation, default block length,
 * and compact duration labels.
 */
class TaskBlocksTest {

    private val start = 1_700_000_000_000L

    @Test
    fun endMillis_nullDuration_usesDefaultOneHour() {
        assertEquals(start + 60 * 60_000L, TaskBlocks.endMillis(start, null))
    }

    @Test
    fun endMillis_explicitDuration_spansExactly() {
        assertEquals(start + 90 * 60_000L, TaskBlocks.endMillis(start, 90))
        assertEquals(start + 15 * 60_000L, TaskBlocks.endMillis(start, 15))
    }

    @Test
    fun endMillis_zeroOrNegative_clampedToOneMinute() {
        assertEquals(start + 60_000L, TaskBlocks.endMillis(start, 0))
        assertEquals(start + 60_000L, TaskBlocks.endMillis(start, -30))
    }

    @Test
    fun blockMillis_noDuration_isDefaultBlock() {
        assertEquals(TaskBlocks.DEFAULT_BLOCK_MINUTES * 60_000L, TaskBlocks.blockMillis(null))
    }

    @Test
    fun label_null_isNull() {
        assertNull(TaskBlocks.label(null))
    }

    @Test
    fun label_minutesOnly() {
        assertEquals("45m", TaskBlocks.label(45))
        assertEquals("15m", TaskBlocks.label(15))
    }

    @Test
    fun label_exactHours() {
        assertEquals("1h", TaskBlocks.label(60))
        assertEquals("2h", TaskBlocks.label(120))
    }

    @Test
    fun label_mixedHoursAndMinutes() {
        assertEquals("1h 30m", TaskBlocks.label(90))
        assertEquals("2h 5m", TaskBlocks.label(125))
    }

    @Test
    fun label_zeroOrNegative_clampedToOneMinute() {
        assertEquals("1m", TaskBlocks.label(0))
        assertEquals("1m", TaskBlocks.label(-10))
    }
}
