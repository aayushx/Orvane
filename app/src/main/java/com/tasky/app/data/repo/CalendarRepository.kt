package com.tasky.app.data.repo

import android.content.Context
import android.provider.CalendarContract
import com.tasky.app.core.calendar.IcsParser
import com.tasky.app.core.util.Time
import com.tasky.app.data.db.CalendarEventDao
import com.tasky.app.data.db.CalendarEventEntity
import com.tasky.app.data.db.EventSource
import com.tasky.app.data.db.IcsFeedDao
import com.tasky.app.data.db.IcsFeedEntity

import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local calendar events, read-only device-calendar sync and ICS feed subscriptions.
 * Everything is cached into `calendar_events` so the UI has one source and works
 * offline after a sync.
 */
@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val calendarEventDao: CalendarEventDao,
    private val icsFeedDao: IcsFeedDao,
    private val taskRepo: TaskRepository,
) {

    fun newId(): String = taskRepo.newId()

    fun observeEvents(rangeStart: Long, rangeEnd: Long) =
        calendarEventDao.observeInRange(rangeStart, rangeEnd)

    // ---------- Local events ----------

    suspend fun addLocalEvent(
        title: String,
        note: String = "",
        start: Long,
        end: Long,
        allDay: Boolean = false,
        linkedTaskId: String? = null,
    ): CalendarEventEntity {
        val event = CalendarEventEntity(
            id = newId(),
            title = title.ifBlank { "(event)" },
            note = note,
            start = start,
            end = end,
            allDay = allDay,
            source = EventSource.LOCAL,
            linkedTaskId = linkedTaskId,
        )
        calendarEventDao.upsert(event)
        return event
    }

    suspend fun deleteEvent(id: String) {
        val e = calendarEventDao.byId(id)
        calendarEventDao.delete(id)
        // Un-schedule the linked time block on the task when its block is removed.
        if (e?.linkedTaskId != null && e.source == EventSource.LOCAL) {
            val stillThere = calendarEventDao.forLinkedTask(e.linkedTaskId!!)
            if (stillThere.isEmpty()) {
                taskRepo.setDue(e.linkedTaskId!!, null, true)
            }
        }
    }

    // ---------- Task scheduling (drag task → calendar) ----------

    /** Blocks [taskId] into a calendar slot and sets its due datetime. */
    suspend fun scheduleTask(taskId: String, title: String, start: Long, end: Long): CalendarEventEntity {
        val event = CalendarEventEntity(
            id = newId(),
            title = title,
            start = start,
            end = end,
            allDay = false,
            source = EventSource.LOCAL,
            linkedTaskId = taskId,
        )
        calendarEventDao.upsert(event)
        taskRepo.setDue(taskId, start, allDay = false)
        return event
    }

    /** Moves a task block to a new slot. */
    suspend fun moveBlock(eventId: String, newStart: Long, newEnd: Long) {
        calendarEventDao.byId(eventId)?.let { e ->
            calendarEventDao.upsert(e.copy(start = newStart, end = newEnd))
            e.linkedTaskId?.let { taskRepo.setDue(it, newStart, allDay = false) }
        }
    }

    // ---------- Device calendars (read-only) ----------

    /** Returns true when a calendar provider is available and permission granted. */
    fun canReadDeviceCalendars(): Boolean {
        return appContext.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads visible device events into the cache. Returns the number of synced
     * events, or −1 when permission is missing.
     */
    suspend fun syncDeviceCalendars(): Int = withContext(Dispatchers.IO) {
        if (!canReadDeviceCalendars()) return@withContext -1
        val zone = ZoneId.systemDefault()
        val rangeStart = Time.startOfDayEpoch(LocalDate.now(zone).minusMonths(1))
        val rangeEnd = Time.startOfDayEpoch(LocalDate.now(zone).plusMonths(12))
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_TIMEZONE,
        )
        val events = mutableListOf<CalendarEventEntity>()
        try {
            appContext.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?",
                arrayOf(rangeStart.toString(), rangeEnd.toString()),
                "${CalendarContract.Events.DTSTART} ASC",
            )?.use { cursor ->
                val cached = calendarEventDao.deviceEvents().associateBy { it.deviceId }
                while (cursor.moveToNext()) {
                    val deviceId = cursor.getLong(0)
                    val calendarId = cursor.getLong(1)
                    val title = cursor.getString(2) ?: "(no title)"
                    val note = cursor.getString(3) ?: ""
                    val location = cursor.getString(4)
                    val dtstart = cursor.getLong(5)
                    val dtend = if (!cursor.isNull(6)) cursor.getLong(6) else dtstart + 3600_000
                    val allDay = cursor.getInt(7) == 1
                    val tz = cursor.getString(8)?.let {
                        runCatching { ZoneId.of(it) }.getOrNull()
                    } ?: zone
                    val existing = cached[deviceId]
                    events.add(
                        CalendarEventEntity(
                            id = existing?.id ?: newId(),
                            title = title,
                            note = note,
                            location = location,
                            start = dtstart,
                            end = dtend,
                            allDay = allDay,
                            source = EventSource.DEVICE,
                            deviceId = deviceId,
                            deviceCalendarId = calendarId,
                        ),
                    )
                }
            }
            // Replace cache: remove stale device events then upsert fresh.
            val freshIds = events.map { it.deviceId }.toSet()
            val stale = calendarEventDao.deviceEvents().filter { it.deviceId !in freshIds }
            for (s in stale) calendarEventDao.delete(s.id)
            calendarEventDao.upsertAll(events)
            events.size
        } catch (e: Exception) {
            // Provider unavailable or security exception: keep the cached copy.
            -2
        }
    }

    // ---------- ICS feeds ----------

    fun observeFeeds() = icsFeedDao.observeAll()

    suspend fun addFeed(url: String, name: String): IcsFeedEntity {
        val feed = IcsFeedEntity(id = newId(), url = url.trim(), name = name.ifBlank { url.trim() })
        icsFeedDao.upsert(feed)
        return feed
    }

    suspend fun deleteFeed(id: String) {
        for (e in calendarEventDao.forFeed(id)) calendarEventDao.delete(e.id)
        icsFeedDao.delete(id)
    }

    /** Fetches + parses all enabled feeds; returns total synced events (−1 on network error). */
    suspend fun syncFeeds(): Int = withContext(Dispatchers.IO) {
        var total = 0
        for (feed in icsFeedDao.all().filter { it.enabled }) {
            val fetched = fetchFeed(feed)
            if (fetched >= 0) {
                total += fetched
                icsFeedDao.upsert(feed.copy(lastSync = System.currentTimeMillis()))
            }
        }
        total
    }

    /** Returns the number of parsed events, or −1 on failure (keeps the old cache). */
    private suspend fun fetchFeed(feed: IcsFeedEntity): Int {
        return try {
            val conn = URL(feed.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val parsed = IcsParser.parse(body)
            val zone = ZoneId.systemDefault()
            val old = calendarEventDao.forFeed(feed.id).associateBy { it.id }
            // stable ids from feed event index+title+start so re-sync keeps identity
            val events = parsed.mapIndexed { i, e ->
                val stableKey = "${feed.id}-$i-${e.title.hashCode()}-${e.start.toInstant().toEpochMilli()}"
                CalendarEventEntity(
                    id = old.values.firstOrNull {
                        it.feedId == feed.id && it.start == e.start.toInstant().toEpochMilli() && it.title == e.title
                    }?.id ?: stableKey,
                    title = e.title,
                    note = e.note,
                    location = e.location,
                    start = e.start.toInstant().toEpochMilli(),
                    end = e.end.toInstant().toEpochMilli(),
                    allDay = e.allDay,
                    source = EventSource.ICS,
                    feedId = feed.id,
                )
            }
            val keepNew = events.map { it.id }.toSet()
            for (o in old.values) if (o.id !in keepNew) calendarEventDao.delete(o.id)
            calendarEventDao.upsertAll(events)
            events.size
        } catch (e: Exception) {
            -1
        }
    }
}
