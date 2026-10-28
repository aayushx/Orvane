package com.orvane.app.ui.focus

import androidx.compose.foundation.Canvas
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvane.app.core.focus.PomodoroPhase
import com.orvane.app.ui.theme.orvaneTokens
import com.orvane.app.data.db.TaskEntity
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
            "Matrix" -> MatrixView(state.matrix, vm)
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
    val tokens = orvaneTokens()
    val pomo = state.pomo
    val running = pomo.phase != PomodoroPhase.IDLE
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(tokens.spacing.xl))
        val stopwatch = pomo.stopwatch
        // Phase pill (Round 2 / C5): "Focus · round 2 of 4"; animated on phase change.
        val phaseLabel = when {
            pomo.phase == PomodoroPhase.FOCUS && stopwatch -> "Stopwatch"
            pomo.phase == PomodoroPhase.FOCUS -> "Focus · round ${pomo.cyclesCompleted + 1} of ${state.configCycles}"
            pomo.phase == PomodoroPhase.SHORT_BREAK -> "Short break"
            pomo.phase == PomodoroPhase.LONG_BREAK -> "Long break"
            else -> "Ready"
        }
        val phaseSuffix = when {
            pomo.paused && pomo.phase != PomodoroPhase.IDLE -> " · paused"
            else -> ""
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            AnimatedContent(
                targetState = phaseLabel + phaseSuffix,
                transitionSpec = {
                    (fadeIn(tween(tokens.motion.shortMs)) + slideInVertically(tween(tokens.motion.shortMs)) { it / 2 }) togetherWith
                        (fadeOut(tween(tokens.motion.shortMs)) + slideOutVertically(tween(tokens.motion.shortMs)) { -it / 2 })
                },
                label = "phasePill",
            ) { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.xs),
                )
            }
        }
        Spacer(Modifier.height(tokens.spacing.lg))
        // Circular progress ring in primary with large numerals (Round 2 / C5).
        val ringColor = when {
            pomo.phase == PomodoroPhase.FOCUS && stopwatch -> MaterialTheme.colorScheme.secondary
            pomo.phase == PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
            pomo.phase == PomodoroPhase.LONG_BREAK || pomo.phase == PomodoroPhase.SHORT_BREAK ->
                MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
        val targetSweep = when {
            pomo.phase == PomodoroPhase.IDLE -> 0f
            pomo.durationSeconds > 0 -> (pomo.elapsedSeconds.toFloat() / pomo.durationSeconds).coerceIn(0f, 1f)
            else -> 0.15f // stopwatch: no fixed total, ring stays half-alive as an indicator
        }
        val sweep by animateFloatAsState(
            targetValue = targetSweep,
            animationSpec = tween(tokens.motion.mediumMs, easing = tokens.motion.emphasizedEasing),
            label = "ringSweep",
        )
        RingTimer(
            progress = sweep,
            ringColor = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(236.dp),
        ) {
            Text(
                formatClock(pomo),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
        Spacer(Modifier.height(tokens.spacing.xl))
        if (!running) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Button(onClick = onSetup) { Text("Start focus") }
                OutlinedButton(onClick = onSetup) { Text("Stopwatch") }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
            ) {
                TextButton(
                    onClick = { vm.skip() },
                    modifier = Modifier.semantics { contentDescription = "Skip phase" },
                ) { Text("Skip") }
                // Large circular play/pause (Round 2 / C5).
                FilledIconButton(
                    onClick = { if (pomo.paused) vm.resume() else vm.pause() },
                    modifier = Modifier
                        .size(72.dp)
                        .semantics {
                            contentDescription = if (pomo.paused) "Resume timer" else "Pause timer"
                        },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        if (pomo.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                    )
                }
                TextButton(
                    onClick = { vm.stop() },
                    modifier = Modifier.semantics { contentDescription = "Stop timer" },
                ) {
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

/**
 * Circular countdown ring (Round 2 / C5): track in surfaceVariant, sweep in
 * [ringColor] starting at 12 o'clock; [content] (the numerals) centers inside.
 */
@Composable
private fun RingTimer(
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val r = (size.minDimension - stroke) / 2
            drawCircle(color = trackColor, radius = r, style = Stroke(stroke))
            if (progress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

private fun formatClock(pomo: com.orvane.app.core.focus.PomodoroState): String {
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
                            com.orvane.app.ui.common.Format.timeLabel(e.start),
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
                            com.orvane.app.ui.common.Format.timeLabel(task.due),
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

/**
 * ±5-minute steppers over the configured pomodoro lengths. Starting a session
 * uses exactly these values, and each press immediately persists to Settings
 * (Round 2 / B2: setup sheet and settings can never diverge).
 */
@Composable
fun PomodoroLengthSteppers(
    configMinutes: Triple<Int, Int, Int>,
    cycles: Int,
    vm: FocusViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stepper(label = "Focus", value = configMinutes.first, onDelta = { vm.nudgePomodoro(0, it) })
            Stepper(label = "Short", value = configMinutes.second, onDelta = { vm.nudgePomodoro(1, it) })
            Stepper(label = "Long", value = configMinutes.third, onDelta = { vm.nudgePomodoro(2, it) })
            Stepper(label = "× before", value = cycles, step = 1, onDelta = { vm.nudgePomodoro(3, it) })
        }
        Text(
            "${configMinutes.first} min focus · ${configMinutes.second} min break · long break every $cycles",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    step: Int = 5,
    onDelta: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                modifier = Modifier.size(28.dp),
                onClick = { onDelta(-step) },
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label by $step")
            }
            Text(
                "$value",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(
                modifier = Modifier.size(28.dp),
                onClick = { onDelta(step) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label by $step")
            }
        }
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
            val tokens = orvaneTokens()
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            ) {
                // Live lengths: steppers below write-through to Settings, so the
                // sheet and Settings can never diverge (Round 2 / B2).
                PomodoroLengthSteppers(
                    configMinutes = state.configMinutes,
                    cycles = state.configCycles,
                    vm = vm,
                )
                Spacer(Modifier.height(tokens.spacing.xs))
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
