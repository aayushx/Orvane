package com.tasky.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE start < :rangeEnd AND end > :rangeStart ORDER BY start ASC")
    fun observeInRange(rangeStart: Long, rangeEnd: Long): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE start < :rangeEnd AND end > :rangeStart ORDER BY start ASC")
    suspend fun inRange(rangeStart: Long, rangeEnd: Long): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE deviceId = :deviceId LIMIT 1")
    suspend fun byDeviceId(deviceId: Long): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events WHERE source = 1")
    suspend fun deviceEvents(): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE feedId = :feedId")
    suspend fun forFeed(feedId: String): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE source = 0")
    suspend fun locals(): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun byId(id: String): CalendarEventEntity?

    @Upsert
    suspend fun upsert(event: CalendarEventEntity)

    @Upsert
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM calendar_events WHERE source = 1")
    suspend fun deleteAllDevice()

    @Query("DELETE FROM calendar_events WHERE feedId = :feedId")
    suspend fun deleteForFeed(feedId: String)

    @Query("DELETE FROM calendar_events WHERE linkedTaskId = :taskId")
    suspend fun deleteLinkedToTask(taskId: String)
}

@Dao
interface IcsFeedDao {
    @Query("SELECT * FROM ics_feeds ORDER BY name ASC")
    fun observeAll(): Flow<List<IcsFeedEntity>>

    @Query("SELECT * FROM ics_feeds")
    suspend fun all(): List<IcsFeedEntity>

    @Query("SELECT * FROM ics_feeds WHERE id = :id")
    suspend fun byId(id: String): IcsFeedEntity?

    @Upsert
    suspend fun upsert(feed: IcsFeedEntity)

    @Query("DELETE FROM ics_feeds WHERE id = :id")
    suspend fun delete(id: String)
}
