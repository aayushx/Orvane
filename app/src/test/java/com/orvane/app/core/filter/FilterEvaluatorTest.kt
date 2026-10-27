package com.orvane.app.core.filter

import com.orvane.app.data.db.DateRangeType
import com.orvane.app.data.db.FilterEntity
import com.orvane.app.data.db.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FilterEvaluatorTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val now: Instant = LocalDate.of(2026, 10, 22).atStartOfDay(zone).toInstant()

    private fun task(
        id: String = "t1",
        listId: String? = null,
        priority: Int = 0,
        due: Long? = null,
        completed: Boolean = false,
    ): TaskEntity = TaskEntity(
        id = id,
        title = id,
        listId = listId,
        priority = priority,
        due = due,
        isDueAllDay = true,
        completedAt = if (completed) now.toEpochMilli() else null,
        createdAt = now.toEpochMilli() - 1_000_000,
        modifiedAt = now.toEpochMilli(),
    )

    private fun dayEpochDay(n: Long): Long = n

    private fun millisOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun filter(
        listId: String? = null,
        tagId: String? = null,
        minPriority: Int = 0,
        dateType: Int = DateRangeType.ANY,
    ): FilterEntity = FilterEntity(
        id = "f",
        name = "f",
        listId = listId,
        tagId = tagId,
        minPriority = minPriority,
        dateType = dateType,
    )

    @Test
    fun `today filter includes today and overdue`() {
        val f = filter(dateType = DateRangeType.TODAY)
        val todayTask = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 22)), emptyList())
        val overdue = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 20)), emptyList())
        val future = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 25)), emptyList())
        val noDate = FilterEvaluator.TaskView(task(), emptyList())
        assertTrue(FilterEvaluator.matches(f, todayTask, now, zone))
        assertTrue(FilterEvaluator.matches(f, overdue, now, zone))
        assertTrue(!FilterEvaluator.matches(f, future, now, zone))
        assertTrue(!FilterEvaluator.matches(f, noDate, now, zone))
    }

    @Test
    fun `next 7 days includes future in range`() {
        val f = filter(dateType = DateRangeType.NEXT_7_DAYS)
        val inRange = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 28)), emptyList())
        val outside = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 30)), emptyList())
        assertTrue(FilterEvaluator.matches(f, inRange, now, zone))
        assertTrue(!FilterEvaluator.matches(f, outside, now, zone))
    }

    @Test
    fun `overdue only past`() {
        val f = filter(dateType = DateRangeType.OVERDUE)
        val overdue = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 21)), emptyList())
        val today = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 22)), emptyList())
        assertTrue(FilterEvaluator.matches(f, overdue, now, zone))
        assertTrue(!FilterEvaluator.matches(f, today, now, zone))
    }

    @Test
    fun `list and priority constraints`() {
        val f = filter(listId = "work", minPriority = 2)
        val good = FilterEvaluator.TaskView(task(listId = "work", priority = 3), emptyList())
        val lowPri = FilterEvaluator.TaskView(task(listId = "work", priority = 1), emptyList())
        val wrongList = FilterEvaluator.TaskView(task(listId = "home", priority = 3), emptyList())
        assertTrue(FilterEvaluator.matches(f, good, now, zone))
        assertTrue(!FilterEvaluator.matches(f, lowPri, now, zone))
        assertTrue(!FilterEvaluator.matches(f, wrongList, now, zone))
    }

    @Test
    fun `tag constraint and completed excluded`() {
        val f = filter(tagId = "urgent")
        val tagged = FilterEvaluator.TaskView(task(), listOf("urgent"))
        val untagged = FilterEvaluator.TaskView(task(), listOf("other"))
        val doneTagged = FilterEvaluator.TaskView(task(completed = true), listOf("urgent"))
        assertTrue(FilterEvaluator.matches(f, tagged, now, zone))
        assertTrue(!FilterEvaluator.matches(f, untagged, now, zone))
        assertTrue(!FilterEvaluator.matches(f, doneTagged, now, zone))
    }

    @Test
    fun `custom date range`() {
        val f = filter(dateType = DateRangeType.CUSTOM).copy(
            dueStart = millisOf(2026, 10, 23),
            dueEnd = millisOf(2026, 10, 27),
        )
        val inside = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 24)), emptyList())
        val before = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 22)), emptyList())
        val after = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 28)), emptyList())
        val startInclusive = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 23)), emptyList())
        val endExclusive = FilterEvaluator.TaskView(task(due = millisOf(2026, 10, 27)), emptyList())
        assertTrue(FilterEvaluator.matches(f, inside, now, zone))
        assertTrue(FilterEvaluator.matches(f, startInclusive, now, zone))
        assertTrue(!FilterEvaluator.matches(f, before, now, zone))
        assertTrue(!FilterEvaluator.matches(f, after, now, zone))
        assertTrue(!FilterEvaluator.matches(f, endExclusive, now, zone))
        assertEquals(2, FilterEvaluator.filterAll(f, listOf(inside, before, after, startInclusive, endExclusive), now).size)
    }
}
