package com.orvane.app.data.repo

import android.content.Context
import android.net.Uri
import com.orvane.app.core.alarms.AlarmScheduler
import com.orvane.app.core.nl.NaturalLanguageDateParser
import com.orvane.app.core.recurrence.RecurrenceEngine
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.AppDatabase
import com.orvane.app.data.db.AttachmentDao
import com.orvane.app.data.db.AttachmentEntity
import com.orvane.app.data.db.FilterDao
import com.orvane.app.data.db.FilterEntity
import com.orvane.app.data.db.FolderEntity
import com.orvane.app.data.db.ListDao
import com.orvane.app.data.db.ListEntity
import com.orvane.app.data.db.MetaDao
import com.orvane.app.data.db.ReminderDao
import com.orvane.app.data.db.ReminderEntity
import com.orvane.app.data.db.SmartList
import com.orvane.app.data.db.TagDao
import com.orvane.app.data.db.TagEntity
import com.orvane.app.data.db.TaskDao
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.db.TaskPriority
import com.orvane.app.data.db.TaskTagCrossRef
import com.orvane.app.data.settings.SettingsRepository
import com.orvane.app.widgets.OrvaneWidgets
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Sort keys for views. */
object SortKeys {
    const val MANUAL = "manual"
    const val DUE = "due"
    const val PRIORITY = "priority"
    const val TITLE = "title"
    const val CREATED = "created"
}

/**
 * All task operations. Room is the source of truth; alarms, reminders and widgets
 * update as side effects of writes.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val db: AppDatabase,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val listDao: ListDao,
    private val folderDao: com.orvane.app.data.db.FolderDao,
    private val tagDao: TagDao,
    private val filterDao: FilterDao,
    private val attachmentDao: AttachmentDao,
    private val metaDao: MetaDao,
    private val alarmScheduler: AlarmScheduler,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: Context,
) {

    // ---------- Observers ----------

    fun observeTask(id: String): Flow<TaskEntity?> = taskDao.observeById(id)

    fun observeChildren(parentId: String): Flow<List<TaskEntity>> = taskDao.observeChildren(parentId)

    fun observeSubtasks(): Flow<List<TaskEntity>> = taskDao.observeSubtasks()

    fun observeTagRefs(): Flow<List<com.orvane.app.data.db.TaskTagCrossRef>> = tagDao.observeRefs()

    fun observeReminders(taskId: String): Flow<List<ReminderEntity>> = reminderDao.observeForTask(taskId)

    fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()

    fun observeFolders(): Flow<List<FolderEntity>> = folderDao.observeAll()

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeAllPlain()

    fun observeTagsForTask(taskId: String): Flow<List<TagEntity>> = tagDao.observeForTask(taskId)

    fun observeFilters(): Flow<List<FilterEntity>> = filterDao.observeAll()

    fun observeTrash(): Flow<List<TaskEntity>> = taskDao.observeTrash()

    /** Live count of tasks due today (for badges). */
    fun observeTodayCount(): Flow<Int> = taskDao.observeCountDueInRange(Time.startOfToday(), Time.endOfToday())

    /** Active top-level tasks due inside a range (calendar dots / agenda). */
    suspend fun tasksDueInRange(rangeStart: Long, rangeEnd: Long): List<TaskEntity> =
        withContext(Dispatchers.IO) { taskDao.dueInRange(rangeStart, rangeEnd) }

    /** Tasks with a due date but no exact time are fine to move freely. */
    suspend fun unscheduledTaskCandidates(rangeStart: Long, rangeEnd: Long): List<TaskEntity> =
        withContext(Dispatchers.IO) { taskDao.dueInRange(rangeStart, rangeEnd).filter { it.isDueAllDay } }

    fun observeCompleted(): Flow<List<TaskEntity>> = taskDao.observeCompleted()

    fun observeSearch(query: String): Flow<List<TaskEntity>> = taskDao.observeSearch(query)

    fun observeInbox(): Flow<List<TaskEntity>> = taskDao.observeInbox()

    fun observeAllActive(): Flow<List<TaskEntity>> = taskDao.observeAllActive()

    fun observeAttachments(taskId: String): Flow<List<AttachmentEntity>> = attachmentDao.observeForTask(taskId)

    // ---------- Reads ----------

    suspend fun getById(id: String): TaskEntity? = taskDao.byId(id)

    suspend fun getReminder(id: String): ReminderEntity? = reminderDao.byId(id)

    suspend fun remindersForTask(taskId: String): List<ReminderEntity> = reminderDao.forTask(taskId)

    suspend fun childrenOf(parentId: String): List<TaskEntity> = taskDao.children(parentId)

    suspend fun childCount(parentId: String): Int = taskDao.childCount(parentId)

    suspend fun completedChildCount(parentId: String): Int = taskDao.completedChildCount(parentId)

    /** Progress percentage 0..100 for a parent task, or null if it has no subtasks. */
    suspend fun progressPercent(taskId: String): Int? {
        val total = taskDao.childCount(taskId)
        if (total == 0) return null
        val done = taskDao.completedChildCount(taskId)
        return ((done * 100L) / total).toInt()
    }

    suspend fun viewTasks(viewKey: String, sortKey: String = SortKeys.MANUAL): List<TaskEntity> =
        withContext(Dispatchers.IO) {
            val list = when (viewKey) {
                SmartList.TODAY -> taskDao.dueBefore(Time.endOfToday()).filter { it.completedAt == null }
                SmartList.TOMORROW -> taskDao.dueInRange(Time.startOfTomorrow(), Time.startOfTomorrow() + MILLIS_DAY).filter { it.completedAt == null }
                SmartList.NEXT_7_DAYS -> taskDao.dueInRange(
                    Time.startOfToday(),
                    Time.startOfDayEpoch(LocalDate.now().plusDays(7)),
                ).filter { it.completedAt == null }
                SmartList.OVERDUE -> taskDao.dueBefore(Time.startOfToday()).filter { it.completedAt == null }
                SmartList.ALL -> taskDao.allActiveOnce()
                SmartList.COMPLETED -> taskDao.completedOnce()
                SmartList.TRASH -> taskDao.trashOnce()
                SmartList.INBOX -> taskDao.inboxOnce()
            else -> {
                if (viewKey.startsWith("filter:")) {
                    val f = filterDao.all().firstOrNull { it.id == viewKey.removePrefix("filter:") }
                    if (f != null) return@withContext filterTasks(f, sortKey)
                    emptyList()
                } else {
                    taskDao.activeInListOnce(viewKey)
                }
            }
            }
            sortList(list, sortKey)
        }

    suspend fun filterTasks(filter: FilterEntity, sortKey: String = SortKeys.DUE): List<TaskEntity> =
        withContext(Dispatchers.IO) {
            val tagIdsFor = HashMap<String, List<String>>()
            val rows = taskDao.allActiveOnce()
            val views = rows.mapNotNull { t ->
                val tags = tagDao.forTask(t.id)
                tagIdsFor[t.id] = tags.map { it.id }
                com.orvane.app.core.filter.FilterEvaluator.TaskView(t, tags.map { it.id })
            }
            val out = com.orvane.app.core.filter.FilterEvaluator.filterAll(
                filter,
                views,
                Instant.now(),
            ).map { it.task }
            sortList(out, sortKey)
        }

    fun sortList(list: List<TaskEntity>, sortKey: String): List<TaskEntity> = when (sortKey) {
        SortKeys.DUE -> list.sortedWith(
            compareBy(
                { it.due ?: Long.MAX_VALUE },
                { -it.priority },
            ),
        )
        SortKeys.PRIORITY -> list.sortedWith(
            compareByDescending<TaskEntity> { it.priority }.thenBy { it.due ?: Long.MAX_VALUE },
        )
        SortKeys.TITLE -> list.sortedBy { it.title.lowercase() }
        SortKeys.CREATED -> list.sortedByDescending { it.createdAt }
        else -> list.sortedBy { it.sortOrder }
    }

    suspend fun todayCount(): Int = taskDao.countDueInRange(Time.startOfToday(), Time.endOfToday())

    suspend fun overdueCount(): Int = taskDao.countDueInRange(0, Time.startOfToday())

    // ---------- Creation ----------

    /**
     * Natural-language quick add. Recognizes dates, times, priorities and tags.
     * Returns the created task (already persisted with tags).
     */
    suspend fun quickAdd(
        text: String,
        listId: String? = null,
        isSubtaskOf: String? = null,
    ): TaskEntity? = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext null
        val now = System.currentTimeMillis()
        val parsed = NaturalLanguageDateParser.parse(clean)
        // An all-metadata input ("tomorrow 5pm !High #work") consumes every
        // token as date/priority/tag and would previously return null — the
        // task was silently never created (Phase D). TickTick-style fallback:
        // keep the raw text as the title when parsing leaves nothing behind.
        val fallbackTitle = parsed.title.ifBlank { clean }
        val targetList = listId ?: settingsRepository.current().defaultListId
        val task = TaskEntity(
            id = newId(),
            parentId = isSubtaskOf,
            title = fallbackTitle,
            due = parsed.due?.toInstant()?.toEpochMilli(),
            isDueAllDay = parsed.isAllDay,
            priority = parsed.priority,
            listId = targetList,
            createdAt = now,
            modifiedAt = now,
            sortOrder = (taskDao.maxSortOrder() ?: 0) + 1,
        )
        taskDao.insert(task)
        for (tagName in parsed.tags) {
            val tag = ensureTagNamed(tagName)
            tagDao.addRef(TaskTagCrossRef(task.id, tag.id))
        }
        if (isSubtaskOf != null) {
            // nothing extra: parent progress is derived
        }
    OrvaneWidgets.refreshAll(appContext)
        task
    }

    fun newId(): String = UUID.randomUUID().toString()

    /** Adds full detail task (from Task detail screen). */
    suspend fun saveTask(task: TaskEntity): TaskEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val toSave = task.copy(modifiedAt = now)
        taskDao.insert(toSave)
        rescheduleTaskReminders(task.id)
        OrvaneWidgets.refreshAll(appContext)
        toSave
    }

    // ---------- Editing ----------

    suspend fun setTitle(id: String, title: String) {
        taskDao.setTitle(id, title.trim(), System.currentTimeMillis())
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun setNote(id: String, note: String) {
        taskDao.setNote(id, note, System.currentTimeMillis())
    }

    suspend fun setDue(id: String, due: Long?, allDay: Boolean = false) {
        val modified = System.currentTimeMillis()
        taskDao.setDue(id, due, allDay, modified)
        rescheduleTaskReminders(id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun setPriority(id: String, priority: Int) {
        taskDao.setPriority(id, priority, System.currentTimeMillis())
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun setRecurrenceRule(id: String, rule: String?) {
        taskDao.setRecurrenceRule(id, rule, System.currentTimeMillis())
    }

    suspend fun setTaskListId(id: String, listId: String?) {
        taskDao.setListId(id, listId, System.currentTimeMillis())
        OrvaneWidgets.refreshAll(appContext)
    }

    /** Calendar drag & drop: schedules a time block. */
    suspend fun scheduleTaskAtTime(taskId: String, startMillis: Long, minutes: Int) {
        taskDao.setDue(taskId, startMillis, false, System.currentTimeMillis())
        rescheduleTaskReminders(taskId)
        OrvaneWidgets.refreshAll(appContext)
    }

    // ---------- Complete / uncomplete ----------

    /** Marks a task (or a whole subtree) complete, advancing recurrence if needed. */
    suspend fun complete(id: String, atMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val task = taskDao.byId(id) ?: return@withContext
        val now = atMillis
        val nowZdt = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
        if (task.recurrenceRule != null) {
            val currentDue = task.due?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
            val next = RecurrenceEngine.nextDue(task.recurrenceRule, currentDue, nowZdt)
            if (next != null) {
                val nextDueMillis = next.toInstant().toEpochMilli()
                // shift reminders by the same delta if possible
                val delta = (task.due?.let { nextDueMillis - it }) ?: 0L
                for (r in reminderDao.forTask(task.id)) {
                    val newAt = if (delta != 0L) Math.max(now, r.at + delta) else nextDueMillis
                    val upd = r.copy(at = newAt)
                    reminderDao.upsert(upd)
                    alarmScheduler.scheduleReminder(upd.id, upd.at)
                }
                taskDao.setDue(task.id, nextDueMillis, task.isDueAllDay, now)
                taskDao.setCompletedAt(task.id, null, now)
                OrvaneWidgets.refreshAll(appContext)
                return@withContext
            }
        }
        taskDao.setCompletedAt(task.id, now, now)
        taskDao.completeChildren(task.id, now, now)
        cancelTaskReminders(task.id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun uncomplete(id: String) {
        taskDao.setCompletedAt(id, null, System.currentTimeMillis())
        rescheduleTaskReminders(id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun completeFromNotification(taskId: String) {
        complete(taskId)
        for (r in reminderDao.forTask(taskId)) {
            com.orvane.app.core.notifications.NotificationHelper.let {
                val nm = androidx.core.app.NotificationManagerCompat.from(appContext)
                runCatching { nm.cancel(com.orvane.app.core.notifications.NotificationHelper.stableId(r.id)) }
            }
        }
    }

    // ---------- Subtasks ----------

    suspend fun addSubtask(parentId: String, title: String, dueMillis: Long? = null): TaskEntity? =
        withContext(Dispatchers.IO) {
            val clean = title.trim()
            if (clean.isBlank()) return@withContext null
            val now = System.currentTimeMillis()
            val sub = TaskEntity(
                id = newId(),
                parentId = parentId,
                title = clean,
                due = dueMillis,
                isDueAllDay = dueMillis == null,
                listId = null,
                createdAt = now,
                modifiedAt = now,
                sortOrder = taskDao.children(parentId).size,
            )
            taskDao.insert(sub)
            sub
        }

    suspend fun updateSubtask(sub: TaskEntity) {
        taskDao.insert(sub.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSubtask(id: String) {
        val kids = taskDao.children(id)
        for (k in kids) {
            taskDao.deleteById(k.id)
        }
        cancelTaskReminders(id)
        taskDao.deleteById(id)
    }

    // ---------- Trash ----------

    /** Move task (and its subtree) to trash. Returns moved timestamp for undo. */
    suspend fun trash(id: String) = withContext(Dispatchers.IO) {
        val deletedAt = System.currentTimeMillis()
        val ids = mutableSetOf(id)
        collectDescendantsInTrash(id, ids)
        for (i in ids) {
            taskDao.markDeleted(i, deletedAt, deletedAt)
        }
        cancelTaskReminders(id)
        OrvaneWidgets.refreshAll(appContext)
        deletedAt
    }

    private suspend fun collectDescendantsInTrash(id: String, acc: MutableSet<String>) {
        for (child in taskDao.children(id)) {
            if (acc.add(child.id)) {
                collectDescendantsInTrash(child.id, acc)
            }
        }
    }

    suspend fun restore(id: String) {
        taskDao.restore(id, System.currentTimeMillis())
        rescheduleTaskReminders(id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun deleteForever(id: String) {
        reminderDao.deleteForTask(id)
        val roots = taskDao.byId(id) ?: return
        for (child in taskDao.children(id)) {
            deleteAttachmentFiles(child.id)
            attachmentDao.deleteForTask(child.id)
            taskDao.deleteById(child.id)
        }
        deleteAttachmentFiles(id)
        attachmentDao.deleteForTask(id)
        tagDao.clearRefsFor(id)
        taskDao.deleteById(id)
    }

    private suspend fun deleteAttachmentFiles(taskId: String) {
        for (a in attachmentDao.forTask(taskId)) {
            runCatching { attachmentFile(a).delete() }
        }
    }

    suspend fun emptyTrash() {
        val trash = taskDao.trashOnce()
        for (t in trash) deleteForever(t.id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun purgeExpiredTrash() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        taskDao.purgeTrashOlderThan(cutoff)
    }

    suspend fun trashCount(): Int = taskDao.trashOnce().size

    // ---------- Batch operations ----------

    sealed interface BatchOp {
        data class Move(val listId: String?) : BatchOp
        data class SetPriority(val priority: Int) : BatchOp
        data object Delete : BatchOp
        data object Complete : BatchOp
        data class SetDateTo(val epochDay: Long) : BatchOp
    }

    suspend fun applyBatch(ids: List<String>, op: BatchOp) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        when (op) {
            is BatchOp.Move -> {
                taskDao.setListIds(ids, op.listId, now)
                for (id in ids) rescheduleTaskReminders(id)
            }
            is BatchOp.SetPriority -> taskDao.setPriorities(ids, op.priority, now)
            is BatchOp.Delete -> {
                for (id in ids) {
                    val deletedAt = now
                    taskDao.markDeleted(id, deletedAt, deletedAt)
                    for (child in taskDao.children(id)) {
                        taskDao.markDeleted(child.id, deletedAt, deletedAt)
                    }
                    cancelTaskReminders(id)
                }
            }
            is BatchOp.Complete -> for (id in ids) complete(id, now)
            is BatchOp.SetDateTo -> {
                for (id in ids) {
                    taskDao.setDue(id, Time.startOfDayEpoch(LocalDate.ofEpochDay(op.epochDay)), true, now)
                    rescheduleTaskReminders(id)
                }
            }
        }
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun reorder(idsInOrder: List<String>) {
        var order = 0
        for (id in idsInOrder) {
            taskDao.setSortOrder(id, order, System.currentTimeMillis())
            order += 1
        }
    }

    suspend fun reorderSubtask(subtaskId: String, indexInParent: Int) {
        // simple: assign +1 gaps around; UI reorders whole list usually
        taskDao.setSortOrder(subtaskId, indexInParent, System.currentTimeMillis())
    }

    // ---------- Reminders ----------

    /** Replaces all reminders of a task with the given absolute times. */
    suspend fun setReminders(taskId: String, timesMillis: List<Long>) = withContext(Dispatchers.IO) {
        val existing = reminderDao.forTask(taskId)
        val keep = timesMillis.toSet()
        for (r in existing) {
            if (!keep.contains(r.at)) {
                alarmScheduler.cancelReminder(r.id)
                reminderDao.delete(r.id)
            }
        }
        for (at in timesMillis) {
            if (!existing.any { it.at == at }) {
                val r = ReminderEntity(id = newId(), taskId = taskId, at = at)
                reminderDao.upsert(r)
                alarmScheduler.scheduleReminder(r.id, r.at)
            }
        }
    }

    suspend fun addReminderAt(taskId: String, at: Long) {
        val r = ReminderEntity(id = newId(), taskId = taskId, at = at)
        reminderDao.upsert(r)
        alarmScheduler.scheduleReminder(r.id, r.at)
    }

    suspend fun removeReminder(reminderId: String) {
        alarmScheduler.cancelReminder(reminderId)
        reminderDao.delete(reminderId)
    }

    suspend fun snoozeReminder(taskId: String, reminderId: String, minutes: Int) {
        val r = reminderDao.byId(reminderId) ?: return
        val newAt = maxOf(System.currentTimeMillis(), r.at) + minutes * 60_000L
        val moved = r.copy(id = newId(), at = newAt)
        alarmScheduler.cancelReminder(reminderId)
        reminderDao.upsert(moved)
        reminderDao.delete(reminderId)
        alarmScheduler.scheduleReminder(moved.id, moved.at)
        // dismiss old notification (best effort)
        val nm = androidx.core.app.NotificationManagerCompat.from(appContext)
        runCatching { nm.cancel(com.orvane.app.core.notifications.NotificationHelper.stableId(r.id)) }
    }

    private suspend fun cancelTaskReminders(taskId: String) {
        for (r in reminderDao.forTask(taskId)) {
            alarmScheduler.cancelReminder(r.id)
        }
        reminderDao.deleteForTask(taskId)
    }

    /** Recreate alarms for all future reminders (boot / app start). */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        for (r in reminderDao.withTasksAfter(now)) {
            val task = taskDao.byId(r.reminder.taskId) ?: continue
            if (task.deletedAt == null && task.completedAt == null) {
                alarmScheduler.scheduleReminder(r.reminder.id, r.reminder.at)
            }
        }
    }

    private suspend fun rescheduleTaskReminders(taskId: String) {
        val task = taskDao.byId(taskId) ?: return
        if (task.deletedAt != null || task.completedAt != null) return
        val now = System.currentTimeMillis()
        for (r in reminderDao.forTask(taskId)) {
            if (r.at > now) alarmScheduler.scheduleReminder(r.id, r.at)
        }
    }

    suspend fun rescheduleAllAlarmsForTask(taskId: String) = rescheduleTaskReminders(taskId)

    // ---------- Lists / folders / tags ----------

    suspend fun createList(name: String, color: Int, groupId: String? = null): String =
        withContext(Dispatchers.IO) {
            val id = newId()
            listDao.upsert(
                ListEntity(
                    id = id,
                    name = name.trim().ifEmpty { "New list" },
                    color = color,
                    groupId = groupId,
                    sortOrder = (listDao.maxSortOrder() ?: 0) + 1,
                ),
            )
            id
        }

    suspend fun renameList(id: String, name: String) {
        listDao.byId(id)?.let { listDao.upsert(it.copy(name = name.trim())) }
    }

    suspend fun setListColor(id: String, color: Int) {
        listDao.byId(id)?.let { listDao.upsert(it.copy(color = color)) }
    }

    suspend fun deleteList(id: String) {
        taskDao.clearListId(id, System.currentTimeMillis())
        listDao.deleteById(id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun createFolder(name: String): String = withContext(Dispatchers.IO) {
        val id = newId()
        folderDao.upsert(
            FolderEntity(id = id, name = name.trim(), sortOrder = (folderDao.maxSortOrder() ?: 0) + 1),
        )
        id
    }

    suspend fun renameFolder(id: String, name: String) {
        val f = folderDao.all().firstOrNull { it.id == id } ?: return
        folderDao.upsert(f.copy(name = name))
    }

    suspend fun deleteFolder(id: String) {
        // Member lists become top-level (QA item A8 semantics).
        listDao.clearFolderMembership(id)
        folderDao.delete(id)
    }

    suspend fun moveListToFolder(listId: String, folderId: String?) {
        val list = listDao.byId(listId) ?: return
        listDao.upsert(list.copy(groupId = folderId))
    }

    suspend fun setListCollapsed(id: String, collapsed: Boolean) = listDao.setCollapsed(id, collapsed)

    suspend fun ensureTagNamed(name: String): TagEntity = withContext(Dispatchers.IO) {
        val clean = name.trim().removePrefix("#")
        tagDao.byName(clean) ?: TagEntity(id = newId(), name = clean).also { tagDao.upsert(it) }
    }

    /** Replaces the tags of a task with given names (creating when missing). */
    suspend fun setTaskTags(taskId: String, names: List<String>) = withContext(Dispatchers.IO) {
        val tags = names.map { ensureTagNamed(it) }
        tagDao.clearRefsFor(taskId)
        for (t in tags) tagDao.addRef(TaskTagCrossRef(taskId, t.id))
    }

    suspend fun addTagToTask(taskId: String, name: String) {
        val tag = ensureTagNamed(name)
        tagDao.addRef(TaskTagCrossRef(taskId, tag.id))
    }

    suspend fun removeTagFromTask(taskId: String, tagId: String) {
        tagDao.removeRef(taskId, tagId)
    }

    /**
     * Renames a tag. If another tag already carries the name (case-insensitive)
     * the rename becomes a MERGE: refs of [tagId] move onto the existing tag
     * and the duplicate is removed (QA item A8 semantics).
     */
    // ---------- Eisenhower matrix (Round 2 / B3) ----------

    /** The four matrix buckets; edges of the grid. */
    enum class MatrixQuadrant { DO_FIRST, SCHEDULE, DELEGATE, LATER }

    /** Bucket predicate: urgent = due falls within the next 2 days (oday+2 boundary). */
    fun isUrgent(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()): Boolean =
        task.due != null && task.due >= nowMillis - 24L * 3600_000 &&
            task.due < Time.startOfDayEpoch(java.time.LocalDate.now().plusDays(2))

    /** Bucket predicate: important = Medium/High priority. */
    fun isImportant(task: TaskEntity): Boolean =
        task.priority >= com.orvane.app.data.db.TaskPriority.MEDIUM

    /** Canonical "urgent" due (today 23:59). */
    private fun endOfTodayDue(): Long = Time.at(java.time.LocalDate.now(), 23, 59)

    /** Canonical "not urgent" due (7 days out, midnight). */
    private fun nextWeekDue(): Long = Time.startOfDayEpoch(java.time.LocalDate.now().plusDays(7))

    /** Applies the (priority, due) changes implied by dropping a task in a quadrant. */
    suspend fun applyMatrixQuadrant(taskId: String, quadrant: MatrixQuadrant) = withContext(Dispatchers.IO) {
        val t = taskDao.byId(taskId) ?: return@withContext
        val p = com.orvane.app.data.db.TaskPriority
        var dueChanges: Long? = null
        when (quadrant) {
            MatrixQuadrant.DO_FIRST -> {
                setPriority(taskId, maxOf(t.priority, p.MEDIUM))
                // Must be URGENT: collapse to today's end-of-day.
                if (t.due == null || t.due >= Time.startOfDayEpoch(java.time.LocalDate.now().plusDays(2))) {
                    setDue(taskId, endOfTodayDue())
                }
            }
            MatrixQuadrant.SCHEDULE -> {
                setPriority(taskId, maxOf(t.priority, p.MEDIUM))
                // NOT urgent: push today/overdue dues to next week.
                if (t.due != null && t.due < Time.startOfDayEpoch(java.time.LocalDate.now().plusDays(2))) {
                    setDue(taskId, nextWeekDue())
                }
            }
            MatrixQuadrant.DELEGATE -> {
                // NOT important: demote to Low.
                setPriority(taskId, p.LOW)
                if (t.due == null || t.due >= Time.startOfDayEpoch(java.time.LocalDate.now().plusDays(2))) {
                    setDue(taskId, endOfTodayDue())
                }
            }
            MatrixQuadrant.LATER -> {
                // Neither urgent nor important: demote, push out urgent dues.
                setPriority(taskId, minOf(t.priority, p.LOW))
                if (t.due != null && t.due < Time.startOfDayEpoch(java.time.LocalDate.now().plusDays(2))) {
                    setDue(taskId, nextWeekDue())
                }
            }
        }
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun renameTag(tagId: String, name: String) {
        val clean = name.trim().removePrefix("#")
        if (clean.isEmpty()) return
        val existing = tagDao.byName(clean)
        if (existing != null && existing.id != tagId) {
            mergeTagInto(fromId = tagId, targetId = existing.id)
        } else {
            tagDao.byId(tagId)?.let { tagDao.upsert(it.copy(name = clean)) }
        }
    }

    /** Moves refs from tag [fromId] onto [targetId], then deletes the source. */
    suspend fun mergeTagInto(fromId: String, targetId: String) {
        if (fromId == targetId) return
        tagDao.moveRefs(fromId, targetId)
        tagDao.clearRefs(fromId)
        tagDao.deleteById(fromId)
    }

    suspend fun deleteTag(tagId: String) {
        tagDao.clearRefs(tagId)
        tagDao.deleteById(tagId)
    }

    // ---------- Filters ----------

    suspend fun saveFilter(filter: FilterEntity) = withContext(Dispatchers.IO) {
        val withOrder = filter.copy(sortOrder = filterDao.maxSortOrder() ?: 0)
        filterDao.upsert(withOrder)
    }

    suspend fun deleteFilter(id: String) = filterDao.delete(id)

    suspend fun getFilter(id: String): FilterEntity? =
        filterDao.all().firstOrNull { it.id == id }

    // ---------- Attachments ----------

    fun attachmentsDir(): File = File(appContext.filesDir, "attachments").apply { mkdirs() }

    fun attachmentFile(a: AttachmentEntity): File = File(attachmentsDir(), a.fileName)

    suspend fun addAttachmentFromUri(taskId: String, uri: Uri): AttachmentEntity? =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = appContext.contentResolver
                val mime = resolver.getType(uri) ?: "image/jpeg"
                val ext = when (mime) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    "image/gif" -> "gif"
                    else -> "jpg"
                }
                val fileName = "${taskId.take(8)}_${UUID.randomUUID()}.$ext"
                val dest = File(attachmentsDir(), fileName)
                resolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: return@runCatching null
                val entity = AttachmentEntity(
                    id = newId(),
                    taskId = taskId,
                    fileName = fileName,
                    mimeType = mime,
                    createdAt = System.currentTimeMillis(),
                )
                attachmentDao.upsert(entity)
                entity
            }.getOrNull()
        }

    suspend fun addAttachmentBytes(taskId: String, name: String, bytes: ByteArray): AttachmentEntity {
        val fileName = "${taskId.take(8)}_${UUID.randomUUID()}.jpg"
        val dest = File(attachmentsDir(), fileName)
        dest.writeBytes(bytes)
        val entity = AttachmentEntity(
            id = newId(),
            taskId = taskId,
            fileName = fileName,
            mimeType = "image/jpeg",
            createdAt = System.currentTimeMillis(),
        )
        attachmentDao.upsert(entity)
        return entity
    }

    suspend fun deleteAttachment(id: String) {
        val a = attachmentDao.byId(id) ?: return
        runCatching { attachmentFile(a).delete() }
        attachmentDao.delete(id)
    }

    suspend fun removeAttachment(entity: AttachmentEntity) {
        runCatching { attachmentFile(entity).delete() }
        attachmentDao.delete(entity.id)
    }

    // ---------- Recurrence previews ----------

    fun parseRecurrence(rule: String?) = RecurrenceEngine.parse(rule)

    fun describeRecurrence(rule: String?) = RecurrenceEngine.describe(rule)

    companion object {
        private const val MILLIS_DAY = 24L * 60 * 60 * 1000
    }

    suspend fun sortModeFor(viewKey: String): String =
        settingsRepository.current().listSortModes[viewKey] ?: SortKeys.MANUAL

    suspend fun setSortModeFor(viewKey: String, sortKey: String) {
        settingsRepository.setListSortMode(viewKey, sortKey)
    }

    suspend fun allTasksForBackup(): List<TaskEntity> = taskDao.allTasks()

    /** Accumulates focus/pomodoro statistics on a task. */
    suspend fun addFocusSeconds(taskId: String, seconds: Long) = withContext(Dispatchers.IO) {
        taskDao.addFocusSeconds(taskId, seconds)
    }

    suspend fun addPomoCount(taskId: String, count: Int) = withContext(Dispatchers.IO) {
        taskDao.addPomoCount(taskId, count)
    }

    suspend fun modifyTree(m: suspend (AppDatabase) -> Unit) { m(db) }

    suspend fun metaPut(key: String, value: String) = metaDao.put(com.orvane.app.data.db.MetaEntity(key, value))

    suspend fun metaGet(key: String): String? = metaDao.get(key)?.value
}
