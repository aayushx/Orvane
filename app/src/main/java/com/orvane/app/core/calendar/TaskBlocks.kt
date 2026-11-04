package com.orvane.app.core.calendar

/**
 * G1: pure helpers for task time blocks on the calendar. A scheduled task with
 * a duration spans start → start + duration; without one it keeps the
 * historical 1-hour block.
 */
object TaskBlocks {

    /** Block length for tasks without a duration (unchanged v1.2 behavior). */
    const val DEFAULT_BLOCK_MINUTES = 60

    /** End of a task block; duration is clamped to at least 1 minute. */
    fun endMillis(startMillis: Long, durationMinutes: Int?): Long =
        startMillis + blockMillis(durationMinutes)

    fun blockMillis(durationMinutes: Int?): Long =
        (durationMinutes?.coerceAtLeast(1) ?: DEFAULT_BLOCK_MINUTES) * 60_000L

    /** Compact duration label ("45m", "1h", "1h 30m"), or null when no duration. */
    fun label(durationMinutes: Int?): String? {
        if (durationMinutes == null) return null
        val m = durationMinutes.coerceAtLeast(1)
        val h = m / 60
        val rem = m % 60
        return when {
            h > 0 && rem > 0 -> "${h}h ${rem}m"
            h > 0 -> "${h}h"
            else -> "${rem}m"
        }
    }
}
