package com.orvane.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.orvane.app.core.backup.AutoBackupWorker
import com.orvane.app.core.notifications.OrvaneNotifications
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class OrvaneApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var googleSyncScheduler: com.orvane.app.core.google.GoogleSyncScheduler

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleAutoBackup()
        scheduleGoogleSync()
    }

    /** G4d: periodic Google Calendar sync; the worker no-ops when signed out. */
    private fun scheduleGoogleSync() {
        runCatching {
            googleSyncScheduler.schedule()
        }
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
        OrvaneNotifications.ensureChannels(this)
    }
}