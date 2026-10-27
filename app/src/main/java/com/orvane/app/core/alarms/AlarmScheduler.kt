package com.orvane.app.core.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.orvane.app.core.notifications.NotificationHelper
import com.orvane.app.core.notifications.OrvaneNotifications
import com.orvane.app.data.settings.SettingsRepository
import com.orvane.app.data.repo.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules reminder/habit alarms with automatic exact-vs-inexact fallback. */
interface AlarmScheduler {
    fun scheduleReminder(id: String, atMillis: Long)
    fun cancelReminder(id: String)
    fun scheduleHabit(habit: com.orvane.app.data.db.HabitEntity)
    fun cancelHabit(habitId: String)
    fun scheduleHabitSnooze(habitId: String, atMillis: Long)
    fun canScheduleExact(): Boolean
}

@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    override fun scheduleReminder(id: String, atMillis: Long) {
        val pi = reminderPendingIntent(id)
        try { alarmManager.cancel(pi) } catch (_: Exception) {}
        setCompat(atMillis, pi)
    }

    override fun cancelReminder(id: String) {
        try { alarmManager.cancel(reminderPendingIntent(id)) } catch (_: Exception) {}
    }

    private fun reminderIntent(id: String): Intent =
        Intent(context, TaskReminderReceiver::class.java).apply {
            data = android.net.Uri.parse("orvane://reminder/$id")
            putExtra(OrvaneNotifications.EXTRA_REMINDER_ID, id)
        }

    private fun reminderPendingIntent(id: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (id.hashCode() and Int.MAX_VALUE),
            reminderIntent(id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun habitIntent(habitId: String, isNag: Boolean): Intent =
        Intent(context, HabitReminderReceiver::class.java).apply {
            data = android.net.Uri.parse("orvane://habit/$habitId/$isNag")
            putExtra(OrvaneNotifications.EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_IS_NAG, isNag)
        }

    private fun habitPendingIntent(habitId: String, isNag: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            ((habitId.hashCode() + if (isNag) 7919 else 0) and Int.MAX_VALUE),
            habitIntent(habitId, isNag),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    override fun scheduleHabitSnooze(habitId: String, atMillis: Long) {
        val intent = habitIntent(habitId, false).apply {
            data = android.net.Uri.parse("orvane://habit/$habitId/snooze/$atMillis")
        }
        val requestCode = ((habitId.hashCode() * 31 + (atMillis % 100000).toInt()) and Int.MAX_VALUE)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try { alarmManager.cancel(pi) } catch (_: Exception) {}
        setCompat(atMillis, pi)
    }

    private fun scheduleHabit(habitId: String, atMillis: Long, isNag: Boolean) {
        val pi = habitPendingIntent(habitId, isNag)
        try { alarmManager.cancel(pi) } catch (_: Exception) {}
        setCompat(atMillis, pi)
    }

    override fun scheduleHabit(habit: com.orvane.app.data.db.HabitEntity) {
        if (habit.archived) return
        if (habit.reminderEnabled) {
            scheduleHabit(habit.id, nextOccurrenceOf(habit.reminderHour, habit.reminderMinute), isNag = false)
        }
        if (habit.nagEnabled) {
            scheduleHabit(habit.id, nextOccurrenceOf(habit.nagHour, habit.nagMinute), isNag = true)
        }
    }

    override fun cancelHabit(habitId: String) {
        for (isNag in listOf(false, true)) {
            try { alarmManager.cancel(habitPendingIntent(habitId, isNag)) } catch (_: Exception) {}
        }
    }

    /** Exact when permitted; windowed inexact otherwise (Android 14 policy fallback). */
    private fun setCompat(atMillis: Long, pi: PendingIntent) {
        if (atMillis <= System.currentTimeMillis() - 1000) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExact()) {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, atMillis, 10L * 60 * 1000, pi)
        } else {
            @Suppress("UNNECESSARY_SAFE_CALL")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun nextOccurrenceOf(hour: Int, minute: Int): Long {
        val zone = java.time.ZoneId.systemDefault()
        var t = java.time.LocalDate.now().atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        if (t <= System.currentTimeMillis()) {
            t += 24L * 60 * 60 * 1000
        }
        return t
    }

    companion object {
        const val EXTRA_IS_NAG = "com.orvane.app.extra.IS_NAG"
    }
}

/** Fires task reminders: posts the notification. */
@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(OrvaneNotifications.EXTRA_REMINDER_ID) ?: return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = taskRepository.getReminder(reminderId)
                val task = reminder?.let { taskRepository.getById(it.taskId) }
                if (task != null && task.deletedAt == null && task.completedAt == null) {
                    val settings = settingsRepository.current()
                    val notif = NotificationHelper.taskReminder(
                        context = context,
                        taskId = task.id,
                        reminderId = reminder.id,
                        title = task.title,
                        notes = task.note.takeIf { it.isNotBlank() },
                        at = reminder.at,
                        snoozePresets = settings.snoozePresets,
                    )
                    NotificationHelper.post(
                        context,
                        NotificationHelper.stableId(reminder.id),
                        notif,
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("OrvaneReminder", "Failed to fire reminder", e)
            } finally {
                result.finish()
            }
        }
    }
}

/** Fires habit reminders and evening nags. */
@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: com.orvane.app.data.repo.HabitRepository

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(OrvaneNotifications.EXTRA_HABIT_ID) ?: return
        val isNag = intent.getBooleanExtra(AndroidAlarmScheduler.EXTRA_IS_NAG, false)
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val habit = habitRepository.getHabit(habitId) ?: return@launch
                val today = java.time.LocalDate.now()
                val checked = habitRepository.isChecked(habitId, today.toEpochDay())
                if (isNag && checked) {
                    habitRepository.scheduleAlarms(habitId)
                    return@launch
                }
                val notif = NotificationHelper.habitReminder(
                    context = context,
                    habitId = habit.id,
                    title = "${habit.emoji} ${habit.name}" + if (isNag) " — Nudge" else "",
                    isNag = isNag,
                )
                NotificationHelper.post(
                    context,
                    NotificationHelper.stableId("habit_" + habit.id + "_" + if (isNag) "nag" else "daily"),
                    notif,
                )
                // Reschedule for the next day's reminder/nag pair.
                habitRepository.scheduleAlarms(habitId)
            } catch (e: Exception) {
                android.util.Log.e("OrvaneHabit", "Failed to fire habit reminder", e)
            } finally {
                result.finish()
            }
        }
    }
}

/** Re-schedules all alarms after a device reboot. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var habitRepository: com.orvane.app.data.repo.HabitRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                taskRepository.rescheduleAll()
                habitRepository.rescheduleAll()
            } catch (e: Exception) {
                android.util.Log.e("OrvaneBoot", "Boot rescheduling failed", e)
            } finally {
                result.finish()
            }
        }
    }
}
