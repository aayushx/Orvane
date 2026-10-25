package com.tasky.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archived = 1 ORDER BY sortOrder ASC, createdAt ASC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC")
    suspend fun all(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE archived = 0 AND reminderEnabled = 1")
    suspend fun withReminder(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE archived = 0 AND nagEnabled = 1")
    suspend fun withNag(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE habits SET archived = :archived, sortOrder = :sortOrder WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, sortOrder: Int)

    @Query("SELECT MAX(sortOrder) FROM habits WHERE archived = 0")
    suspend fun maxSortOrder(): Int?

    @Query("UPDATE habits SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int)
}

@Dao
interface HabitCheckDao {
    @Query("SELECT * FROM habit_checks WHERE habitId = :habitId ORDER BY epochDay ASC")
    suspend fun forHabit(habitId: String): List<HabitCheckEntity>

    @Query("SELECT * FROM habit_checks WHERE habitId = :habitId ORDER BY epochDay ASC")
    fun observeForHabit(habitId: String): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habit_checks")
    suspend fun all(): List<HabitCheckEntity>

    @Query("SELECT * FROM habit_checks")
    fun observeAll(): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habit_checks WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun find(habitId: String, epochDay: Long): HabitCheckEntity?

    @Query("SELECT * FROM habit_checks WHERE epochDay = :epochDay")
    suspend fun forDay(epochDay: Long): List<HabitCheckEntity>

    @Query("SELECT * FROM habit_checks WHERE epochDay = :epochDay")
    fun observeForDay(epochDay: Long): Flow<List<HabitCheckEntity>>

    @Upsert
    suspend fun upsert(check: HabitCheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checks: List<HabitCheckEntity>)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteFor(habitId: String, epochDay: Long)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: String)
}

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)

    @Upsert
    suspend fun upsert(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<FocusSessionEntity>)

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :from ORDER BY startedAt DESC")
    suspend fun since(from: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE startedAt >= :from ORDER BY startedAt DESC")
    fun observeSince(from: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    suspend fun forTask(taskId: String): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt ASC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt ASC")
    suspend fun all(): List<FocusSessionEntity>

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM focus_sessions WHERE startedAt >= :from AND type = 0")
    suspend fun focusedSecondsSince(from: Long): Long

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM focus_sessions WHERE startedAt >= :from AND type = 0")
    fun observeFocusedSecondsSince(from: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(seconds), 0) FROM focus_sessions WHERE taskId = :taskId AND type = 0")
    fun observeSecondsForTask(taskId: String): Flow<Long>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun byId(id: String): FocusSessionEntity?
}
