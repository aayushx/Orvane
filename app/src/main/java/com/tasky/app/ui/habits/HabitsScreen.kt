package com.tasky.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasky.app.core.streak.StreakCalculator
import com.tasky.app.data.db.CheckStatus
import com.tasky.app.data.db.HabitEntity
import com.tasky.app.data.db.HabitFreqType
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
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                progress = { state.dashboardPercent / 100f },
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (state.scheduledTodayCount == 0) {
                        "No habits scheduled today"
                    } else {
                        "${state.doneToday}/${state.scheduledTodayCount} done today"
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
    onCheck: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    val habit = row.habit
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable { expanded = !expanded }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    freqLabel(habit) +
                        if (row.stats.currentStreak > 0) " · ${row.stats.currentStreak} day streak" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (row.skippedToday) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent)
                    .clickable { onSkip() }
                    .semantics { contentDescription = "Skip today" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "»",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (row.skippedToday) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (row.checkedToday) Color(habit.color) else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (row.checkedToday) Color(habit.color) else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .clickable { onCheck() }
                    .semantics {
                        contentDescription = if (row.checkedToday) "Done today, tap to undo" else "Mark habit done"
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (row.checkedToday) {
                    Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
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
    val habit = row.habit
    val stats = row.stats
    Column {
        Row(Modifier.fillMaxWidth()) {
            StatCell("Last 7 days", windowPercent(stats.checkedLast7, row.scheduled7), Modifier.weight(1f))
            StatCell("Last 30 days", windowPercent(stats.checkedLast30, row.scheduled30), Modifier.weight(1f))
            StatCell("Last 90 days", windowPercent(stats.checkedLast90, row.scheduled90), Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCell("Best streak", "${stats.longestStreak} d", Modifier.weight(1f))
            StatCell("All time", "${stats.totalChecked} done · ${stats.totalSkipped} skipped", Modifier.weight(1f))
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
            "Recent weeks",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        WeekBars(row)
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

/** Bars for the last 6 weeks: fraction of scheduled check-ins completed. */
@Composable
private fun WeekBars(row: HabitRow) {
    val habit = row.habit
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weeks = (5 downTo 0).map { i ->
        val start = monday.minusWeeks(i.toLong())
        val done = (0 until 7).count { d ->
            val day = start.plusDays(d.toLong())
            !day.isAfter(today) && row.dayStatus[day.toEpochDay()] == CheckStatus.CHECKED
        }
        val scheduled = (0 until 7).count { d ->
            val day = start.plusDays(d.toLong())
            !day.isAfter(today) && StreakCalculator.isScheduledDay(habit, day)
        }
        Triple(start, done, scheduled)
    }
    Row(
        Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((start, done, scheduled) in weeks) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(
                                if (scheduled <= 0) 4.dp
                                else (52.dp * (done / scheduled.toFloat())).coerceAtLeast(4.dp),
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (scheduled > 0) Color(habit.color) else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                    )
                }
                Text(
                    "${start.monthValue}/${start.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (h in listOf(6, 7, 8, 9, 12, 18, 20)) {
                            FilterChip(
                                selected = reminderHour == h,
                                onClick = { reminderHour = h },
                                label = { Text("%02d:00".format(h)) },
                            )
                        }
                    }
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
