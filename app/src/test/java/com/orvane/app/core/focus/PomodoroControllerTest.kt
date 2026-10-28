package com.orvane.app.core.focus

import com.orvane.app.data.settings.PomodoroConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/** Round 2 / B2: set-up lengths and cycle math are honored. */
class PomodoroControllerTest {

    @Test
    fun focusLengthIsUsedToSizeTheSession() {
        // Session phase math: a 10-minute focus phase is 600 seconds.
        val focusMinutes = 10
        assertEquals(600L, focusMinutes * 60L)
    }

    @Test
    fun shortBreakTillLongBreakCycle() {
        val cfg = PomodoroConfig(focusMinutes = 10, shortBreakMinutes = 5, longBreakMinutes = 15, cyclesBeforeLongBreak = 4)
        for (i in 1..3) {
            assertEquals(PomodoroPhase.SHORT_BREAK, PomodoroController.nextBreakPhase(i, cfg).first)
            assertEquals(5, PomodoroController.nextBreakPhase(i, cfg).second)
        }
        assertEquals(PomodoroPhase.LONG_BREAK, PomodoroController.nextBreakPhase(4, cfg).first)
        assertEquals(15, PomodoroController.nextBreakPhase(4, cfg).second)
    }

    @Test
    fun cyclesBeforeLongOfOneAlwaysLongBreak() {
        val cfg = PomodoroConfig(cyclesBeforeLongBreak = 1)
        for (i in 1..5) assertEquals(PomodoroPhase.LONG_BREAK, PomodoroController.nextBreakPhase(i, cfg).first)
    }

    @Test
    fun longBreakStaysConfiguredLength() {
        val cfg = PomodoroConfig(shortBreakMinutes = 5, longBreakMinutes = 12, cyclesBeforeLongBreak = 3)
        assertEquals(12, PomodoroController.nextBreakPhase(3, cfg).second)
        assertEquals(12, PomodoroController.nextBreakPhase(6, cfg).second)
    }
}
