package com.orvane.app.core.focus

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.orvane.app.MainActivity
import com.orvane.app.R
import com.orvane.app.core.notifications.OrvaneNotifications
import com.orvane.app.data.db.SessionType
import com.orvane.app.widgets.PomodoroMiniWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that runs the pomodoro timer: persistent countdown notification,
 * pause/resume/stop actions, completion chime, session recording, survives backgrounding
 * and screen lock.
 */
@AndroidEntryPoint
class PomodoroService : Service() {

    @Inject lateinit var controller: PomodoroController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var noisyPlayer: WhiteNoisePlayer? = null
    private var chimePlayer: android.media.MediaPlayer? = null
    private var ticker: kotlinx.coroutines.Job? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val focusMin = intent.getIntExtra(EXTRA_FOCUS_MIN, 25)
                val breakMin = intent.getIntExtra(EXTRA_BREAK_MIN, 5)
                val longBreakMin = intent.getIntExtra(EXTRA_LONG_BREAK_MIN, 15)
                val cycles = intent.getIntExtra(EXTRA_CYCLES, 4)
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                val noise = intent.getStringExtra(EXTRA_NOISE)
                val stopwatch = intent.getBooleanExtra(EXTRA_STOPWATCH, false)
                startForegroundCompat()
                scope.launch {
                    controller.startSession(focusMin, breakMin, longBreakMin, cycles, taskId, stopwatch)
                }
                if (!noise.isNullOrEmpty()) {
                    getNoisePlayer().play(noise)
                }
                startTicking()
            }
            ACTION_PAUSE -> {
                controller.pause()
                noisyPlayer?.pause()
                stopTicking()
            }
            ACTION_RESUME -> {
                controller.resume()
                runCatching { noisyPlayer?.resume() }
                startTicking()
            }
            ACTION_STOP -> {
                // Detached scope: the DB write for partial focus time must survive stopSelf().
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { controller.stop() }
                stopSelfQuiet()
            }
            ACTION_SKIP -> {
                scope.launch { controller.skipPhase() }
                updateNow()
                refreshWidget()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        // The fg notification must have an existing channel; create channels idempotently
        // in case Application.onCreate did not run (e.g. instrumentation).
        OrvaneNotifications.ensureChannels(applicationContext)
        val notification = buildNotification(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTicking() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                if (controller.state.value.phase == PomodoroPhase.IDLE) break
                val finished = controller.tick()
                updateNow()
                refreshWidget()
                if (finished) {
                    playChime()
                    controller.phaseComplete()
                    updateNow()
                    refreshWidget()
                }
                delay(1000)
            }
            stopSelfQuiet()
        }
    }

    private fun stopTicking() {
        ticker?.cancel()
        updateNow()
        refreshWidget()
    }

    private fun updateNow() {
        scope.launch {
            val state = controller.state.value
            val taskTitle = controller.linkedTaskTitle(state.taskId)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            try {
                nm.notify(NOTIFICATION_ID, buildNotification(taskTitle))
            } catch (e: Exception) {
                android.util.Log.e("OrvaneFocus", "notify failed", e)
            }
            if (state.phase == PomodoroPhase.IDLE) {
                stopSelfQuiet()
            }
        }
    }

    private fun refreshWidget() {
        scope.launch {
            runCatching { PomodoroMiniWidget().updateAll(applicationContext) }
        }
    }

    private fun stopSelfQuiet() {
        noisyPlayer?.stop()
        chimePlayer?.release()
        chimePlayer = null
        stopSelf()
    }

    private fun getNoisePlayer(): WhiteNoisePlayer {
        if (noisyPlayer == null) noisyPlayer = WhiteNoisePlayer(applicationContext)
        return noisyPlayer!!
    }

    private fun playChime() {
        try {
            if (chimePlayer == null) {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                chimePlayer = android.media.MediaPlayer().apply {
                    setDataSource(applicationContext, uri)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    isLooping = false
                    prepare()
                }
                chimePlayer?.start()
            }
        } catch (e: Exception) {
            android.util.Log.w("OrvaneFocus", "chime failed", e)
        }
    }

    private fun remainingLabel(): String {
        val state = controller.state.value
        val secs = state.remainingSeconds
        val m = secs / 60
        val s = secs % 60
        return "%02d:%02d".format(m, s)
    }

    private fun buildNotification(taskTitle: String?): Notification {
        val state = controller.state.value

        val open = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPi = PendingIntent.getActivity(
            applicationContext, 101, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val phaseLabel = when (state.phase) {
            PomodoroPhase.FOCUS -> "Focus"
            PomodoroPhase.SHORT_BREAK -> "Short break"
            PomodoroPhase.LONG_BREAK -> "Long break"
            PomodoroPhase.IDLE -> "Done"
        }
        val builder = NotificationCompat.Builder(applicationContext, OrvaneNotifications.CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_orvane_check)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .setContentTitle("$phaseLabel • ${remainingLabel()}")
            .setContentText(
                when {
                    state.paused -> "Paused"
                    taskTitle != null -> "Focusing on: $taskTitle"
                    else -> "Stay in the zone"
                },
            )
            .setContentIntent(openPi)

        val pauseResumeIntent = Intent(applicationContext, PomodoroService::class.java).apply {
            action = if (state.paused) ACTION_RESUME else ACTION_PAUSE
        }
        val pausePi = PendingIntent.getService(
            applicationContext, 102, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(
            if (state.paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
            if (state.paused) "Resume" else "Pause",
            pausePi,
        )

        val skipIntent = Intent(applicationContext, PomodoroService::class.java).apply { action = ACTION_SKIP }
        val skipPi = PendingIntent.getService(applicationContext, 103, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_media_next, "Skip", skipPi)

        val stopIntent = Intent(applicationContext, PomodoroService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(applicationContext, 104, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
        return builder.build()
    }

    override fun onDestroy() {
        noisyPlayer?.stop()
        noisyPlayer = null
        ticker?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 4242
        const val ACTION_START = "com.orvane.app.focus.START"
        const val ACTION_PAUSE = "com.orvane.app.focus.PAUSE"
        const val ACTION_RESUME = "com.orvane.app.focus.RESUME"
        const val ACTION_STOP = "com.orvane.app.focus.STOP"
        const val ACTION_SKIP = "com.orvane.app.focus.SKIP"
        const val EXTRA_FOCUS_MIN = "focus_min"
        const val EXTRA_BREAK_MIN = "break_min"
        const val EXTRA_LONG_BREAK_MIN = "long_break_min"
        const val EXTRA_CYCLES = "cycles"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_NOISE = "noise"
        const val EXTRA_STOPWATCH = "stopwatch"

        fun start(
            context: Context,
            focusMinutes: Int,
            shortBreakMinutes: Int,
            longBreakMinutes: Int,
            cycles: Int,
            taskId: String?,
            noise: String?,
            stopwatch: Boolean = false,
        ) {
            val intent = Intent(context, PomodoroService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FOCUS_MIN, focusMinutes)
                putExtra(EXTRA_BREAK_MIN, shortBreakMinutes)
                putExtra(EXTRA_LONG_BREAK_MIN, longBreakMinutes)
                putExtra(EXTRA_CYCLES, cycles)
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_NOISE, noise)
                putExtra(EXTRA_STOPWATCH, stopwatch)
            }
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, PomodoroService::class.java).apply { action = ACTION_PAUSE })
        }

        fun resume(context: Context) {
            context.startService(Intent(context, PomodoroService::class.java).apply { action = ACTION_RESUME })
        }

        fun stop(context: Context) {
            context.startService(Intent(context, PomodoroService::class.java).apply { action = ACTION_STOP })
        }
    }
}
