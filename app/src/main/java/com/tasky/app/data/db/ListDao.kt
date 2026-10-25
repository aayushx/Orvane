package com.tasky.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, name ASC")
    suspend fun all(): List<ListEntity>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun byId(id: String?): ListEntity?

    @Query("SELECT COUNT(*) FROM lists")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: ListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lists: List<ListEntity>)

    @Delete
    suspend fun delete(list: ListEntity)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteById(id: String?)

    @Query("UPDATE lists SET collapsed = :collapsed WHERE id = :id")
    suspend fun setCollapsed(id: String, collapsed: Boolean)

    @Query("SELECT MAX(sortOrder) FROM lists")
    suspend fun maxSortOrder(): Int?
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    suspend fun all(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE folders SET collapsed = :collapsed WHERE id = :id")
    suspend fun setCollapsed(id: String, collapsed: Boolean)

    @Query("SELECT MAX(sortOrder) FROM folders")
    suspend fun maxSortOrder(): Int?
}

@Dao
interface TagDao {
    @Query("SELECT tags.*, COUNT(taskId) AS taskCount FROM tags LEFT JOIN task_tags ON task_tags.tagId = tags.id GROUP BY tags.id ORDER BY tags.name ASC")
    fun observeAll(): Flow<List<TagWithCount>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAllPlain(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun all(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun byId(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun byName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM task_tags WHERE tagId = :tagId")
    suspend fun clearRefs(tagId: String)

    @Query("SELECT tags.* FROM tags INNER JOIN task_tags ON task_tags.tagId = tags.id WHERE task_tags.taskId = :taskId ORDER BY tags.name ASC")
    fun observeForTask(taskId: String): Flow<List<TagEntity>>

    @Query("SELECT tags.* FROM tags INNER JOIN task_tags ON task_tags.tagId = tags.id WHERE task_tags.taskId = :taskId ORDER BY tags.name ASC")
    suspend fun forTask(taskId: String): List<TagEntity>

    @Transaction
    suspend fun replaceForTask(taskId: String, tags: List<TagEntity>) {
        for (t in tags) upsert(t)
        clearRefsFor(taskId)
        for (t in tags) addRef(taskId, t.id)
    }

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun clearRefsFor(taskId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRef(crossRef: TaskTagCrossRef)

    @Query("INSERT OR REPLACE INTO task_tags (taskId, tagId) VALUES (:taskId, :tagId)")
    suspend fun addRef(taskId: String, tagId: String)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun removeRef(taskId: String, tagId: String)

    @Query("SELECT sorted.tagId FROM task_tags sorted WHERE sorted.taskId = :taskId")
    fun observeTagIdsForTask(taskId: String): Flow<List<String>>
}

data class TagWithCount(
    @Suppress("unused") val id: String,
    val name: String,
    val color: Int,
    val taskCount: Long,
)

@Dao
interface FilterDao {
    @Query("SELECT * FROM filters ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<FilterEntity>>

    @Query("SELECT * FROM filters ORDER BY sortOrder ASC, name ASC")
    suspend fun all(): List<FilterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(filter: FilterEntity)

    @Query("DELETE FROM filters WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT MAX(sortOrder) FROM filters")
    suspend fun maxSortOrder(): Int?
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeForTask(taskId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE taskId = :taskId")
    suspend fun forTask(taskId: String): List<AttachmentEntity>

    @Upsert
    suspend fun upsert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM attachments WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("DELETE FROM attachments WHERE taskId IN (:taskIds)")
    suspend fun deleteForTasks(taskIds: List<String>)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM kv_meta")
    suspend fun all(): List<MetaEntity>

    @Query("SELECT * FROM kv_meta WHERE `key` = :key")
    suspend fun get(key: String): MetaEntity?

    @Upsert
    suspend fun put(entry: MetaEntity)

    @Query("DELETE FROM kv_meta WHERE `key` = :key")
    suspend fun delete(key: String)

    @Upsert
    suspend fun putAll(entries: List<MetaEntity>)
}
