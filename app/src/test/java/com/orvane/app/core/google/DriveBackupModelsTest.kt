package com.orvane.app.core.google

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * G5: pure parsing of the Drive REST v3 file list payloads.
 */
class DriveBackupModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun fileList_parsesFiles() {
        val body = """
            {
              "kind": "drive#fileList",
              "files": [
                {"id": "abc", "name": "orvane-drive-backup-20261104-120000.json",
                 "modifiedTime": "2026-11-04T12:00:00.000Z"},
                {"id": "def", "name": "orvane-drive-backup-20261103-120000.json",
                 "modifiedTime": "2026-11-03T12:00:00.000Z"}
              ]
            }
        """.trimIndent()
        val list = json.decodeFromString<DriveBackupModels.FileList>(body)
        assertEquals(2, list.files.size)
        assertEquals("abc", list.files[0].id)
        assertEquals("orvane-drive-backup-20261104-120000.json", list.files[0].name)
    }

    @Test
    fun parseMillis_rfc3339() {
        assertEquals(
            1_793_793_600_000L,
            DriveBackupModels.parseMillis("2026-11-04T12:00:00.000Z"),
        )
    }

    @Test
    fun parseMillis_nullOrGarbage_isNull() {
        assertNull(DriveBackupModels.parseMillis(null))
        assertNull(DriveBackupModels.parseMillis("not-a-date"))
    }
}
