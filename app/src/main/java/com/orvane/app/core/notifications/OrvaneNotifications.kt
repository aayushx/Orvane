package com.orvane.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object OrvaneNotifications {
    const val CHANNEL_REMINDERS = "channel_reminders"
    const val CHANNEL_HABITS = "channel_habits"
    const val CHANNEL_FOCUS = "channel_focus"
    const val CHANNEL_AI = "channel_ai"

    const val GROUP_REMINDERS = "com.orvane.app.REMINDER_GROUP"

    const val EXTRA_TASK_ID = "com.orvane.app.extra.TASK_ID"
    const val EXTRA_REMINDER_ID = "com.orvane.app.extra.REMINDER_ID"
    const val EXTRA_HABIT_ID = "com.orvane.app.extra.HABIT_ID"
    const val EXTRA_SNOOZE_MIN = "com.orvane.app.extra.SNOOZE_MIN"
    const val ACTION_COMPLETE_TASK = "com.orvane.app.action.COMPLETE_TASK"
    const val ACTION_SNOOZE_TASK = "com.orvane.app.action.SNOOZE_TASK"
    const val ACTION_DONE_HABIT = "com.orvane.app.action.DONE_HABIT"
    const val ACTION_SNOOZE_HABIT = "com.orvane.app.action.SNOOZE_HABIT_HABIT"
    const val ACTION_DISMISS_REMINDER = "com.orvane.app.action.DISMISS_REMINDER"

    /**
     * Creates all notification channels (idempotent). Called at app start and defensively
     * before any foreground-service start; fg-service notifications are rejected as
     * "bad notification" when their channel does not exist yet.
     */
    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Task due-date and reminder notifications" },
            NotificationChannel(
                CHANNEL_HABITS, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Daily habit reminders and evening nudges" },
            NotificationChannel(
                CHANNEL_FOCUS, "Focus", NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Persistent pomodoro timer notification"
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_AI, "AI Assistant", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "AI assistant activity" },
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }
}
