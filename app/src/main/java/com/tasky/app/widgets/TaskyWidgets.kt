package com.tasky.app.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.tasky.app.data.repo.HabitRepository
import com.tasky.app.data.repo.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry point so widgets/action callbacks (created by Glance, not by Hilt)
 * can obtain repositories.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TaskyWidgetsEntryPoint {
    fun taskRepository(): TaskRepository

    fun habitRepository(): HabitRepository
}

internal fun widgetsEntryPoint(context: Context): TaskyWidgetsEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, TaskyWidgetsEntryPoint::class.java)

/**
 * Central helper for refreshing all Tasky widgets. Repositories call [refreshAll]
 * after data changes. Active pomodoro state is mirrored via [pomodoroTick] so the
 * mini widget can tick.
 */
object TaskyWidgets {

    private val _pomodoroTick = MutableStateFlow(0L)
    val pomodoroTick: StateFlow<Long> get() = _pomodoroTick

    fun bumpPomodoro() {
        _pomodoroTick.value = System.currentTimeMillis()
    }

    suspend fun refreshAll(context: Context) {
        runCatching {
            TodayWidget().updateAll(context)
        }.onFailure {
            android.util.Log.w("TaskyWidgets", "today refresh failed", it)
        }
        runCatching {
            com.tasky.app.widgets.HabitCheckWidget().updateAll(context)
        }.onFailure {
            android.util.Log.w("TaskyWidgets", "habit refresh failed", it)
        }
        runCatching {
            com.tasky.app.widgets.PomodoroMiniWidget().updateAll(context)
        }.onFailure {
            android.util.Log.w("TaskyWidgets", "pomodoro refresh failed", it)
        }
    }
}
