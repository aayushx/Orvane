package com.orvane.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * G1: migration 1→2 adds the nullable `durationMinutes` column to `tasks`.
 * Existing rows must keep their data and read back as null duration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration12Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2_preservesRows_andAddsNullableDuration() {
        val db = helper.createDatabase(DB, 1)
        db.execSQL(
            "INSERT INTO tasks (id, parentId, title, note, due, isDueAllDay, priority, listId, " +
                "recurrenceRule, completedAt, deletedAt, createdAt, modifiedAt, sortOrder, " +
                "focusSeconds, pomoCount) VALUES " +
                "('t1', NULL, 'Write report', '', 1700000000000, 1, 0, NULL, NULL, NULL, NULL, " +
                "1700000000000, 1700000000000, 0, 0, 0)",
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(DB, 2, true, AppDatabase.MIGRATION_1_2)
        migrated.query("SELECT title, durationMinutes FROM tasks WHERE id = 't1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Write report", c.getString(0))
            assertNull(c.getString(1))
        }
        migrated.close()
    }

    @Test
    fun migrate1To2_newColumnAcceptsValues() {
        helper.createDatabase(DB, 1).close()
        val migrated = helper.runMigrationsAndValidate(DB, 2, true, AppDatabase.MIGRATION_1_2)
        migrated.execSQL(
            "INSERT INTO tasks (id, parentId, title, note, due, isDueAllDay, priority, listId, " +
                "recurrenceRule, completedAt, deletedAt, createdAt, modifiedAt, sortOrder, " +
                "focusSeconds, pomoCount, durationMinutes) VALUES " +
                "('t2', NULL, 'Deep work', '', NULL, 1, 0, NULL, NULL, NULL, NULL, " +
                "1700000000000, 1700000000000, 0, 0, 0, 90)",
        )
        migrated.query("SELECT durationMinutes FROM tasks WHERE id = 't2'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(90, c.getInt(0))
        }
        migrated.close()
    }

    companion object {
        private const val DB = "migration-1-2-test.db"
    }
}
