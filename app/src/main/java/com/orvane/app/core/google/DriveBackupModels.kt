package com.orvane.app.core.google

/**
 * G5: minimal DTOs for the Google Drive REST API (v3) restricted to the
 * appDataFolder space. Pure parsing helpers, unit-testable.
 */
object DriveBackupModels {

    const val API_BASE = "https://www.googleapis.com/drive/v3"
    const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
    const val APP_DATA_FOLDER = "appDataFolder"

    @kotlinx.serialization.Serializable
    data class DriveFile(
        val id: String? = null,
        val name: String? = null,
        val modifiedTime: String? = null,
    )

    @kotlinx.serialization.Serializable
    data class FileList(val files: List<DriveFile> = emptyList())

    /** RFC3339 modifiedTime → epoch millis; null when unparseable. */
    fun parseMillis(rfc3339: String?): Long? {
        val raw = rfc3339 ?: return null
        return try {
            java.time.Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
