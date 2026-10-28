package com.orvane.app.ui.focus

import com.orvane.app.data.settings.PomodoroConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/** B2: in-sheet stepper deltas clamp to the spec'd ranges (pure bounds math). */
class FocusSteppersBoundsTest {

    private val cfg =
        PomodoroConfig(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15, cyclesBeforeLongBreak = 4)

    // ---- focus 5..90 ----

    @Test
    fun focusStaysInRange() {
        assertEquals(30, FocusViewModel.boundedConfig(cfg, 0, 5).focusMinutes)
        assertEquals(20, FocusViewModel.boundedConfig(cfg, 0, -5).focusMinutes)
    }

    @Test
    fun focusClampedAtMax90() {
        assertEquals(90, FocusViewModel.boundedConfig(cfg.copy(focusMinutes = 88), 0, 5).focusMinutes)
        assertEquals(90, FocusViewModel.boundedConfig(cfg.copy(focusMinutes = 90), 0, 5).focusMinutes)
    }

    @Test
    fun focusClampedAtMin5() {
        assertEquals(5, FocusViewModel.boundedConfig(cfg.copy(focusMinutes = 10), 0, -10).focusMinutes)
        assertEquals(5, FocusViewModel.boundedConfig(cfg.copy(focusMinutes = 5), 0, -5).focusMinutes)
    }

    // ---- short break 1..30 ----

    @Test
    fun shortBreakStaysInRange() {
        assertEquals(10, FocusViewModel.boundedConfig(cfg, 1, 5).shortBreakMinutes)
    }

    @Test
    fun shortBreakClampedAtMax30() {
        assertEquals(30, FocusViewModel.boundedConfig(cfg.copy(shortBreakMinutes = 28), 1, 5).shortBreakMinutes)
    }

    @Test
    fun shortBreakClampedAtMin1() {
        assertEquals(1, FocusViewModel.boundedConfig(cfg.copy(shortBreakMinutes = 2), 1, -5).shortBreakMinutes)
    }

    // ---- long break 5..60 ----

    @Test
    fun longBreakStaysInRange() {
        assertEquals(20, FocusViewModel.boundedConfig(cfg, 2, 5).longBreakMinutes)
    }

    @Test
    fun longBreakClampedAtMax60() {
        assertEquals(60, FocusViewModel.boundedConfig(cfg.copy(longBreakMinutes = 58), 2, 5).longBreakMinutes)
    }

    @Test
    fun longBreakClampedAtMin5() {
        assertEquals(5, FocusViewModel.boundedConfig(cfg.copy(longBreakMinutes = 7), 2, -5).longBreakMinutes)
    }

    // ---- rounds-until-long-break 2..8 ----

    @Test
    fun cyclesStaysInRange() {
        assertEquals(5, FocusViewModel.boundedConfig(cfg, 3, 1).cyclesBeforeLongBreak)
        assertEquals(3, FocusViewModel.boundedConfig(cfg, 3, -1).cyclesBeforeLongBreak)
    }

    @Test
    fun cyclesClampedAtMax8() {
        assertEquals(8, FocusViewModel.boundedConfig(cfg.copy(cyclesBeforeLongBreak = 8), 3, 1).cyclesBeforeLongBreak)
    }

    @Test
    fun cyclesClampedAtMin2() {
        assertEquals(2, FocusViewModel.boundedConfig(cfg.copy(cyclesBeforeLongBreak = 6), 3, -5).cyclesBeforeLongBreak)
    }
}
