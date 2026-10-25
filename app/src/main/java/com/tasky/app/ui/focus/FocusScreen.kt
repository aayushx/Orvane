package com.tasky.app.ui.focus

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tasky.app.core.focus.PomodoroPhase
import com.tasky.app.data.db.TaskEntity
import java.util.Locale

private val TIMER_TABS = listOf("Timer", "Stats", "Matrix", "Today plan")

/**
 * Focus tab: pomodoro timer backed by the foreground service, white-noise
 * picker, focus statistics, the Eisenhower matrix and the Today plan.
 */
@Composable
fun FocusScreen(vm: FocusViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val noise by vm.selectedNoise.collectAsState()
    var tab by rememberSaveable { mutableStateOf("Timer") }
    var setupOpen by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (t in TIMER_TABS) {
                FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text(t) })
            }
        }
        when (tab) {
            "Stats" -> StatsView(state)
            "Matrix" -> MatrixView(state)
            "Today plan" -> TodayPlanView(state, vm)
            else -> TimerView(state, noise, vm) { setupOpen = true }
        }
    }

    if (setupOpen) {
        StartSetupDialog(
            state = state,
            selectedNoise = noise,
            vm = vm,
            onDismiss = { setupOpen = false },
            onStart = {
                setupOpen = false
                vm.startPomodoro()
            },
            onStartStopwatch = {
                setupOpen = false
                vm.startStopwatch()
            },
        )
    }
}

@Composable
private fun TimerView(state: FocusUiState, noise: String, vm: FocusViewModel, onSetup: () -> Unit) {
    val pomo = state.pomo
    val running = pomo.phase != PomodoroPhase.IDLE
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        val stopwatch = pomo.stopwatch
        Text(
            when {
                pomo.phase == PomodoroPhase.FOCUS && stopwatch -> if (pomo.paused) "Stopwatch (paused)" else "Stopwatch"
                pomo.phase == PomodoroPhase.FOCUS -> if (pomo.paused) "Focus (paused)" else "Focus"
                pomo.phase == PomodoroPhase.SHORT_BREAK -> if (pomo.paused) "Short break (paused)" else "Short break"
                pomo.phase == PomodoroPhase.LONG_BREAK -> if (pomo.paused) "Long break (paused)" else "Long break"
                else -> "Ready"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(
                    when {
                        pomo.phase == PomodoroPhase.FOCUS && stopwatch -> MaterialTheme.colorScheme.secondaryContainer
                        pomo.phase == PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer
                        pomo.phase == PomodoroPhase.LONG_BREAK || pomo.phase == PomodoroPhase.SHORT_BREAK ->
                            MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                formatClock(pomo),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
        pomo.taskId?.let { taskId ->
            val linked = state.activeTasks.firstOrNull { it.id == taskId }
            if (linked != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Linked task: ${linked.title.take(60)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (running) {
            Spacer(Modifier.height(6.dp))
            Text(
                "${pomo.cyclesCompleted} cycles completed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!running) {
                Button(onClick = onSetup) { Text("Start focus") }
                OutlinedButton(onClick = onSetup) { Text("Stopwatch") }
            } else {
                Button(
                    onClick = { if (pomo.paused) vm.resume() else vm.pause() },
                    modifier = Modifier.width(110.dp),
                ) { Text(if (pomo.paused) "Resume" else "Pause") }
                OutlinedButton(onClick = { vm.skip() }) { Text("Skip") }
                OutlinedButton(onClick = { vm.stop() }, enabled = true) {
                    Text("Stop", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Noise picker
        Text("White noise during focus", style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NOISES.forEach { n ->
                FilterChip(
                    selected = noise == n,
                    onClick = { vm.selectNoise(n) },
                    label = { Text(n.replaceFirstChar { it.uppercase(Locale.ENGLISH) }) },
                )
            }
            FilterChip(
                selected = false,
                onClick = { vm.selectNoise("") },
                label = { Text("None") },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "The timer keeps running in the notification panel even when the app is closed.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(120.dp))
    }
}

private fun formatClock(pomo: com.tasky.app.core.focus.PomodoroState): String {
    val total = if (pomo.stopwatch) pomo.elapsedSeconds else pomo.remainingSeconds
    val mins = total / 60
    val secs = total % 60
    return "%02d:%02d".format(mins, secs)
}

// ---------------- Stats ----------------

@Composable
private fun StatsView(state: FocusUiState) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth()) {
                StatBox("Today", hours(state.stats.todaySeconds), Modifier.weight(1f))
                StatBox("This week", hours(state.stats.weekSeconds), Modifier.weight(1f))
                StatBox("30 days", hours(state.stats.monthSeconds), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.stats.sessionsToday} focus session${if (state.stats.sessionsToday == 1) "" else "s"} today",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text("Top tasks (30 days)", style = MaterialTheme.typography.titleSmall)
        }
        if (state.stats.perTask.isEmpty()) {
            item {
                Text(
                    "No focus time recorded yet. Start a pomodoro linked to a task to build stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(state.stats.perTask, key = { it.first }) { (title, secs) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        hours(secs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(140.dp)) }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier.padding(end = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun hours(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "${seconds}s"
    }
}

// ---------------- Matrix ----------------

@Composable
private fun MatrixView(state: FocusUiState) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text(
                "Eisenhower matrix — urgent means due within 2 days; important means Medium/High priority.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        items(state.matrix, key = { it.title }) { cell ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ),
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        cell.title + " (${cell.tasks.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (cell.tasks.isEmpty()) {
                        Text(
                            "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        for (t in cell.tasks.take(6)) {
                            Text(
                                "• " + t.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (cell.tasks.size > 6) {
                            Text(
                                "…and ${cell.tasks.size - 6} more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(140.dp)) }
    }
}

// ---------------- Today plan ----------------

@Composable
private fun TodayPlanView(state: FocusUiState, vm: FocusViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Today plan", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.todayTasks.size} tasks scheduled today · " +
                    "${state.todayEvents.size} event${if (state.todayEvents.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (state.todayEvents.isNotEmpty()) {
            item { Text("Events", style = MaterialTheme.typography.titleSmall) }
            for (e in state.todayEvents.take(8)) {
                item(key = "e" + e.id) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            com.tasky.app.ui.common.Format.timeLabel(e.start),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.width(64.dp),
                        )
                        Text(
                            e.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)); Text("Tasks", style = MaterialTheme.typography.titleSmall) }
        }
        if (state.todayTasks.isEmpty()) {
            item {
                Text(
                    "Nothing scheduled for today — enjoy the open day or pick a task from your lists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(state.todayTasks, key = { it.id }) { task ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!task.isDueAllDay && task.due != null) {
                        Text(
                            com.tasky.app.ui.common.Format.timeLabel(task.due),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(64.dp),
                        )
                    } else {
                        Spacer(Modifier.width(64.dp))
                    }
                    Checkbox(
                        checked = task.completedAt != null,
                        onCheckedChange = { vm.toggleTask(task) },
                    )
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (task.completedAt != null) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(140.dp)) }
    }
}

// ---------------- Setup dialog ----------------

@Composable
private fun StartSetupDialog(
    state: FocusUiState,
    selectedNoise: String,
    vm: FocusViewModel,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onStartStopwatch: () -> Unit,
) {
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    vm.selectTask(null)
                    onStartStopwatch()
                },
            ) { Text("Start stopwatch") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        vm.selectTask(chosen)
                        onStart()
                    },
                ) { Text("Start") }
            }
        },
        title = { Text("Set up focus session") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${state.configMinutes.first} min focus · ${state.configMinutes.second} min short break · " +
                        "${state.configMinutes.third} min long break (edit in Settings)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text("White noise: ${selectedNoise.replaceFirstChar { it.uppercase(Locale.ENGLISH) }}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Link a task", style = MaterialTheme.typography.labelLarge)
                RadioButton(selected = chosen == null, onClick = { chosen = null })
                for (task in state.activeTasks.take(8)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { chosen = task.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = chosen == task.id, onClick = { chosen = task.id })
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
    )
}
