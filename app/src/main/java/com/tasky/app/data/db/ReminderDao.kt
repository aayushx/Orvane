package com.tasky.app.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Upsert
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    @Query("SELECT * FROM reminders WHERE at > :onlyAfter ORDER BY at ASC")
    suspend fun future(onlyAfter: Long): List<ReminderEntity>

    @Query("SELECT r.*, t.title AS taskTitle FROM reminders r INNER JOIN tasks t ON t.id = r.taskId WHERE r.at > :onlyAfter ORDER BY r.at ASC")
    suspend fun withTasksAfter(onlyAfter: Long): List<ReminderWithTask>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY at ASC")
    fun observeForTask(taskId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY at ASC")
    suspend fun forTask(taskId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun byId(id: String): ReminderEntity?

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("DELETE FROM reminders WHERE taskId IN (:taskIds)")
    suspend fun deleteForTasks(taskIds: List<String>)
}

data class ReminderWithTask(
    @androidx.room.Embedded val reminder: ReminderEntity,
    val taskTitle: String?,
)
