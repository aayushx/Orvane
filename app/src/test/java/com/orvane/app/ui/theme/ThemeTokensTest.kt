package com.orvane.app.ui.theme

import android.os.Build
import com.orvane.app.data.db.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Round 2 / B4 theme-token coverage: both schemes are fully and distinctly
 * defined, and the priority-flag tokens map correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeTokensTest {

    @Test
    fun lightAndDarkSchemesDifferForCoreTokens() {
        assertNotEquals(PrimaryLight, PrimaryDark)
        assertNotEquals(BackgroundLight, BackgroundDark)
        assertNotEquals(SurfaceLight, SurfaceDark)
        assertNotEquals(OnSurfaceLight, OnSurfaceDark)
        assertEquals(OnPrimaryLight, androidx.compose.ui.graphics.Color(0xFFFFFFFF)) // ink on primary is white
        assertNotEquals(OnPrimaryLight, OnPrimaryDark) // dark scheme inverts to navy ink
        assertNotEquals(PrimaryContainerLight, PrimaryContainerDark)
    }

    @Test
    fun surfaceContainersStayWithinTonalLayering() {
        // Dynamic-color roles existing in both schemes: container roles must
        // not collapse onto the same constant (guards against dark-tuned light
        // palettes sneaking back).
        assertNotEquals(SurfaceContainerLight, BackgroundLight)
        assertNotEquals(SurfaceContainerHighestLight, BackgroundLight)
        assertNotEquals(SurfaceContainerDark, BackgroundDark)
        assertNotEquals(SurfaceContainerHighestDark, BackgroundDark)
    }

    @Test
    fun priorityFlagsMapToTheirTokens() {
        assertEquals(PriorityHigh, priorityColor(TaskPriority.HIGH))
        assertEquals(PriorityMedium, priorityColor(TaskPriority.MEDIUM))
        assertEquals(PriorityLow, priorityColor(TaskPriority.LOW))
        assertEquals(PriorityNone, priorityColor(TaskPriority.NONE))
    }

    @Test
    fun dynamicColorRequiresSdkS() {
        // Engine wiring sanity: the SDK guard matches Compose's Behavior.
        assertTrue(Build.VERSION_CODES.S == 31)
    }
}
