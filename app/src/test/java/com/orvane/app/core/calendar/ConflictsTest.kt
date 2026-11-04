package com.orvane.app.core.calendar

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G2: half-open interval overlap — the boundary cases matter (touching
 * endpoints are not a conflict).
 */
class ConflictsTest {

    private val h = 60 * 60_000L

    @Test
    fun touchingEndpoints_isNotOverlap() {
        // 14:00–15:00 vs 15:00–16:00: back-to-back is fine.
        assertFalse(Conflicts.overlaps(14 * h, 15 * h, 15 * h, 16 * h))
        assertFalse(Conflicts.overlaps(15 * h, 16 * h, 14 * h, 15 * h))
    }

    @Test
    fun identicalRanges_overlap() {
        assertTrue(Conflicts.overlaps(14 * h, 15 * h, 14 * h, 15 * h))
    }

    @Test
    fun newBlockContainedInExisting_overlaps() {
        assertTrue(Conflicts.overlaps(14 * h, 16 * h, 14 * h + 30 * 60_000L, 15 * h))
    }

    @Test
    fun existingContainedInNewBlock_overlaps() {
        assertTrue(Conflicts.overlaps(13 * h, 17 * h, 14 * h, 15 * h))
    }

    @Test
    fun partialOverlap_eitherSide() {
        assertTrue(Conflicts.overlaps(14 * h, 15 * h, 14 * h + 30 * 60_000L, 16 * h))
        assertTrue(Conflicts.overlaps(14 * h, 15 * h, 13 * h, 14 * h + 30 * 60_000L))
    }

    @Test
    fun disjointRanges_doNotOverlap() {
        assertFalse(Conflicts.overlaps(14 * h, 15 * h, 16 * h, 17 * h))
        assertFalse(Conflicts.overlaps(16 * h, 17 * h, 14 * h, 15 * h))
    }

    @Test
    fun emptyInterval_neverOverlaps() {
        // Degenerate zero-length range touches but doesn't overlap.
        assertFalse(Conflicts.overlaps(14 * h, 14 * h, 13 * h, 15 * h))
    }
}
