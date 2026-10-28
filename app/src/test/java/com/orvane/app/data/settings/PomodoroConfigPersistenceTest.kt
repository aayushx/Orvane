package com.orvane.app.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * B2: pomodoro lengths persist to / round-trip from the EXISTING settings
 * store (DataStore key `pomodoro_config`) — the setup sheet's write-through
 * must land in the same place the timer and service read from.
 */
@RunWith(RobolectricTestRunner::class)
class PomodoroConfigPersistenceTest {

    @Test
    fun pomodoroConfigRoundTripsThroughSettings() = runBlocking {
        val repo = SettingsRepository(ApplicationProvider.getApplicationContext<Context>())
        val written =
            PomodoroConfig(focusMinutes = 50, shortBreakMinutes = 3, longBreakMinutes = 21, cyclesBeforeLongBreak = 6)
        repo.setPomodoroConfig(written)
        assertEquals(written, repo.current().pomodoro)
    }
}
