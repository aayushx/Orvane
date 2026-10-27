package com.orvane.app.core.ai

import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Minimal OpenAI-compatible chat-completions client. Only used when the user
 * configures a base URL + key in Settings; every failure degrades to plain
 * offline behaviour (returns null, caller shows a friendly message).
 */
@Singleton
class AiClient @Inject constructor() {

    data class Suggestion(val title: String, val subtasks: List<String>)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Expands a raw task line into a clean title + up to 5 subtask steps using
     * an OpenAI-compatible endpoint. Never throws; null on any failure.
     */
    suspend fun suggestTask(raw: String, baseUrl: String, apiKey: String, model: String): Suggestion? =
        withContext(Dispatchers.IO) {
            val url = baseUrl.trim()
            val key = apiKey.trim()
            if (url.isBlank() || key.isBlank()) return@withContext null
            try {
                val response = post(
                    url.trimEnd('/') + "/chat/completions",
                    body = buildJsonObject {
                        put("model", model.ifBlank { "gpt-4o-mini" })
                        put("temperature", 0.2)
                        put("max_tokens", 300)
                        putJsonArray("messages") {
                            add(buildJsonObject { put("role", "system"); put("content", SYSTEM_PROMPT) })
                            add(buildJsonObject { put("role", "user"); put("content", raw.take(500)) })
                        }
                    },
                    apiKey = key,
                ) ?: return@withContext null
                val content = response.firstChoiceContent() ?: return@withContext null
                parseSuggestion(content)
            } catch (e: Exception) {
                null
            }
        }

    private fun post(url: String, body: JsonObject, apiKey: String): JsonObject? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 12_000
            conn.readTimeout = 30_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.outputStream.use {
                it.write(json.encodeToString(JsonObject.serializer(), body).toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code !in 200..299) return null
            conn.inputStream.use { inp ->
                json.parseToJsonElement(inp.readBytes().toString(Charsets.UTF_8)) as? JsonObject
            }
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun JsonObject.firstChoiceContent(): String? =
        (this["choices"] as? kotlinx.serialization.json.JsonArray)
            ?.firstOrNull()
            ?.let { it as? JsonObject }
            ?.get("message")
            ?.let { it as? JsonObject }
            ?.get("content")
            ?.let { it as? JsonPrimitive }
            ?.contentOrNull

    /** Accepts either a JSON object {"title":…,"subtasks":[…]} or plain lines. */
    fun parseSuggestion(content: String): Suggestion? {
        val text = content.trim()
        if (text.isEmpty()) return null
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start >= 0 && end > start) {
            val parsed = runCatching {
                json.parseToJsonElement(text.substring(start, end + 1)) as? JsonObject
            }.getOrNull()
            if (parsed != null) {
                val title = (parsed["title"] as? JsonPrimitive)?.contentOrNull
                    ?.takeIf { it.isNotBlank() }
                val subtasks = (parsed["subtasks"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    ?.filter { it.isNotBlank() }
                    ?.take(5)
                    ?: emptyList()
                if (title != null || subtasks.isNotEmpty()) {
                    return Suggestion(title ?: text.lineSequence().first().take(80), subtasks)
                }
            }
        }
        // Plain text: first line is the title, following lines are subtasks.
        val lines = text.lines().filter { it.isNotBlank() }
        return Suggestion(lines.first().take(80), lines.drop(1).take(5))
    }

    private companion object {
        val SYSTEM_PROMPT = """
            You help expand quick task entries. Reply with strict JSON only:
            {"title":"clean short task title","subtasks":["step 1","step 2"]}
            Keep the title under 80 characters and give at most 5 concrete subtask steps.
            If the entry is already simple, subtasks may be empty.
        """.trimIndent()
    }
}
