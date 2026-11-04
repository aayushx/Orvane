package com.orvane.app.data.google

import android.app.Activity
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.orvane.app.core.google.AccessTokenProvider
import com.orvane.app.core.google.GoogleCalendarModels
import com.orvane.app.data.db.AppDatabase
import com.orvane.app.data.db.CalendarEventEntity
import com.orvane.app.data.db.EventSource
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * G4: the pull engine against a fake API — inserts, edit-aware skip,
 * remote cancels, and the 410 → full-pull fallback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleCalendarSyncTest {

    private lateinit var db: AppDatabase
    private lateinit var sync: GoogleCalendarSync
    private lateinit var fake: FakeApi

    private class FakeApi : GoogleCalendarApi {
        var pages = mutableListOf<GoogleCalendarModels.EventsPage>()
        var syncTokenCalls = 0
        var fullPullCalls = 0
        var failSyncTokenWith410 = false
        var inserted = mutableListOf<GoogleCalendarModels.Event>()
        var patched = mutableListOf<Pair<String, GoogleCalendarModels.Event>>()

        override suspend fun listEvents(
            calendarId: String,
            syncToken: String?,
            timeMin: String?,
            pageToken: String?,
        ): Result<GoogleCalendarModels.EventsPage> {
            if (syncToken != null && failSyncTokenWith410) {
                syncTokenCalls++
                throw IOException("HTTP 410 from GET: Sync token is no longer valid")
            }
            if (syncToken != null) syncTokenCalls++
            if (timeMin != null) fullPullCalls++
            val page = pages.removeFirstOrNull() ?: GoogleCalendarModels.EventsPage()
            return Result.success(page)
        }

        override suspend fun ensureCalendar(summary: String): Result<String> = Result.success("cal_orvane")

        override suspend fun insertEvent(calendarId: String, event: GoogleCalendarModels.Event): Result<GoogleCalendarModels.Event> {
            inserted += event
            return Result.success(event.copy(id = "remote_${inserted.size}"))
        }

        override suspend fun patchEvent(calendarId: String, eventId: String, event: GoogleCalendarModels.Event): Result<GoogleCalendarModels.Event> {
            patched += eventId to event
            return Result.success(event.copy(id = eventId))
        }

        override suspend fun deleteEvent(calendarId: String, eventId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeTokens : AccessTokenProvider {
        override suspend fun accessToken(activity: Activity?): String? = "test-token"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fake = FakeApi()
        sync = GoogleCalendarSync(fake, FakeTokens(), db.calendarEventDao(), db.metaDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun pull_insertsRemoteEvents_andStoresSyncToken() = runTest {
        fake.pages += page(
            event("g1", "Remote standup", start = "2026-11-04T10:00:00Z", end = "2026-11-04T11:00:00Z"),
            event("g2", "Gone", status = "cancelled"),
        )
        val result = sync.pull().getOrThrow()
        assertEquals(1, result)
        val stored = db.calendarEventDao().byGoogleEventId("g1")
        assertNotNull(stored)
        assertEquals(EventSource.GOOGLE, stored!!.source)
        assertEquals("Remote standup", stored.title)
        assertEquals("cal_orvane", db.metaDao().get("gcal_calendar_id")!!.value)
        assertNotNull(db.metaDao().get("gcal_sync_token"))
    }

    @Test
    fun pull_skipsRemote_whenLocalSyncedAfterRemoteUpdate() = runTest {
        // First pull seeds the event.
        fake.pages += page(
            event("g1", "Remote title", start = "2026-11-04T10:00:00Z", end = "2026-11-04T11:00:00Z",
                updated = "2026-11-04T09:00:00Z"),
        )
        sync.pull().getOrThrow()
        val local = db.calendarEventDao().byGoogleEventId("g1")!!
        // User edits locally after the sync.
        db.calendarEventDao().upsert(local.copy(title = "Local edit", lastSyncedAt = local.lastSyncedAt!! + 60_000))
        // Remote still reports the older updated time.
        fake.pages += page(
            event("g1", "Remote title", start = "2026-11-04T10:00:00Z", end = "2026-11-04T11:00:00Z",
                updated = "2026-11-04T09:00:00Z"),
        )
        sync.pull().getOrThrow()
        assertEquals("Local edit", db.calendarEventDao().byGoogleEventId("g1")!!.title)
    }

    @Test
    fun pull_appliesRemote_whenNewerThanLocalSync() = runTest {
        fake.pages += page(
            event("g1", "v1", start = "2026-11-04T10:00:00Z", end = "2026-11-04T11:00:00Z",
                updated = "2026-11-04T09:00:00Z"),
        )
        sync.pull().getOrThrow()
        // Remote was edited AFTER our last sync (far-future timestamp keeps the
        // test deterministic against the real wall clock).
        fake.pages += page(
            event("g1", "v2 from remote", start = "2026-11-04T10:00:00Z", end = "2026-11-04T11:00:00Z",
                updated = "2099-01-01T00:00:00Z"),
        )
        sync.pull().getOrThrow()
        assertEquals("v2 from remote", db.calendarEventDao().byGoogleEventId("g1")!!.title)
    }

    @Test
    fun pull_410FallsBackToFullPull_andClearsToken() = runTest {
        db.metaDao().put(com.orvane.app.data.db.MetaEntity("gcal_sync_token", "stale"))
        fake.failSyncTokenWith410 = true
        fake.pages += page(
            event("g9", "Recovered", start = "2026-11-05T08:00:00Z", end = "2026-11-05T09:00:00Z"),
        )
        val applied = sync.pull().getOrThrow()
        assertEquals(1, applied)
        assertEquals(1, fake.fullPullCalls)
        // A fresh sync token from the full pull replaces the stale one.
        assertEquals("sync-token-1", db.metaDao().get("gcal_sync_token")!!.value)
    }

    @Test
    fun push_createsLocalEvents_andBackfillsGoogleId() = runTest {
        db.calendarEventDao().upsert(
            CalendarEventEntity(
                id = "l1", title = "Local only", note = "", start = 1_700_000_000_000,
                end = 1_700_000_360_000, source = EventSource.LOCAL,
            ),
        )
        val pushed = sync.push().getOrThrow()
        assertEquals(1, pushed)
        assertEquals(1, fake.inserted.size)
        val stored = db.calendarEventDao().byId("l1")!!
        assertEquals("remote_1", stored.googleEventId)
        assertNotNull(stored.lastSyncedAt)
    }

    @Test
    fun push_patchesEventsWithExistingGoogleId() = runTest {
        db.calendarEventDao().upsert(
            CalendarEventEntity(
                id = "l1", title = "Edited", note = "", start = 1_700_000_000_000,
                end = 1_700_000_360_000, source = EventSource.LOCAL, googleEventId = "g1",
            ),
        )
        sync.push().getOrThrow()
        assertEquals(0, fake.inserted.size)
        assertEquals(1, fake.patched.size)
        assertEquals("g1", fake.patched[0].first)
        assertEquals("Edited", fake.patched[0].second.summary)
    }

    // ---- helpers ----

    private fun page(vararg events: GoogleCalendarModels.Event) = GoogleCalendarModels.EventsPage(
        items = events.toList(),
        nextSyncToken = "sync-token-1",
    )

    private fun event(
        id: String,
        summary: String,
        start: String = "2026-11-04T10:00:00Z",
        end: String = "2026-11-04T11:00:00Z",
        status: String = "confirmed",
        updated: String? = null,
    ) = GoogleCalendarModels.Event(
        id = id,
        summary = summary,
        status = status,
        updated = updated,
        start = GoogleCalendarModels.DateTime(dateTime = start),
        end = GoogleCalendarModels.DateTime(dateTime = end),
    )
}
