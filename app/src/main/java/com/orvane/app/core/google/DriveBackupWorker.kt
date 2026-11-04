package com.orvane.app.core.google

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orvane.app.data.account.AccountSessionStore
import com.orvane.app.data.google.DriveBackupSync
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * G5: daily Drive AppData backup. No-ops when signed out, so the schedule can
 * stay installed unconditionally. Failures retry with WorkManager backoff.
 */
@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val driveBackup: DriveBackupSync,
    private val accountStore: AccountSessionStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (accountStore.session.first() == null) return Result.success()
        // Catch-up semantics: skip when the last backup is still fresh. This
        // also makes the on-open catch-up job a no-op most days.
        val last = driveBackup.lastBackupAt()
        if (last != null && System.currentTimeMillis() - last < STALE_AFTER_MS) {
            return Result.success()
        }
        return driveBackup.backupNow().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    companion object {
        const val WORK_NAME = "orvane_drive_backup"
        const val CATCH_UP_NAME = "orvane_drive_backup_catchup"
        val PERIOD_HOURS = 24L

        /** Catch-up threshold: run a one-time backup when the last one is older than this. */
        const val STALE_AFTER_MS = 24L * 3600_000L
    }
}

/** Installs the periodic backup and the catch-up-on-open one-time job. */
@javax.inject.Singleton
class DriveBackupScheduler @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    fun schedulePeriodic() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DriveBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DriveBackupWorker>(DriveBackupWorker.PERIOD_HOURS, TimeUnit.HOURS).build(),
        )
    }

    /**
     * Catch-up on open: enqueue a one-time backup. The worker itself skips
     * when the last backup is still fresh, so this is cheap to call often.
     */
    fun scheduleCatchUp() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DriveBackupWorker.CATCH_UP_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DriveBackupWorker>().build(),
        )
    }
}
