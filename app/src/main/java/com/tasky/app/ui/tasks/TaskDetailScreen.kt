package com.tasky.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tasky.app.core.util.Time
import com.tasky.app.data.db.TaskPriority
import com.tasky.app.ui.common.Format
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    snackbarHostState: androidx.compose.material3.SnackbarHostState = remember { androidx.compose.material3.SnackbarHostState() },
    onBack: () -> Unit,
    vm: TaskDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    LaunchedEffect(taskId) {
        vm.load(taskId)
    }

    val task by vm.task.collectAsState()
    val children by vm.children.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val tags by vm.tags.collectAsState()
    val lists by vm.lists.collectAsState()

    val t = task ?: return

    var showDueEditor by rememberSaveable { mutableStateOf(false) }
    var showRepeatEditor by rememberSaveable { mutableStateOf(false) }
    var showReminderEditor by rememberSaveable { mutableStateOf(false) }
    var subtaskInput by rememberSaveable { mutableStateOf("") }

    val listName = lists.firstOrNull { it.id == t.listId }?.name
    val doneChildren = children.count { it.completedAt != null }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                if (listName != null) "In \"$listName\"" else "Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                vm.trashThis { onBack() }
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Move to trash")
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Title + done checkbox
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = t.completedAt != null,
                    onCheckedChange = { vm.toggleDone() },
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    t.title,
                    style = MaterialTheme.typography.headlineSmall,
                    textDecoration = if (t.completedAt != null) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f).padding(top = 14.dp),
                )
            }
            Spacer(Modifier.height(10.dp))

            // Meta chips
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = { showDueEditor = true },
                    label = {
                        Text(
                            t.due?.let { "📅 " + Format.dueChip(it, t.isDueAllDay) } ?: "📅 No date",
                        )
                    },
                )
                Spacer(Modifier.width(6.dp))
                AssistChip(
                    onClick = { showRepeatEditor = true },
                    label = { Text("🔁 " + (Format.repeatName(t.recurrenceRule).takeIf { it.isNotEmpty() } ?: "No repeat")) },
                )
                Spacer(Modifier.width(6.dp))
                AssistChip(
                    onClick = { showReminderEditor = true },
                    label = { Text("🔔 " + if (reminders.isEmpty()) "No reminder" else "${reminders.size}") },
                )
            }
            Spacer(Modifier.height(10.dp))

            // Priority
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(10.dp))
                listOf("None" to TaskPriority.NONE, "Low" to TaskPriority.LOW, "Medium" to TaskPriority.MEDIUM, "High" to TaskPriority.HIGH)
                    .forEach { (label, value) ->
                        FilterChip(
                            selected = t.priority == value,
                            onClick = { vm.setPriority(value) },
                            label = { Text(label) },
                        )
                        Spacer(Modifier.width(6.dp))
                    }
            }
            Spacer(Modifier.height(10.dp))

            // List
            var listMenu by rememberSaveable { mutableStateOf(false) }
            Box {
                AssistChip(
                    onClick = { listMenu = true },
                    label = { Text("🗂 " + (listName ?: "Inbox")) },
                )
                androidx.compose.material3.DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Inbox") },
                        onClick = { vm.setListId(null); listMenu = false },
                    )
                    for (l in lists) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(l.name) },
                            onClick = { vm.setListId(l.id); listMenu = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // Tags
            var tagInput by rememberSaveable { mutableStateOf("") }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (tag in tags) {
                    AssistChip(
                        onClick = {
                            vm.setTagNames(tags.map { it.name } - tag.name)
                        },
                        label = { Text("#${tag.name}") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove tag", modifier = Modifier.size(12.dp)) },
                    )
                    Spacer(Modifier.width(6.dp))
                }
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("Add tag") },
                    singleLine = true,
                    modifier = Modifier.width(140.dp),
                    trailingIcon = {
                        IconButton(onClick = {
                            val name = tagInput.removePrefix("#").trim()
                            if (name.isNotEmpty()) {
                                vm.setTagNames(tags.map { it.name } + name)
                                tagInput = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add tag")
                        }
                    },
                )
            }
            Spacer(Modifier.height(10.dp))

            // Notes
            var noteDraft by rememberSaveable(t.id) { mutableStateOf(t.note) }
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { vm.setNote(noteDraft) }, enabled = noteDraft != t.note) {
                    Text("Save note")
                }
            }
            Spacer(Modifier.height(6.dp))

            // Subtasks
            val progress: Int? = if (children.isEmpty()) null else (100 * doneChildren / children.size)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Subtasks", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                if (progress != null) Text("$doneChildren/${children.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (progress != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress / 100f)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            for (child in children.sortedBy { it.sortOrder }) {
                SubtaskRow(
                    child = child,
                    onToggle = { vm.toggleSubtask(child) },
                    onDelete = { vm.deleteSubtask(child.id) },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = subtaskInput,
                    onValueChange = { subtaskInput = it },
                    label = { Text("New subtask") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    vm.addSubtask(subtaskInput)
                    subtaskInput = ""
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add subtask")
                }
            }

            // Reminders detail
            if (reminders.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Reminders", style = MaterialTheme.typography.labelLarge)
                for (r in reminders.sortedBy { it.at }) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            Format.dueChip(r.at, false),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.removeReminder(r.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove reminder", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Created ${Format.dateLabel(t.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDueEditor) {
        DueEditorDialog(
            initialDay = t.due?.let { Format.dateOf(it) },
            initialTime = t.due?.takeIf { !t.isDueAllDay }?.let { Format.timeOf(it) },
            onDismiss = { showDueEditor = false },
            onSave = { day, time ->
                vm.setDue(day?.toEpochDay(), time?.hour, time?.minute)
                showDueEditor = false
            },
        )
    }
    if (showRepeatEditor) {
        RepeatEditorDialog(
            current = t.recurrenceRule,
            onDismiss = { showRepeatEditor = false },
            onSave = {
                vm.setRecurrence(it)
                showRepeatEditor = false
            },
        )
    }
    if (showReminderEditor) {
        ReminderEditorDialog(
            taskDay = t.due?.let { Format.dateOf(it) },
            onDismiss = { showReminderEditor = false },
            onAdd = {
                vm.addReminderAt(it)
                showReminderEditor = false
            },
        )
    }
}

@Composable
private fun SubtaskRow(child: com.tasky.app.data.db.TaskEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = child.completedAt != null, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(8.dp))
        Text(
            child.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (child.completedAt != null) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        child.due?.let {
            Text(
                Format.dueChip(it, child.isDueAllDay),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete subtask", modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueEditorDialog(
    initialDay: LocalDate?,
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onSave: (LocalDate?, LocalTime?) -> Unit,
) {
    var day by rememberSaveable { mutableStateOf(initialDay?.toEpochDay() ?: LocalDate.now().toEpochDay()) }
    var useTime by rememberSaveable { mutableStateOf(initialTime != null) }
    var hour by rememberSaveable { mutableStateOf(initialTime?.hour ?: 9) }
    var minute by rememberSaveable { mutableStateOf(initialTime?.minute ?: 0) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(LocalDate.ofEpochDay(day), if (useTime) LocalTime.of(hour, minute) else null) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null, null); onDismiss() }) {
                Text("Remove")
            }
        },
        title = { Text("Due date") },
        text = {
            Column {
                TextButton(onClick = { showDatePicker = true }) {
                    Text("📅 " + Format.dateLabel(LocalDate.ofEpochDay(day).atStartOfDay(Time.zone()).toInstant().toEpochMilli()))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = useTime, onCheckedChange = { useTime = it })
                    Spacer(Modifier.width(10.dp))
                    Text("Specific time")
                }
                if (useTime) {
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("⏰ " + "%02d:%02d".format(hour, minute))
                    }
                }
            }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = Time.startOfDayEpoch(LocalDate.ofEpochDay(day)))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { day = Format.dateOf(it).toEpochDay() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            title = { Text("Time") },
            text = { TimePicker(state = timeState) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatEditorDialog(
    current: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var choice by rememberSaveable {
        mutableStateOf(
            current?.let { c ->
                when {
                    c.contains("FREQ=DAILY") -> "daily"
                    c.contains("FREQ=MONTHLY") -> "monthly"
                    c.contains("FREQ=YEARLY") -> "yearly"
                    c.contains("COMPLETION_AFTER_DAYS") -> "completion_days"
                    c.contains("COMPLETION_AFTER_WEEKS") -> "completion_weeks"
                    c.contains("COMPLETION_AFTER_MONTHS") -> "completion_months"
                    c.contains("BYDAY") -> null // keep custom; not editable here, show as none
                    else -> "weekly"
                }
            } ?: "none",
        )
    }
    var interval by rememberSaveable { mutableStateOf(current?.split(";")?.firstOrNull { it.startsWith("INTERVAL=") }?.removePrefix("INTERVAL=") ?: "1") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    when (choice) {
                        "none" -> null
                        "daily" -> "FREQ=DAILY;INTERVAL=$interval"
                        "weekly" -> "FREQ=WEEKLY;INTERVAL=$interval"
                        "monthly" -> "FREQ=MONTHLY;INTERVAL=$interval"
                        "yearly" -> "FREQ=YEARLY;INTERVAL=$interval"
                        "completion_days" -> "COMPLETION_AFTER_DAYS;INTERVAL=$interval"
                        "completion_weeks" -> "COMPLETION_AFTER_WEEKS;INTERVAL=$interval"
                        "completion_months" -> "COMPLETION_AFTER_MONTHS;INTERVAL=$interval"
                        else -> null
                    },
                )
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Repeat") },
        text = {
            Column {
                val options = listOf(
                    "Never" to "none",
                    "Every N days" to "daily",
                    "Every N weeks" to "weekly",
                    "Every N months" to "monthly",
                    "Every N years" to "yearly",
                    "N days after completion" to "completion_days",
                    "N weeks after completion" to "completion_weeks",
                    "N months after completion" to "completion_months",
                )
                for ((label, value) in options) {
                    Row(
                        Modifier.fillMaxWidth().clickable { choice = value },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(selected = choice == value, onClick = { choice = value })
                        Text(label)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = interval,
                    onValueChange = { v -> interval = v.filter { it.isDigit() }.ifEmpty { "1" } },
                    label = { Text("Every N (1 = always)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorDialog(
    taskDay: LocalDate?,
    onDismiss: () -> Unit,
    onAdd: (Long) -> Unit,
) {
    var useTaskDay by rememberSaveable { mutableStateOf(taskDay != null) }
    var day by rememberSaveable { mutableStateOf((taskDay ?: LocalDate.now()).toEpochDay()) }
    var hour by rememberSaveable { mutableStateOf(taskDay?.let { 9 } ?: 9) }
    var minute by rememberSaveable { mutableStateOf(0) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = Time.at(
                    LocalDate.ofEpochDay(day),
                    hour,
                    minute,
                )
                onAdd(millis)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New reminder") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = useTaskDay, onCheckedChange = {
                        useTaskDay = it
                        taskDay?.let { d -> day = d.toEpochDay() }
                    })
                    Spacer(Modifier.width(10.dp))
                    Text("On task's date")
                }
                TextButton(onClick = { showDatePicker = true }) {
                    Text("📅 " + Format.dateLabel(Time.at(LocalDate.ofEpochDay(day), 9, 0)))
                }
                TextButton(onClick = { showTimePicker = true }) {
                    Text("⏰ " + "%02d:%02d".format(hour, minute))
                }
            }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = Time.startOfDayEpoch(LocalDate.ofEpochDay(day)))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { day = Format.dateOf(it).toEpochDay() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            title = { Text("Reminder time") },
            text = { TimePicker(state = timeState) },
        )
    }
}
