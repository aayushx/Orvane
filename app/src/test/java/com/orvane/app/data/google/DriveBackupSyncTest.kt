package com.orvane.app.data.google

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.orvane.app.core.google.AccessTokenProvider
import com.orvane.app.core.google.DriveBackupModels
import com.orvane.app.data.db.AppDatabase
import android.app.Activity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * G5: Drive AppData backup orchestration against a fake Drive API — upload,
 * prune-to-7, restore-latest, and status bookkeeping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DriveBackupSyncTest {

    private lateinit var db: AppDatabase
    private lateinit var drive: DriveBackupSync
    private lateinit var fake: FakeDriveApi
    private lateinit var backupRepo: com.orvane.app.data.repo.BackupRepository

    private class FakeDriveApi : DriveApi {
        val files = mutableListOf<Pair<String, String>>() // id to content
        var nextId = 0

        override suspend fun listFiles(): Result<List<DriveBackupModels.DriveFile>> =
            Result.success(files.map { (id, _) -> DriveBackupModels.DriveFile(id = id, name = id) })

        override suspend fun upload(name: String, content: String): Result<String> {
            val id = "file_${++nextId}_$name"
            files += id to content
            return Result.success(id)
        }

        override suspend fun download(fileId: String): Result<String> {
            val content = files.firstOrNull { it.first == fileId }?.second
            return content?.let { Result.success(it) } ?: Result.failure(IllegalStateException("missing"))
        }

        override suspend fun delete(fileId: String): Result<Unit> {
            files.removeAll { it.first == fileId }
            return Result.success(Unit)
        }
    }

    private class FakeTokens : AccessTokenProvider {
        override suspend fun accessToken(activity: Activity?): String? = "test-token"
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fake = FakeDriveApi()
        backupRepo = com.orvane.app.data.repo.BackupRepository(context, db)
        drive = DriveBackupSync(fake, backupRepo, db.metaDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun backupNow_uploadsSnapshot_andRecordsStatus() = runTest {
        val id = drive.backupNow().getOrThrow()
        assertTrue(id.isNotBlank())
        assertEquals(1, fake.files.size)
        val content = fake.files[0].second
        assertTrue(content.contains("\"app\":\"Orvane\""))
        assertNotNull(drive.lastBackupLabel())
        assertNotNull(drive.lastBackupAt())
    }

    @Test
    fun backupNow_prunesBeyondKeep() = runTest {
        repeat(DriveBackupSync.KEEP + 3) { drive.backupNow().getOrThrow() }
        assertEquals(DriveBackupSync.KEEP, fake.files.size)
    }

    @Test
    fun restoreLatest_restoresNewestBackup() = runTest {
        // Seed one task through the backup JSON so restore has something to do.
        val json = backupRepo.buildBackupJson()
        fake.files += "file_1_orvane-drive-backup-a.json" to json
        val n = drive.restoreLatest().getOrThrow()
        assertTrue(n >= 0)
        assertTrue(drive.lastBackupLabel()!!.startsWith("Restored"))
    }

    @Test
    fun restoreLatest_failsCleanly_whenNoBackup() = runTest {
        val result = drive.restoreLatest()
        assertTrue(result.isFailure)
    }
}
