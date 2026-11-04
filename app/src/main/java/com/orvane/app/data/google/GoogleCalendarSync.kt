package com.orvane.app.data.google

import com.orvane.app.core.google.AccessTokenProvider
import com.orvane.app.core.google.GoogleCalendarModels
import com.orvane.app.data.db.CalendarEventDao
import com.orvane.app.data.db.CalendarEventEntity
import com.orvane.app.data.db.EventSource
import com.orvane.app.data.db.MetaDao
import com.orvane.app.data.db.MetaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G4: two-way Google Calendar sync for a dedicated "Orvane" calendar.
 *
 * Pull uses incremental sync tokens stored in kv_meta; a 410 (token expired)
 * falls back to a bounded full pull. Remote cancels delete the local row.
 */
@Singleton
class GoogleCalendarSync @Inject constructor(
    private val api: GoogleCalendarApi,
    private val tokenProvider: AccessTokenProvider,
    private val eventDao: CalendarEventDao,
    private val metaDao: MetaDao,
) {

    companion object {
        const val CALENDAR_NAME = "Orvane"
        private const val KEY_SYNC_TOKEN = "gcal_sync_token"
        private const val KEY_CALENDAR_ID = "gcal_calendar_id"

        /** Full pull window when no (or an expired) sync token exists. */
        const val FULL_PULL_DAYS = 90L
    }

    /** Pulls remote changes into the local DB. Returns the number of applied events. */
    suspend fun pull(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val calendarId = ensureCalendarId()
            val savedToken = metaDao.get(KEY_SYNC_TOKEN)?.value
            var page = try {
                api.listEvents(calendarId, syncToken = savedToken).getOrThrow()
            } catch (e: IOException) {
                // Expired/invalid sync token → bounded full pull.
                if (savedToken != null && e.message?.contains("410") == true) {
                    metaDao.delete(KEY_SYNC_TOKEN)
                    fullPull(calendarId)
                } else {
                    throw e
                }
            }
            var applied = apply(page)
            while (page.nextPageToken != null) {
                page = api.listEvents(calendarId, pageToken = page.nextPageToken).getOrThrow()
                applied += apply(page)
            }
            page.nextSyncToken?.let { metaDao.put(MetaEntity(KEY_SYNC_TOKEN, it)) }
            applied
        }
    }

    /**
     * Pushes local events to the "Orvane" calendar. Events without a Google id
     * are created; ones with one are patched (idempotent, keeps remote in
     * step with local edits). Returns the number of pushed events.
     */
    suspend fun push(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val calendarId = ensureCalendarId()
            var pushed = 0
            for (e in eventDao.locals()) {
                val remote = GoogleEventMapper.from(e)
                val gid = e.googleEventId
                if (gid == null) {
                    val created = api.insertEvent(calendarId, remote).getOrThrow()
                    val newId = created.id ?: continue
                    eventDao.upsert(
                        e.copy(googleEventId = newId, lastSyncedAt = System.currentTimeMillis()),
                    )
                } else {
                    api.patchEvent(calendarId, gid, remote).getOrThrow()
                    eventDao.upsert(e.copy(lastSyncedAt = System.currentTimeMillis()))
                }
                pushed++
            }
            pushed
        }
    }

    /** Pull then push; the status line reports both counts. */
    suspend fun syncNow(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val pulled = pull().getOrThrow()
            val pushed = push().getOrThrow()
            pulled to pushed
        }
    }

    private suspend fun fullPull(calendarId: String): GoogleCalendarModels.EventsPage {
        // Bounded window: events starting within the last FULL_PULL_DAYS.
        val timeMinMillis = System.currentTimeMillis() - FULL_PULL_DAYS * 24 * 3600_000L
        val timeMin = java.time.Instant.ofEpochMilli(timeMinMillis).toString()
        return api.listEvents(calendarId, timeMin = timeMin).getOrThrow()
    }

    private suspend fun apply(page: GoogleCalendarModels.EventsPage): Int {
        var applied = 0
        val now = System.currentTimeMillis()
        for (g in page.items) {
            val gid = g.id ?: continue
            val existing = eventDao.byGoogleEventId(gid)
            if (g.status == "cancelled") {
                if (existing != null) {
                    eventDao.delete(existing.id)
                    applied++
                }
                continue
            }
            if (g.recurrence != null) {
                // Recurring masters are out of scope for v1 sync; skip.
                continue
            }
            // G4c: don't clobber local edits — when the local copy was synced
            // after the remote was last updated, keep the local version (push
            // will send it upstream).
            val remoteUpdated = GoogleCalendarModels.parseMillis(
                GoogleCalendarModels.DateTime(dateTime = g.updated),
            )
            if (existing != null &&
                existing.lastSyncedAt != null &&
                remoteUpdated != null &&
                remoteUpdated <= existing.lastSyncedAt
            ) {
                continue
            }
            val startMillis = GoogleCalendarModels.parseMillis(g.start) ?: continue
            val endMillis = GoogleCalendarModels.parseMillis(g.end) ?: (startMillis + 3600_000L)
            val upserted = CalendarEventEntity(
                id = existing?.id ?: "g_$gid",
                title = g.summary ?: "(untitled)",
                note = g.description ?: "",
                location = g.location,
                start = startMillis,
                end = endMillis,
                allDay = GoogleCalendarModels.isAllDay(g.start),
                source = EventSource.GOOGLE,
                googleEventId = gid,
                lastSyncedAt = now,
            )
            eventDao.upsert(upserted)
            applied++
        }
        return applied
    }

    private suspend fun ensureCalendarId(): String {
        val saved = metaDao.get(KEY_CALENDAR_ID)?.value
        if (!saved.isNullOrBlank()) return saved
        val id = api.ensureCalendar(CALENDAR_NAME).getOrThrow()
        metaDao.put(MetaEntity(KEY_CALENDAR_ID, id))
        return id
    }
}
