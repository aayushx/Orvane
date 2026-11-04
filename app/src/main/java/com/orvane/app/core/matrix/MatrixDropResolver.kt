package com.orvane.app.core.matrix

import com.orvane.app.core.util.Time
import java.time.LocalDate
import java.time.ZoneId

/**
 * F9: pure resolver for Eisenhower-matrix drag-drops.
 *
 * Current behavior (unchanged bucketing): a task is URGENT when its due date
 * falls within the next 2 days (and is not older than ~24h ago), and IMPORTANT
 * when its priority is Medium or High. Dropping a chip into a quadrant used to
 * rewrite due AND priority unconditionally; v1.3.0 refines this:
 *
 *  - Priority ALWAYS snaps to the quadrant's importance level (important
 *    quadrants → Medium, not-important → Low).
 *  - The due date changes ONLY when the drop crosses the urgency axis:
 *      · entering Urgent   → due today (end of day);
 *      · entering Not-urgent → if the task's date falls within 2 days, move it
 *        to +7 days; otherwise keep it (no-date tasks stay dateless);
 *      · same-column drops never touch the date.
 */
object MatrixDropResolver {

    enum class MatrixTarget(val urgent: Boolean, val important: Boolean, val label: String) {
        DO_FIRST(true, true, "Urgent"),
        SCHEDULE(false, true, "Not urgent"),
        DELEGATE(true, false, "Delegate"),
        LATER(false, false, "Later"),
    }

    data class Result(
        val newPriority: Int,
        /** The due to persist; null when the task keeps having no due date. */
        val newDueMillis: Long?,
        /** True only when the drop actually changed the due date. */
        val dueChanged: Boolean,
        /** Snackbar text, e.g. "Moved to Urgent · due today". */
        val description: String,
        /** Pre-drop values, for Undo. */
        val oldPriority: Int,
        val oldDueMillis: Long?,
    )

    fun resolve(
        taskPriority: Int,
        taskDueMillis: Long?,
        taskCurrentlyUrgent: Boolean,
        target: MatrixTarget,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Result {
        val p = com.orvane.app.data.db.TaskPriority
        val newPriority = if (target.important) p.MEDIUM else p.LOW

        val crossesUrgency = taskCurrentlyUrgent != target.urgent
        var newDue: Long? = taskDueMillis
        var dueChanged = false
        if (crossesUrgency) {
            if (target.urgent) {
                newDue = Time.at(today, 23, 59)
                // "Due today" only counts as a change when the day differs.
                dueChanged = taskDueMillis == null ||
                    java.time.Instant.ofEpochMilli(taskDueMillis).atZone(zone).toLocalDate() != today
            } else {
                val withinTwoDays = taskDueMillis != null &&
                    taskDueMillis < Time.startOfDayEpoch(today.plusDays(2))
                if (withinTwoDays) {
                    newDue = Time.startOfDayEpoch(today.plusDays(7))
                    dueChanged = true
                }
            }
        }

        val description = buildString {
            append("Moved to ").append(target.label)
            if (dueChanged) {
                append(" · ").append(if (target.urgent) "due today" else "due in 7 days")
            }
        }
        return Result(
            newPriority = newPriority,
            newDueMillis = newDue,
            dueChanged = dueChanged,
            description = description,
            oldPriority = taskPriority,
            oldDueMillis = taskDueMillis,
        )
    }
}
