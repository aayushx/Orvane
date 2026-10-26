package com.tasky.app.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.tasky.app.MainActivity
import com.tasky.app.core.focus.PomodoroController
import com.tasky.app.core.focus.PomodoroPhase
import com.tasky.app.core.focus.PomodoroService
import com.tasky.app.data.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

/**
 * Entry point so widgets (created by Glance, not by Hilt members-injection)
 * can read live focus state and settings.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetControllerEntryPoint {
    fun pomodoroController(): PomodoroController

    fun settingsRepository(): SettingsRepository
}

/** Quick-add shortcut widget: one tap opens Tasky straight into quick add. */
class QuickAddWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*>? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(darkBg)
                        .padding(8.dp),
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionRunCallback<QuickAddAction>()),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                    ) {
                        Text(
                            "+",
                            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = accentFg),
                            maxLines = 1,
                        )
                        Text(
                            "New task",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = whiteFg),
                            maxLines = 1,
                            modifier = GlanceModifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@AndroidEntryPoint
class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}

/** Opens MainActivity straight into the quick-add sheet. */
class QuickAddAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            putExtra(MainActivity.EXTRA_OPEN_QUICK_ADD, true)
        }
        runCatching { context.startActivity(intent) }
    }
}

/** Mini pomodoro widget: countdown + start / pause / stop from the home screen. */
class PomodoroMiniWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*>? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetControllerEntryPoint::class.java,
        )
        val state = ep.pomodoroController().state.value
        val defaults = runBlocking { ep.settingsRepository().current() }.pomodoro
        provideContent {
            GlanceTheme {
                PomodoroContent(state, defaults.focusMinutes)
            }
        }
    }

    @Composable
    private fun PomodoroContent(state: com.tasky.app.core.focus.PomodoroState, idleMinutes: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(darkBg)
                .padding(10.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            val phaseLabel = when (state.phase) {
                PomodoroPhase.FOCUS -> "Focus"
                PomodoroPhase.SHORT_BREAK -> "Break"
                PomodoroPhase.LONG_BREAK -> "Long break"
                PomodoroPhase.IDLE -> "Pomodoro"
            }
            Text(
                phaseLabel,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = grayFg),
                maxLines = 1,
            )
            val secs = if (state.phase == PomodoroPhase.IDLE) idleMinutes * 60L else state.remainingSeconds
            Text(
                "%02d:%02d".format(secs / 60, secs % 60),
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = whiteFg),
                maxLines = 1,
                modifier = GlanceModifier
                    .padding(top = 2.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            when {
                state.phase == PomodoroPhase.IDLE -> {
                    Text(
                        "▶ Start focus",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accentFg),
                        maxLines = 1,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionRunCallback<StartPomodoroAction>()),
                    )
                }
                state.paused -> {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    ) {
                        Text(
                            "▶ Resume",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accentFg),
                            maxLines = 1,
                            modifier = GlanceModifier.clickable(actionRunCallback<ResumePomodoroAction>()),
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Text(
                            "⏹ Stop",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = grayFg),
                            maxLines = 1,
                            modifier = GlanceModifier.clickable(actionRunCallback<StopPomodoroAction>()),
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    ) {
                        Text(
                            "⏸ Pause",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = whiteFg),
                            maxLines = 1,
                            modifier = GlanceModifier.clickable(actionRunCallback<PausePomodoroAction>()),
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Text(
                            "⏹ Stop",
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = grayFg),
                            maxLines = 1,
                            modifier = GlanceModifier.clickable(actionRunCallback<StopPomodoroAction>()),
                        )
                    }
                }
            }
        }
    }
}

@AndroidEntryPoint
class PomodoroMiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomodoroMiniWidget()
}

/** Starts a pomodoro with the user's configured durations from the widget. */
class StartPomodoroAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val settings = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetControllerEntryPoint::class.java,
        ).settingsRepository().current()
        val config = settings.pomodoro
        PomodoroService.start(
            context = context,
            focusMinutes = config.focusMinutes,
            shortBreakMinutes = config.shortBreakMinutes,
            longBreakMinutes = config.longBreakMinutes,
            cycles = config.cyclesBeforeLongBreak,
            taskId = null,
            noise = parameters[NOISE],
        )
        PomodoroMiniWidget().updateAll(context)
    }

    companion object {
        val NOISE = ActionParameters.Key<String>("noise")
    }
}

class PausePomodoroAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PomodoroService.pause(context)
        PomodoroMiniWidget().updateAll(context)
    }
}

class ResumePomodoroAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PomodoroService.resume(context)
        PomodoroMiniWidget().updateAll(context)
    }
}

class StopPomodoroAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        PomodoroService.stop(context)
        PomodoroMiniWidget().updateAll(context)
    }
}

private val darkBg = widgetColor(Color(0xF2121319))
private val whiteFg = widgetColor(Color.White)
private val accentFg = widgetColor(Color(0xFFB8C1FF))
private val grayFg = widgetColor(Color(0xFF9A9AA4))
