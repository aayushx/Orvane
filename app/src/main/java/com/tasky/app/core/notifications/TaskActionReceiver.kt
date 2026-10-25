package com.tasky.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tasky.app.data.repo.HabitRepository
import com.tasky.app.data.repo.TaskRepository
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
                    TaskyNotifications.ACTION_COMPLETE_TASK -> {
                        val taskId = intent.getStringExtra(TaskyNotifications.EXTRA_TASK_ID)
                        if (taskId != null) {
                            taskRepository.completeFromNotification(taskId)
                        }
                    }
                    TaskyNotifications.ACTION_SNOOZE_TASK -> {
                        val taskId = intent.getStringExtra(TaskyNotifications.EXTRA_TASK_ID)
                        val reminderId = intent.getStringExtra(TaskyNotifications.EXTRA_REMINDER_ID)
                        val minutes = intent.getIntExtra(TaskyNotifications.EXTRA_SNOOZE_MIN, 10)
                        if (taskId != null && reminderId != null) {
                            taskRepository.snoozeReminder(taskId, reminderId, minutes)
                        }
                    }
                    TaskyNotifications.ACTION_DONE_HABIT -> {
                        val habitId = intent.getStringExtra(TaskyNotifications.EXTRA_HABIT_ID)
                        if (habitId != null) {
                            habitRepository.checkToday(habitId)
                        }
                    }
                    TaskyNotifications.ACTION_SNOOZE_HABIT -> {
                        val habitId = intent.getStringExtra(TaskyNotifications.EXTRA_HABIT_ID)
                        if (habitId != null) {
                            habitRepository.snoozeHabitToday(habitId, 30)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TaskyAction", "Action failed", e)
            } finally {
                result.finish()
            }
        }
    }
}
