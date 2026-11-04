package com.orvane.app.data.google

import com.orvane.app.core.google.AccessTokenProvider
import com.orvane.app.core.google.GoogleCalendarModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * G4: Google Calendar REST v3 client over HttpURLConnection (no extra HTTP
 * dependency). Everything the sync engine needs, behind an interface so tests
 * can fake it.
 */
interface GoogleCalendarApi {

    /** One page of events; [syncToken] enables incremental listing, [timeMin] bounds full pulls. */
    suspend fun listEvents(
        calendarId: String,
        syncToken: String? = null,
        timeMin: String? = null,
        pageToken: String? = null,
    ): Result<GoogleCalendarModels.EventsPage>

    /** Creates (or returns the existing id of) a calendar with the given name. */
    suspend fun ensureCalendar(summary: String): Result<String>

    suspend fun insertEvent(calendarId: String, event: GoogleCalendarModels.Event): Result<GoogleCalendarModels.Event>

    suspend fun patchEvent(calendarId: String, eventId: String, event: GoogleCalendarModels.Event): Result<GoogleCalendarModels.Event>

    suspend fun deleteEvent(calendarId: String, eventId: String): Result<Unit>
}

@Singleton
class HttpGoogleCalendarApi @Inject constructor(
    private val tokenProvider: AccessTokenProvider,
) : GoogleCalendarApi {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    override suspend fun listEvents(
        calendarId: String,
        syncToken: String?,
        timeMin: String?,
        pageToken: String?,
    ): Result<GoogleCalendarModels.EventsPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val q = mutableListOf("maxResults=250", "singleEvents=false", "showDeleted=true")
                if (syncToken != null) q.add("syncToken=" + URLEncoder.encode(syncToken, "UTF-8"))
                if (timeMin != null) q.add("timeMin=" + URLEncoder.encode(timeMin, "UTF-8"))
                if (pageToken != null) q.add("pageToken=" + URLEncoder.encode(pageToken, "UTF-8"))
                val path = "${GoogleCalendarModels.API_BASE}/calendars/$calendarId/events?${q.joinToString("&")}"
                val body = get(path) ?: error("Not signed in")
                json.decodeFromString<GoogleCalendarModels.EventsPage>(body)
            }
        }

    override suspend fun ensureCalendar(summary: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // List calendars first; reuse when one with this summary exists.
            val listPath = "${GoogleCalendarModels.API_BASE}/users/me/calendarList"
            val listBody = get(listPath) ?: error("Not signed in")
            val list = json.decodeFromString<CalendarListResponse>(listBody)
            list.items.firstOrNull { it.summary == summary }?.id
                ?: run {
                    val created = post(
                        "${GoogleCalendarModels.API_BASE}/calendars",
                        json.encodeToString(NewCalendar(summary = summary)),
                    )
                    json.decodeFromString<NewCalendar>(created).id ?: error("Calendar creation returned no id")
                }
        }
    }

    override suspend fun insertEvent(calendarId: String, event: GoogleCalendarModels.Event): Result<GoogleCalendarModels.Event> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = post(
                    "${GoogleCalendarModels.API_BASE}/calendars/$calendarId/events",
                    json.encodeToString(event),
                )
                json.decodeFromString<GoogleCalendarModels.Event>(body)
            }
        }

    override suspend fun patchEvent(calendarId: String, eventId: String, event: GoogleCalendarModels.Event): Result<GoogleCalendarModels.Event> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = patch(
                    "${GoogleCalendarModels.API_BASE}/calendars/$calendarId/events/$eventId",
                    json.encodeToString(event),
                )
                json.decodeFromString<GoogleCalendarModels.Event>(body)
            }
        }

    override suspend fun deleteEvent(calendarId: String, eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            delete("${GoogleCalendarModels.API_BASE}/calendars/$calendarId/events/$eventId")
        }
    }

    // ---- HTTP plumbing ----

    @kotlinx.serialization.Serializable
    data class CalendarListResponse(val items: List<NewCalendar> = emptyList())

    @kotlinx.serialization.Serializable
    data class NewCalendar(val id: String? = null, val summary: String)

    private suspend fun get(path: String): String? {
        val token = tokenProvider.accessToken(null) ?: return null
        return request(path, "GET", null, token)
    }

    private suspend fun post(path: String, body: String): String {
        val token = tokenProvider.accessToken(null) ?: error("Not signed in")
        return request(path, "POST", body, token)
    }

    private suspend fun patch(path: String, body: String): String {
        val token = tokenProvider.accessToken(null) ?: error("Not signed in")
        return request(path, "PATCH", body, token)
    }

    private suspend fun delete(path: String) {
        val token = tokenProvider.accessToken(null) ?: error("Not signed in")
        request(path, "DELETE", null, token)
    }

    private fun request(path: String, method: String, body: String?, token: String): String {
        val conn = URL(path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.readBytes()?.toString(Charsets.UTF_8).orEmpty()
            if (code !in 200..299) {
                throw IOException("HTTP $code from $method $path: ${text.take(300)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }
}
