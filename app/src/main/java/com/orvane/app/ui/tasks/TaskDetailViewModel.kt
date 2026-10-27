package com.orvane.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.ReminderEntity
import com.orvane.app.data.db.TagEntity
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.repo.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repo: TaskRepository,
) : ViewModel() {

    val taskId = MutableStateFlow<String?>(null)

    val task: StateFlow<TaskEntity?> = taskId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repo.observeTask(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), null)

    val children: StateFlow<List<TaskEntity>> = taskId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.observeChildren(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = taskId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.observeReminders(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), emptyList())

    val tags: StateFlow<List<TagEntity>> = taskId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repo.observeTagsForTask(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), emptyList())

    val lists: StateFlow<List<com.orvane.app.data.db.ListEntity>> =
        repo.observeLists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), emptyList())

    fun load(id: String?) {
        taskId.value = id
    }

    // ---------- Mutations ----------

    fun toggleDone() {
        val t = task.value ?: return
        viewModelScope.launch {
            if (t.completedAt == null) repo.complete(t.id) else repo.uncomplete(t.id)
        }
    }

    fun setTitle(title: String) {
        val id = taskId.value ?: return
        if (title.isNotBlank()) viewModelScope.launch { repo.setTitle(id, title) }
    }

    fun setNote(note: String) {
        val id = taskId.value ?: return
        viewModelScope.launch { repo.setNote(id, note) }
    }

    fun setDue(epochDay: Long?, hour: Int?, minute: Int?) {
        val id = taskId.value ?: return
        viewModelScope.launch {
            if (epochDay == null) {
                repo.setDue(id, null, false)
            } else {
                val day = LocalDate.ofEpochDay(epochDay)
                val millis = if (hour != null) {
                    Time.at(day, hour, minute ?: 0)
                } else {
                    Time.at(day, 9, 0)
                }
                repo.setDue(id, millis, hour == null)
            }
        }
    }

    fun setPriority(priority: Int) {
        val id = taskId.value ?: return
        viewModelScope.launch { repo.setPriority(id, priority) }
    }

    fun setListId(listId: String?) {
        val id = taskId.value ?: return
        viewModelScope.launch { repo.setTaskListId(id, listId) }
    }

    fun setRecurrence(rule: String?) {
        val id = taskId.value ?: return
        viewModelScope.launch { repo.setRecurrenceRule(id, rule) }
    }

    fun addSubtask(title: String) {
        val id = taskId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch { repo.addSubtask(id, title) }
    }

    fun toggleSubtask(child: TaskEntity) {
        viewModelScope.launch {
            if (child.completedAt == null) repo.complete(child.id) else repo.uncomplete(child.id)
        }
    }

    fun deleteSubtask(childId: String) {
        viewModelScope.launch { repo.deleteSubtask(childId) }
    }

    fun addReminderAt(atMillis: Long) {
        val id = taskId.value ?: return
        viewModelScope.launch { repo.addReminderAt(id, atMillis) }
    }

    fun removeReminder(reminderId: String) {
        viewModelScope.launch { repo.removeReminder(reminderId) }
    }

    fun setTagNames(names: List<String>) {
        val id = taskId.value ?: return
        viewModelScope.launch { repo.setTaskTags(id, names) }
    }

    fun trashThis(onDone: () -> Unit) {
        val id = taskId.value ?: return
        viewModelScope.launch {
            repo.trash(id)
            onDone()
        }
    }
}
