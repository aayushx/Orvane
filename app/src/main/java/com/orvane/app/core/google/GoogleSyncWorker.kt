package com.orvane.app.core.google

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orvane.app.data.account.AccountSessionStore
import com.orvane.app.data.google.GoogleCalendarSync
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * G4d: periodic two-way Google Calendar sync (~every 30 minutes). No-ops when
 * signed out, so the schedule can stay installed unconditionally.
 */
@HiltWorker
class GoogleSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sync: GoogleCalendarSync,
    private val accountStore: AccountSessionStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (accountStore.session.first() == null) return Result.success()
        return sync.syncNow().fold(
            onSuccess = { (pulled, pushed) ->
                sync.recordLastSync("ok — $pulled pulled, $pushed pushed")
                Result.success()
            },
            onFailure = { e ->
                sync.recordLastSync("failed: ${e.message?.take(120) ?: "error"}")
                Result.retry()
            },
        )
    }

    companion object {
        const val WORK_NAME = "orvane_google_sync"
        val PERIOD_MINUTES = 30L
    }
}

/** Installs/removes the periodic sync work. */
@javax.inject.Singleton
class GoogleSyncScheduler @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
) {
    fun schedule() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GoogleSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<GoogleSyncWorker>(GoogleSyncWorker.PERIOD_MINUTES, TimeUnit.MINUTES).build(),
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(GoogleSyncWorker.WORK_NAME)
    }
}
