package com.orvane.app.ui.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.core.focus.PomodoroController
import com.orvane.app.core.focus.PomodoroPhase
import com.orvane.app.core.focus.PomodoroService
import com.orvane.app.core.focus.PomodoroState
import com.orvane.app.data.db.CalendarEventEntity
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.repo.CalendarRepository
import com.orvane.app.data.repo.FocusRepository
import com.orvane.app.data.repo.TaskRepository
import com.orvane.app.data.settings.AppSettings
import com.orvane.app.data.settings.PomodoroConfig
import com.orvane.app.data.settings.SettingsRepository
import com.orvane.app.core.util.Time
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** White-noise loops bundled in assets/whitenoise. */
val NOISES = listOf("brown", "rain", "waves", "forest", "cafe")

/** Shared bound for pomodoro in-sheet steppers (minutes). */
const val POMO_MIN_MINUTES = 1

data class FocusStatsState(
    val todaySeconds: Long = 0,
    val weekSeconds: Long = 0,
    val monthSeconds: Long = 0,
    val perTask: List<Pair<String, Long>> = emptyList(),
    val sessionsToday: Int = 0,
)

data class MatrixCell(
    val title: String,
    val tasks: List<TaskEntity>,
)

data class FocusUiState(
    val pomo: PomodoroState = PomodoroState(),
    val configMinutes: Triple<Int, Int, Int> = Triple(25, 5, 15),
    val configCycles: Int = 4,
    val activeTasks: List<TaskEntity> = emptyList(),
    val stats: FocusStatsState = FocusStatsState(),
    val matrix: List<MatrixCell> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val todayEvents: List<CalendarEventEntity> = emptyList(),
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: PomodoroController,
    private val focusRepo: FocusRepository,
    private val taskRepo: TaskRepository,
    calendarRepo: CalendarRepository,
    private val settingsRepo: SettingsRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val pomo: StateFlow<PomodoroState> = controller.state

    val selectedNoise = MutableStateFlow(NOISES.first())

    fun selectNoise(noise: String) {
        selectedNoise.value = noise
    }

    private val refresh = MutableStateFlow(0)

    private val settingsState = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), null)

    private val window = combine(refresh, settingsState) { r, s -> s }
        .flatMapLatest { _ ->
            combine(
                focusRepo.observeSince(Time.startOfToday()),
                focusRepo.observeSince(System.currentTimeMillis() - 7L * 24 * 3600_000),
                focusRepo.observeSince(System.currentTimeMillis() - 30L * 24 * 3600_000),
            ) { today, week, month -> Triple(today, week, month) }
        }

    private val todayTasksFlow = combine(refresh, taskRepo.observeAllActive()) { _, active ->
        active.filter { t -> t.due != null && t.due >= Time.startOfToday() && t.due < Time.endOfToday() }
            .sortedBy { it.due }
    }

    private val todayEventsFlow = calendarRepo.observeEvents(
        Time.startOfToday(),
        Time.endOfToday(),
    )

    val state: StateFlow<FocusUiState> = combine(
        combine(pomo, settingsState) { p, s -> p to s?.pomodoro },
        combine(taskRepo.observeAllActive(), todayTasksFlow) { active, today -> active to today },
        window,
        combine(todayEventsFlow, refresh) { ev, _ -> ev },
    ) { (pomoState, cfg), (active, todayTasks), windows, todayEvents ->
        val (today, week, month) = windows
        val stats = FocusStatsState(
            todaySeconds = today.filter { it.type == com.orvane.app.data.db.SessionType.POMODORO_FOCUS }
                .sumOf { it.seconds },
            weekSeconds = week.filter { it.type == com.orvane.app.data.db.SessionType.POMODORO_FOCUS }
                .sumOf { it.seconds },
            monthSeconds = month.filter { it.type == com.orvane.app.data.db.SessionType.POMODORO_FOCUS }
                .sumOf { it.seconds },
            perTask = month
                .filter { it.taskId != null && it.type == com.orvane.app.data.db.SessionType.POMODORO_FOCUS }
                .groupBy { it.taskId!! }
                .map { (tid, list) ->
                    tid to list.sumOf { it.seconds }
                }
                .map { (tid, secs) ->
                    (active.firstOrNull { it.id == tid }?.title ?: "Task") to secs
                }
                .sortedByDescending { it.second }
                .take(5),
            sessionsToday = today
                .count { it.type == com.orvane.app.data.db.SessionType.POMODORO_FOCUS },
        )
        // Eisenhower: importance from priority, urgency from the due date.
        val urgent: (TaskEntity) -> Boolean = { t ->
            t.due != null && t.due < Time.startOfDayEpoch(LocalDate.now().plusDays(2))
        }
        val important: (TaskEntity) -> Boolean = { t ->
            t.priority >= com.orvane.app.data.db.TaskPriority.MEDIUM
        }
        val matrix = listOf(
            MatrixCell(
                "Do first (urgent + important)",
                active.filter { urgent(it) && important(it) }.sortedBy { it.due ?: Long.MAX_VALUE },
            ),
            MatrixCell(
                "Schedule (important, not urgent)",
                active.filter { !urgent(it) && important(it) }.sortedBy { it.due ?: Long.MAX_VALUE },
            ),
            MatrixCell(
                "Delegate (urgent, less important)",
                active.filter { urgent(it) && !important(it) }.sortedBy { it.due ?: Long.MAX_VALUE },
            ),
            MatrixCell(
                "Later (neither)",
                active.filter { !urgent(it) && !important(it) }.sortedBy { it.due ?: Long.MAX_VALUE },
            ),
        )
        FocusUiState(
            pomo = pomoState,
            configMinutes = Triple(
                cfg?.focusMinutes ?: 25,
                cfg?.shortBreakMinutes ?: 5,
                cfg?.longBreakMinutes ?: 15,
            ),
            configCycles = cfg?.cyclesBeforeLongBreak ?: 4,
            activeTasks = active,
            stats = stats,
            matrix = matrix,
            todayTasks = todayTasks,
            todayEvents = todayEvents.sortedBy { it.start },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), FocusUiState())

    init {
        // keep stats fresh when a session gets recorded by the service
        pomo.onEach {
            if (it.phase == PomodoroPhase.IDLE) refresh.value += 1
        }.launchIn(viewModelScope)
    }

    // ---- Control ----

    fun setPomodoro(config: PomodoroConfig) {
        viewModelScope.launch { settingsRepo.setPomodoroConfig(config) }
    }

    /** Nudge a pomodoro length by ±minutes (sheet ±5 steppers), persisted. */
    fun nudgePomodoro(field: Int, delta: Int) {
        viewModelScope.launch {
            val cfg = settingsRepo.current().pomodoro
            val next = when (field) {
                0 -> cfg.copy(focusMinutes = (cfg.focusMinutes + delta).coerceIn(POMO_MIN_MINUTES, 180))
                1 -> cfg.copy(shortBreakMinutes = (cfg.shortBreakMinutes + delta).coerceIn(POMO_MIN_MINUTES, 30))
                2 -> cfg.copy(longBreakMinutes = (cfg.longBreakMinutes + delta).coerceIn(POMO_MIN_MINUTES, 60))
                else -> cfg.copy(cyclesBeforeLongBreak = (cfg.cyclesBeforeLongBreak + delta).coerceIn(1, 12))
            }
            settingsRepo.setPomodoroConfig(next)
        }
    }

    fun startPomodoro() {
        viewModelScope.launch {
            val cfg = settingsRepo.current().pomodoro
            val taskId = pomo.value.taskId ?: selectedTaskId
            PomodoroService.start(
                appContext,
                cfg.focusMinutes,
                cfg.shortBreakMinutes,
                cfg.longBreakMinutes,
                cfg.cyclesBeforeLongBreak,
                taskId,
                selectedNoise.value,
                stopwatch = false,
            )
            refresh.value += 1
        }
    }

    var selectedTaskId: String? = null
        private set

    fun selectTask(taskId: String?) {
        selectedTaskId = taskId
    }

    fun startStopwatch() {
        viewModelScope.launch {
            val taskId = pomo.value.taskId ?: selectedTaskId
            PomodoroService.start(
                appContext,
                0, 0, 0, 1,
                taskId,
                null,
                stopwatch = true,
            )
            refresh.value += 1
        }
    }

    fun pause() = PomodoroService.pause(appContext)

    fun resume() = PomodoroService.resume(appContext)

    fun stop() {
        PomodoroService.stop(appContext)
        refresh.value += 1
    }

    fun skip() {
        viewModelScope.launch {
            controller.skipPhase()
            refresh.value += 1
        }
    }

    /** Completes (or un-completes) a task from the Today plan. */
    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (task.completedAt == null) {
                taskRepo.complete(task.id)
            } else {
                taskRepo.uncomplete(task.id)
            }
            refresh.value += 1
        }
    }
}

