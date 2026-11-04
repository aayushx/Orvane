package com.orvane.app.core.matrix

import com.orvane.app.data.db.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * F9: pure matrix drop resolver — priority snaps to the quadrant's importance
 * level; the due date changes ONLY when the drop crosses the urgency axis
 * (urgent → today, not-urgent + within-2-days → +7 days, else keep).
 */
class MatrixDropResolverTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today: LocalDate = LocalDate.of(2026, 11, 4)

    // ---- same-column drops never touch the date ----

    @Test
    fun `same column urgent to urgent keeps date`() {
        // Task due today (urgent) dropped into DELEGATE (also urgent column).
        val dueToday = com.orvane.app.core.util.Time.at(today, 15, 0)
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.MEDIUM,
            taskDueMillis = dueToday,
            taskCurrentlyUrgent = true,
            target = MatrixDropResolver.MatrixTarget.DELEGATE,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.LOW, r.newPriority)
        assertFalse(r.dueChanged)
        assertEquals(dueToday, r.newDueMillis)
        assertEquals("Moved to Delegate", r.description)
    }

    @Test
    fun `same column not urgent to not urgent keeps date`() {
        // Task due far out (not urgent) dropped into LATER (also not urgent).
        val far = com.orvane.app.core.util.Time.startOfDayEpoch(today.plusDays(30))
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.MEDIUM,
            taskDueMillis = far,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.LATER,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.LOW, r.newPriority)
        assertFalse(r.dueChanged)
        assertEquals(far, r.newDueMillis)
        assertEquals("Moved to Later", r.description)
    }

    // ---- crossing into Urgent: due today ----

    @Test
    fun `crossing into urgent sets due today`() {
        val far = com.orvane.app.core.util.Time.startOfDayEpoch(today.plusDays(20))
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.LOW,
            taskDueMillis = far,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.DO_FIRST,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.MEDIUM, r.newPriority)
        assertTrue(r.dueChanged)
        assertEquals(com.orvane.app.core.util.Time.at(today, 23, 59), r.newDueMillis)
        assertEquals("Moved to Urgent · due today", r.description)
    }

    // ---- crossing into Not-urgent ----

    @Test
    fun `crossing into not urgent moves within-2-days date to plus 7`() {
        val tomorrow = com.orvane.app.core.util.Time.startOfDayEpoch(today.plusDays(1))
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.MEDIUM,
            taskDueMillis = tomorrow,
            taskCurrentlyUrgent = true,
            target = MatrixDropResolver.MatrixTarget.SCHEDULE,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.MEDIUM, r.newPriority)
        assertTrue(r.dueChanged)
        assertEquals(com.orvane.app.core.util.Time.startOfDayEpoch(today.plusDays(7)), r.newDueMillis)
        assertEquals("Moved to Not urgent · due in 7 days", r.description)
    }

    @Test
    fun `crossing into not urgent keeps date beyond 2 days`() {
        val far = com.orvane.app.core.util.Time.startOfDayEpoch(today.plusDays(9))
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.HIGH,
            taskDueMillis = far,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.SCHEDULE,
            today = today,
            zone = zone,
        )
        assertFalse(r.dueChanged)
        assertEquals(far, r.newDueMillis)
        assertEquals("Moved to Not urgent", r.description)
    }

    // ---- no-date task ----

    @Test
    fun `no-date task into urgent gets due today`() {
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.NONE,
            taskDueMillis = null,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.DO_FIRST,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.MEDIUM, r.newPriority)
        assertTrue(r.dueChanged)
        assertEquals(com.orvane.app.core.util.Time.at(today, 23, 59), r.newDueMillis)
    }

    @Test
    fun `no-date task into not urgent stays dateless`() {
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.HIGH,
            taskDueMillis = null,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.LATER,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.LOW, r.newPriority)
        assertFalse(r.dueChanged)
        assertEquals(null, r.newDueMillis)
    }

    // ---- priority always snaps ----

    @Test
    fun `high priority snapped down to medium in important quadrant`() {
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.HIGH,
            taskDueMillis = null,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.SCHEDULE,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.MEDIUM, r.newPriority)
    }

    @Test
    fun `medium priority snapped down to low in not-important quadrant`() {
        val r = MatrixDropResolver.resolve(
            taskPriority = TaskPriority.MEDIUM,
            taskDueMillis = null,
            taskCurrentlyUrgent = false,
            target = MatrixDropResolver.MatrixTarget.DELEGATE,
            today = today,
            zone = zone,
        )
        assertEquals(TaskPriority.LOW, r.newPriority)
    }
}
