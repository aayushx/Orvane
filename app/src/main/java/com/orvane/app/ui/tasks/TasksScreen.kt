package com.orvane.app.ui.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.orvane.app.data.db.SmartList
import com.orvane.app.data.repo.SortKeys
import com.orvane.app.data.repo.TaskRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TasksScreen(
    vm: TasksViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenTask: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val selection by vm.selection.collectAsState()
    val scope = rememberCoroutineScope()
    val showCompletedAtBottom = state.viewKey.startsWith("list:") || state.viewKey == SmartList.COMPLETED
    val inSelection = selection.isNotEmpty()

    Column(Modifier.fillMaxSize()) {
        if (inSelection) {
            SelectionBar(
                count = selection.size,
                listOptions = state.listNames.map { (id, name) -> id to name }.sortedBy { it.second },
                onComplete = { vm.completeSelected() },
                onSetDate = { epochDay -> vm.applyBatch(TaskRepository.BatchOp.SetDateTo(epochDay)) },
                onSetPriority = { p -> vm.applyBatch(TaskRepository.BatchOp.SetPriority(p)) },
                onMoveList = { listId -> vm.applyBatch(TaskRepository.BatchOp.Move(listId)) },
                onDelete = {
                    val ids = selection.toList()
                    vm.trashSelected { n ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Moved $n task${if (n == 1) "" else "s"} to trash",
                                actionLabel = "Undo",
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                for (id in ids) vm.restore(id)
                            }
                        }
                    }
                },
                onCancel = { vm.clearSelection() },
            )
        } else {
            SortBar(state.sortKey, onSort = { vm.setSort(it) })
        }

        val rowsInSections = state.sections.sumOf { it.tasks.size }
        if (rowsInSections == 0 && state.completed.isEmpty() && !inSelection) {
            EmptyTasks(state.viewTitle)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                for (section in state.sections) {
                    item(key = "h_" + section.title) { SectionHeader(section.title, section.tasks.size) }
                    items(section.tasks, key = { it.id }) { task ->
                        SwipeableRow(
                            enabled = !inSelection,
                            onComplete = {
                                if (task.completedAt == null) vm.complete(task.id) else vm.uncomplete(task.id)
                            },
                            onDelete = {
                                vm.trashSingle(task.id)
                                val id = task.id
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar("Task deleted", "Undo")
                                    if (result == SnackbarResult.ActionPerformed) {
                                        vm.restore(id)
                                    }
                                }
                            },
                        ) {
                            TaskRow(
                                task = task,
                                listName = state.listNames[task.listId],
                                subtaskTotal = state.childCounts[task.id]?.second ?: 0,
                                subtaskDone = state.childCounts[task.id]?.first ?: 0,
                                selected = task.id in selection,
                                inSelectionMode = inSelection,
                                onToggleComplete = {
                                    if (task.completedAt == null) vm.complete(task.id) else vm.uncomplete(task.id)
                                },
                                onToggleSelect = { vm.toggleSelect(task.id) },
                                onClick = { onOpenTask(task.id) },
                            )
                        }
                    }
                }
                if (showCompletedAtBottom && state.completed.isNotEmpty()) {
                    item(key = "h_completed") { SectionHeader("Completed", state.completed.size) }
                    items(state.completed, key = { "c_" + it.id }) { task ->
                        TaskRow(
                            task = task,
                            listName = state.listNames[task.listId],
                            subtaskTotal = state.childCounts[task.id]?.second ?: 0,
                            subtaskDone = state.childCounts[task.id]?.first ?: 0,
                            selected = task.id in selection,
                            inSelectionMode = inSelection,
                            onToggleComplete = { vm.uncomplete(task.id) },
                            onToggleSelect = { vm.toggleSelect(task.id) },
                            onClick = { onOpenTask(task.id) },
                        )
                    }
                }
                item(key = "bottom_pad") { Spacer(Modifier.height(120.dp)) }
            }
        }
    }
}

/** Small sort selector aligned under the app bar. */
@Composable
private fun SortBar(sortKey: String, onSort: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Sort options",
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                "Manual" to SortKeys.MANUAL,
                "Due date" to SortKeys.DUE,
                "Priority" to SortKeys.PRIORITY,
                "Title" to SortKeys.TITLE,
                "Date created" to SortKeys.CREATED,
            ).forEach { (label, key) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSort(key)
                        expanded = false
                    },
                    trailingIcon = {
                        if (sortKey == key) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                )
            }
        }
    }
}

/** Long-press selection action bar. */
@Composable
private fun SelectionBar(
    count: Int,
    listOptions: List<Pair<String, String>>,
    onComplete: () -> Unit,
    onSetDate: (Long) -> Unit,
    onSetPriority: (Int) -> Unit,
    onMoveList: (String?) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    var dateMenu by remember { mutableStateOf(false) }
    var prioMenu by remember { mutableStateOf(false) }
    var listMenu by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
            }
            Text("$count selected", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onComplete) {
                Icon(Icons.Default.DoneAll, contentDescription = "Complete selected")
            }
            Box {
                IconButton(onClick = { dateMenu = true }) {
                    Icon(Icons.Default.Schedule, contentDescription = "Set date")
                }
                DropdownMenu(expanded = dateMenu, onDismissRequest = { dateMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Today") },
                        onClick = {
                            onSetDate(LocalDate.now().toEpochDay())
                            dateMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Tomorrow") },
                        onClick = {
                            onSetDate(LocalDate.now().plusDays(1).toEpochDay())
                            dateMenu = false
                        },
                    )
                }
            }
            Box {
                IconButton(onClick = { prioMenu = true }) {
                    Icon(Icons.Default.Flag, contentDescription = "Set priority")
                }
                DropdownMenu(expanded = prioMenu, onDismissRequest = { prioMenu = false }) {
                    listOf("None" to 0, "Low" to 1, "Medium" to 2, "High" to 3).forEach { (label, p) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onSetPriority(p)
                                prioMenu = false
                            },
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { listMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Move to list")
                }
                DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Inbox") },
                        onClick = {
                            onMoveList(null)
                            listMenu = false
                        },
                    )
                    for ((id, name) in listOptions) {
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onMoveList(id)
                                listMenu = false
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected")
            }
        }
    }
}

/** Row that swipes right to toggle complete and left to trash, springing back after action. */
@Composable
private fun SwipeableRow(
    enabled: Boolean,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val anim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 84.dp.toPx() }

    fun resetAnimated() {
        scope.launch { anim.animateTo(0f, spring()) }
    }

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(anim.value.roundToInt(), 0) }
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val next = (anim.value + delta)
                                .coerceIn(-threshold * 1.6f, threshold * 1.6f)
                            anim.snapTo(next)
                        }
                    },
                    onDragStopped = {
                        when {
                            anim.value >= threshold -> onComplete()
                            anim.value <= -threshold -> onDelete()
                        }
                        resetAnimated()
                    },
                ),
        ) { content() }
    }
}

/** Empty state for task views. */
@Composable
private fun EmptyTasks(title: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("☑", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text("Nothing here", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            "No tasks in \"$title\". Tap + to add one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
