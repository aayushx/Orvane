package com.orvane.app.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * G4: migration 2→3 adds Google sync bookkeeping columns to `calendar_events`.
 * Existing rows keep their data and read back as null for both new columns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration23Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate2To3_preservesRows_andAddsNullableSyncColumns() {
        val db = helper.createDatabase(DB, 2)
        db.execSQL(
            "INSERT INTO calendar_events (id, title, note, location, start, \"end\", allDay, source, " +
                "deviceId, deviceCalendarId, feedId, linkedTaskId) VALUES " +
                "('e1', 'Standup', '', NULL, 1700000000000, 1700003600000, 0, 0, NULL, NULL, NULL, NULL)",
        )
        db.close()

        val migrated = helper.runMigrationsAndValidate(DB, 3, true, AppDatabase.MIGRATION_2_3)
        migrated.query("SELECT title, googleEventId, lastSyncedAt FROM calendar_events WHERE id = 'e1'").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Standup", c.getString(0))
            assertNull(c.getString(1))
            assertNull(c.getString(2))
        }
        migrated.close()
    }

    @Test
    fun migrate2To3_newColumnsAcceptValues() {
        helper.createDatabase(DB, 2).close()
        val migrated = helper.runMigrationsAndValidate(DB, 3, true, AppDatabase.MIGRATION_2_3)
        migrated.execSQL(
            "INSERT INTO calendar_events (id, title, note, location, start, \"end\", allDay, source, " +
                "deviceId, deviceCalendarId, feedId, linkedTaskId, googleEventId, lastSyncedAt) VALUES " +
                "('e2', 'Synced', '', NULL, 1700000000000, 1700003600000, 0, 3, NULL, NULL, NULL, NULL, " +
                "'gcal_abc123', 1700005000000)",
        )
        migrated.query("SELECT googleEventId, lastSyncedAt FROM calendar_events WHERE id = 'e2'").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("gcal_abc123", c.getString(0))
            assertEquals(1700005000000L, c.getLong(1))
        }
        migrated.close()
    }

    companion object {
        private const val DB = "migration-2-3-test.db"
    }
}
