package com.tasky.app.data.repo

import android.content.Context
import android.net.Uri
import com.tasky.app.data.db.AppDatabase
import com.tasky.app.data.db.CheckStatus
import com.tasky.app.data.db.DateRangeType
import com.tasky.app.data.db.FilterEntity
import com.tasky.app.data.db.FolderEntity
import com.tasky.app.data.db.HabitCheckEntity
import com.tasky.app.data.db.HabitEntity
import com.tasky.app.data.db.HabitFreqType
import com.tasky.app.data.db.ListEntity
import com.tasky.app.data.db.ReminderEntity
import com.tasky.app.data.db.TaskEntity
import com.tasky.app.data.db.TagEntity
import com.tasky.app.data.db.TaskPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/** Backup document shape version. */
const val BACKUP_FORMAT = 1

/**
 * Full JSON backup / restore of user data plus CSV export of tasks. File
 * contents are read and written through SAF [Uri]s supplied by the user, so
 * no storage permission is needed.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val db: AppDatabase,
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---------- Build JSON ----------

    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        val tagRefs = db.tagDao().observeRefs().first()
        val root = buildJsonObject {
            put("format", BACKUP_FORMAT)
            put("exportedAt", System.currentTimeMillis())
            put("app", "Tasky")
            put("lists", arr(db.listDao().all()) { l ->
                buildJsonObject {
                    put("id", l.id); put("name", l.name); put("color", l.color)
                    l.groupId?.let { put("groupId", it) }
                    put("sortOrder", l.sortOrder); put("collapsed", l.collapsed)
                }
            })
            put("folders", arr(db.folderDao().all()) { f ->
                buildJsonObject {
                    put("id", f.id); put("name", f.name)
                    put("sortOrder", f.sortOrder); put("collapsed", f.collapsed)
                }
            })
            put("tags", arr(db.tagDao().all()) { t ->
                buildJsonObject { put("id", t.id); put("name", t.name); put("color", t.color) }
            })
            put("taskTags", arr(tagRefs) { r ->
                buildJsonObject { put("taskId", r.taskId); put("tagId", r.tagId) }
            })
            put("filters", arr(db.filterDao().all()) { f ->
                buildJsonObject {
                    put("id", f.id); put("name", f.name)
                    f.listId?.let { put("listId", it) }
                    f.tagId?.let { put("tagId", it) }
                    put("minPriority", f.minPriority); put("dateType", f.dateType)
                    f.dueStart?.let { put("dueStart", it) }
                    f.dueEnd?.let { put("dueEnd", it) }
                    put("sortOrder", f.sortOrder)
                }
            })
            put("tasks", arr(db.taskDao().allTasks()) { t ->
                buildJsonObject {
                    put("id", t.id); t.parentId?.let { put("parentId", it) }
                    put("title", t.title); put("note", t.note)
                    t.due?.let { put("due", it) }
                    put("isDueAllDay", t.isDueAllDay)
                    put("priority", t.priority)
                    t.listId?.let { put("listId", it) }
                    t.recurrenceRule?.let { put("recurrenceRule", it) }
                    t.completedAt?.let { put("completedAt", it) }
                    t.deletedAt?.let { put("deletedAt", it) }
                    put("createdAt", t.createdAt); put("modifiedAt", t.modifiedAt)
                    put("sortOrder", t.sortOrder)
                    put("focusSeconds", t.focusSeconds); put("pomoCount", t.pomoCount)
                }
            })
            put("reminders", arr(reminders()) { r ->
                buildJsonObject { put("id", r.id); put("taskId", r.taskId); put("at", r.at) }
            })
            put("habits", arr(db.habitDao().all()) { h ->
                buildJsonObject {
                    put("id", h.id); put("name", h.name); put("emoji", h.emoji)
                    put("color", h.color); h.category?.let { put("category", it) }
                    put("note", h.note); put("freqType", h.freqType)
                    put("weekdays", JsonArray(h.weekdays.map { kotlinx.serialization.json.JsonPrimitive(it) }))
                    put("timesPerWeek", h.timesPerWeek); put("timesPerMonth", h.timesPerMonth)
                    put("reminderEnabled", h.reminderEnabled)
                    put("reminderHour", h.reminderHour); put("reminderMinute", h.reminderMinute)
                    put("nagEnabled", h.nagEnabled); put("nagHour", h.nagHour); put("nagMinute", h.nagMinute)
                    put("archived", h.archived); put("sortOrder", h.sortOrder); put("createdAt", h.createdAt)
                }
            })
            put("habitChecks", arr(db.habitCheckDao().all()) { c ->
                buildJsonObject {
                    put("habitId", c.habitId); put("epochDay", c.epochDay)
                    put("status", c.status); put("createdAt", c.createdAt)
                }
            })
            put("focusSessions", arr(db.focusSessionDao().all()) { s ->
                buildJsonObject {
                    put("id", s.id); s.taskId?.let { put("taskId", it) }
                    put("type", s.type); put("startedAt", s.startedAt)
                    put("endedAt", s.endedAt); put("seconds", s.seconds)
                    put("countedTaskSeconds", s.countedTaskSeconds)
                }
            })
        }
        json.encodeToString(JsonObject.serializer(), root)
    }

    private suspend fun reminders(): List<ReminderEntity> = db.reminderDao().future(0L)

    private fun <T> arr(list: List<T>, transform: (T) -> JsonObject): JsonArray =
        buildJsonArray { for (item in list) add(transform(item)) }

    // ---------- Restore JSON ----------

    /** Returns the number of top-level tasks restored, or -1 when invalid / not a Tasky backup. */
    suspend fun restoreBackupJson(text: String): Int = withContext(Dispatchers.IO) {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return@withContext -1
        }
        val format = root["format"]?.jsonPrimitive?.intOrNull
        val app = root["app"]?.jsonPrimitive?.contentOrNull
        if (format != BACKUP_FORMAT && app != "Tasky") return@withContext -1

        root.array("folders")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            db.folderDao().upsert(
                FolderEntity(
                    id = id,
                    name = o.str("name") ?: "?",
                    sortOrder = o.int("sortOrder") ?: 0,
                    collapsed = o.bool("collapsed") ?: false,
                ),
            )
        }
        root.array("lists")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            db.listDao().upsert(
                ListEntity(
                    id = id,
                    name = o.str("name") ?: "?",
                    color = o.int("color") ?: 0xFF4A5FDB.toInt(),
                    groupId = o.strOrNull("groupId"),
                    sortOrder = o.int("sortOrder") ?: 0,
                    collapsed = o.bool("collapsed") ?: false,
                ),
            )
        }
        root.array("tags")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            db.tagDao().upsert(
                TagEntity(id = id, name = o.str("name") ?: "?", color = o.int("color") ?: 0xFF7A5466.toInt()),
            )
        }
        root.array("taskTags")?.forEach { el ->
            val o = el.jsonObject
            val taskId = o.str("taskId") ?: return@forEach
            val tagId = o.str("tagId") ?: return@forEach
            db.tagDao().addRef(taskId, tagId)
        }
        root.array("filters")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            db.filterDao().upsert(
                FilterEntity(
                    id = id,
                    name = o.str("name") ?: "?",
                    listId = o.strOrNull("listId"),
                    tagId = o.strOrNull("tagId"),
                    minPriority = o.int("minPriority") ?: 0,
                    dateType = o.int("dateType") ?: DateRangeType.ANY,
                    dueStart = o.longOf("dueStart"),
                    dueEnd = o.longOf("dueEnd"),
                    sortOrder = o.int("sortOrder") ?: 0,
                ),
            )
        }
        root.array("tasks")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            db.taskDao().insert(
                TaskEntity(
                    id = id,
                    parentId = o.strOrNull("parentId"),
                    title = o.str("title") ?: "?",
                    note = o.strOrNull("note") ?: "",
                    due = o.longOf("due"),
                    isDueAllDay = o.bool("isDueAllDay") ?: true,
                    priority = o.int("priority") ?: TaskPriority.NONE,
                    listId = o.strOrNull("listId"),
                    recurrenceRule = o.strOrNull("recurrenceRule"),
                    completedAt = o.longOf("completedAt"),
                    deletedAt = o.longOf("deletedAt"),
                    createdAt = o.longOf("createdAt") ?: System.currentTimeMillis(),
                    modifiedAt = o.longOf("modifiedAt") ?: System.currentTimeMillis(),
                    sortOrder = o.int("sortOrder") ?: 0,
                    focusSeconds = o.longOf("focusSeconds") ?: 0L,
                    pomoCount = o.int("pomoCount") ?: 0,
                ),
            )
        }
        root.array("reminders")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            val taskId = o.str("taskId") ?: return@forEach
            val at = o.longOf("at") ?: return@forEach
            db.reminderDao().upsert(ReminderEntity(id = id, taskId = taskId, at = at))
        }
        root.array("habits")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            db.habitDao().upsert(
                HabitEntity(
                    id = id,
                    name = o.str("name") ?: "?",
                    emoji = o.strOrNull("emoji") ?: "✅",
                    color = o.int("color") ?: 0xFF4A5FDB.toInt(),
                    category = o.strOrNull("category"),
                    note = o.strOrNull("note") ?: "",
                    freqType = o.int("freqType") ?: HabitFreqType.DAILY,
                    weekdays = o.strList("weekdays") ?: emptyList(),
                    timesPerWeek = o.int("timesPerWeek") ?: 3,
                    timesPerMonth = o.int("timesPerMonth") ?: 10,
                    reminderEnabled = o.bool("reminderEnabled") ?: false,
                    reminderHour = o.int("reminderHour") ?: 8,
                    reminderMinute = o.int("reminderMinute") ?: 0,
                    nagEnabled = o.bool("nagEnabled") ?: true,
                    nagHour = o.int("nagHour") ?: 21,
                    nagMinute = o.int("nagMinute") ?: 0,
                    archived = o.bool("archived") ?: false,
                    sortOrder = o.int("sortOrder") ?: 0,
                    createdAt = o.longOf("createdAt") ?: System.currentTimeMillis(),
                ),
            )
        }
        root.array("habitChecks")?.forEach { el ->
            val o = el.jsonObject
            val habitId = o.str("habitId") ?: return@forEach
            val epochDay = o.longOf("epochDay") ?: return@forEach
            db.habitCheckDao().upsert(
                HabitCheckEntity(
                    habitId = habitId,
                    epochDay = epochDay,
                    status = o.int("status") ?: CheckStatus.CHECKED,
                    createdAt = o.longOf("createdAt") ?: System.currentTimeMillis(),
                ),
            )
        }
        root.array("focusSessions")?.forEach { el ->
            val o = el.jsonObject
            val id = o.str("id") ?: return@forEach
            val startedAt = o.longOf("startedAt") ?: return@forEach
            val endedAt = o.longOf("endedAt") ?: return@forEach
            db.focusSessionDao().upsert(
                com.tasky.app.data.db.FocusSessionEntity(
                    id = id,
                    taskId = o.strOrNull("taskId"),
                    type = o.int("type") ?: 0,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    seconds = o.longOf("seconds") ?: 0,
                    countedTaskSeconds = o.bool("countedTaskSeconds") ?: true,
                ),
            )
        }
        db.taskDao().allTasks().count { it.parentId == null }
    }

    // ---------- CSV ----------

    /** CSV: header + one line per task; subtasks appear indented under parents. */
    suspend fun buildTasksCsv(): String = withContext(Dispatchers.IO) {
        val tasks = db.taskDao().allTasks()
        val byParent = tasks.filter { it.parentId != null }.groupBy { it.parentId!! }
        val listNames = db.listDao().all().associate { it.id to it.name }
        val sb = StringBuilder()
        sb.append("id,title,note,due,priority,list,tags,completed\n")
        for (t in tasks.filter { it.parentId == null }) {
            sb.append(csvRow(t, listNames[t.listId] ?: ""))
            for (sub in byParent[t.id].orEmpty()) {
                sb.append(csvRow(sub, listNames[sub.listId] ?: "", t.tagsOrEmpty(db), prefix = "  - "))
            }
        }
        sb.toString()
    }

    private suspend fun TaskEntity.tagsOrEmpty(db: AppDatabase): String =
        db.tagDao().forTask(id).joinToString(";") { it.name }

    private suspend fun csvRow(
        t: TaskEntity,
        listName: String,
        tags: String = "",
        prefix: String = "",
    ): String {
        val cells = listOf(
            t.id,
            prefix + t.title,
            t.note,
            t.due?.toString() ?: "",
            t.priority.toString(),
            listName,
            tags,
            t.completedAt?.toString() ?: "",
        )
        return cells.joinToString(",") { csvEscape(it) } + "\n"
    }

    private fun csvEscape(value: String): String =
        if (value.contains('"') || value.contains(',') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    // ---------- SAF I/O ----------

    /** Writes UTF-8 text to a user-chosen document; false on failure. */
    suspend fun writeToUri(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            appContext.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
    }

    // ---------- JSON element helpers ----------

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.strOrNull(key: String): String? {
        val v = this[key] ?: return null
        if (v is JsonNull) return null
        return v.jsonPrimitive.contentOrNull.takeIf { it != "null" }
    }

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.longOf(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.strList(key: String): List<String>? =
        (this[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }

    private fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray
}
