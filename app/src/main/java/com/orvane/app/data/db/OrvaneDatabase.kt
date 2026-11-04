package com.orvane.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        ListEntity::class,
        FolderEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        FilterEntity::class,
        HabitEntity::class,
        HabitCheckEntity::class,
        FocusSessionEntity::class,
        CalendarEventEntity::class,
        IcsFeedEntity::class,
        AttachmentEntity::class,
        MetaEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun listDao(): ListDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun filterDao(): FilterDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCheckDao(): HabitCheckDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun icsFeedDao(): IcsFeedDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun metaDao(): MetaDao

    companion object {
        const val NAME = "orvane.db"

        /** G1: tasks gain an optional durationMinutes column (nullable, no default). */
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER")
            }
        }

        /** G4: calendar events gain Google sync bookkeeping columns. */
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN googleEventId TEXT")
                db.execSQL("ALTER TABLE calendar_events ADD COLUMN lastSyncedAt INTEGER")
            }
        }
    }
}
