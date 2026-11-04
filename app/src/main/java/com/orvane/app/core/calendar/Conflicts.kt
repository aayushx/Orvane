package com.orvane.app.core.calendar

/**
 * G2: pure overlap logic for scheduling conflict warnings.
 */
object Conflicts {

    /**
     * True when the half-open intervals [aStart, aEnd) and [bStart, bEnd)
     * intersect. Touching endpoints (a.end == b.start) are NOT overlap —
     * back-to-back blocks are fine. Empty intervals overlap nothing.
     */
    fun overlaps(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Boolean =
        maxOf(aStart, bStart) < minOf(aEnd, bEnd)
}
