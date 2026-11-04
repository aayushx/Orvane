package com.orvane.app.core.google

/**
 * G4: minimal DTOs for the Google Calendar REST API (v3) and pure parsing
 * helpers. Kept dependency-free so parsing is unit-testable.
 */
object GoogleCalendarModels {

    const val API_BASE = "https://www.googleapis.com/calendar/v3"

    @kotlinx.serialization.Serializable
    data class DateTime(val dateTime: String? = null, val date: String? = null)

    @kotlinx.serialization.Serializable
    data class Event(
        val id: String? = null,
        val summary: String? = null,
        val description: String? = null,
        val location: String? = null,
        val start: DateTime? = null,
        val end: DateTime? = null,
        val status: String? = null,
        val recurrence: List<String>? = null,
    )

    @kotlinx.serialization.Serializable
    data class EventsPage(
        val items: List<Event> = emptyList(),
        val nextPageToken: String? = null,
        val nextSyncToken: String? = null,
    )

    /** RFC3339 dateTime/date → epoch millis; null when missing or unparseable. */
    fun parseMillis(dt: DateTime?): Long? {
        val raw = dt?.dateTime ?: dt?.date ?: return null
        return try {
            java.time.Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            try {
                // All-day "date" fields lack a zone; interpret in UTC.
                java.time.LocalDate.parse(raw)
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    /** True when the event carries a date-only start (all-day event). */
    fun isAllDay(dt: DateTime?): Boolean = dt?.dateTime == null && dt?.date != null
}
