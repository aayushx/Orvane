package com.tasky.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tasky.app.core.filter.FilterEvaluator
import com.tasky.app.core.util.Time
import com.tasky.app.data.db.FilterEntity
import com.tasky.app.data.db.FolderEntity
import com.tasky.app.data.db.ListEntity
import com.tasky.app.data.db.SmartList
import com.tasky.app.data.db.TaskEntity
import com.tasky.app.data.repo.SortKeys
import com.tasky.app.data.repo.TaskRepository
import com.tasky.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A group of tasks shown under one header (e.g. "Overdue", "Today"). */
data class TaskSection(val title: String, val tasks: List<TaskEntity>)

data class TasksUiState(
    val viewKey: String = SmartList.TODAY,
    val viewTitle: String = "Today",
    val sortKey: String = SortKeys.DUE,
    val sections: List<TaskSection> = emptyList(),
    val completed: List<TaskEntity> = emptyList(),
    val listNames: Map<String, String> = emptyMap(),
    val childCounts: Map<String, Pair<Int, Int>> = emptyMap(),
    val totalActive: Int = 0,
)

/** All reactive DB data in one immutable snapshot. */
private data class Snapshot(
    val active: List<TaskEntity> = emptyList(),
    val completed: List<TaskEntity> = emptyList(),
    val trash: List<TaskEntity> = emptyList(),
    val subtasks: List<TaskEntity> = emptyList(),
    val lists: List<ListEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val filters: List<FilterEntity> = emptyList(),
    val tagRefs: Map<String, List<String>> = emptyMap(),
    val sortMap: Map<String, String> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TaskRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Current list/smart view. */
    val viewKey = MutableStateFlow(SmartList.TODAY)

    val selection = MutableStateFlow<Set<String>>(emptySet())

    private fun snapshotFlow() = combine(
        repo.observeAllActive(),
        repo.observeCompleted(),
        repo.observeTrash(),
    ) { active, completed, trash ->
        Snapshot(active = active, completed = completed, trash = trash)
    }.combine(repo.observeSubtasks()) { s, subs ->
        s.copy(subtasks = subs)
    }.combine(repo.observeLists()) { s, lists -> s.copy(lists = lists) }
        .combine(repo.observeFolders()) { s, folders -> s.copy(folders = folders) }
        .combine(repo.observeFilters()) { s, filters -> s.copy(filters = filters) }
        .combine(repo.observeTagRefs()) { s, refs ->
            s.copy(tagRefs = refs.groupBy({ it.taskId }, { it.tagId }))
        }
        .combine(settings.settings) { s, st -> s.copy(sortMap = st.listSortModes) }

    /** Reactive DB snapshot with the current view selection. */
    val state: StateFlow<TasksUiState> = combine(snapshotFlow(), viewKey) { snap, key ->
        val sortKey = snap.sortMap[key] ?: defaultSortFor(key)
        buildState(key, sortKey, snap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), TasksUiState())

    private fun childCounts(subtasks: List<TaskEntity>): Map<String, Pair<Int, Int>> =
        subtasks.filter { it.parentId != null }.groupBy { it.parentId!! }.mapValues { (_, children) ->
            val total = children.size
            val done = children.count { it.completedAt != null }
            done to total
        }

    private fun buildState(key: String, sortKey: String, snap: Snapshot): TasksUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startToday = Time.startOfDayEpoch(today)
        val endToday = Time.startOfDayEpoch(today.plusDays(1))
        val childCounts = childCounts(snap.subtasks)
        val listNames = snap.lists.associate { it.id to it.name }
        val base = TasksUiState(
            viewKey = key,
            viewTitle = viewTitleFor(key, snap),
            sortKey = sortKey,
            listNames = listNames,
            childCounts = childCounts,
            totalActive = snap.active.size,
        )
        return when (key) {
            SmartList.TODAY -> {
                val dueToday = snap.active.filter { it.due != null && it.due < endToday }
                val overdue = dueToday.filter { (it.due ?: startToday) < startToday }
                val now = dueToday.filter { (it.due ?: startToday) >= startToday }
                val sections = mutableListOf<TaskSection>()
                if (overdue.isNotEmpty()) sections.add(TaskSection("Overdue", sortList(overdue, sortKey)))
                if (now.isNotEmpty()) sections.add(TaskSection("Today", sortList(now, sortKey)))
                base.copy(sections = sections)
            }
            SmartList.TOMORROW -> {
                val start = Time.startOfDayEpoch(today.plusDays(1))
                val list = sortList(
                    snap.active.filter { it.due != null && it.due >= start && it.due < start + MILLIS_DAY },
                    sortKey,
                )
                base.copy(sections = if (list.isEmpty()) emptyList() else listOf(TaskSection("Tomorrow", list)))
            }
            SmartList.NEXT_7_DAYS -> {
                val end = Time.startOfDayEpoch(today.plusDays(7))
                val list = sortList(
                    snap.active.filter { it.due != null && it.due >= startToday && it.due < end },
                    sortKey,
                )
                base.copy(sections = if (list.isEmpty()) emptyList() else listOf(TaskSection("Next 7 days", list)))
            }
            SmartList.OVERDUE -> {
                val list = sortList(
                    snap.active.filter { it.due != null && it.due < startToday },
                    sortKey,
                )
                base.copy(sections = if (list.isEmpty()) emptyList() else listOf(TaskSection("Overdue", list)))
            }
            SmartList.ALL -> {
                val list = sortList(snap.active, sortKey)
                base.copy(sections = if (list.isEmpty()) emptyList() else listOf(TaskSection("All tasks", list)))
            }
            SmartList.INBOX -> {
                val list = sortList(snap.active.filter { it.listId == null }, sortKey)
                base.copy(sections = if (list.isEmpty()) emptyList() else listOf(TaskSection("Inbox", list)))
            }
            SmartList.COMPLETED -> base.copy(
                sections = listOf(TaskSection("Completed", sortList(snap.completed) { _, _ -> true })),
                completed = snap.completed,
            )
            SmartList.TRASH -> {
                // Only top-level rows; subtasks in trash are shown via their parent's restore.
                val top = snap.trash.filter { it.parentId == null }
                base.copy(sections = listOf(TaskSection("Trash", sortList(top) { _, _ -> true })))
            }
            else -> base.copy(sections = listOf(TaskSection("Tasks", snap.active)))
        }.let { intermediate ->
            when {
                key.startsWith("list:") -> {
                    val listId = key.removePrefix("list:")
                    val active = sortList(snap.active.filter { it.listId == listId }, sortKey)
                    val done = snap.completed.filter { it.listId == listId }
                    intermediate.copy(
                        sections = listOf(TaskSection(viewTitleFor(key, snap), active)),
                        completed = done,
                    )
                }
                key.startsWith("folder:") -> {
                    val folderId = key.removePrefix("folder:")
                    val listIds = snap.lists.filter { it.groupId == folderId }.map { it.id }
                    intermediate.copy(
                        sections = listOf(TaskSection(viewTitleFor(key, snap), sortList(snap.active.filter { it.listId in listIds }, sortKey))),
                    )
                }
                key.startsWith("filter:") -> {
                    val filterId = key.removePrefix("filter:")
                    val filter = snap.filters.firstOrNull { it.id == filterId }
                    val matches = if (filter != null) {
                        snap.active.filter {
                            FilterEvaluator.matches(
                                filter,
                                FilterEvaluator.TaskView(it, snap.tagRefs[it.id] ?: emptyList()),
                                Instant.now(),
                                zone,
                            )
                        }
                    } else emptyList()
                    intermediate.copy(
                        sections = listOf(TaskSection(viewTitleFor(key, snap), sortList(matches, sortKey))),
                    )
                }
                else -> intermediate
            }
        }
    }

    private fun sortList(tasks: List<TaskEntity>, sortKey: String): List<TaskEntity> = when (sortKey) {
        SortKeys.MANUAL -> tasks.sortedBy { it.sortOrder }
        SortKeys.DUE -> tasks.sortedBy { it.due ?: MAX_LONG }
        SortKeys.PRIORITY -> tasks.sortedWith(
            compareByDescending<TaskEntity> { it.priority }.thenBy { it.due ?: MAX_LONG },
        )
        SortKeys.TITLE -> tasks.sortedBy { it.title.lowercase() }
        SortKeys.CREATED -> tasks.sortedByDescending { it.createdAt }
        else -> tasks
    }

    /** The smart-view "completed" branches pass a dummy predicate to avoid eager sorting. */
    private fun sortList(tasks: List<TaskEntity>, transform: (TaskEntity, Int) -> Boolean): List<TaskEntity> =
        tasks.sortedByDescending { it.completedAt ?: it.modifiedAt }

    private fun defaultSortFor(key: String): String = when {
        key.startsWith("list:") -> SortKeys.MANUAL
        key.startsWith("folder:") -> SortKeys.MANUAL
        else -> SortKeys.DUE
    }

    private fun viewTitleFor(key: String, snap: Snapshot? = null): String = when {
        key.isEmpty() -> "Tasks"
        key == SmartList.TODAY -> "Today"
        key == SmartList.TOMORROW -> "Tomorrow"
        key == SmartList.NEXT_7_DAYS -> "Next 7 days"
        key == SmartList.OVERDUE -> "Overdue"
        key == SmartList.ALL -> "All"
        key == SmartList.COMPLETED -> "Completed"
        key == SmartList.INBOX -> "Inbox"
        key == SmartList.TRASH -> "Trash"
        key.startsWith("list:") -> snap?.lists?.firstOrNull { it.id == key.removePrefix("list:") }?.name ?: "List"
        key.startsWith("folder:") -> snap?.folders?.firstOrNull { it.id == key.removePrefix("folder:") }?.name ?: "Folder"
        key.startsWith("filter:") -> snap?.filters?.firstOrNull { it.id == key.removePrefix("filter:") }?.name ?: "Filter"
        else -> key
    }

    // ---------- Mutations ----------

    fun selectView(key: String) {
        viewKey.value = key
        selection.value = emptySet()
    }

    fun toggleSelect(id: String) {
        val cur = selection.value
        selection.value = if (id in cur) cur - id else cur + id
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun complete(id: String) {
        viewModelScope.launch { repo.complete(id) }
    }

    fun uncomplete(id: String) {
        viewModelScope.launch { repo.uncomplete(id) }
    }

    fun trashSelected(onDone: (Int) -> Unit) {
        val ids = selection.value
        viewModelScope.launch {
            for (id in ids) repo.trash(id)
            clearSelection()
            onDone(ids.size)
        }
    }

    fun trashSingle(id: String) {
        viewModelScope.launch { repo.trash(id) }
    }

    fun completeSelected() {
        val ids = selection.value
        viewModelScope.launch {
            for (id in ids) repo.complete(id)
            clearSelection()
        }
    }

    fun applyBatch(op: TaskRepository.BatchOp) {
        val ids = selection.value.toList()
        viewModelScope.launch {
            repo.applyBatch(ids, op)
            clearSelection()
        }
    }

    fun setSort(key: String) {
        viewModelScope.launch { settings.setListSortMode(viewKey.value, key) }
    }

    fun restore(id: String) {
        viewModelScope.launch { repo.restore(id) }
    }

    fun deleteForever(id: String) {
        viewModelScope.launch { repo.deleteForever(id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repo.emptyTrash() }
    }

    companion object {
        private const val MILLIS_DAY = 24L * 60 * 60 * 1000
        private const val MAX_LONG = Long.MAX_VALUE
    }
}
