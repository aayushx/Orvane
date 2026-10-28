package com.orvane.app.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orvane.app.core.streak.StreakCalculator
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.CheckStatus
import com.orvane.app.data.db.HabitEntity
import com.orvane.app.data.db.HabitFreqType
import com.orvane.app.data.repo.HabitRepository
import com.orvane.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One habit row with today's check state, computed stats and per-day statuses. */
data class HabitRow(
    val habit: HabitEntity,
    val stats: StreakCalculator.Stats,
    val checkedToday: Boolean,
    val skippedToday: Boolean,
    val scheduledToday: Boolean,
    /** epochDay → CheckStatus for the last 90 days (absent = no record). */
    val dayStatus: Map<Long, Int>,
    /** Scheduled-day count per window (today inclusive, counting back). */
    val scheduled7: Int,
    val scheduled30: Int,
    val scheduled90: Int,
)

data class HabitsUiState(
    val rows: List<HabitRow> = emptyList(),
    /** Share of active habits checked today (dashboard). */
    val dashboardPercent: Int = 0,
    val doneToday: Int = 0,
    val scheduledTodayCount: Int = 0,
    val archived: List<HabitEntity> = emptyList(),
    /** Habit nag cutoff from Settings ("Reminds until …" (Round 2 / C2)). */
    val nagCutoffHour: Int = 21,
    val nagCutoffMinute: Int = 0,
)

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val repo: HabitRepository,
    settings: SettingsRepository,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)

    val state: StateFlow<HabitsUiState> = combine(
        combine(
            repo.observeActive(),
            repo.observeAllChecks(),
            repo.observeArchived(),
            refresh,
        ) { habits, checks, archived, _ ->
            Triple(habits, checks, archived)
        },
        settings.settings,
    ) { (habits, checks, archived), st ->
        val today = LocalDate.now(Time.zone())
        val rows = habits.map { habit ->
            val mine = checks.filter { it.habitId == habit.id }
            val stats = StreakCalculator.compute(habit, mine, today)
            val todayStatus = checks
                .firstOrNull { it.habitId == habit.id && it.epochDay == today.toEpochDay() }
                ?.status
            val dayStatus = mine
                .filter { it.epochDay > today.toEpochDay() - 90 && it.epochDay <= today.toEpochDay() }
                .associate { it.epochDay to it.status }
            val scheduled = { daysBack: Int ->
                (0 until daysBack).count { StreakCalculator.isScheduledDay(habit, today.minusDays(it.toLong())) }
            }
            HabitRow(
                habit = habit,
                stats = stats,
                checkedToday = todayStatus == CheckStatus.CHECKED,
                skippedToday = todayStatus == CheckStatus.SKIPPED,
                scheduledToday = StreakCalculator.isScheduledDay(habit, today),
                dayStatus = dayStatus,
                scheduled7 = scheduled(7),
                scheduled30 = scheduled(30),
                scheduled90 = scheduled(90),
            )
        }
        val scheduled = rows.filter { it.scheduledToday }
        val done = scheduled.count { it.checkedToday || it.skippedToday }
        HabitsUiState(
            rows = rows,
            dashboardPercent = if (scheduled.isEmpty()) 0 else (100 * done / scheduled.size),
            doneToday = done,
            scheduledTodayCount = scheduled.size,
            archived = archived,
            nagCutoffHour = st.habitNagCutoffHour,
            nagCutoffMinute = st.habitNagCutoffMinute,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(4000), HabitsUiState())

    // ---- Writes ----

    fun toggleCheck(habit: HabitEntity) {
        viewModelScope.launch {
            val today = Time.todayEpochDay()
            if (repo.isChecked(habit.id, today)) {
                repo.removeCheck(habit.id, today)
            } else {
                repo.check(habit.id, today)
            }
            refresh.value += 1
        }
    }

    fun toggleSkip(habit: HabitEntity) {
        viewModelScope.launch {
            val today = Time.todayEpochDay()
            val status = repo.statusForDay(habit.id, today)
            if (status == CheckStatus.SKIPPED) {
                repo.removeCheck(habit.id, today)
            } else {
                repo.skip(habit.id, today)
            }
            refresh.value += 1
        }
    }

    fun save(
        id: String?,
        name: String,
        emoji: String,
        note: String,
        freqType: Int,
        weekdays: List<String>,
        timesPerWeek: Int,
        timesPerMonth: Int,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        nagEnabled: Boolean,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val existing = id?.let { repo.getHabit(it) }
            val habit = (existing ?: HabitEntity(
                id = repo.newId(),
                name = name,
                createdAt = System.currentTimeMillis(),
            )).copy(
                name = name.ifBlank { "Habit" },
                emoji = emoji.ifBlank { "✅" },
                note = note,
                freqType = freqType,
                weekdays = weekdays,
                timesPerWeek = timesPerWeek,
                timesPerMonth = timesPerMonth,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                nagEnabled = nagEnabled,
                archived = false,
            )
            repo.upsert(habit)
            refresh.value += 1
            onSaved()
        }
    }

    fun archive(id: String) {
        viewModelScope.launch {
            repo.archive(id)
            refresh.value += 1
        }
    }

    fun unarchive(id: String) {
        viewModelScope.launch {
            repo.unarchive(id)
            refresh.value += 1
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            refresh.value += 1
        }
    }
}
