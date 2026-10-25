package com.tasky.app.data.db

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
    version = 1,
    exportSchema = false,
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
        const val NAME = "tasky.db"
    }
}
