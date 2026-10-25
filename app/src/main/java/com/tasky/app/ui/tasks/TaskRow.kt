package com.tasky.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tasky.app.core.util.Time
import com.tasky.app.data.db.TaskEntity
import com.tasky.app.data.db.TaskPriority
import com.tasky.app.ui.common.Format

fun priorityColor(priority: Int): Color = when (priority) {
    TaskPriority.HIGH -> Color(0xFFE53935)
    TaskPriority.MEDIUM -> Color(0xFFF6A623)
    TaskPriority.LOW -> Color(0xFF4CA6E8)
    else -> Color.Transparent
}

/** One task row: check circle, title, and meta chips (due, repeat, list, subtask progress). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: TaskEntity,
    listName: String?,
    subtaskTotal: Int,
    subtaskDone: Int,
    selected: Boolean,
    inSelectionMode: Boolean,
    completed: Boolean = task.completedAt != null,
    onToggleComplete: () -> Unit,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val selectedBg = MaterialTheme.colorScheme.secondaryContainer
    Surface(
        color = if (selected) selectedBg else surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onToggleSelect,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(
                        color = when {
                            completed -> MaterialTheme.colorScheme.primary
                            task.priority >= TaskPriority.LOW -> priorityColor(task.priority).copy(alpha = 0.18f)
                            else -> Color.Transparent
                        },
                        shape = if (task.priority == TaskPriority.NONE) CircleShape else RoundedCornerShape(20.dp),
                    )
                    .combinedClickable(
                        onClick = if (inSelectionMode) onToggleSelect else onToggleComplete,
                        onLongClick = onToggleSelect,
                    )
                    .semantics { contentDescription = if (completed) "Completed, mark not done" else "Mark done" },
                contentAlignment = Alignment.Center,
            ) {
                if (completed) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                } else if (task.priority >= TaskPriority.LOW) {
                    Box(
                        Modifier
                            .size(13.dp)
                            .background(priorityColor(task.priority), CircleShape),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                val chips = buildList {
                    task.due?.let {
                        val overdue = !completed && it < Time.startOfToday()
                        if (overdue) add("⚠ " + Format.dueChip(it, task.isDueAllDay))
                        else add(Format.dueChip(it, task.isDueAllDay))
                    }
                    if (task.recurrenceRule != null) add("\uD83D\uDD01 " + Format.repeatName(task.recurrenceRule))
                    listName?.let { add("\uD83D\uDCC2 $it") }
                    if (subtaskTotal > 0) add("$subtaskDone/$subtaskTotal")
                }
                if (chips.isNotEmpty()) {
                    Text(
                        chips.joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
    }
    // thin divider
    Box(
        Modifier
            .padding(start = 44.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

/** Header row for a section ("Overdue", "Today", "Completed"...). */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title + (count?.let { " ($it)" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (action != null) action()
    }
}
