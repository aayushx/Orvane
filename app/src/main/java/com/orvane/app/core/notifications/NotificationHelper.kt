package com.orvane.app.core.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.orvane.app.MainActivity
import com.orvane.app.R
import com.orvane.app.core.util.Time
import java.time.ZonedDateTime

/**
 * Builds and posts app notifications (reminders, habits). All posting is permission-aware
 * (Android 13+ POST_NOTIFICATIONS) and no-ops when permission is absent.
 */
object NotificationHelper {

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun post(context: Context, id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = NotificationManagerCompat.from(context)
        runCatching { nm.notify(id, notification) }
    }

    /** Stable notification id derived from task/reminder id strings. */
    fun stableId(key: String): Int = key.hashCode()

    fun taskReminder(
        context: Context,
        taskId: String,
        reminderId: String,
        title: String,
        notes: String?,
        at: Long,
        snoozePresets: List<Int>,
    ): Notification {
        val open = Intent(context, MainActivity::class.java).apply {
            putExtra(OrvaneNotifications.EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, stableId("open_$taskId"), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val complete = Intent(context, TaskActionReceiver::class.java).apply {
            action = OrvaneNotifications.ACTION_COMPLETE_TASK
            putExtra(OrvaneNotifications.EXTRA_TASK_ID, taskId)
            putExtra(OrvaneNotifications.EXTRA_REMINDER_ID, reminderId)
        }
        val completePi = PendingIntent.getBroadcast(
            context, stableId("comp_$taskId$reminderId"), complete,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, OrvaneNotifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_orvane_check)
            .setContentTitle(title)
            .setContentText(reminderText(context, at) + (notes?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(android.R.drawable.ic_menu_save, "Complete", completePi)
            .setGroup(OrvaneNotifications.GROUP_REMINDERS)

        // Add up to 2 snooze actions + Complete (3 max incl. Complete)
        var added = 0
        for (minutes in snoozePresets.sorted()) {
            if (added >= 2) break
            val snooze = Intent(context, TaskActionReceiver::class.java).apply {
                action = OrvaneNotifications.ACTION_SNOOZE_TASK
                putExtra(OrvaneNotifications.EXTRA_TASK_ID, taskId)
                putExtra(OrvaneNotifications.EXTRA_REMINDER_ID, reminderId)
                putExtra(OrvaneNotifications.EXTRA_SNOOZE_MIN, minutes)
            }
            val snoozePi = PendingIntent.getBroadcast(
                context, stableId("snooze_${minutes}_$taskId$reminderId"), snooze,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                android.R.drawable.ic_menu_preferences,
                "Snooze " + formatMinutes(minutes),
                snoozePi,
            )
            added += 1
        }
        return builder.build()
    }

    fun habitReminder(
        context: Context,
        habitId: String,
        title: String,
        isNag: Boolean,
    ): Notification {
        val open = Intent(context, MainActivity::class.java).apply {
            putExtra(OrvaneNotifications.EXTRA_HABIT_ID, habitId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, stableId("hopen_$habitId"), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = Intent(context, TaskActionReceiver::class.java).apply {
            action = OrvaneNotifications.ACTION_DONE_HABIT
            putExtra(OrvaneNotifications.EXTRA_HABIT_ID, habitId)
        }
        val donePi = PendingIntent.getBroadcast(
            context, stableId("hdone_$habitId"), done,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = Intent(context, TaskActionReceiver::class.java).apply {
            action = OrvaneNotifications.ACTION_SNOOZE_HABIT
            putExtra(OrvaneNotifications.EXTRA_HABIT_ID, habitId)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context, stableId("hsnooze_$habitId"), snooze,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = if (isNag) "Still open today — a few minutes now keeps the streak!" else "Time to check in!"
        return NotificationCompat.Builder(context, OrvaneNotifications.CHANNEL_HABITS)
            .setSmallIcon(R.drawable.ic_orvane_check)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(android.R.drawable.ic_menu_save, "Done", donePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Later", snoozePi)
            .setGroup(OrvaneNotifications.GROUP_REMINDERS)
            .build()
    }

    fun reminderText(context: Context, at: Long): String {
        val zdt = Time.zdt(at)
        val now = ZonedDateTime.now()
        val dayWord = when {
            zdt.toLocalDate() == now.toLocalDate() -> "Today"
            zdt.toLocalDate() == now.plusDays(1).toLocalDate() -> "Tomorrow"
            else -> zdt.toLocalDate().toString()
        }
        return "$dayWord ${zdt.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))}"
    }

    fun formatMinutes(minutes: Int): String = when {
        minutes >= 1440 && minutes % 1440 == 0 -> "${minutes / 1440}d"
        minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes}m"
    }
}
