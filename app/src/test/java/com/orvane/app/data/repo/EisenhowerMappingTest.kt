package com.orvane.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.orvane.app.data.db.AppDatabase
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.db.TaskPriority
import com.orvane.app.core.alarms.AlarmScheduler
import com.orvane.app.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
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
import org.robolectric.annotation.Config

/**
 * Round 2 / B3: moving a task between Eisenhower quadrants updates the task so
 * it re-buckets consistently with the same urgent/important predicates everywhere.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EisenhowerMappingTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repo = TaskRepository(
            db = db,
            taskDao = db.taskDao(),
            reminderDao = db.reminderDao(),
            listDao = db.listDao(),
            folderDao = db.folderDao(),
            tagDao = db.tagDao(),
            filterDao = db.filterDao(),
            attachmentDao = db.attachmentDao(),
            metaDao = db.metaDao(),
            alarmScheduler = NoopAlarmScheduler,
            settingsRepository = SettingsRepository(context),
            appContext = context,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private object NoopAlarmScheduler : AlarmScheduler {
        override fun scheduleReminder(id: String, atMillis: Long) {}
        override fun cancelReminder(id: String) {}
        override fun scheduleHabit(habit: com.orvane.app.data.db.HabitEntity, nagHour: Int, nagMinute: Int) {}
        override fun cancelHabit(habitId: String) {}
        override fun scheduleHabitSnooze(habitId: String, atMillis: Long) {}
        override fun canScheduleExact(): Boolean = false
    }

    private fun task(id: String, dueDaysFromNow: Long?, priority: Int): TaskEntity {
        val due = dueDaysFromNow?.let { d ->
            LocalDate.now().plusDays(d).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        return TaskEntity(id = id, title = id, due = due, priority = priority, createdAt = 1L, modifiedAt = 1L)
    }

    @Test
    fun doFirstImplementsUrgentAndImportant() = runBlocking {
        db.taskDao().insert(task("a", dueDaysFromNow = 5, priority = TaskPriority.NONE))
        repo.applyMatrixQuadrant("a", TaskRepository.MatrixQuadrant.DO_FIRST)
        val t = db.taskDao().byId("a")!!
        assertTrue(repo.isImportant(t))
        assertTrue(repo.isUrgent(t))
        assertEquals(TaskPriority.MEDIUM, t.priority)
    }

    @Test
    fun scheduleUnUrges() = runBlocking {
        db.taskDao().insert(task("b", dueDaysFromNow = 0, priority = TaskPriority.MEDIUM))
        repo.applyMatrixQuadrant("b", TaskRepository.MatrixQuadrant.SCHEDULE)
        val t = db.taskDao().byId("b")!!
        assertTrue(repo.isImportant(t))
        assertFalse(repo.isUrgent(t))
    }

    @Test
    fun delegateDemotesImportance() = runBlocking {
        db.taskDao().insert(task("c", dueDaysFromNow = null, priority = TaskPriority.HIGH))
        repo.applyMatrixQuadrant("c", TaskRepository.MatrixQuadrant.DELEGATE)
        val t = db.taskDao().byId("c")!!
        assertEquals(TaskPriority.LOW, t.priority)
        assertFalse(repo.isImportant(t))
        assertTrue(repo.isUrgent(t))
    }

    @Test
    fun laterIsNeither() = runBlocking {
        db.taskDao().insert(task("d", dueDaysFromNow = 0, priority = TaskPriority.HIGH))
        repo.applyMatrixQuadrant("d", TaskRepository.MatrixQuadrant.LATER)
        val t = db.taskDao().byId("d")!!
        assertFalse(repo.isImportant(t))
        assertFalse(repo.isUrgent(t))
        assertEquals(TaskPriority.LOW, t.priority)
    }
}
