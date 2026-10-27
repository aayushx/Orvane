package com.orvane.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orvane.app.data.repo.HabitRepository
import com.orvane.app.data.repo.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles notification action buttons: Complete / Snooze for tasks, Done / Later for habits.
 */
@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var habitRepository: HabitRepository

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    OrvaneNotifications.ACTION_COMPLETE_TASK -> {
                        val taskId = intent.getStringExtra(OrvaneNotifications.EXTRA_TASK_ID)
                        if (taskId != null) {
                            taskRepository.completeFromNotification(taskId)
                        }
                    }
                    OrvaneNotifications.ACTION_SNOOZE_TASK -> {
                        val taskId = intent.getStringExtra(OrvaneNotifications.EXTRA_TASK_ID)
                        val reminderId = intent.getStringExtra(OrvaneNotifications.EXTRA_REMINDER_ID)
                        val minutes = intent.getIntExtra(OrvaneNotifications.EXTRA_SNOOZE_MIN, 10)
                        if (taskId != null && reminderId != null) {
                            taskRepository.snoozeReminder(taskId, reminderId, minutes)
                        }
                    }
                    OrvaneNotifications.ACTION_DONE_HABIT -> {
                        val habitId = intent.getStringExtra(OrvaneNotifications.EXTRA_HABIT_ID)
                        if (habitId != null) {
                            habitRepository.checkToday(habitId)
                        }
                    }
                    OrvaneNotifications.ACTION_SNOOZE_HABIT -> {
                        val habitId = intent.getStringExtra(OrvaneNotifications.EXTRA_HABIT_ID)
                        if (habitId != null) {
                            habitRepository.snoozeHabitToday(habitId, 30)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OrvaneAction", "Action failed", e)
            } finally {
                result.finish()
            }
        }
    }
}
