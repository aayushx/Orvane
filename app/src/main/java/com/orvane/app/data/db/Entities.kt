package com.orvane.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Task priority levels. */
object TaskPriority {
    const val NONE = 0
    const val LOW = 1
    const val MEDIUM = 2
    const val HIGH = 3
}

/** Trash retention policy. */
object TaskTrash {
    const val RETENTION_DAYS = 30L
}

/** Habit frequency types. */
object HabitFreqType {
    const val DAILY = 0
    const val WEEKDAYS = 1      // specific weekdays from [weekdays]
    const val WEEK_COUNT = 2    // timesPerWeek times per week, any day
    const val MONTH_COUNT = 3   // timesPerMonth times per month, any day
}

/** Habit check statuses. */
object CheckStatus {
    const val CHECKED = 0
    const val SKIPPED = 1
}

/** Focus session types. */
object SessionType {
    const val POMODORO_FOCUS = 0
    const val POMODORO_BREAK = 1
    const val STOPWATCH = 2
}

/** Calendar event sources. */
object EventSource {
    const val LOCAL = 0
    const val DEVICE = 1
    const val ICS = 2
}

/**
 * Tasks and subtasks share one table; subtasks are tasks with non-null [parentId]
 * (unlimited nesting depth).
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("parentId"),
        Index("listId"),
        Index("due"),
        Index("completedAt"),
        Index("deletedAt"),
        Index("sortOrder"),
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val parentId: String? = null,
    val title: String,
    val note: String = "",
    val due: Long? = null,             // epoch millis
    val isDueAllDay: Boolean = true,   // when due only has date meaning
    val priority: Int = TaskPriority.NONE,
    val listId: String? = null,        // null = Inbox
    val recurrenceRule: String? = null,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,       // in trash
    val createdAt: Long,
    val modifiedAt: Long,
    val sortOrder: Int = 0,            // manual ordering inside parent/list
    val focusSeconds: Long = 0L,       // accumulated focus time
    val pomoCount: Int = 0,
)

@Entity(tableName = "reminders", indices = [Index("taskId"), Index("at")])
data class ReminderEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val at: Long,                       // epoch millis of the alarm
)

@Entity(tableName = "lists", indices = [Index("groupId")])
data class ListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int = 0xFF4A5FDB.toInt(),
    val groupId: String? = null,        // folder
    val sortOrder: Int = 0,
    val collapsed: Boolean = false,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val collapsed: Boolean = false,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int = 0xFF7A5466.toInt(),
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    indices = [Index("taskId"), Index("tagId")]
)
data class TaskTagCrossRef(
    val taskId: String,
    val tagId: String,
)

@Entity(tableName = "filters")
data class FilterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val listId: String? = null,
    val tagId: String? = null,
    val minPriority: Int = 0,
    val dateType: Int = DateRangeType.ANY,
    val dueStart: Long? = null,
    val dueEnd: Long? = null,
    val sortOrder: Int = 0,
)

@Entity(tableName = "habits", indices = [Index("sortOrder")])
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String = "✅",
    val color: Int = 0xFF4A5FDB.toInt(),
    val category: String? = null,
    val note: String = "",
    val freqType: Int = HabitFreqType.DAILY,
    val weekdays: List<String> = emptyList(),  // e.g. ["MO","WE"] for specific weekdays
    val timesPerWeek: Int = 3,                 // for WEEK_COUNT
    val timesPerMonth: Int = 10,               // for MONTH_COUNT
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val nagEnabled: Boolean = true,
    val nagHour: Int = 21,
    val nagMinute: Int = 0,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
)

@Entity(
    tableName = "habit_checks",
    indices = [Index("habitId"), Index("epochDay"), Index(value = ["habitId", "epochDay"], unique = true)]
)
data class HabitCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val epochDay: Long,                 // which day this check-in counts for
    val status: Int = CheckStatus.CHECKED,
    val createdAt: Long,
)

@Entity(tableName = "focus_sessions", indices = [Index("taskId"), Index("startedAt")])
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val taskId: String? = null,
    val type: Int = SessionType.POMODORO_FOCUS,
    val startedAt: Long,
    val endedAt: Long,
    val seconds: Long,
    val countedTaskSeconds: Boolean = true,
)

/**
 * Calendar events. Distinct entity from tasks.
 * source LOCAL: user-created inside Orvane, DEVICE: mirrored android CalendarContract read-only,
 * ICS: read-only subscription feed.
 */
@Entity(
    tableName = "calendar_events",
    indices = [Index("start"), Index("end"), Index("source"), Index("deviceId"), Index("feedId")]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String = "",
    val location: String? = null,
    val start: Long,
    val end: Long,
    val allDay: Boolean = false,
    val source: Int = EventSource.LOCAL,
    val deviceId: Long? = null,           // CalendarContract.Events._ID
    val deviceCalendarId: Long? = null,
    val feedId: String? = null,           // IcsFeedEntity.id when source==ICS
    val linkedTaskId: String? = null,     // task that was blocked into a slot
)

@Entity(tableName = "ics_feeds", indices = [Index(value = ["url"], unique = true)])
data class IcsFeedEntity(
    @PrimaryKey val id: String,
    val url: String,
    val name: String,
    val color: Int = 0xFF006C4C.toInt(),
    val enabled: Boolean = true,
    val lastSync: Long? = null,
)

@Entity(tableName = "attachments", indices = [Index("taskId")])
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val fileName: String,
    val mimeType: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "kv_meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String = "",
)
