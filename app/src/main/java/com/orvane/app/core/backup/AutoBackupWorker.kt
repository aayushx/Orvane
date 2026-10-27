package com.orvane.app.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orvane.app.data.repo.BackupRepository
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily automatic backup: writes a full JSON snapshot into the app's private
 * files directory and prunes older files, keeping the newest [KEEP].
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val json = backupRepository.buildBackupJson()
            val dir = File(applicationContext.filesDir, BACKUP_DIR).apply { mkdirs() }
            val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now())
            File(dir, "orvane-backup-$stamp.json").writeText(json)
            prune(dir)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun prune(dir: File) {
        val files = dir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        for (f in files.drop(KEEP)) f.delete()
    }

    companion object {
        const val BACKUP_DIR = "backups"
        const val KEEP = 7
        const val WORK_NAME = "orvane_auto_backup"
    }
}
