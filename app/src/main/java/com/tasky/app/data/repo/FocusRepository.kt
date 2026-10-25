package com.tasky.app.data.repo

import com.tasky.app.data.db.AppDatabase
import com.tasky.app.data.db.FocusSessionDao
import com.tasky.app.data.db.FocusSessionEntity
import com.tasky.app.data.db.SessionType
import com.tasky.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepository @Inject constructor(
    private val db: AppDatabase,
    private val sessionDao: FocusSessionDao,
    private val settingsRepository: SettingsRepository,
) {

    fun observeAll(): Flow<List<FocusSessionEntity>> = sessionDao.observeAll()

    fun observeSince(fromMillis: Long): Flow<List<FocusSessionEntity>> = sessionDao.observeSince(fromMillis)

    fun observeFocusedSecondsSince(fromMillis: Long): Flow<Long> = sessionDao.observeFocusedSecondsSince(fromMillis)

    fun observeSecondsForTask(taskId: String): Flow<Long> = sessionDao.observeSecondsForTask(taskId)

    suspend fun all(): List<FocusSessionEntity> = sessionDao.all()

    suspend fun recordSession(
        id: String,
        taskId: String?,
        type: Int,
        startedAt: Long,
        endedAt: Long,
        seconds: Long,
    ) = withContext(Dispatchers.IO) {
        sessionDao.insert(
            FocusSessionEntity(
                id = id,
                taskId = taskId,
                type = type,
                startedAt = startedAt,
                endedAt = endedAt,
                seconds = seconds,
            ),
        )
    }

    suspend fun recordStopwatchSession(taskId: String?, seconds: Long) = withContext(Dispatchers.IO) {
        if (seconds <= 0) return@withContext
        sessionDao.insert(
            FocusSessionEntity(
                id = java.util.UUID.randomUUID().toString(),
                taskId = taskId,
                type = SessionType.STOPWATCH,
                startedAt = System.currentTimeMillis() - seconds * 1000,
                endedAt = System.currentTimeMillis(),
                seconds = seconds,
            ),
        )
        if (taskId != null) {
            db.taskDao().addFocusSeconds(taskId, seconds)
        }
    }

    suspend fun focusedSecondsInLast(days: Int): List<Pair<Long, Long>> = withContext(Dispatchers.IO) {
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        val out = mutableListOf<Pair<Long, Long>>()
        for (i in (days - 1) downTo 0) {
            val d = today.minusDays(i.toLong())
            val start = d.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val secs = sessionDao.since(start).sumOf { s -> if (s.startedAt < end && s.type == SessionType.POMODORO_FOCUS) s.seconds else 0 }
            out.add(d.toEpochDay() to secs)
        }
        out
    }

    suspend fun sessionsPerTask(last30Days: Boolean = true): List<Pair<String, Long>> = withContext(Dispatchers.IO) {
        val from = if (last30Days) System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 else 0
        sessionDao.since(from)
            .filter { it.type == SessionType.POMODORO_FOCUS && it.taskId != null }
            .groupBy { it.taskId!! }
            .map { (task, list) -> task to list.sumOf { it.seconds } }
            .sortedByDescending { it.second }
    }

    suspend fun totalFocusSecondsAllTime(): Long = withContext(Dispatchers.IO) {
        sessionDao.since(0).filter { it.type == SessionType.POMODORO_FOCUS }.sumOf { it.seconds }
    }
}
