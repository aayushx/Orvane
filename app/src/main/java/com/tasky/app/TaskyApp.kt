package com.tasky.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tasky.app.core.backup.AutoBackupWorker
import com.tasky.app.core.notifications.TaskyNotifications
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TaskyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleAutoBackup()
    }

    /** Automatic daily JSON backup; keeps the newest 7 files. */
    private fun scheduleAutoBackup() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AutoBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS).build(),
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                TaskyNotifications.CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task due-date and reminder notifications"
            },
            NotificationChannel(
                TaskyNotifications.CHANNEL_HABITS,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily habit reminders and evening nudges"
            },
            NotificationChannel(
                TaskyNotifications.CHANNEL_FOCUS,
                "Focus",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent pomodoro timer notification"
                setShowBadge(false)
            },
            NotificationChannel(
                TaskyNotifications.CHANNEL_AI,
                "AI Assistant",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "AI assistant activity"
            },
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }
}
