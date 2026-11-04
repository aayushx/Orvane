package com.orvane.app.data.google

import com.orvane.app.core.google.DriveBackupModels
import com.orvane.app.data.db.MetaDao
import com.orvane.app.data.db.MetaEntity
import com.orvane.app.data.repo.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G5: Drive AppData backup. Uploads a full JSON snapshot into the hidden
 * per-app Drive folder, keeps the newest [KEEP] files, and restores the
 * newest one on demand. Status lives in kv_meta.
 */
@Singleton
class DriveBackupSync @Inject constructor(
    private val api: DriveApi,
    private val backupRepository: BackupRepository,
    private val metaDao: MetaDao,
) {

    companion object {
        const val FILE_PREFIX = "orvane-drive-backup-"
        const val FILE_SUFFIX = ".json"

        /** Newest backups kept in Drive; older ones are pruned after upload. */
        const val KEEP = 7

        private const val KEY_LAST_BACKUP = "gdrive_last_backup"
        private const val KEY_LAST_BACKUP_AT = "gdrive_last_backup_at"
        private const val KEY_LAST_RESTORE = "gdrive_last_restore"
    }

    /** Builds the JSON snapshot, uploads it, prunes older files, records status. */
    suspend fun backupNow(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val json = backupRepository.buildBackupJson()
            val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now())
            val name = "$FILE_PREFIX$stamp$FILE_SUFFIX"
            val id = api.upload(name, json).getOrThrow()
            pruneOld()
            val stampHuman = DateTimeFormatter.ofPattern("MMM d, HH:mm", java.util.Locale.getDefault())
                .withZone(ZoneOffset.UTC).format(Instant.now())
            metaDao.put(MetaEntity(KEY_LAST_BACKUP, "Backed up $stampHuman"))
            metaDao.put(MetaEntity(KEY_LAST_BACKUP_AT, System.currentTimeMillis().toString()))
            id
        }
    }

    /** Downloads the newest backup file and restores it. Returns the restored count. */
    suspend fun restoreLatest(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val newest = newestFile() ?: error("No Drive backup found")
            val text = api.download(newest.id!!).getOrThrow()
            val n = backupRepository.restoreBackupJson(text)
            if (n < 0) error("Backup file is not a valid Orvane backup")
            val stampHuman = DateTimeFormatter.ofPattern("MMM d, HH:mm", java.util.Locale.getDefault())
                .withZone(ZoneOffset.UTC).format(Instant.now())
            metaDao.put(MetaEntity(KEY_LAST_RESTORE, "Restored $n items $stampHuman"))
            n
        }
    }

    /** Human-readable status line for the Settings card. */
    suspend fun lastBackupLabel(): String? =
        metaDao.get(KEY_LAST_BACKUP)?.value ?: metaDao.get(KEY_LAST_RESTORE)?.value

    /** Epoch millis of the last successful backup, or null when never backed up. */
    suspend fun lastBackupAt(): Long? = metaDao.get(KEY_LAST_BACKUP_AT)?.value?.toLongOrNull()

    private suspend fun newestFile(): DriveBackupModels.DriveFile? =
        api.listFiles().getOrThrow()
            .filter { it.id != null && it.name != null }
            .maxByOrNull { it.name!! }

    /** Deletes older backups beyond [KEEP]. */
    private suspend fun pruneOld() {
        val files = api.listFiles().getOrThrow()
            .filter { it.id != null && it.name != null }
            .sortedByDescending { it.name!! }
        for (f in files.drop(KEEP)) {
            runCatching { api.delete(f.id!!) }
        }
    }

    /** True when [DriveBackupModels.parseMillis] likes the modifiedTime (used by tests). */
    fun modifiedTimeMillis(f: DriveBackupModels.DriveFile): Long? =
        DriveBackupModels.parseMillis(f.modifiedTime)
}
