package com.tasky.app

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tasky.app.data.db.HabitEntity
import com.tasky.app.data.db.HabitFreqType
import com.tasky.app.data.repo.BackupRepository
import com.tasky.app.data.repo.HabitRepository
import com.tasky.app.data.repo.TaskRepository
import com.tasky.app.data.settings.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Shared harness: DataStore must say onboarded=true before MainActivity
 * starts, so each test seeds that flag before the activity launches.
 */
abstract class TaskyUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var settingsRepository: SettingsRepository

    @Before
    fun seedOnboarded() {
        hiltRule.inject()
        runBlocking { settingsRepository.setOnboarded(true) }
    }
}

/** Scenario A: quick-add a task, then complete it from the Today list row. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TaskCreateCompleteTest : TaskyUiTest() {

    @get:Rule(order = 1)
    val compose: ComposeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createTaskThenCompleteIt() {
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Quick add").performClick()
        compose.onNodeWithTag("quick_add_input").performTextInput("Buy milk")
        compose.onNodeWithText("Add task").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Buy milk").assertExists()
        compose.onNodeWithContentDescription("Mark done").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Completed, mark not done").assertExists()
    }
}

/** Scenario B: check in a habit from the Habits tab. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HabitCheckInTest : TaskyUiTest() {

    @get:Rule(order = 1)
    val compose: ComposeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var habits: HabitRepository

    @Test
    fun createdHabitCanBeCheckedIn() {
        runBlocking {
            habits.upsert(
                HabitEntity(
                    id = habits.newId(),
                    name = "Drink water",
                    emoji = "💧",
                    freqType = HabitFreqType.DAILY,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText("Habits").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Mark habit done").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Done today, tap to undo").assertExists()
    }
}

/** Scenario C: start / pause / resume / stop the pomodoro foreground service. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PomodoroServiceTest : TaskyUiTest() {

    @get:Rule(order = 1)
    val compose: ComposeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun pomodoroStartPauseStop() {
        compose.waitForIdle()
        compose.onNodeWithText("Focus").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Start focus").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Start").performClick()
        compose.waitForIdle()
        // Running: shows Pause.
        compose.onNodeWithText("Pause").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Resume").assertExists()
        compose.onNodeWithText("Resume").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Stop").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Start focus").assertExists()
    }
}

/** Scenario D: JSON backup build + restore round trip against the real Room DB. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest : TaskyUiTest() {

    @Inject lateinit var backupRepository: BackupRepository
    @Inject lateinit var tasks: TaskRepository

    @Test
    fun backupThenRestoreKeepsTasks() = runBlocking {
        val before = tasks.quickAdd("Backup round trip task")
        assertTrue(before != null)
        val json = backupRepository.buildBackupJson()
        val restored = backupRepository.restoreBackupJson(json)
        assertTrue(restored >= 1)
        val again = backupRepository.buildBackupJson()
        assertTrue(again.contains("Backup round trip task"))
    }
}
