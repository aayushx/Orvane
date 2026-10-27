package com.orvane.app.di

import android.content.Context
import androidx.room.Room
import com.orvane.app.core.alarms.AlarmScheduler
import com.orvane.app.core.alarms.AndroidAlarmScheduler
import com.orvane.app.data.db.AppDatabase
import com.orvane.app.data.db.AttachmentDao
import com.orvane.app.data.db.CalendarEventDao
import com.orvane.app.data.db.FilterDao
import com.orvane.app.data.db.FocusSessionDao
import com.orvane.app.data.db.FolderDao
import com.orvane.app.data.db.HabitCheckDao
import com.orvane.app.data.db.HabitDao
import com.orvane.app.data.db.IcsFeedDao
import com.orvane.app.data.db.ListDao
import com.orvane.app.data.db.MetaDao
import com.orvane.app.data.db.ReminderDao
import com.orvane.app.data.db.TagDao
import com.orvane.app.data.db.TaskDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Database + DAO providers for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .build()

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideListDao(db: AppDatabase): ListDao = db.listDao()
    @Provides fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()
    @Provides fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
    @Provides fun provideFilterDao(db: AppDatabase): FilterDao = db.filterDao()
    @Provides fun provideHabitDao(db: AppDatabase): HabitDao = db.habitDao()
    @Provides fun provideHabitCheckDao(db: AppDatabase): HabitCheckDao = db.habitCheckDao()
    @Provides fun provideFocusSessionDao(db: AppDatabase): FocusSessionDao = db.focusSessionDao()
    @Provides fun provideCalendarEventDao(db: AppDatabase): CalendarEventDao = db.calendarEventDao()
    @Provides fun provideIcsFeedDao(db: AppDatabase): IcsFeedDao = db.icsFeedDao()
    @Provides fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
    @Provides fun provideMetaDao(db: AppDatabase): MetaDao = db.metaDao()
}

/** Binds the concrete scheduler so consumers can inject the [AlarmScheduler] interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler
}
