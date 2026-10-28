package com.orvane.app.core.focus

import com.orvane.app.data.db.FocusSessionDao
import com.orvane.app.data.db.FocusSessionEntity
import com.orvane.app.data.db.SessionType
import com.orvane.app.data.repo.TaskRepository
import com.orvane.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class PomodoroPhase {
    IDLE,
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK,
}

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.IDLE,
    val paused: Boolean = false,
    val endAtMillis: Long = 0,          // epoch millis when the phase completes
    val durationSeconds: Long = 0,      // full phase length
    val remainingSeconds: Long = 0,
    val taskId: String? = null,
    val sessionStartedAt: Long = 0,
    val elapsedSeconds: Long = 0,       // in the current phase
    val cyclesCompleted: Int = 0,
    val stopwatch: Boolean = false,
)

/**
 * Owns live pomodoro state and phase transitions. The [PomodoroService] drives
 * the per-second ticking; UI and widgets observe [state].
 */
@Singleton
class PomodoroController @Inject constructor(
    private val sessionDao: FocusSessionDao,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state

    suspend fun tick(): Boolean {
        val cur = _state.value
        if (cur.phase == PomodoroPhase.IDLE || cur.paused) {
            return false
        }
        val now = System.currentTimeMillis()
        if (cur.stopwatch) {
            // Stopwatch counts UP to the cap; it never auto-completes.
            val elapsed = ((now - cur.sessionStartedAt) / 1000L).coerceAtLeast(0)
            _state.value = cur.copy(remainingSeconds = elapsed, elapsedSeconds = elapsed)
            return false
        }
        val remaining = ((cur.endAtMillis - now) / 1000L).coerceAtLeast(0)
        val elapsed = (cur.durationSeconds - remaining).coerceAtLeast(0)
        _state.value = cur.copy(remainingSeconds = remaining, elapsedSeconds = elapsed)
        return remaining <= 0
    }

    suspend fun startSession(
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        cyclesBeforeLong: Int,
        taskId: String?,
        stopwatch: Boolean,
    ) {
        _state.value = focusPhase(
            com.orvane.app.data.settings.PomodoroConfig(
                focusMinutes = focusMinutes,
                shortBreakMinutes = shortBreakMinutes,
                longBreakMinutes = longBreakMinutes,
                cyclesBeforeLongBreak = cyclesBeforeLong,
            ),
            taskId,
            stopwatch,
        )
    }

    /** Records the elapsed focus time and advances the phase. */
    suspend fun phaseComplete() = withContext(Dispatchers.IO) {
        val cur = _state.value
        if (cur.stopwatch) {
            recordFocusSession(cur)
            _state.value = PomodoroState()
            return@withContext
        }
        when (cur.phase) {
            PomodoroPhase.FOCUS -> {
                val settings = settingsRepository.current().pomodoro
                val cycles = cur.cyclesCompleted + 1
                val (phase, minutes) = PomodoroController.nextBreakPhase(cycles, settings)
                recordFocusSession(cur)
                setPhase(phase, minutes * 60L, cur.copy(cyclesCompleted = cycles))
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> {
                sessionDao.insert(
                    FocusSessionEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = null,
                        type = SessionType.POMODORO_BREAK,
                        startedAt = System.currentTimeMillis() - cur.durationSeconds * 1000,
                        endedAt = System.currentTimeMillis(),
                        seconds = cur.durationSeconds,
                    ),
                )
                val settings = settingsRepository.current().pomodoro
                setPhase(PomodoroPhase.FOCUS, settings.focusMinutes * 60L, cur)
            }
            PomodoroPhase.IDLE -> {}
        }
    }

    private suspend fun recordFocusSession(cur: PomodoroState) {
        val seconds = cur.elapsedSeconds
        if (seconds <= 0) return
        sessionDao.insert(
            FocusSessionEntity(
                id = UUID.randomUUID().toString(),
                taskId = cur.taskId,
                type = SessionType.POMODORO_FOCUS,
                startedAt = cur.sessionStartedAt,
                endedAt = System.currentTimeMillis(),
                seconds = seconds,
            ),
        )
        if (cur.taskId != null) {
            taskRepository.addFocusSeconds(cur.taskId, seconds)
            taskRepository.addPomoCount(cur.taskId, 1)
        }
    }

    private fun setPhase(phase: PomodoroPhase, seconds: Long, from: PomodoroState) {
        _state.value = from.copy(
            phase = phase,
            paused = false,
            durationSeconds = seconds,
            endAtMillis = System.currentTimeMillis() + seconds * 1000,
            remainingSeconds = seconds,
            elapsedSeconds = 0,
        )
    }

    fun pause() {
        _state.value = _state.value.copy(paused = true)
    }

    fun resume() {
        val cur = _state.value
        if (cur.phase == PomodoroPhase.IDLE) return
        _state.value = cur.copy(
            paused = false,
            endAtMillis = System.currentTimeMillis() + cur.remainingSeconds * 1000,
        )
    }

    /** Skip to the next phase immediately. */
    suspend fun skipPhase() {
        val cur = _state.value
        if (cur.phase == PomodoroPhase.IDLE) return
        _state.value = cur.copy(remainingSeconds = 0)
        phaseComplete()
    }

    /** Stop; keep partial focus time when a minute or more ran. */
    suspend fun stop() {
        val cur = _state.value
        if (cur.phase == PomodoroPhase.FOCUS && cur.elapsedSeconds >= 60) {
            withContext(Dispatchers.IO) { recordFocusSession(cur) }
        }
        _state.value = PomodoroState()
    }

    /** Title of the linked task, for the persistent notification. */
    suspend fun linkedTaskTitle(taskId: String?): String? =
        taskId?.let { taskRepository.getById(it)?.title }

    companion object {
        /**
         * The initial FOCUS state for a session of the configured length,
         * sized from `cfg.focusMinutes` — pure (clock injected) so tests can
         * pin that the timer honors persisted lengths (Round 2 / B2).
         */
        fun focusPhase(
            cfg: com.orvane.app.data.settings.PomodoroConfig,
            taskId: String?,
            stopwatch: Boolean,
            nowMillis: Long = System.currentTimeMillis(),
        ): PomodoroState = PomodoroState(
            phase = PomodoroPhase.FOCUS,
            paused = false,
            endAtMillis = nowMillis + cfg.focusMinutes * 60_000L,
            durationSeconds = cfg.focusMinutes * 60L,
            remainingSeconds = cfg.focusMinutes * 60L,
            taskId = taskId,
            sessionStartedAt = nowMillis,
            elapsedSeconds = 0,
            cyclesCompleted = 0,
            stopwatch = stopwatch,
        )

        /**
         * Which break follows the Nth completed focus cycle and how long it
         * runs — pure so tests can pin the cycle math (Round 2 / B2).
         */
        fun nextBreakPhase(cyclesCompleted: Int, cfg: com.orvane.app.data.settings.PomodoroConfig): Pair<PomodoroPhase, Int> =
            if (cyclesCompleted % cfg.cyclesBeforeLongBreak.coerceAtLeast(1) == 0) {
                PomodoroPhase.LONG_BREAK to cfg.longBreakMinutes
            } else {
                PomodoroPhase.SHORT_BREAK to cfg.shortBreakMinutes
            }
    }
}
