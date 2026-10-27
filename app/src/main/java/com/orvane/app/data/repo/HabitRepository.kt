package com.orvane.app.data.repo

import com.orvane.app.core.alarms.AlarmScheduler
import com.orvane.app.core.streak.StreakCalculator
import com.orvane.app.core.util.Time
import com.orvane.app.data.db.AppDatabase
import com.orvane.app.data.db.CheckStatus
import com.orvane.app.data.db.HabitCheckDao
import com.orvane.app.data.db.HabitCheckEntity
import com.orvane.app.data.db.HabitDao
import com.orvane.app.data.db.HabitEntity
import com.orvane.app.widgets.OrvaneWidgets
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val db: AppDatabase,
    private val habitDao: HabitDao,
    private val checkDao: HabitCheckDao,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val appContext: Context,
) {

    fun observeActive(): Flow<List<HabitEntity>> = habitDao.observeActive()

    fun observeArchived(): Flow<List<HabitEntity>> = habitDao.observeArchived()

    fun observeAll(): Flow<List<HabitEntity>> = habitDao.observeAll()

    fun observeHabit(id: String): Flow<HabitEntity?> = habitDao.observeById(id)

    fun observeChecksForHabit(habitId: String): Flow<List<HabitCheckEntity>> = checkDao.observeForHabit(habitId)

    fun observeChecksForToday(): Flow<List<HabitCheckEntity>> = checkDao.observeForDay(Time.todayEpochDay())

    fun observeAllChecks(): Flow<List<HabitCheckEntity>> = checkDao.observeAll()

    fun newId(): String = UUID.randomUUID().toString()

    /** Today's check status: null when nothing recorded. */
    suspend fun statusForDay(habitId: String, epochDay: Long): Int? =
        checkDao.find(habitId, epochDay)?.status

    suspend fun getHabit(id: String): HabitEntity? = habitDao.byId(id)

    suspend fun allHabits(): List<HabitEntity> = habitDao.all()

    suspend fun allChecks(): List<HabitCheckEntity> = checkDao.all()

    suspend fun checksForHabit(habitId: String): List<HabitCheckEntity> = checkDao.forHabit(habitId)

    suspend fun isChecked(habitId: String, epochDay: Long): Boolean =
        checkDao.find(habitId, epochDay)?.status == CheckStatus.CHECKED

    /** One-tap check-in (or uncheck) for today. */
    suspend fun checkToday(habitId: String) {
        check(habitId, Time.todayEpochDay())
    }

    /** Toggle the check-in for a given day (supports backdated checks). */
    suspend fun check(habitId: String, epochDay: Long) = withContext(Dispatchers.IO) {
        val existing = checkDao.find(habitId, epochDay)
        when {
            existing == null ->
                checkDao.upsert(
                    HabitCheckEntity(
                        habitId = habitId,
                        epochDay = epochDay,
                        status = CheckStatus.CHECKED,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            existing.status == CheckStatus.CHECKED -> checkDao.deleteFor(habitId, epochDay)
            else -> checkDao.upsert(existing.copy(status = CheckStatus.CHECKED, createdAt = System.currentTimeMillis()))
        }
        OrvaneWidgets.refreshAll(appContext)
    }

    /** Skip today: doesn't break the streak, distinct visual. */
    suspend fun skip(habitId: String, epochDay: Long = Time.todayEpochDay()) = withContext(Dispatchers.IO) {
        val existing = checkDao.find(habitId, epochDay)
        checkDao.upsert(
            (existing ?: HabitCheckEntity(habitId = habitId, epochDay = epochDay, createdAt = System.currentTimeMillis()))
                .copy(status = CheckStatus.SKIPPED, createdAt = System.currentTimeMillis()),
        )
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun removeCheck(habitId: String, epochDay: Long) {
        checkDao.deleteFor(habitId, epochDay)
        OrvaneWidgets.refreshAll(appContext)
    }

    // ---------- CRUD ----------

    suspend fun upsert(habit: HabitEntity, schedule: Boolean = true): String = withContext(Dispatchers.IO) {
        val id = habit.id.ifEmpty { UUID.randomUUID().toString() }
        val toSave = if (habit.id.isEmpty()) habit.copy(id = id, createdAt = System.currentTimeMillis()) else habit
        habitDao.upsert(toSave)
        if (schedule) scheduleAlarms(toSave.id)
        OrvaneWidgets.refreshAll(appContext)
        id
    }

    suspend fun archive(id: String) {
        val habit = habitDao.byId(id) ?: return
        habitDao.setArchived(id, true, (habitDao.maxSortOrder() ?: 0) + 1)
        alarmScheduler.cancelHabit(id)
        OrvaneWidgets.refreshAll(appContext)
    }

    suspend fun unarchive(id: String) {
        habitDao.setArchived(id, false, (habitDao.maxSortOrder() ?: 0) + 1)
        scheduleAlarms(id)
    }

    suspend fun delete(id: String) {
        alarmScheduler.cancelHabit(id)
        checkDao.deleteForHabit(id)
        habitDao.delete(id)
        OrvaneWidgets.refreshAll(appContext)
    }

    // ---------- Stats ----------

    fun statsFor(habit: HabitEntity, checks: List<HabitCheckEntity>): StreakCalculator.Stats =
        StreakCalculator.compute(habit, checks, LocalDate.now())

    /** Heatmap grid: for the last N days, status per day. */
    fun heatmapFor(habit: HabitEntity, checks: List<HabitCheckEntity>, days: Int = 30): Map<Long, Int> {
        val today = Time.todayEpochDay()
        val by = checks.associateBy { it.epochDay }
        val out = HashMap<Long, Int>()
        for (i in (days - 1) downTo 0) {
            val day = today - i
            val e = by[day]
            out[day] = when {
                e == null -> 0              // none
                e.status == CheckStatus.CHECKED -> 2
                e.status == CheckStatus.SKIPPED -> 1
                else -> 0
            }
        }
        return out
    }

    // ---------- Reminders ----------

    suspend fun scheduleAlarms(habitId: String) {
        val habit = habitDao.byId(habitId) ?: return
        alarmScheduler.scheduleHabit(habit)
    }

    suspend fun scheduleAlarms(habit: HabitEntity) {
        alarmScheduler.scheduleHabit(habit)
    }

    suspend fun snoozeHabitToday(habitId: String, minutes: Int) {
        // Simple snooze: schedule a one-shot habit nudge in N minutes.
        alarmScheduler.scheduleHabitSnooze(
            habitId = habitId,
            atMillis = System.currentTimeMillis() + minutes * 60_000L,
        )
    }

    suspend fun rescheduleAll() {
        for (h in habitDao.withReminder() + habitDao.withNag()) {
            alarmScheduler.scheduleHabit(h)
        }
    }

    /** Habits scheduled on the given date (for Today Plan). */
    suspend fun scheduledFor(date: LocalDate): List<HabitEntity> {
        val dowCode = when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "MO"
            java.time.DayOfWeek.TUESDAY -> "TU"
            java.time.DayOfWeek.WEDNESDAY -> "WE"
            java.time.DayOfWeek.THURSDAY -> "TH"
            java.time.DayOfWeek.FRIDAY -> "FR"
            java.time.DayOfWeek.SATURDAY -> "SA"
            else -> "SU"
        }
        val epochDay = date.toEpochDay()
        val checkedKeys = checkDao.forDay(epochDay).associateBy { it.habitId }
        return habitDao.all()
            .filter { it.archived == false }
            .filter { h ->
                when (h.freqType) {
                    com.orvane.app.data.db.HabitFreqType.DAILY -> true
                    com.orvane.app.data.db.HabitFreqType.WEEKDAYS -> h.weekdays.contains(dowCode)
                    else -> true // X times a week/month: always shown as a candidate
                }
            }
            .filter { !checkedKeys.containsKey(it.id) }
    }

    suspend fun overdueUncheckedForToday(): List<HabitEntity> {
        val today = LocalDate.now()
        return scheduledFor(today)
    }
}
