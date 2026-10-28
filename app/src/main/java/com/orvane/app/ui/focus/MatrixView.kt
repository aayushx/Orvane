package com.orvane.app.ui.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.ui.theme.priorityColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Round 2 / B3: a TRUE 2x2 Eisenhower matrix.
 *
 * urgent = due within 2 days; important = Medium/High priority — the same
 * predicates as [com.orvane.app.data.repo.TaskRepository.applyMatrixQuadrant],
 * so dropping a chip into a different quadrant rewrites the task (priority/due)
 * and it re-buckets itself consistently everywhere in the app.
 */
enum class MatrixQuadrant(val label: String, val hint: String) {
    DO_FIRST("Do first", "Urgent + important"),
    SCHEDULE("Schedule", "Important, not urgent"),
    DELEGATE("Delegate", "Urgent, not important"),
    LATER("Later", "Neither"),
}

/** Maps the VM's ordered four cells onto quadrants (same order as the old list view). */
fun matrixCellsToQuadrants(matrix: List<MatrixCell>): Map<MatrixQuadrant, List<TaskEntity>> =
    mapOf(
        MatrixQuadrant.DO_FIRST to matrix.getOrNull(0)?.tasks.orEmpty(),
        MatrixQuadrant.SCHEDULE to matrix.getOrNull(1)?.tasks.orEmpty(),
        MatrixQuadrant.DELEGATE to matrix.getOrNull(2)?.tasks.orEmpty(),
        MatrixQuadrant.LATER to matrix.getOrNull(3)?.tasks.orEmpty(),
    )

/** Root state holder for one drag across quadrants. */
internal class MatrixDragState {
    var id: String? by mutableStateOf(null)
    var pos: Offset by mutableStateOf(Offset.Zero)
    var target: MatrixQuadrant? by mutableStateOf(null)
}

@Composable
fun MatrixView(
    matrix: List<MatrixCell>,
    vm: FocusViewModel,
) {
    val cells = remember(matrix) { matrixCellsToQuadrants(matrix) }
    val drag = remember { MatrixDragState() }
    val chipBounds = remember { mutableMapOf<String, Rect>() }
    val quadrantBounds = remember { mutableMapOf<MatrixQuadrant, Rect>() }
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf<MatrixQuadrant?>(null) }

    fun hitTest(root: Offset): MatrixQuadrant? =
        quadrantBounds.entries.firstOrNull { it.value.contains(root) }?.key

    fun commit() {
        val id = drag.id
        val target = drag.target
        drag.id = null
        if (id != null && target != null) {
            scope.launch { vm.commitMatrixMove(id, target) }
        }
    }

    fun onDragStart(id: String, local: Offset, chipTopLeft: Offset) {
        drag.id = id
        drag.pos = chipTopLeft + local
        drag.target = hitTest(drag.pos)
    }

    fun onDragMove(root: Offset) {
        drag.pos = root
        drag.target = hitTest(root)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ---- URGENT / NOT URGENT axis header ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp, end = 12.dp, top = 8.dp),
            ) {
                AxisLabel("URGENT", Modifier.weight(1f), centered = true)
                AxisLabel("NOT URGENT", Modifier.weight(1f), centered = true)
            }
            Spacer(Modifier.height(2.dp))
            // ---- Row 1: Do first | Schedule (important) ----
            Row(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Column(Modifier.width(38.dp)) {
                    SideAxisLabel("IMPORTANT", Modifier.weight(1f))
                }
                Quadrant(
                    quadrant = MatrixQuadrant.DO_FIRST,
                    tasks = cells.getValue(MatrixQuadrant.DO_FIRST),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    registerQuadrant = { quadrantBounds[MatrixQuadrant.DO_FIRST] = it },
                    registerChip = { id, rect -> chipBounds[id] = rect },
                    drag = drag,
                    onExpand = { expanded = MatrixQuadrant.DO_FIRST },
                    onDragStart = ::onDragStart,
                    onDragMove = ::onDragMove,
                    onDrop = ::commit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Quadrant(
                    quadrant = MatrixQuadrant.SCHEDULE,
                    tasks = cells.getValue(MatrixQuadrant.SCHEDULE),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    registerQuadrant = { quadrantBounds[MatrixQuadrant.SCHEDULE] = it },
                    registerChip = { id, rect -> chipBounds[id] = rect },
                    drag = drag,
                    onExpand = { expanded = MatrixQuadrant.SCHEDULE },
                    onDragStart = ::onDragStart,
                    onDragMove = ::onDragMove,
                    onDrop = ::commit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(2.dp))
            // ---- Row 2: Delegate | Later (not important) ----
            Row(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Column(Modifier.width(38.dp)) {
                    SideAxisLabel("NOT IMPORTANT", Modifier.weight(1f))
                }
                Quadrant(
                    quadrant = MatrixQuadrant.DELEGATE,
                    tasks = cells.getValue(MatrixQuadrant.DELEGATE),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    registerQuadrant = { quadrantBounds[MatrixQuadrant.DELEGATE] = it },
                    registerChip = { id, rect -> chipBounds[id] = rect },
                    drag = drag,
                    onExpand = { expanded = MatrixQuadrant.DELEGATE },
                    onDragStart = ::onDragStart,
                    onDragMove = ::onDragMove,
                    onDrop = ::commit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Quadrant(
                    quadrant = MatrixQuadrant.LATER,
                    tasks = cells.getValue(MatrixQuadrant.LATER),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    registerQuadrant = { quadrantBounds[MatrixQuadrant.LATER] = it },
                    registerChip = { id, rect -> chipBounds[id] = rect },
                    drag = drag,
                    onExpand = { expanded = MatrixQuadrant.LATER },
                    onDragStart = ::onDragStart,
                    onDragMove = ::onDragMove,
                    onDrop = ::commit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        // ---- drag ghost flying with the pointer ----
        val draggingId = drag.id
        if (draggingId != null) {
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            (drag.pos.x - 70.dp.toPx()).roundToInt(),
                            (drag.pos.y - 18.dp.toPx()).roundToInt(),
                        )
                    }
                    .alpha(0.92f),
            ) {
                Text(
                    "• " + allTaskTitles(cells, draggingId),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    // ---- expanded quadrant (for long lists) ----
    expanded?.let { q ->
        AlertDialog(
            onDismissRequest = { expanded = null },
            title = { Text(quadrantTitle(q)) },
            text = {
                Column(Modifier.heightIn(max = 420.dp)) {
                    val list = cells.getValue(q)
                    if (list.isEmpty()) {
                        Text("Nothing here yet — drag tasks in.")
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            for (t in list) {
                                Text(
                                    "• ${t.title}" + (t.due?.let { " · " + dueLabel(it) } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { expanded = null }) { Text("Close") }
            },
        )
    }
}

private fun allTaskTitles(cells: Map<MatrixQuadrant, List<TaskEntity>>, id: String): String {
    for (list in cells.values) {
        for (t in list) if (t.id == id) return t.title
    }
    return ""
}

@Composable
private fun AxisLabel(
    text: String,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun SideAxisLabel(text: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                rotationZ = -90f
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            },
        )
    }
}

/**
 * One quadrant card: tap-to-expand header + task chips (drag sources) or a
 * dashed-border drop zone when empty. The whole card is the drop destination.
 */
@Composable
private fun Quadrant(
    quadrant: MatrixQuadrant,
    tasks: List<TaskEntity>,
    containerColor: Color,
    registerQuadrant: (Rect) -> Unit,
    registerChip: (String, Rect) -> Unit,
    drag: MatrixDragState,
    onExpand: () -> Unit,
    onDragStart: (id: String, local: Offset, chipTopLeft: Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val isTarget = drag.id != null && drag.target == quadrant && !tasks.any { it.id == drag.id }
    val targetRing = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val bg by animateColorAsState(
        targetValue = if (isTarget) containerColor.copy(alpha = 0.7f) else containerColor,
        label = "quadrantBg",
    )
    val chipTopLefts = remember { mutableMapOf<String, Offset>() }

    Box(
        modifier
            .padding(6.dp)
            .onGloballyPositioned { registerQuadrant(it.boundsInRoot()) }
            .clip(shape)
            .background(bg)
            .border(
                width = if (isTarget) 2.dp else 1.dp,
                color = if (isTarget) targetRing else outline,
                shape = shape,
            ),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        quadrant.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        quadrant.hint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${tasks.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tasks.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp)) {
                    DashedDropZone(active = isTarget, modifier = Modifier.fillMaxSize())
                }
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                ) {
                    for (t in tasks.take(10)) {
                        MatrixTaskChip(
                            task = t,
                            dragging = t.id == drag.id,
                            registerTopLeft = { chipTopLefts[t.id] = it },
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDrop,
                        )
                    }
                    if (tasks.size > 10) {
                        Text(
                            "…and ${tasks.size - 10} more — tap header to expand",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One task chip in a quadrant: long-press (~1s) picks it up; while dragging the
 * root position is reported so the parent can hit-test the drop quadrant.
 */
@Composable
private fun MatrixTaskChip(
    task: TaskEntity,
    dragging: Boolean,
    registerTopLeft: (Offset) -> Unit,
    onDragStart: (id: String, local: Offset, chipTopLeft: Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    var topLeft by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .onGloballyPositioned {
                topLeft = it.positionInRoot()
                registerTopLeft(it.positionInRoot())
            }
            .alpha(if (dragging) 0.30f else 1f)
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { local ->
                        onDragStart(task.id, local, topLeft)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDragMove(topLeft + change.position)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(end = 6.dp)
                    .size(8.dp)
                    .background(priorityColor(task.priority), CircleShape),
            )
            Text(
                task.title,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            task.due?.let { due ->
                Spacer(Modifier.width(4.dp))
                Text(
                    dueLabel(due),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Dashed-border drop zone for empty quadrants (Round 2 / B3). */
@Composable
private fun DashedDropZone(active: Boolean, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    Box(
        modifier.drawBehind {
            val stroke = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                cap = StrokeCap.Round,
            )
            drawRoundRect(
                color = if (active) highlight else outline,
                style = stroke,
                cornerRadius = CornerRadius(16.dp.toPx()),
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Drop tasks here",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun quadrantTitle(q: MatrixQuadrant): String = "${q.label} — ${q.hint.lowercase()}"

private fun dueLabel(dueMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val d = Instant.ofEpochMilli(dueMillis).atZone(zone).toLocalDate()
    return when (d) {
        LocalDate.now() -> "today"
        LocalDate.now().plusDays(1) -> "tomorrow"
        else -> "%d/%d".format(d.monthValue, d.dayOfMonth)
    }
}
