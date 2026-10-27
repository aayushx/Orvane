package com.orvane.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND listId = :listId AND completedAt IS NULL")
    fun observeActiveByList(listId: String?): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NOT NULL AND deletedAt IS NULL")
    fun observeSubtasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL")
    fun observeAllActive(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<TaskEntity>

    @Query("SELECT parentId FROM tasks WHERE id = :id")
    suspend fun parentIdOf(id: String): String?

    @Query("SELECT * FROM tasks WHERE parentId = :parentId AND deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun children(parentId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId = :parentId AND deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeChildren(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :parentId AND deletedAt IS NULL")
    suspend fun childCount(parentId: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId = :parentId AND deletedAt IS NULL AND completedAt IS NOT NULL")
    suspend fun completedChildCount(parentId: String): Int

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND listId IS NULL")
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND listId IS NULL AND completedAt IS NULL")
    suspend fun inboxOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND due >= :rangeStart AND due < :rangeEnd")
    suspend fun dueInRange(rangeStart: Long, rangeEnd: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND due >= :rangeStart AND due < :rangeEnd")
    suspend fun allInRange(rangeStart: Long, rangeEnd: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND due < :rangeEnd")
    suspend fun dueBefore(rangeEnd: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND (completedAt IS NULL OR completedAt >= :sinceMillis)")
    suspend fun completedSince(sinceMillis: Long): List<TaskEntity>

    /**
     * Tasks to display in the calendar UI as blocks: has due with a time, in range, not completed.
     */
    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND due IS NOT NULL AND isDueAllDay = 0 AND due >= :rangeStart AND due < :rangeEnd ORDER BY due ASC")
    suspend fun calendarTasksInRange(rangeStart: Long, rangeEnd: Long): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND due IS NOT NULL AND due >= :rangeStart AND due < :rangeEnd")
    suspend fun countDueInRange(rangeStart: Long, rangeEnd: Long): Int

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND (listId = :listId OR :listId IS NULL)")
    suspend fun activeInListOnce(listId: String?): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL")
    suspend fun allActiveOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt > :purgeBefore")
    suspend fun trashNotOlderThan(purgeBefore: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun trashOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt >= :dayStart AND completedAt < :dayEnd")
    fun observeCompletedBetween(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NOT NULL ORDER BY completedAt DESC")
    suspend fun completedOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR title = :query)")
    fun observeSearch(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    suspend fun searchOnce(query: String): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun allTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE tasks SET deletedAt = :deletedAt, modifiedAt = :modifiedAt, completedAt = NULL WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long, modifiedAt: Long)

    @Query("UPDATE tasks SET deletedAt = NULL, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun restore(id: String, modifiedAt: Long)

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun purgeTrash()

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrashOlderThan(cutoff: Long)

    @Query("UPDATE tasks SET completedAt = :completedAt, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setCompletedAt(id: String, completedAt: Long?, modifiedAt: Long)

    @Query("UPDATE tasks SET completedAt = :completedAt, modifiedAt = :modifiedAt WHERE parentId = :parentId AND completedAt IS NULL")
    suspend fun completeChildren(parentId: String, completedAt: Long?, modifiedAt: Long)

    @Query("UPDATE tasks SET listId = :listId, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setListId(id: String, listId: String?, modifiedAt: Long)

    @Query("UPDATE tasks SET listId = :listId, modifiedAt = :modifiedAt WHERE deletedAt IS NULL AND id IN (:ids)")
    suspend fun setListIds(ids: List<String>, listId: String?, modifiedAt: Long)

    @Query("UPDATE tasks SET due = :due, isDueAllDay = :allDay, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setDue(id: String, due: Long?, allDay: Boolean, modifiedAt: Long)

    @Query("UPDATE tasks SET priority = :priority, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setPriority(id: String, priority: Int, modifiedAt: Long)

    @Query("UPDATE tasks SET priority = :priority, modifiedAt = :modifiedAt WHERE deletedAt IS NULL AND id IN (:ids)")
    suspend fun setPriorities(ids: List<String>, priority: Int, modifiedAt: Long)

    @Query("UPDATE tasks SET title = :title, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setTitle(id: String, title: String, modifiedAt: Long)

    @Query("UPDATE tasks SET note = :note, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setNote(id: String, note: String, modifiedAt: Long)

    @Query("UPDATE tasks SET recurrenceRule = :rule, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setRecurrenceRule(id: String, rule: String?, modifiedAt: Long)

    @Query("UPDATE tasks SET sortOrder = :sortOrder, modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int, modifiedAt: Long)

    @Query("UPDATE tasks SET focusSeconds = focusSeconds + :delta WHERE id = :id")
    suspend fun addFocusSeconds(id: String?, delta: Long)

    @Query("UPDATE tasks SET pomoCount = pomoCount + :delta WHERE id = :id")
    suspend fun addPomoCount(id: String?, delta: Int)

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL")
    suspend fun maxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND due >= :rangeStart AND due < :rangeEnd")
    fun observeCountDueInRange(rangeStart: Long, rangeEnd: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE parentId IS NULL AND deletedAt IS NULL AND completedAt IS NULL AND due < :rangeEnd")
    fun observeCountOverdue(rangeEnd: Long): Flow<Int>

    @Transaction
    suspend fun reassignChildren(oldParent: String, newParent: String?) {
        // no-op helper, real logic in repository
    }
}
