package com.orvane.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvane.app.core.streak.StreakCalculator
import com.orvane.app.data.db.CheckStatus
import com.orvane.app.data.db.HabitEntity
import com.orvane.app.data.db.HabitFreqType
import com.orvane.app.ui.common.StatChip
import com.orvane.app.ui.theme.orvaneTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val HABIT_EMOJIS = listOf(
    "✅", "🌱", "🏃", "💪", "📚", "🧘", "💧", "🥗", "🎸", "✍️", "🧹", "💊", "😴", "🎧",
)

private val DOW_LABELS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

/**
 * Habits tab: today's dashboard, habit cards with check-in / skip, an add/edit
 * dialog and per-habit stats (7/30/90-day completion, 30-day heatmap, weekly bars).
 */
@Composable
fun HabitsScreen(vm: HabitsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Habits", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { editingId = null; editorOpen = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
        if (state.rows.isNotEmpty()) {
            Dashboard(state)
        }
        if (state.rows.isEmpty()) {
            EmptyHabits()
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.rows, key = { it.habit.id }) { row ->
                    HabitCard(
                        row = row,
                        nagCutoff = if (row.habit.nagEnabled) {
                            "%02d:%02d".format(state.nagCutoffHour, state.nagCutoffMinute)
                        } else {
                            null
                        },
                        onCheck = { vm.toggleCheck(row.habit) },
                        onSkip = { vm.toggleSkip(row.habit) },
                        onEdit = { editingId = row.habit.id; editorOpen = true },
                        onArchive = { vm.archive(row.habit.id) },
                    )
                }
                if (state.archived.isNotEmpty()) {
                    item(key = "archived_hdr") {
                        Text(
                            "Archived",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(state.archived, key = { "a_" + it.id }) { habit ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(habit.emoji, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                habit.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = { vm.unarchive(habit.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Unarchive, contentDescription = "Restore habit", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { vm.delete(habit.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete habit", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                item(key = "pad") { Spacer(Modifier.height(140.dp)) }
            }
        }
    }

    if (editorOpen) {
        val existing = editingId?.let { id -> state.rows.firstOrNull { it.habit.id == id }?.habit }
        HabitEditorDialog(
            existing = existing,
            onDismiss = { editorOpen = false },
            onSave = { values ->
                vm.save(
                    id = editingId,
                    name = values.name,
                    emoji = values.emoji,
                    note = values.note,
                    freqType = values.freqType,
                    weekdays = values.weekdays,
                    timesPerWeek = values.timesPerWeek,
                    timesPerMonth = values.timesPerMonth,
                    reminderEnabled = values.reminderEnabled,
                    reminderHour = values.reminderHour,
                    reminderMinute = values.reminderMinute,
                    nagEnabled = values.nagEnabled,
                    onSaved = { editorOpen = false },
                )
            },
        )
    }
}

@Composable
private fun Dashboard(state: HabitsUiState) {
    val tokens = orvaneTokens()
    Card(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = tokens.spacing.lg,
                vertical = tokens.spacing.sm,
            ),
        shape = RoundedCornerShape(tokens.corners.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            Modifier.padding(tokens.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                progress = { state.dashboardPercent / 100f },
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (state.rows.isEmpty() || state.scheduledTodayCount == 0) {
                        "No habits scheduled today"
                    } else {
                        "${state.rows.size} habits · ${state.doneToday} done today"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Check in to keep your streaks alive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EmptyHabits() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("🌱", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text("Build your first habit", style = MaterialTheme.typography.titleMedium)
        Text(
            "Tap + to add daily, weekday, weekly or monthly habits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HabitCard(
    row: HabitRow,
    nagCutoff: String? = null,
    onCheck: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    val habit = row.habit
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tokens = orvaneTokens()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.xs)
            .clip(RoundedCornerShape(tokens.corners.card))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable { expanded = !expanded }
            .padding(tokens.spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon (emoji) in a tinted circle of the habit's own color.
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(habit.color).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(tokens.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when {
                    row.stats.currentStreak > 0 -> Text(
                        "🔥 ${row.stats.currentStreak}-day streak",
                        style = MaterialTheme.typography.titleSmall,
                        color = androidx.compose.ui.graphics.lerp(
                            Color(habit.color),
                            MaterialTheme.colorScheme.onSurface,
                            0.2f,
                        ),
                    )
                    else -> Text(
                        freqLabel(habit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (nagCutoff != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            " Reminds until $nagCutoff",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (row.skippedToday) {
                // Skipped needs a text affordance, not a color-only state (QA).
                Row(
                    Modifier
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                        .clickable { onSkip() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .semantics { contentDescription = "Skipped today. Tap to undo" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "»",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "Skipped",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            } else {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onSkip() }
                        .semantics { contentDescription = "Skip today" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "»",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(tokens.spacing.sm))
            HabitCheckButton(row.checkedToday, habit.color, onCheck)
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit habit", modifier = Modifier.size(14.dp))
            }
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            HabitStats(row)
            TextButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Archive")
            }
        }
    }
}

@Composable
private fun HabitStats(row: HabitRow) {
    val tokens = orvaneTokens()
    Column {
        Row(Modifier.fillMaxWidth()) {
            StatChip(
                value = windowPercent(checked = row.stats.checkedLast30, scheduled = row.scheduled30),
                label = "30-day rate",
                modifier = Modifier.weight(1f),
            )
            StatChip(
                value = "${row.stats.longestStreak} d",
                label = "Best streak",
                modifier = Modifier.weight(1f),
            )
            StatChip(
                value = "${row.stats.totalChecked}",
                label = "Completions",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Last 30 days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Heatmap(row)
        Spacer(Modifier.height(10.dp))
        Text(
            "This week",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        WeekdayBars(row)
    }
}

private fun windowPercent(checked: Int, scheduled: Int): String = when {
    scheduled <= 0 -> "—"
    else -> "${(100 * checked) / scheduled}%"
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(end = 6.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 30-day heatmap: green = checked, grey = scheduled not done, amber = skip. */
@Composable
private fun Heatmap(row: HabitRow) {
    val habit = row.habit
    val today = LocalDate.now()
    val firstDay = today.minusDays(29)
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    val rows = mutableListOf<List<LocalDate>>()
    var cursor = gridStart
    while (!cursor.isAfter(today)) {
        val week = (0 until 7).map { cursor.plusDays(it.toLong()) }
        rows.add(week)
        cursor = cursor.plusWeeks(1)
    }
    Column {
        for (week in rows) {
            Row {
                for (day in week) {
                    val inRange = !day.isBefore(firstDay) && !day.isAfter(today)
                    val color = if (inRange) {
                        when (row.dayStatus[day.toEpochDay()]) {
                            CheckStatus.CHECKED -> Color(habit.color)
                            CheckStatus.SKIPPED -> MaterialTheme.colorScheme.tertiary
                            else -> {
                                if (StreakCalculator.isScheduledDay(habit, day)) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            }
                        }
                    } else {
                        Color.Transparent
                    }
                    val mark = when (row.dayStatus[day.toEpochDay()]) {
                        CheckStatus.CHECKED -> "check"
                        CheckStatus.SKIPPED -> "skip"
                        else -> "none"
                    }
                    Box(
                        Modifier
                            .padding(1.dp)
                            .size(13.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                            .then(
                                if (inRange && day == today) {
                                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .semantics {
                                if (inRange) {
                                    contentDescription = "$mark ${day.dayOfMonth}"
                                }
                            },
                    )
                }
            }
        }
    }
}

/**
 * Weekday bars: seven equal columns so every bar sits EXACTLY over its weekday
 * label (the Round-1 QA misalignment came from centering narrow bars inside
 * wide columns). Today renders full-opacity in the habit color, other days at
 * 60%; a tap selects the day and prints its detail line (Round 2 / C2).
 */
@Composable
private fun WeekdayBars(row: HabitRow) {
    val habit = row.habit
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            for (i in 0 until 7) {
                val day = monday.plusDays(i.toLong())
                val future = day.isAfter(today)
                val checked = row.dayStatus[day.toEpochDay()] == CheckStatus.CHECKED
                val skipped = row.dayStatus[day.toEpochDay()] == CheckStatus.SKIPPED
                val scheduled = !future && StreakCalculator.isScheduledDay(habit, day)
                val fraction = when {
                    checked -> 1f
                    skipped -> 0.45f
                    scheduled -> 0.25f
                    else -> 0.12f
                }
                val baseColor = when {
                    checked || skipped -> Color(habit.color)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val alpha = when {
                    day == today && !checked -> 0.95f
                    day == today -> 1f
                    selected == day.toEpochDay() -> 1f
                    else -> 0.6f
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(baseColor.copy(alpha = alpha))
                            .fillMaxWidth(0.62f)
                            .fillMaxHeight(fraction)
                            .clickable(enabled = !future) {
                                selected = if (selected == day.toEpochDay()) null else day.toEpochDay()
                            }
                            .semantics {
                                contentDescription =
                                    day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) +
                                        when {
                                            checked -> ", done"
                                            skipped -> ", skipped"
                                            scheduled -> ", not done"
                                            else -> ", no habit scheduled"
                                        }
                            },
                    )
                }
            }
        }
        // Weekday labels: same weighted row, same weight — exact alignment.
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val day = monday.plusDays(i.toLong())
                Text(
                    DOW_LABELS[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day == today) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        selected?.let { epochDay ->
            val day = LocalDate.ofEpochDay(epochDay)
            val status = when {
                day.isAfter(today) -> "No data"
                row.dayStatus[epochDay] == CheckStatus.CHECKED -> "Done"
                row.dayStatus[epochDay] == CheckStatus.SKIPPED -> "Skipped"
                StreakCalculator.isScheduledDay(habit, day) -> "Not done"
                else -> "No habit scheduled"
            }
            Text(
                day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " +
                    day.monthValue + "/" + day.dayOfMonth + " · " + status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Today-check button with a satisfying animated ring fill: when checked, a
 * sweep animates around the habit-colored circle and a check scale-bounces in.
 */
@Composable
private fun HabitCheckButton(
    checked: Boolean,
    colorInt: Int,
    onCheck: () -> Unit,
) {
    val color = Color(colorInt)
    val tokens = orvaneTokens()
    val sweep = remember { Animatable(0f) }
    val checkScale = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        if (checked) {
            sweep.snapTo(0f)
            sweep.animateTo(
                1f,
                tween(durationMillis = 300, easing = tokens.motion.emphasizedEasing),
            )
            checkScale.snapTo(0.6f)
            checkScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            sweep.snapTo(0f)
            checkScale.snapTo(1f)
        }
    }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        Modifier
            .size(48.dp) // 48dp minimum interaction target (Round 2 / C6)
            .clickable {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onCheck()
            }
            .semantics {
                contentDescription = if (checked) "Done today, tap to undo" else "Mark habit done"
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            val stroke = 2.dp.toPx()
            val radius = (size.minDimension - stroke) / 2
            drawCircle(
                color = color.copy(alpha = if (checked) 1f else 0.15f),
                radius = radius,
                style = Stroke(stroke),
            )
            if (checked && sweep.value > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep.value,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            if (checked && sweep.value >= 1f) {
                drawCircle(color = color, radius = radius)
            }
        }
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = checkScale.value
                        scaleY = checkScale.value
                    },
            )
        }
    }
}

private fun freqLabel(habit: HabitEntity): String = when (habit.freqType) {
    HabitFreqType.DAILY -> "Daily"
    HabitFreqType.WEEKDAYS -> "Weekdays: " + DOW_LABELS.filter { habit.weekdays.contains(it) }.joinToString(" ")
    HabitFreqType.WEEK_COUNT -> "${habit.timesPerWeek}× per week"
    else -> "${habit.timesPerMonth}× per month"
}

// ---------------- Editor ----------------

data class HabitValues(
    val name: String,
    val emoji: String,
    val note: String,
    val freqType: Int,
    val weekdays: List<String>,
    val timesPerWeek: Int,
    val timesPerMonth: Int,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val nagEnabled: Boolean,
)

@Composable
private fun HabitEditorDialog(
    existing: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitValues) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "✅") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var freqType by rememberSaveable { mutableIntStateOf(existing?.freqType ?: HabitFreqType.DAILY) }
    var weekdays by rememberSaveable {
        mutableStateOf(existing?.weekdays?.toSet() ?: setOf("MO"))
    }
    var timesPerWeek by rememberSaveable { mutableIntStateOf(existing?.timesPerWeek ?: 3) }
    var timesPerMonth by rememberSaveable { mutableIntStateOf(existing?.timesPerMonth ?: 10) }
    var reminderEnabled by rememberSaveable { mutableStateOf(existing?.reminderEnabled ?: false) }
    var reminderHour by rememberSaveable { mutableIntStateOf(existing?.reminderHour ?: 8) }
    var reminderMinute by rememberSaveable { mutableIntStateOf(existing?.reminderMinute ?: 0) }
    var nagEnabled by rememberSaveable { mutableStateOf(existing?.nagEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        HabitValues(
                            name = name,
                            emoji = emoji,
                            note = note,
                            freqType = freqType,
                            weekdays = weekdays.toList(),
                            timesPerWeek = timesPerWeek,
                            timesPerMonth = timesPerMonth,
                            reminderEnabled = reminderEnabled,
                            reminderHour = reminderHour,
                            reminderMinute = reminderMinute,
                            nagEnabled = nagEnabled,
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (existing == null) "New habit" else "Edit habit") },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    HABIT_EMOJIS.forEach { e ->
                        Text(
                            e,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (e == emoji) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent,
                                )
                                .clickable { emoji = e }
                                .padding(6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Repeats", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    FilterChip(
                        selected = freqType == HabitFreqType.DAILY,
                        onClick = { freqType = HabitFreqType.DAILY },
                        label = { Text("Daily") },
                    )
                    FilterChip(
                        selected = freqType == HabitFreqType.WEEKDAYS,
                        onClick = { freqType = HabitFreqType.WEEKDAYS },
                        label = { Text("Weekdays") },
                    )
                    FilterChip(
                        selected = freqType == HabitFreqType.WEEK_COUNT,
                        onClick = { freqType = HabitFreqType.WEEK_COUNT },
                        label = { Text("×/week") },
                    )
                    FilterChip(
                        selected = freqType == HabitFreqType.MONTH_COUNT,
                        onClick = { freqType = HabitFreqType.MONTH_COUNT },
                        label = { Text("×/month") },
                    )
                }
                when (freqType) {
                    HabitFreqType.WEEKDAYS -> {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            DOW_LABELS.forEachIndexed { index, code ->
                                FilterChip(
                                    selected = code in weekdays,
                                    onClick = {
                                        weekdays = if (code in weekdays) weekdays - code else weekdays + code
                                    },
                                    label = {
                                        Text(DayOfWeek.of(index + 1).getDisplayName(TextStyle.NARROW, Locale.ENGLISH))
                                    },
                                )
                            }
                        }
                    }
                    HabitFreqType.WEEK_COUNT -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (n in 1..7) {
                                FilterChip(
                                    selected = timesPerWeek == n,
                                    onClick = { timesPerWeek = n },
                                    label = { Text("$n") },
                                )
                            }
                        }
                    }
                    HabitFreqType.MONTH_COUNT -> {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (n in 2..28 step 2) {
                                FilterChip(
                                    selected = timesPerMonth == n,
                                    onClick = { timesPerMonth = n },
                                    label = { Text("$n") },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    Spacer(Modifier.width(10.dp))
                    Text("Reminder")
                }
                if (reminderEnabled) {
                    // Presets + Custom… time picker (Round 2 / B1): any HH:MM
                    // selectable for a habit's daily reminder.
                    com.orvane.app.ui.common.CustomTimeChipRow(
                        presetHours = listOf(6, 7, 8, 9, 12, 18, 20),
                        currentHour = reminderHour,
                        currentMinute = reminderMinute,
                        onSelect = { h, m ->
                            reminderHour = h
                            reminderMinute = m
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = nagEnabled, onCheckedChange = { nagEnabled = it })
                        Spacer(Modifier.width(10.dp))
                        Text("Re-remind in the evening if unchecked")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}
