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
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.tasky.app.data.db.SmartList
import com.tasky.app.data.db.TaskEntity
import com.tasky.app.data.repo.SortKeys
import com.tasky.app.data.repo.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

/** Home-screen widget with today's tasks; check them off right on the home screen. */
class TodayWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*>? = null

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = widgetsEntryPoint(context).taskRepository()
        val tasks: List<TaskEntity> = runBlocking {
            repo.viewTasks(SmartList.TODAY, SortKeys.DUE).take(7)
        }
        provideContent {
            GlanceTheme {
                TodayContent(tasks)
            }
        }
    }

    @Composable
    private fun TodayContent(tasks: List<TaskEntity>) {
        val bg = ColorProvider(Color.argb(0xF2, 0x12, 0x13, 0x19))
        val fg = ColorProvider(Color.WHITE)
        Column(
            modifier = GlanceModifier.fillMaxSize().background(bg).padding(10.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>())) {
                Text(
                    "Today's Tasks",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = fg),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    "${tasks.size}",
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.argb(0xFF, 0xB8, 0xC1, 0xFF))),
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (tasks.isEmpty()) {
                Text(
                    "Nothing due today — enjoy!",
                    style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.argb(0xFF, 0xAA, 0xAA, 0xB4))),
                )
            }
            for (task in tasks) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .width(22.dp)
                            .height(22.dp)
                            .background(ColorProvider(Color.argb(0xFF, 0x33, 0x38, 0x45)))
                            .clickable(
                                actionRunCallback<ToggleTaskAction>(
                                    actionParametersOf(ToggleTaskAction.TASK_ID to task.id),
                                ),
                            ),
                    ) {
                        Text(
                            if (task.completedAt != null) "✓" else "",
                            style = TextStyle(fontSize = 14.sp, color = fg),
                            modifier = GlanceModifier.padding(3.dp),
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        task.title,
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = if (task.completedAt != null) ColorProvider(Color.argb(0xFF, 0x8a, 0x8a, 0x92)) else fg,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()),
                    )
                    if (task.priority >= 2) {
                        Box(
                            modifier = GlanceModifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(
                                    ColorProvider(
                                        if (task.priority == 3) Color.RED else 0xFFE6B800.toInt(),
                                    ),
                                ),
                        ) {}
                    }
                }
            }
        }
    }
}

@AndroidEntryPoint
class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/** Toggles a task complete/uncomplete from the widget. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TASK_ID] ?: return
        val repo: TaskRepository = widgetsEntryPoint(context).taskRepository()
        val task = repo.getById(taskId)
        if (task?.completedAt != null) {
            repo.uncomplete(taskId)
        } else {
            repo.complete(taskId)
        }
        TaskyWidgets.refreshAll(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
    }
}
