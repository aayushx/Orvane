package com.orvane.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.CalendarEventEntity
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.ui.common.Format
import com.orvane.app.ui.tasks.TaskRow
import com.orvane.app.ui.theme.orvaneTokens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val HOUR_HEIGHT = 52.dp
private const val MILLIS_DAY = 24L * 60 * 60 * 1000
private const val MILLIS_HOUR = 60L * 60 * 1000

/**
 * Calendar tab: month grid with task/event dots, a week time grid where tapping
 * an empty hour schedules a task or an event, and a 14-day agenda.
 */
@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    var mode by rememberSaveable { mutableStateOf("month") }
    var eventEditorSlot by remember { mutableStateOf<LocalDateTime?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(selected = mode == "month", onClick = { mode = "month" }, label = { Text("Month") })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = mode == "week", onClick = { mode = "week" }, label = { Text("Week") })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = mode == "agenda", onClick = { mode = "agenda" }, label = { Text("Agenda") })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.syncDeviceCalendars() }) {
                Icon(Icons.Default.Sync, contentDescription = "Sync device calendars")
            }
        }
        state.syncMessage?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Spacer(Modifier.height(4.dp))

        when (mode) {
            "week" -> WeekGrid(state, vm, onAddEvent = { slot -> eventEditorSlot = slot })
            "agenda" -> AgendaView(state)
            else -> MonthWithAgenda(state, vm, onAddEvent = { day ->
                eventEditorSlot = LocalDateTime.of(day, LocalTime.of(9, 0))
            })
        }
    }

    eventEditorSlot?.let { slot ->
        EventEditorDialog(
            day = slot.toLocalDate(),
            initialHour = slot.hour,
            onSave = { title, allDay, start, end ->
                vm.addLocalEvent(title, start, end, allDay)
            },
            onDismiss = { eventEditorSlot = null },
        )
    }
}

// ---------------- Month ----------------

@Composable
private fun MonthWithAgenda(state: CalendarUiState, vm: CalendarViewModel, onAddEvent: (LocalDate) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + state.month.year,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.selectMonth(state.month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                }
                IconButton(onClick = { vm.selectMonth(state.month.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                }
            }
            MonthGrid(state) { day -> vm.selectDay(day) }
            Spacer(Modifier.height(8.dp))
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, " +
                        "${state.selected.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} " +
                        "${state.selected.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { onAddEvent(state.selected) }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Event")
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        if (state.agendaEvents.isEmpty() && state.agendaTasks.isEmpty()) {
            item {
                Text(
                    "Nothing scheduled this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        } else {
            items(state.agendaEvents, key = { "e" + it.id }) { event ->
                EventRow(event, onDelete = { vm.deleteEvent(event.id) })
            }
            items(state.agendaTasks, key = { "t" + it.id }) { task ->
                Box(Modifier.padding(horizontal = 8.dp)) {
                    TaskRow(
                        task = task,
                        listName = null,
                        subtaskTotal = 0,
                        subtaskDone = 0,
                        selected = false,
                        inSelectionMode = false,
                        completed = false,
                        onToggleComplete = {},
                        onToggleSelect = {},
                        onClick = {},
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            FeedsSection(state, vm)
            Spacer(Modifier.height(12.dp))
        }
        item { Spacer(Modifier.height(140.dp)) }
    }
}

/** Subscribe, sync and remove ICS feeds. */
@Composable
private fun FeedsSection(state: CalendarUiState, vm: CalendarViewModel) {
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ICS feeds", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { vm.syncFeedsNow() }) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sync all")
            }
        }
        for (feed in state.feeds) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(feed.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        feed.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    feed.lastSync?.let {
                        Text(
                            "Last sync: " + Format.dateLabel(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { vm.removeFeed(feed.id) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove feed", modifier = Modifier.size(18.dp))
                }
            }
        }
        if (state.feeds.isEmpty()) {
            Text(
                "Subscribe to a calendar ICS URL to overlay shared events.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { showAdd = true }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add feed")
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(
                    enabled = url.contains("://"),
                    onClick = {
                        vm.addFeed(url, name)
                        url = ""
                        name = ""
                        showAdd = false
                    },
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
            title = { Text("Add ICS feed") },
            text = {
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("ICS URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onDayTapped: (LocalDate) -> Unit) {
    val month = state.month
    val today = LocalDate.now(Time.zone())
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (dow in listOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY,
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY,
        )) {
            Text(
                dow.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
    val firstDow = month.atDay(1).dayOfWeek.value - 1
    val rows = mutableListOf<List<LocalDate?>>()
    var current = mutableListOf<LocalDate?>()
    repeat(firstDow) { current.add(null) }
    for (d in 1..month.lengthOfMonth()) {
        current.add(month.atDay(d))
        if (current.size == 7) {
            rows.add(current.toList())
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) {
        while (current.size < 7) current.add(null)
        rows.add(current.toList())
    }
    Column {
        for (row in rows) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val day = row[i]
                    DayCell(
                        day = day,
                        isToday = day != null && day == today,
                        isSelected = day != null && day == state.selected,
                        dots = day?.let { state.dots[it] },
                        modifier = Modifier.weight(1f),
                        onClick = { if (day != null) onDayTapped(day) },
                    )
                }
            }
        }
    }
}

/**
 * One calendar day cell (Round 2 / C4): TODAY is a filled circle in primary,
 * the SELECTED day is a tonal pill in the scheme container; task dots render
 * in their list colors (fallback primary), event dots in the scheme accent.
 */
@Composable
private fun DayCell(
    day: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    dots: DayDots?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> Color.Transparent
                },
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        if (day != null) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isToday -> MaterialTheme.colorScheme.onPrimary
                        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Spacer(Modifier.height(2.dp))
            Row {
                val taskDots = dots?.taskColors?.take(3) ?: emptyList()
                for (taskColor in taskDots) {
                    Box(
                        Modifier
                            .padding(horizontal = 1.dp)
                            .size(5.dp)
                            .background(
                                Color(taskColor).copy(alpha = 0.85f),
                                CircleShape,
                            ),
                    )
                }
                val eventDots = (dots?.eventCount ?: 0).coerceAtMost(2)
                for (i in 0 until eventDots) {
                    Box(
                        Modifier
                            .padding(horizontal = 1.dp)
                            .size(5.dp)
                            .background(
                                if (isSelected && taskDots.isEmpty()) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.tertiary,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

// ---------------- Week time grid ----------------

@Composable
private fun WeekGrid(
    state: CalendarUiState,
    vm: CalendarViewModel,
    onAddEvent: (LocalDateTime) -> Unit,
) {
    val monday = state.selected.minusDays((state.selected.dayOfWeek.value - 1).toLong())
    var scheduleSlot by remember { mutableStateOf<LocalDateTime?>(null) }
    var openEventId by remember { mutableStateOf<String?>(null) }

    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${monday.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${monday.dayOfMonth} – " +
                monday.plusDays(6).let {
                    "${it.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${it.dayOfMonth}"
                },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { vm.selectDay(state.selected.minusWeeks(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous week")
        }
        IconButton(onClick = { vm.selectDay(state.selected.plusWeeks(1)) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next week")
        }
    }
    Text(
        "Tap an empty hour to schedule a task or add an event. Tapping a block opens options.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Spacer(Modifier.height(4.dp))

    val scroll = rememberScrollState()
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(44.dp)
                .verticalScroll(scroll),
        ) {
            Spacer(Modifier.height(20.dp))
            for (h in 0 until 24) {
                Box(Modifier.height(HOUR_HEIGHT)) {
                    Text(
                        "%02d:00".format(h),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
        Row(
            Modifier
                .weight(1f)
                .verticalScroll(scroll),
        ) {
            for (i in 0 until 7) {
                val day = monday.plusDays(i.toLong())
                DayColumn(
                    day = day,
                    events = state.weekEvents.filter { Time.toLocalDate(it.start) == day },
                    tasks = state.weekTasks.filter { task ->
                        task.due != null && Time.toLocalDate(task.due) == day
                    },
                    onSlotTapped = { slot -> scheduleSlot = slot },
                    onBlockTapped = { id -> openEventId = id },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    scheduleSlot?.let { slot ->
        ScheduleInSlotDialog(
            slot = slot,
            state = state,
            vm = vm,
            onDismiss = { scheduleSlot = null },
            onAddEvent = {
                scheduleSlot = null
                onAddEvent(slot)
            },
        )
    }
    openEventId?.let { id ->
        val e = state.weekEvents.firstOrNull { it.id == id }
        if (e != null) {
            BlockDialog(
                event = e,
                onDismiss = { openEventId = null },
                onDelete = {
                    vm.deleteEvent(e.id)
                    openEventId = null
                },
                onMove = { deltaMillis ->
                    vm.moveBlock(e.id, e.start + deltaMillis)
                    openEventId = null
                },
            )
        }
    }
}

@Composable
private fun BlockDialog(
    event: CalendarEventEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(if (event.linkedTaskId != null) "Unschedule" else "Delete")
            }
        },
        title = { Text(event.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(
                    if (event.allDay) {
                        "All day"
                    } else {
                        Format.timeLabel(event.start) + " – " + Format.timeLabel(event.end)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (event.note.isNotBlank()) {
                    Text(event.note, style = MaterialTheme.typography.bodySmall)
                }
                if (event.linkedTaskId != null) {
                    Text(
                        "Scheduled task block",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (event.linkedTaskId != null && !event.allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { onMove(-MILLIS_HOUR) }) { Text("− 1 h") }
                        TextButton(onClick = { onMove(MILLIS_HOUR) }) { Text("+ 1 h") }
                        TextButton(onClick = { onMove(MILLIS_DAY) }) { Text("+ 1 day") }
                    }
                }
            }
        },
    )
}

@Composable
private fun DayColumn(
    day: LocalDate,
    events: List<CalendarEventEntity>,
    tasks: List<TaskEntity>,
    onSlotTapped: (LocalDateTime) -> Unit,
    onBlockTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnHeight = HOUR_HEIGHT * 24
    Box(
        modifier
            .height(columnHeight)
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        for (h in 0 until 24) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(HOUR_HEIGHT)
                    .offset(y = HOUR_HEIGHT * h)
                    .clickable {
                        onSlotTapped(LocalDateTime.of(day, LocalTime.of(h, 0)))
                    },
            )
        }
        // task time blocks (due datetime)
        for (t in tasks) {
            val due = t.due ?: continue
            val lt = Time.toLocalTime(due)
            val top = HOUR_HEIGHT * (lt.hour + lt.minute / 60f)
            Box(
                Modifier
                    .padding(start = 2.dp, end = 4.dp)
                    .fillMaxWidth()
                    .offset(y = top)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        val linked = events.firstOrNull { it.linkedTaskId == t.id }
                        if (linked != null) onBlockTapped(linked.id)
                    }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // event blocks
        for (e in events) {
            val top = HOUR_HEIGHT * (Time.toLocalTime(e.start).hour + Time.toLocalTime(e.start).minute / 60f)
            val height: Dp = if (e.allDay) {
                HOUR_HEIGHT * 0.5f
            } else {
                val hours = ((e.end - e.start).toFloat() / MILLIS_HOUR).coerceIn(0.75f, 6f)
                HOUR_HEIGHT * hours
            }
            Box(
                Modifier
                    .padding(start = 2.dp, end = 2.dp)
                    .fillMaxWidth()
                    .offset(y = top)
                    .height(height)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (e.linkedTaskId != null) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer,
                    )
                    .clickable { onBlockTapped(e.id) }
                    .padding(3.dp),
            ) {
                Text(
                    e.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (e.linkedTaskId != null) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ScheduleInSlotDialog(
    slot: LocalDateTime,
    state: CalendarUiState,
    vm: CalendarViewModel,
    onDismiss: () -> Unit,
    onAddEvent: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = {
            Text(
                "Schedule — %02d:00 %s %d".format(
                    slot.hour,
                    slot.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    slot.dayOfMonth,
                ),
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val candidates = state.candidateTasks
                for (t in candidates.take(10)) {
                    TextButton(
                        onClick = {
                            vm.scheduleTaskAt(t.id, t.title, Time.at(slot.toLocalDate(), slot.hour, 0))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            t.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onAddEvent, modifier = Modifier.fillMaxWidth()) {
                    Text("Add event instead…")
                }
            }
        },
    )
}

// ---------------- Agenda ----------------

@Composable
private fun AgendaView(state: CalendarUiState) {
    val today = LocalDate.now(Time.zone())
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Upcoming 14 days",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        for (i in 0 until 14) {
            val day = today.plusDays(i.toLong())
            item(key = "hdr_$i") {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " +
                        day.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " ${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            val dayEvents = state.upcomingEvents[day].orEmpty()
            val dayTasks = state.upcomingTasks[day].orEmpty()
            if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                item(key = "none_$i") {
                    Text(
                        if (i == 0) "Nothing scheduled today." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            } else {
                items(dayEvents, key = { "ev_${i}_${it.id}" }) { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            (if (e.allDay) "" else Format.timeLabel(e.start) + " ") + e.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                items(dayTasks, key = { "tk_${i}_${it.id}" }) { t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            t.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (t.completedAt != null) TextDecoration.LineThrough else null,
                        )
                        Spacer(Modifier.weight(1f))
                        if (!t.isDueAllDay && t.due != null) {
                            Text(
                                Format.timeLabel(t.due),
                                style = MaterialTheme.typography.labelMedium,
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

// ---------------- Shared rows / dialogs ----------------

/** Agenda event card (Round 2 / C4): 4dp leading bar, time range, 12dp radius card. */
@Composable
private fun EventRow(event: CalendarEventEntity, onDelete: (String) -> Unit = {}) {
    val tokens = orvaneTokens()
    Row(
        Modifier
            .padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.xs)
            .clip(RoundedCornerShape(tokens.corners.card))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(vertical = tokens.spacing.sm)
                .width(4.dp)
                .height(38.dp)
                .background(
                    if (event.linkedTaskId != null) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.tertiary,
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(Modifier.width(tokens.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                (if (event.allDay) {
                    "All day"
                } else {
                    Format.timeLabel(event.start) + " – " + Format.timeLabel(event.end)
                }) + (event.location?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(tokens.spacing.sm))
        IconButton(onClick = { onDelete(event.id) }, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove event",
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(tokens.spacing.xs))
    }
}

@Composable
private fun EventEditorDialog(
    day: LocalDate,
    initialHour: Int,
    onSave: (title: String, allDay: Boolean, start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var allDay by rememberSaveable { mutableStateOf(false) }
    var startHour by rememberSaveable { mutableStateOf(initialHour) }
    var startMinute by rememberSaveable { mutableStateOf(0) }
    var durationHours by rememberSaveable { mutableStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    if (allDay) {
                        val s = Time.startOfDayEpoch(day)
                        onSave(title, true, s, s + MILLIS_DAY)
                    } else {
                        val s = Time.at(day, startHour, startMinute)
                        onSave(title, false, s, s + durationHours * MILLIS_HOUR)
                    }
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New event") },
        text = {
            Column(
                Modifier.padding(vertical = orvaneTokens().spacing.xs),
                verticalArrangement = Arrangement.spacedBy(orvaneTokens().spacing.sm),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                    Spacer(Modifier.width(orvaneTokens().spacing.sm))
                    Text("All day")
                }
                if (!allDay) {
                    Text(
                        "${day.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${day.dayOfMonth}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(orvaneTokens().spacing.xs))
                    com.orvane.app.ui.common.CustomTimeChipRow(
                        presetHours = listOf(7, 8, 9, 10, 11, 13, 14, 15, 16, 18, 19),
                        currentHour = startHour,
                        currentMinute = startMinute,
                        onSelect = { h, m ->
                            startHour = h
                            startMinute = m
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(orvaneTokens().spacing.sm)) {
                        FilterChip(selected = durationHours == 1, onClick = { durationHours = 1 }, label = { Text("1 h") })
                        FilterChip(selected = durationHours == 2, onClick = { durationHours = 2 }, label = { Text("2 h") })
                        FilterChip(selected = durationHours == 4, onClick = { durationHours = 4 }, label = { Text("4 h") })
                    }
                }
            }
        },
    )
}
