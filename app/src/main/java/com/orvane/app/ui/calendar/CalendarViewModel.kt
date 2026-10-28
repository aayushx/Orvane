package com.orvane.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.CalendarEventEntity
import com.orvane.app.data.db.IcsFeedEntity
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.repo.CalendarRepository
import com.orvane.app.data.repo.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Per-day dot counts for the month grid. */
data class DayDots(
    val taskCount: Int = 0,
    val eventCount: Int = 0,
    /** List colors of that day's tasks (task dots render in the list color, C4). */
    val taskColors: List<Int> = emptyList(),
)

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val selected: LocalDate = LocalDate.now(),
    val dots: Map<LocalDate, DayDots> = emptyMap(),
    /** Agenda rows for the currently selected day. */
    val agendaTasks: List<TaskEntity> = emptyList(),
    val agendaEvents: List<CalendarEventEntity> = emptyList(),
    /** Blocks on the week time grid (selected day's week). */
    val weekEvents: List<CalendarEventEntity> = emptyList(),
    val weekTasks: List<TaskEntity> = emptyList(),
    /** Next 14 days of tasks/events, grouped by day (agenda view). */
    val upcomingTasks: Map<LocalDate, List<TaskEntity>> = emptyMap(),
    val upcomingEvents: Map<LocalDate, List<CalendarEventEntity>> = emptyMap(),
    /** Active top-level tasks, candidates for scheduling into a slot. */
    val candidateTasks: List<TaskEntity> = emptyList(),
    val feeds: List<IcsFeedEntity> = emptyList(),
    val syncMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepo: CalendarRepository,
    private val taskRepo: TaskRepository,
) : ViewModel() {

    val month = MutableStateFlow(YearMonth.now())
    val selected = MutableStateFlow(LocalDate.now())

    /** Bumped when suspending writes change DB state so query-based flows refresh. */
    private val refresh = MutableStateFlow(0)

    private var lastSyncMessage: String? = null

    // ---- Reactive sources ----

    private val monthTasks = combine(month, refresh) { m, _ -> m }
        .flatMapLatest { m ->
            val start = Time.startOfDayEpoch(m.atDay(1).minusDays(7))
            val end = Time.startOfDayEpoch(m.plusMonths(1).atDay(1).plusDays(7))
            flow { emit(taskRepo.tasksDueInRange(start, end)) }
        }

    private val monthEvents = month.flatMapLatest { m ->
        val start = Time.startOfDayEpoch(m.atDay(1).minusDays(7))
        val end = Time.startOfDayEpoch(m.plusMonths(1).atDay(1).plusDays(7))
        calendarRepo.observeEvents(start, end)
    }

    private val weekStart = combine(selected, refresh) { sel, _ -> sel }
        .flatMapLatest { sel ->
            val monday = sel.minusDays((sel.dayOfWeek.value - 1).toLong())
            flow { emit(Time.startOfDayEpoch(monday)) }
        }

    private val weekEvents = weekStart.flatMapLatest { start ->
        calendarRepo.observeEvents(start, start + MILLIS_DAY * 7)
    }

    private val weekTasks = weekStart.flatMapLatest { start ->
        flow { emit(taskRepo.tasksDueInRange(start, start + MILLIS_DAY * 7)) }
    }

    private val todayKey = combine(selected, refresh) { sel, _ -> sel }
        .flatMapLatest { _ ->
            flow {
                val start = Time.startOfToday()
                emit(taskRepo.tasksDueInRange(start, start + MILLIS_DAY * 14))
            }
        }

    private val upcomingEvents = calendarRepo.observeEvents(
        Time.startOfToday(),
        Time.startOfToday() + MILLIS_DAY * 14,
    )

    private val upcomingPair = combine(todayKey, upcomingEvents) { tasks, events -> tasks to events }

    val state: StateFlow<CalendarUiState> = combine(
        combine(month, selected) { m, sel -> m to sel },
        combine(monthTasks, monthEvents) { mt, me -> mt to me },
        combine(weekTasks, weekEvents) { wt, we -> wt to we },
        combine(
            taskRepo.observeAllActive(),
            calendarRepo.observeFeeds(),
            taskRepo.observeLists(),
        ) { active, feeds, lists ->
            active to feeds to lists
        },
        upcomingPair,
    ) { monthSel, monthPair, weekPair, activeFeedsLists, upPair ->
        val (m, sel) = monthSel
        val (monthTaskList, monthEventList) = monthPair
        val (weekTaskList, weekEventList) = weekPair
        val (pair, lists) = activeFeedsLists
        val (active, feeds) = pair
        val listColorById = lists.associate { it.id to it.color }
        val (upcomingTaskList, upcomingEventList) = upPair
        build(
            m, sel, monthTaskList, monthEventList,
            weekTaskList, weekEventList, active, feeds,
            upcomingTaskList, upcomingEventList,
            listColorById,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), CalendarUiState())

    private fun build(
        month: YearMonth,
        sel: LocalDate,
        monthTasks: List<TaskEntity>,
        monthEvents: List<CalendarEventEntity>,
        weekTasks: List<TaskEntity>,
        weekEvents: List<CalendarEventEntity>,
        active: List<TaskEntity>,
        feeds: List<IcsFeedEntity>,
        upcomingTasks: List<TaskEntity>,
        upcomingEvents: List<CalendarEventEntity>,
        listColorById: Map<String, Int>,
    ): CalendarUiState {
        // --- month dots (task dots carry the owning list's color) ---
        val dots = mutableMapOf<LocalDate, DayDots>()
        fun bumpTask(day: LocalDate, listColor: Int?) {
            val cur = dots.getOrDefault(day, DayDots())
            dots[day] = cur.copy(
                taskCount = cur.taskCount + 1,
                taskColors = (cur.taskColors + listOfNotNull(listColor)).takeLast(3),
            )
        }
        fun bumpEvent(day: LocalDate) {
            val cur = dots.getOrDefault(day, DayDots())
            dots[day] = cur.copy(eventCount = cur.eventCount + 1)
        }
        for (t in monthTasks) t.due?.let { bumpTask(Time.toLocalDate(it), t.listId?.let(listColorById::get)) }
        for (e in monthEvents) {
            if (e.allDay) {
                val from = Time.toLocalDate(e.start)
                val last = Time.toLocalDate(e.end).minusDays(1)
                var d = if (from.isAfter(last)) last else from
                while (!d.isAfter(last)) {
                    bumpEvent(d)
                    d = d.plusDays(1)
                }
            } else {
                bumpEvent(Time.toLocalDate(e.start))
            }
        }
        // --- selected-day agenda ---
        val dayStart = Time.startOfDayEpoch(sel)
        val dayEnd = dayStart + MILLIS_DAY
        val agendaTasks = monthTasks
            .filter { it.due != null && it.due >= dayStart && it.due < dayEnd }
            .sortedBy { it.due }
        val agendaEvents = monthEvents
            .filter { it.start < dayEnd && it.end > dayStart }
            .sortedBy { it.start }
        // --- agenda (next 14 days) ---
        val upcomingTaskMap = upcomingTasks
            .filter { it.due != null }
            .groupBy { Time.toLocalDate(it.due!!) }
            .mapValues { (_, list) -> list.sortedBy { it.due } }
        val upcomingEventMap = upcomingEvents
            .groupBy { Time.toLocalDate(it.start) }
            .mapValues { (_, list) -> list.sortedBy { it.start } }
        // --- week grid ---
        val weekTaskBlocks = weekTasks.filter { it.due != null && !it.isDueAllDay }
        return CalendarUiState(
            month = month,
            selected = sel,
            dots = dots,
            agendaTasks = agendaTasks,
            agendaEvents = agendaEvents,
            weekEvents = weekEvents,
            weekTasks = weekTaskBlocks,
            upcomingTasks = upcomingTaskMap,
            upcomingEvents = upcomingEventMap,
            candidateTasks = active,
            feeds = feeds,
            syncMessage = lastSyncMessage,
        )
    }

    // ---- Selection ----

    fun selectMonth(m: YearMonth) {
        month.value = m
    }

    fun selectDay(day: LocalDate) {
        selected.value = day
        val m = YearMonth.from(day)
        if (m != month.value) month.value = m
    }

    // ---- Writes ----

    fun addLocalEvent(title: String, start: Long, end: Long, allDay: Boolean, note: String = "") {
        viewModelScope.launch {
            calendarRepo.addLocalEvent(title, note, start, end, allDay)
            refresh.value += 1
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            calendarRepo.deleteEvent(id)
            refresh.value += 1
        }
    }

    /** Blocks a task into the tapped hour slot (task → calendar scheduling). */
    fun scheduleTaskAt(taskId: String, title: String, start: Long) {
        viewModelScope.launch {
            calendarRepo.scheduleTask(taskId, title, start, start + 3600_000L)
            refresh.value += 1
        }
    }

    fun moveBlock(eventId: String, newStart: Long) {
        viewModelScope.launch {
            calendarRepo.moveBlock(eventId, newStart, newStart + 3600_000L)
            refresh.value += 1
        }
    }

    fun syncDeviceCalendars() {
        viewModelScope.launch {
            val n = calendarRepo.syncDeviceCalendars()
            lastSyncMessage = when {
                n == -1 -> "Calendar permission needed – grant READ_CALENDAR"
                n == -2 -> "Device calendar unavailable"
                else -> "Synced $n device events"
            }
            refresh.value += 1
        }
    }

    fun syncFeedsNow() {
        viewModelScope.launch {
            val n = calendarRepo.syncFeeds()
            lastSyncMessage = if (n >= 0) {
                "Fetched $n events from feeds"
            } else {
                "Feed sync failed (offline?)"
            }
            refresh.value += 1
        }
    }

    fun addFeed(url: String, name: String) {
        viewModelScope.launch {
            calendarRepo.addFeed(url, name)
            syncFeedsNow()
        }
    }

    fun removeFeed(id: String) {
        viewModelScope.launch {
            calendarRepo.deleteFeed(id)
            refresh.value += 1
        }
    }

    companion object {
        private const val MILLIS_DAY = 24L * 60 * 60 * 1000
    }
}
