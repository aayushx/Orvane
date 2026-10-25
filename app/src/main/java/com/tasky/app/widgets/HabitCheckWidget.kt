package com.tasky.app.widgets

import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.unit.ColorProvider
import com.tasky.app.MainActivity
import com.tasky.app.core.util.Time
import com.tasky.app.data.repo.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Habit check-in widget: shows active habits with one-tap check buttons. */
class HabitCheckWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*>? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo: HabitRepository = widgetsEntryPoint(context).habitRepository()
        val habits = runBlocking {
            repo.observeActive().first().take(6)
        }
        val done = runBlocking {
            val today = Time.todayEpochDay()
            repo.allChecks().filter {
                it.epochDay == today && it.habitId in habits.map { h -> h.id }
            }.associate { it.habitId to it.status }
        }
        provideContent {
            GlanceTheme {
                HabitContent(habits, done)
            }
        }
    }

    @Composable
    private fun HabitContent(
        habits: List<com.tasky.app.data.db.HabitEntity>,
        done: Map<String, Int>,
    ) {
        val bg = ColorProvider(Color.argb(0xF2, 0x12, 0x13, 0x19))
        val fg = ColorProvider(Color.WHITE)
        Column(
            modifier = GlanceModifier.fillMaxSize().background(bg).padding(10.dp),
        ) {
            Text(
                "Habits today",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = fg),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (habits.isEmpty()) {
                Text(
                    "No habits yet",
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.argb(0xFF, 0xAA, 0xAA, 0xB4))),
                )
            }
            for (habit in habits) {
                val checked = done[habit.id] == com.tasky.app.data.db.CheckStatus.CHECKED
                val skipped = done[habit.id] == com.tasky.app.data.db.CheckStatus.SKIPPED
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        "${habit.emoji} ",
                        style = TextStyle(fontSize = 14.sp, color = fg),
                    )
                    Text(
                        habit.name + (if (skipped) " (skipped)" else ""),
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = if (checked || skipped) ColorProvider(Color.argb(0xFF, 0x8a, 0x8a, 0x92)) else fg,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                    )
                    Box(
                        modifier = GlanceModifier
                            .width(34.dp)
                            .height(24.dp)
                            .background(
                                ColorProvider(
                                    when {
                                        checked -> 0xFF22B573.toInt()
                                        skipped -> 0xFF546A7B.toInt()
                                        else -> Color.argb(0xFF, 0x33, 0x38, 0x45)
                                    },
                                ),
                            )
                            .clickable(
                                actionRunCallback<ToggleHabitAction>(
                                    actionParametersOf(ToggleHabitAction.HABIT_ID to habit.id),
                                ),
                            ),
                    ) {
                        Text(
                            if (checked) "✓" else if (skipped) "→" else "",
                            style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.WHITE)),
                            modifier = GlanceModifier.padding(3.dp),
                        )
                    }
                }
            }
        }
    }
}

@AndroidEntryPoint
class HabitCheckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitCheckWidget()
}

/** Toggles habit check-in from the widget. */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[HABIT_ID] ?: return
        val repo: HabitRepository = widgetsEntryPoint(context).habitRepository()
        repo.check(habitId, com.tasky.app.core.util.Time.todayEpochDay())
        TaskyWidgets.refreshAll(context)
    }

    companion object {
        val HABIT_ID = ActionParameters.Key<String>("habit_id")
    }
}
