package com.orvane.app.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.orvane.app.data.repo.HabitRepository
import com.orvane.app.data.repo.TaskRepository
import com.orvane.app.data.settings.SettingsRepository
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
interface OrvaneWidgetsEntryPoint {
    fun taskRepository(): TaskRepository

    fun habitRepository(): HabitRepository

    fun settingsRepository(): SettingsRepository
}

internal fun widgetsEntryPoint(context: Context): OrvaneWidgetsEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, OrvaneWidgetsEntryPoint::class.java)

/**
 * Central helper for refreshing all Orvane widgets. Repositories call [refreshAll]
 * after data changes. Active pomodoro state is mirrored via [pomodoroTick] so the
 * mini widget can tick.
 */
object OrvaneWidgets {

    private val _pomodoroTick = MutableStateFlow(0L)
    val pomodoroTick: StateFlow<Long> get() = _pomodoroTick

    fun bumpPomodoro() {
        _pomodoroTick.value = System.currentTimeMillis()
    }

    suspend fun refreshAll(context: Context) {
        runCatching {
            TodayWidget().updateAll(context)
        }.onFailure {
            android.util.Log.w("OrvaneWidgets", "today refresh failed", it)
        }
        runCatching {
            com.orvane.app.widgets.HabitCheckWidget().updateAll(context)
        }.onFailure {
            android.util.Log.w("OrvaneWidgets", "habit refresh failed", it)
        }
        runCatching {
            com.orvane.app.widgets.PomodoroMiniWidget().updateAll(context)
        }.onFailure {
            android.util.Log.w("OrvaneWidgets", "pomodoro refresh failed", it)
        }
    }
}
