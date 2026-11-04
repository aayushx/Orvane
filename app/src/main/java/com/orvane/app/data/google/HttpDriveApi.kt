package com.orvane.app.data.google

import com.orvane.app.core.google.AccessTokenProvider
import com.orvane.app.core.google.DriveBackupModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G5: Google Drive REST v3 client for the appDataFolder space — the hidden
 * per-app folder only this app can see. Behind an interface so tests fake it.
 */
interface DriveApi {

    /** Lists backup files in appDataFolder, newest name first is not guaranteed — sort by name. */
    suspend fun listFiles(): Result<List<DriveBackupModels.DriveFile>>

    /** Uploads [content] as a new file with [name] in appDataFolder; returns the file id. */
    suspend fun upload(name: String, content: String): Result<String>

    /** Downloads a file's content. */
    suspend fun download(fileId: String): Result<String>

    /** Permanently deletes a file. */
    suspend fun delete(fileId: String): Result<Unit>
}

@Singleton
class HttpDriveApi @Inject constructor(
    private val tokenProvider: AccessTokenProvider,
) : DriveApi {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun listFiles(): Result<List<DriveBackupModels.DriveFile>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = "name contains 'orvane-drive-backup'"
                val url = "${DriveBackupModels.API_BASE}/files" +
                    "?spaces=${DriveBackupModels.APP_DATA_FOLDER}" +
                    "&fields=" + URLEncoder.encode("files(id,name,modifiedTime)", "UTF-8") +
                    "&pageSize=100" +
                    "&q=" + URLEncoder.encode(q, "UTF-8")
                val body = request(url, "GET", null) ?: error("Not signed in")
                json.decodeFromString<DriveBackupModels.FileList>(body).files
            }
        }

    override suspend fun upload(name: String, content: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "${DriveBackupModels.UPLOAD_BASE}/files?uploadType=multipart"
                val boundary = "orvane_${System.currentTimeMillis()}"
                val metadata = """{"name":"$name","parents":["${DriveBackupModels.APP_DATA_FOLDER}"]}"""
                val body = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata).append("\r\n")
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(content).append("\r\n")
                    append("--$boundary--")
                }
                val response = request(url, "POST", body, multipartBoundary = boundary)
                    ?: error("Not signed in")
                json.parseToJsonElement(response).jsonObject["id"]?.jsonPrimitive?.content
                    ?: error("Upload returned no file id")
            }
        }

    override suspend fun download(fileId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${DriveBackupModels.API_BASE}/files/$fileId?alt=media"
            request(url, "GET", null) ?: error("Not signed in")
        }
    }

    override suspend fun delete(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            request("${DriveBackupModels.API_BASE}/files/$fileId", "DELETE", null)
            Unit
        }
    }

    private suspend fun token(): String =
        tokenProvider.accessToken(null) ?: throw IOException("Not signed in")

    private suspend fun request(
        url: String,
        method: String,
        body: String?,
        multipartBoundary: String? = null,
    ): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.setRequestProperty("Authorization", "Bearer ${token()}")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                if (multipartBoundary != null) {
                    conn.setRequestProperty("Content-Type", "multipart/related; boundary=$multipartBoundary")
                } else {
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.readBytes()?.toString(Charsets.UTF_8).orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code from $method $url: ${text.take(300)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
