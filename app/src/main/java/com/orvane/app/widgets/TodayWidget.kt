package com.orvane.app.widgets

import android.content.Context
import androidx.compose.ui.graphics.Color
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
import com.orvane.app.MainActivity
import com.orvane.app.data.db.SmartList
import com.orvane.app.data.db.TaskEntity
import com.orvane.app.data.repo.SortKeys
import com.orvane.app.data.repo.TaskRepository
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
        val bg = widgetColor(Color(0xF2121319))
        val fg = widgetColor(Color.White)
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
                    style = TextStyle(fontSize = 12.sp, color = widgetColor(Color(0xFFB8C1FF))),
                )
            }
            Spacer(modifier = GlanceModifier.height(6.dp))
            if (tasks.isEmpty()) {
                Text(
                    "Nothing due today — enjoy!",
                    style = TextStyle(fontSize = 12.sp, color = widgetColor(Color(0xFFAAAAB4))),
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
                            .background(widgetColor(Color(0xFF333845)))
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
                            color = if (task.completedAt != null) widgetColor(Color(0xFF8A8A92)) else fg,
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
                                    widgetColor(
                                        if (task.priority == 3) com.orvane.app.ui.theme.PriorityHigh else com.orvane.app.ui.theme.PriorityMedium,
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
        OrvaneWidgets.refreshAll(context)
    }

    companion object {
        val TASK_ID = ActionParameters.Key<String>("task_id")
    }
}
