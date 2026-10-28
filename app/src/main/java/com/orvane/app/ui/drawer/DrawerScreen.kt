package com.orvane.app.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.data.db.FilterEntity
import com.orvane.app.data.db.FolderEntity
import com.orvane.app.data.db.ListEntity
import com.orvane.app.data.db.SmartList
import com.orvane.app.data.db.StarterColors
import com.orvane.app.data.db.TagEntity
import com.orvane.app.data.repo.HabitRepository
import com.orvane.app.data.repo.TaskRepository
import com.orvane.app.ui.theme.orvaneTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DrawerUiState(
    val lists: List<ListEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val filters: List<FilterEntity> = emptyList(),
    val todayCount: Int = 0,
    val habitsCount: Int = 0,
    val trashCount: Int = 0,
    /** Smart-list counts derived from the active top-level tasks (Round 2 / C3). */
    val tomorrowCount: Int = 0,
    val next7Count: Int = 0,
    val overdueCount: Int = 0,
    val allCount: Int = 0,
    val inboxCount: Int = 0,
    val completedCount: Int = 0,
    /** Active task count per user list id, for list row counts (Round 2 / C3). */
    val listCounts: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val repo: TaskRepository,
    habitRepo: HabitRepository,
) : ViewModel() {

    private val listsAndFolders = combine(repo.observeLists(), repo.observeFolders()) { l, f -> l to f }
    private val tagsAndFilters = combine(repo.observeTags(), repo.observeFilters()) { t, f -> t to f }

    /** Smart-list counts from the active top-level tasks, same bounds as the sections use. */
    private data class SmartCounts(
        val tomorrow: Int,
        val next7: Int,
        val overdue: Int,
        val all: Int,
        val inbox: Int,
        /** Active task count per list id (user list rows show counts, C3). */
        val perList: Map<String, Int>,
    )

    private val smartCounts = repo.observeAllActive().map { actives ->
        val startToday = com.orvane.app.core.util.Time.startOfToday()
        val startTomorrow = com.orvane.app.core.util.Time.startOfTomorrow()
        val endTomorrow = com.orvane.app.core.util.Time.startOfDayEpoch(
            java.time.LocalDate.now().plusDays(2),
        )
        val end7 = com.orvane.app.core.util.Time.startOfDayEpoch(
            java.time.LocalDate.now().plusDays(7),
        )
        SmartCounts(
            tomorrow = actives.count { it.due != null && it.due >= startTomorrow && it.due < endTomorrow },
            next7 = actives.count { it.due != null && it.due >= startTomorrow && it.due < end7 },
            overdue = actives.count { it.due != null && it.due < startToday },
            all = actives.size,
            inbox = actives.count { it.listId == null },
            perList = actives.groupingBy { it.listId }.eachCount()
                .filterKeys { it != null }
                .mapKeys { (k, _) -> requireNotNull(k) },
        )
    }

    private val counts = combine(
        repo.observeTodayCount(),
        repo.observeCompleted().map { it.size },
        smartCounts,
        habitRepo.observeActive().map { it.size },
        repo.observeTrash().map { trash -> trash.count { it.parentId == null } },
    ) { today, completed, smart, habits, trash ->
        CountBundle(today, completed, smart, habits, trash)
    }

    private data class CountBundle(
        val today: Int,
        val completed: Int,
        val smart: SmartCounts,
        val habits: Int,
        val trash: Int,
    )

    val state: StateFlow<DrawerUiState> = combine(listsAndFolders, tagsAndFilters, counts) { lf, tf, c ->
        DrawerUiState(
            lists = lf.first,
            folders = lf.second,
            tags = tf.first,
            filters = tf.second,
            todayCount = c.today,
            habitsCount = c.habits,
            trashCount = c.trash,
            tomorrowCount = c.smart.tomorrow,
            next7Count = c.smart.next7,
            overdueCount = c.smart.overdue,
            allCount = c.smart.all,
            inboxCount = c.smart.inbox,
            completedCount = c.completed,
            listCounts = c.smart.perList,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), DrawerUiState())

    // ---------- List / folder / tag management (QA item 8) ----------

    fun createList(name: String, color: Int, folderId: String? = null) {
        viewModelScope.launch { repo.createList(name, color, folderId) }
    }

    fun updateList(id: String, name: String, color: Int, folderId: String?) {
        viewModelScope.launch {
            repo.renameList(id, name)
            repo.setListColor(id, color)
            repo.moveListToFolder(id, folderId)
        }
    }

    /** Deleting a list sends its tasks to Inbox (repository handles the move). */
    fun deleteList(id: String) = viewModelScope.launch { repo.deleteList(id) }

    fun createFolder(name: String) = viewModelScope.launch { repo.createFolder(name) }

    fun renameFolder(id: String, name: String) = viewModelScope.launch { repo.renameFolder(id, name) }

    /** Deleting a folder ungroups its member lists back to top level. */
    fun deleteFolder(id: String) = viewModelScope.launch { repo.deleteFolder(id) }

    /**
     * Renaming a tag onto an existing (other) tag name MERGES: refs move over
     * and the duplicate disappears.
     */
    fun renameTag(id: String, name: String) = viewModelScope.launch { repo.renameTag(id, name) }

    fun deleteTag(id: String) = viewModelScope.launch { repo.deleteTag(id) }

    fun mergeTag(fromId: String, intoId: String) = viewModelScope.launch { repo.mergeTagInto(fromId, intoId) }
}

@Composable
fun DrawerContent(
    currentKey: String,
    onOpenList: (String) -> Unit,
    vm: DrawerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var listDialog by remember { mutableStateOf(ManageTarget.ListTarget(null)) }
    var folderDialog by remember { mutableStateOf<ManageTarget.FolderTarget?>(null) }
    var tagDialogTag by remember { mutableStateOf<TagEntity?>(null) }

    ModalDrawerSheet {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            DrawerHeader()
            SmartRow("Today", SmartList.TODAY, Icons.Default.Today, currentKey, state.todayCount, onOpenList)
            SmartRow("Tomorrow", SmartList.TOMORROW, Icons.Default.DateRange, currentKey, state.tomorrowCount, onOpenList)
            SmartRow("Next 7 days", SmartList.NEXT_7_DAYS, Icons.Default.CalendarViewDay, currentKey, state.next7Count, onOpenList)
            SmartRow("Overdue", SmartList.OVERDUE, Icons.Default.EventAvailable, currentKey, state.overdueCount, onOpenList)
            SmartRow("All tasks", SmartList.ALL, Icons.Default.AllInbox, currentKey, state.allCount, onOpenList)
            SmartRow("Inbox", SmartList.INBOX, Icons.Default.Inbox, currentKey, state.inboxCount, onOpenList)
            SmartRow("Completed", SmartList.COMPLETED, Icons.Default.DoneAll, currentKey, state.completedCount, onOpenList)
            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            if (state.folders.isNotEmpty()) {
                SectionWithAction("Folders", onCreate = { folderDialog = ManageTarget.FolderTarget(null) })
                for (folder in state.folders) {
                    DrawerRow(
                        key = "folder:${folder.id}",
                        title = folder.name,
                        icon = Icons.Default.Folder,
                        isSelected = currentKey == "folder:${folder.id}",
                        count = null,
                        leadingTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onOpenList,
                        onEdit = { folderDialog = ManageTarget.FolderTarget(folder) },
                    )
                }
            }

            SectionWithAction("Lists", onCreate = { listDialog = ManageTarget.ListTarget(null) })
            for (list in state.lists) {
                DrawerRow(
                    key = "list:${list.id}",
                    title = list.name,
                    icon = null,
                    isSelected = currentKey == "list:${list.id}",
                    count = state.listCounts[list.id],
                    leadingTint = Color(list.color),
                    onOpenList,
                    onEdit = { listDialog = ManageTarget.ListTarget(list) },
                )
            }

            if (state.filters.isNotEmpty()) {
                SectionLabel("Filters")
                for (filter in state.filters) {
                    DrawerRow(
                        key = "filter:${filter.id}",
                        title = filter.name,
                        icon = Icons.Default.FilterList,
                        isSelected = currentKey == "filter:${filter.id}",
                        count = null,
                        leadingTint = MaterialTheme.colorScheme.primary,
                        onOpenList,
                    )
                }
            }

            if (state.tags.isNotEmpty()) {
                CollapsibleTagsSection(state.tags, onEditTag = { tagDialogTag = it })
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            DrawerRow(
                key = SmartList.TRASH,
                title = "Trash",
                icon = Icons.Default.Delete,
                isSelected = currentKey == SmartList.TRASH,
                count = state.trashCount.takeIf { it > 0 },
                leadingTint = MaterialTheme.colorScheme.onSurfaceVariant,
                onOpenList,
            )
        }
    }

    // ---------- Manage dialogs ----------

    when (val target = listDialog) {
        is ManageTarget.ListTarget ->
            if (target.isCreate) {
                ListEditorDialog(
                    existing = null,
                    folders = state.folders,
                    onSave = { name, color, folderId ->
                        vm.createList(name, color, folderId)
                        listDialog = ManageTarget.ListTarget(null)
                    },
                    onDelete = null,
                    onDismiss = { listDialog = ManageTarget.ListTarget(null) },
                )
            } else {
                val list = target.list
                checkNotNull(list)
                ListEditorDialog(
                    existing = list,
                    folders = state.folders,
                    onSave = { name, color, folderId ->
                        vm.updateList(list.id, name, color, folderId)
                        listDialog = ManageTarget.ListTarget(null)
                    },
                    onDelete = {
                        vm.deleteList(list.id)
                        listDialog = ManageTarget.ListTarget(null)
                    },
                    onDismiss = { listDialog = ManageTarget.ListTarget(null) },
                )
            }
    }

    folderDialog?.let { target ->
        if (target.folder != null) {
            val f = target.folder
            FolderEditorDialog(
                existing = f,
                onSave = {
                    vm.renameFolder(f.id, it)
                    folderDialog = null
                },
                onDelete = {
                    vm.deleteFolder(f.id)
                    folderDialog = null
                },
                onDismiss = { folderDialog = null },
            )
        } else {
            FolderEditorDialog(
                existing = null,
                onSave = {
                    vm.createFolder(it)
                    folderDialog = null
                },
                onDelete = null,
                onDismiss = { folderDialog = null },
            )
        }
    }

    tagDialogTag?.let { tag ->
        TagManageDialog(
            tag = tag,
            allTags = state.tags,
            onRename = {
                vm.renameTag(tag.id, it)
                tagDialogTag = null
            },
            onMerge = {
                vm.mergeTag(tag.id, it)
                tagDialogTag = null
            },
            onDelete = {
                vm.deleteTag(tag.id)
                tagDialogTag = null
            },
            onDismiss = { tagDialogTag = null },
        )
    }
}

/** Which entity is being created/edited in the manage dialogs. */
sealed class ManageTarget {
    data class ListTarget(val list: ListEntity?) {
        val isCreate: Boolean get() = list == null
    }
    data class FolderTarget(val folder: FolderEntity?)
}

/* ---------- List editor: rename, color picker, folder membership, delete ---------- */

@Composable
private fun ListEditorDialog(
    existing: ListEntity?,
    folders: List<FolderEntity>,
    onSave: (name: String, color: Int, folderId: String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: StarterColors.LIST_COLORS.first()) }
    var folderId by remember { mutableStateOf(existing?.groupId) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New list" else "Edit list") },

        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    for (c in StarterColors.LIST_COLORS) {
                        ColorSwatch(c, selected = c == color, onSelect = { color = c })
                    }
                }
                Text("Folder", style = MaterialTheme.typography.labelMedium)
                FolderMemberPicker(
                    folders = folders,
                    selectedFolderId = folderId,
                    onSelect = { folderId = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, color, folderId) },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        modifier = Modifier.semantics { contentDescription = "Delete list" },
                        onClick = { confirmDelete = true },
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete list?") },
            text = { Text("Its tasks will move to Inbox. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/* ---------- Folder editor ---------- */

@Composable
private fun FolderEditorDialog(
    existing: FolderEntity?,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New folder" else "Edit folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name") },
            )
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        modifier = Modifier.semantics { contentDescription = "Delete folder" },
                        onClick = { confirmDelete = true },
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete folder?") },
            text = { Text("Lists inside it become top-level. Lists themselves are not deleted.") },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/** Simple folder-membership selector: clicking cycles No folder → each folder. */
@Composable
private fun FolderMemberPicker(
    folders: List<FolderEntity>,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit,
) {
    val options: List<Pair<String, String?>> =
        listOf("No folder" to null) + folders.map { it.name to it.id }
    val currentLabel = options.firstOrNull { it.second == selectedFolderId }?.first ?: "No folder"
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                val idx = options.indexOfFirst { it.second == selectedFolderId }
                val next = options[(idx + 1) % options.size]
                onSelect(next.second)
            }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(currentLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/* ---------- Tag manage: rename / merge / delete ---------- */

@Composable
private fun TagManageDialog(
    tag: TagEntity,
    allTags: List<TagEntity>,
    onRename: (String) -> Unit,
    onMerge: (targetId: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(tag.name) }
    var mergeInto by remember { mutableStateOf<String?>(null) }
    val others = allTags.filter { it.id != tag.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage tag \"${tag.name}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Rename (same name = merge)") },
                )
                if (others.isNotEmpty()) {
                    Text("Merge into…", style = MaterialTheme.typography.labelMedium)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val order = others
                                val idx = order.indexOfFirst { it.id == mergeInto }
                                mergeInto = order[(idx + 1) % order.size].id
                            }
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            mergeInto?.let { id -> allTags.firstOrNull { it.id == id }?.name ?: "Choose tag" }
                                ?: "Choose tag",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onRename(name) },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onMerge(mergeInto!!) },
                    enabled = mergeInto != null,
                ) { Text("Merge") }
                TextButton(
                    modifier = Modifier.semantics { contentDescription = "Delete tag" },
                    onClick = onDelete,
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ColorSwatch(color: Int, selected: Boolean, onSelect: () -> Unit) {
    val desc = "Color ${Integer.toHexString(color)}"
    // 48dp touch target with a 28dp visual dot (48dp minimum, Round 2 / C6).
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = if (selected) "$desc, selected" else desc }
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(Color(color), CircleShape),
        )
        if (selected) {
            Box(
                Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

/* ---------- Shared row machinery ---------- */

/**
 * Branded tonal header (Round 2 / C3): wordmark tile + "Orvane" + app version.
 * No account UI — the app is offline-first and has no accounts.
 */
@Composable
private fun DrawerHeader() {
    val tokens = orvaneTokens()
    val ctx = LocalContext.current
    val version = remember(ctx) {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull()
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .padding(
                start = tokens.spacing.md,
                end = tokens.spacing.md,
                top = tokens.spacing.md,
                bottom = tokens.spacing.sm,
            )
            .fillMaxWidth(),
        shape = RoundedCornerShape(tokens.corners.card),
    ) {
        Row(
            Modifier.padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "O",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Spacer(Modifier.width(tokens.spacing.md))
            Column {
                Text("Orvane", style = MaterialTheme.typography.titleLarge)
                if (version != null) {
                    Text(
                        "v$version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

/** Tags section that folds down to a one-line header (Round 2 / C3). */
@Composable
private fun CollapsibleTagsSection(
    tags: List<TagEntity>,
    onEditTag: (TagEntity) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 2.dp)
            .semantics {
                contentDescription = if (expanded) "Collapse tags" else "Expand tags"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Tags",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "${tags.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
    if (expanded) {
        Column(Modifier.padding(start = 16.dp)) {
            for (tag in tags) {
                ManageableTagRow(tag, onEdit = { onEditTag(tag) })
            }
        }
    } else {
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SmartRow(
    title: String,
    key: String,
    icon: ImageVector,
    currentKey: String,
    count: Int?,
    onOpenList: (String) -> Unit,
) {
    DrawerRow(key, title, icon, currentKey == key, count, MaterialTheme.colorScheme.primary, onOpenList)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp),
    )
}

/** Section label with a trailing "+" create affordance (list/folder management). */
@Composable
private fun SectionWithAction(
    text: String,
    onCreate: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(text)
        Spacer(Modifier.weight(1f))
        IconButton(
            modifier = Modifier.semantics { contentDescription = "New ${text.lowercase().removeSuffix("s")}" },
            onClick = onCreate,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/** Tag row with a rename/delete/merge entry point. */
@Composable
private fun ManageableTagRow(tag: TagEntity, onEdit: () -> Unit) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Tag,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(tag.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Manage tag ${tag.name}" },
            onClick = onEdit,
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * A single selectable drawer item; [icon] null means a colored list dot.
 * Selected rows render as a rounded pill with container color (M3 style,
 * not the old full-width gray bar). Long-press opens the manage menu when
 * [onEdit] is set (C3); the trailing ⋮ stays as the discoverable affordance.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRow(
    key: String,
    title: String,
    icon: ImageVector?,
    isSelected: Boolean,
    count: Int?,
    leadingTint: Color,
    onOpenList: (String) -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    val tokens = orvaneTokens()
    val selectedText = MaterialTheme.colorScheme.onSecondaryContainer
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.sm)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else Color.Transparent,
            )
            .combinedClickable(
                onClick = { onOpenList(key) },
                onLongClick = onEdit,
            )
            .padding(start = tokens.spacing.md, end = tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) selectedText else leadingTint,
            )
        } else {
            Box(
                Modifier
                    .size(14.dp)
                    .padding(2.dp)
                    .background(leadingTint, CircleShape),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) selectedText else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onEdit != null) {
            IconButton(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Manage $title" },
                onClick = onEdit,
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
