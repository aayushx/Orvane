package com.orvane.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.orvane.app.data.db.AppDatabase
import com.orvane.app.data.db.FolderEntity
import com.orvane.app.data.db.ListEntity
import com.orvane.app.data.db.TagEntity
import com.orvane.app.data.db.TaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Round 2 / A8 drawer-management semantics against a real in-memory Room DB:
 * list delete → tasks fall back to Inbox; folder delete → member lists ungroup;
 * tag rename onto an existing name → merge of references.
 */
@RunWith(RobolectricTestRunner::class)
class DrawerManagementTest {

    private lateinit var db: AppDatabase
    private var nextId = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newId(prefix: String) = "$prefix-${nextId++}"

    private fun newTask(listId: String?): TaskEntity = TaskEntity(
        id = newId("t"),
        title = newId("task"),
        listId = listId,
        createdAt = 1_000_000L,
        modifiedAt = 1_000_000L,
    )

    // ----- list delete → tasks fall back to Inbox -----

    @Test
    fun deletingAListSendsItsTasksToInbox() = runBlocking {
        val listEntity = ListEntity(id = newId("l"), name = "Errands", color = 0xFF0B7285.toInt())
        db.listDao().upsert(listEntity)
        val task = newTask(listEntity.id)
        db.taskDao().insert(task)
        assertEquals(listEntity.id, db.taskDao().byId(task.id)!!.listId)

        // What repository.deleteList() does under the hood:
        db.taskDao().clearListId(listEntity.id, modifiedAt = 2_000_000L)
        db.listDao().deleteById(listEntity.id)

        assertNull(db.taskDao().byId(task.id)!!.listId)
        assertEquals(2_000_000L, db.taskDao().byId(task.id)!!.modifiedAt)
        assertNull(db.listDao().byId(listEntity.id))
    }

    // ----- folder delete → member lists ungroup -----

    @Test
    fun deletingAFolderUngroupsItsMemberLists() = runBlocking {
        val folder = FolderEntity(id = newId("f"), name = "Home", sortOrder = 0)
        db.folderDao().upsert(folder)
        val inFolder = ListEntity(id = newId("l"), name = "Groceries", groupId = folder.id)
        val topLevel = ListEntity(id = newId("l"), name = "Work", groupId = null)
        db.listDao().upsert(inFolder)
        db.listDao().upsert(topLevel)

        // What repository.deleteFolder() does under the hood:
        db.listDao().clearFolderMembership(folder.id)
        db.folderDao().delete(folder.id)

        assertNull(db.listDao().byId(inFolder.id)!!.groupId)
        assertNull(db.listDao().byId(topLevel.id)!!.groupId)
        assertTrue(db.folderDao().all().none { it.id == folder.id })
    }

    // ----- tag rename onto an existing name = merge -----

    @Test
    fun tagRenameOntoExistingNameMergesReferences() = runBlocking {
        // Repository-level flow: renameTag(work, "Personal") → mergeTagInto.
        db.tagDao().upsert(TagEntity(id = "work", name = "Work"))
        db.tagDao().upsert(TagEntity(id = "personal", name = "Personal"))
        db.tagDao().upsert(TagEntity(id = "errands", name = "Errands"))

        val t1 = newTask(null); val t2 = newTask(null); val t3 = newTask(null)
        db.taskDao().insert(t1); db.taskDao().insert(t2); db.taskDao().insert(t3)
        db.tagDao().addRef(t1.id, "work")
        db.tagDao().addRef(t2.id, "work")
        db.tagDao().addRef(t3.id, "personal") // prove merge de-dupes if the task already has the target

        val collision = db.tagDao().byName("Personal")!! // rename target found (NOCASE)
        db.tagDao().moveRefs("work", collision.id)
        db.tagDao().clearRefs("work")
        db.tagDao().deleteById("work")

        assertNull(db.tagDao().byId("work"))
        assertEquals("Personal", db.tagDao().byName("personal")!!.name)
        assertTrue(db.tagDao().forTask(t1.id).any { it.id == "personal" })
        assertTrue(db.tagDao().forTask(t2.id).any { it.id == "personal" })
        // Task already tagged Personal keeps exactly ONE ref (PK dedupe):
        val t3Refs = db.tagDao().forTask(t3.id).map { it.id }
        assertEquals(1, t3Refs.count { it == "personal" })
    }

    @Test
    fun deleteTagRemovesItAndItsRefs() = runBlocking {
        db.tagDao().upsert(TagEntity(id = "gone", name = "Old"))
        val task = newTask(null)
        db.taskDao().insert(task)
        db.tagDao().addRef(task.id, "gone")

        db.tagDao().clearRefs("gone")
        db.tagDao().deleteById("gone")

        assertNull(db.tagDao().byId("gone"))
        assertTrue(db.tagDao().forTask(task.id).isEmpty())
        assertFalse(db.tagDao().all().any { it.name == "Old" })
    }

    @Test
    fun byNameIsCaseInsensitive() = runBlocking {
        db.tagDao().upsert(TagEntity(id = "x", name = "Project Alpha"))
        assertEquals("x", db.tagDao().byName("project alpha")!!.id)
        assertEquals("x", db.tagDao().byName("PROJECT ALPHA")!!.id)
    }
}
