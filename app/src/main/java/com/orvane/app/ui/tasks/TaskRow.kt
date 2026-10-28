package com.orvane.app.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.db.TaskPriority
import com.orvane.app.ui.common.Format
import com.orvane.app.ui.theme.orvaneTokens
import com.orvane.app.ui.theme.priorityColor

/**
 * One task row (Round 2 / C1): animated check chip (scale bounce), title in
 * titleMedium, and a meta row with the color-coded priority flag, due chip
 * (red overdue / orange today / neutral upcoming), list color dot, outlined
 * tag chips, recurrence icon, and the "2/5" subtask mini-progress.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: TaskEntity,
    listName: String?,
    subtaskTotal: Int,
    subtaskDone: Int,
    selected: Boolean,
    inSelectionMode: Boolean,
    listColor: Int? = null,
    tags: List<String> = emptyList(),
    completed: Boolean = task.completedAt != null,
    onToggleComplete: () -> Unit,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = orvaneTokens()
    val titleColor by animateColorAsState(
        targetValue = if (completed) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(tokens.motion.shortMs, easing = tokens.motion.emphasizedEasing),
        label = "taskTitleColor",
    )
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onToggleSelect,
                )
                .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckChip(
                completed = completed,
                priority = task.priority,
                enabled = !inSelectionMode,
                onChange = onToggleComplete,
                onLongPress = onToggleSelect,
            )
            Spacer(Modifier.width(tokens.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                )
                TaskMetaRow(
                    task = task,
                    completed = completed,
                    listName = listName,
                    listColor = listColor,
                    tags = tags,
                    subtaskTotal = subtaskTotal,
                    subtaskDone = subtaskDone,
                )
            }
        }
    }
    Box(
        Modifier
            .padding(start = tokens.spacing.xl)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

/**
 * The round check control: filled + check when completed, tinted priority dot
 * when a priority is set, otherwise a quiet outlined circle. Completing plays
 * a spring scale bounce.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CheckChip(
    completed: Boolean,
    priority: Int,
    enabled: Boolean,
    onChange: () -> Unit,
    onLongPress: () -> Unit,
) {
    val tokens = orvaneTokens()
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(completed) {
        if (completed) {
            bounce.snapTo(1.25f)
            bounce.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        } else {
            bounce.snapTo(1f)
        }
    }
    val checkAlpha by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = tween(tokens.motion.shortMs, easing = tokens.motion.emphasizedEasing),
        label = "checkAlpha",
    )
    val checkColor = MaterialTheme.colorScheme.primary
    val chipColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .size(26.dp)
            .background(chipColor, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .combinedClickable(
                enabled = enabled,
                onClick = onChange,
                onLongClick = onLongPress,
            )
            .semantics {
                contentDescription = if (completed) "Completed, mark not done" else "Mark done"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (checkAlpha > 0f) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { alpha = checkAlpha },
            )
        } else if (priority >= TaskPriority.LOW) {
            Box(
                Modifier
                    .size(13.dp)
                    .background(priorityColor(priority), CircleShape),
            )
        }
    }
}

/** Meta information row under the title; everything is labeled for a11y dots/icons. */
@Composable
private fun TaskMetaRow(
    task: TaskEntity,
    completed: Boolean,
    listName: String?,
    listColor: Int?,
    tags: List<String>,
    subtaskTotal: Int,
    subtaskDone: Int,
) {
    val tokens = orvaneTokens()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val startToday = Time.startOfToday()
    val endToday = Time.endOfToday()
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (task.priority >= TaskPriority.LOW) {
            Flag(
                priority = task.priority,
            )
        }
        task.due?.let { due ->
            DuePill(
                due = due,
                isAllDay = task.isDueAllDay,
                overdue = !completed && due < startToday,
                today = due in startToday..endToday,
            )
        }
        if (task.recurrenceRule != null) {
            Icon(
                Icons.Filled.Repeat,
                contentDescription = "Repeats: " + Format.repeatName(task.recurrenceRule),
                tint = muted,
                modifier = Modifier
                    .size(13.dp)
                    .padding(top = 1.dp),
            )
        }
        listName?.let { name ->
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(listColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary),
            )
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                maxLines = 1,
            )
        }
        for (tag in tags) {
            OutlinedTagChip(tag)
        }
        if (subtaskTotal > 0) {
            Text(
                "$subtaskDone/$subtaskTotal",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}

/** Color-coded priority flag (theme token, never hardcoded). */
@Composable
private fun Flag(priority: Int) {
    Icon(
        Icons.Filled.Flag,
        contentDescription = "Priority: " + Format.priorityName(priority),
        tint = priorityColor(priority),
        modifier = Modifier.size(12.dp),
    )
}

/**
 * Due chip: red when overdue, orange when due today, neutral otherwise —
 * hue carries state, but never alone: the label itself stays readable.
 */
@Composable
private fun DuePill(
    due: Long,
    isAllDay: Boolean,
    overdue: Boolean,
    today: Boolean,
) {
    val tokens = orvaneTokens()
    val tint = when {
        overdue -> MaterialTheme.colorScheme.error
        today -> tokens.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        contentColor = tint,
    ) {
        Text(
            Format.dueChip(due, isAllDay),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = 2.dp),
        )
    }
}

/** Outlined tag chip (#name) with a hairline border. */
@Composable
private fun OutlinedTagChip(name: String) {
    val tokens = orvaneTokens()
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            "#$name",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = 1.dp),
        )
    }
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
