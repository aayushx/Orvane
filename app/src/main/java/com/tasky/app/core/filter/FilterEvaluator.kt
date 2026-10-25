package com.tasky.app.core.filter

import com.tasky.app.data.db.DateRangeType
import com.tasky.app.data.db.FilterEntity
import com.tasky.app.data.db.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * In-memory evaluation of a custom filter against tasks (with their tags resolved).
 * Mirrors what the repository serves for FilterEntity-driven views.
 */
object FilterEvaluator {

    data class TaskView(
        val task: TaskEntity,
        val tagIds: List<String>,
    )

    fun matches(
        filter: FilterEntity,
        view: TaskView,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val t = view.task
        if (t.parentId != null) return false
        if (t.deletedAt != null) return false
        if (t.completedAt != null) return false
        if (filter.listId != null && t.listId != filter.listId) return false
        if (filter.minPriority > 0 && t.priority < filter.minPriority) return false
        if (filter.tagId != null && !view.tagIds.contains(filter.tagId)) return false

        val today = dateOf(now, zone)
        when (filter.dateType) {
            DateRangeType.TODAY -> {
                if (t.due == null) return false
                val day = dateOfMillis(t.due, zone)
                if (!(day == today || day.isBefore(today))) return false
            }
            DateRangeType.NEXT_7_DAYS -> {
                if (t.due == null) return false
                val day = dateOfMillis(t.due, zone)
                if (day.isAfter(today.plusDays(6))) return false
            }
            DateRangeType.OVERDUE -> {
                if (t.due == null) return false
                if (!dateOfMillis(t.due, zone).isBefore(today)) return false
            }
            DateRangeType.CUSTOM -> {
                if (filter.dueStart != null && filter.dueEnd != null && t.due == null) return false
                filter.dueStart?.let { start -> if (t.due != null && t.due < start) return false }
                filter.dueEnd?.let { end -> if (t.due != null && t.due >= end) return false }
            }
            else -> {}
        }
        return true
    }

    fun filterAll(
        filter: FilterEntity,
        tasks: List<TaskView>,
        now: Instant,
    ): List<TaskView> = tasks.filter { matches(filter, it, now) }

    private fun dateOf(instant: Instant, zone: ZoneId): LocalDate =
        instant.atZone(zone).toLocalDate()

    private fun dateOfMillis(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
