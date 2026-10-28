package com.orvane.app.ui.focus

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.orvane.app.data.repo.TaskRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** B3: unit tests for the pure part of the quadrant drag-drop state logic. */
class MatrixDragStateLogicTest {

    // ---- hitQuadrant: the drop target lookup over registered bounds ----

    private val bounds = linkedMapOf(
        MatrixQuadrant.DO_FIRST to Rect(0f, 0f, 200f, 300f),      // top-left
        MatrixQuadrant.SCHEDULE to Rect(200f, 0f, 400f, 300f),   // top-right
        MatrixQuadrant.DELEGATE to Rect(0f, 300f, 200f, 600f),   // bottom-left
        MatrixQuadrant.LATER to Rect(200f, 300f, 400f, 600f),    // bottom-right
    )

    @Test
    fun pointOverCenterOfEachQuadrantHitsIt() {
        assertEquals(MatrixQuadrant.DO_FIRST, hitQuadrant(bounds, Offset(100f, 150f)))
        assertEquals(MatrixQuadrant.SCHEDULE, hitQuadrant(bounds, Offset(300f, 150f)))
        assertEquals(MatrixQuadrant.DELEGATE, hitQuadrant(bounds, Offset(100f, 450f)))
        assertEquals(MatrixQuadrant.LATER, hitQuadrant(bounds, Offset(300f, 450f)))
    }

    @Test
    fun pointOnSharedFreeSpaceMissesEveryQuadrant() {
        assertNull(hitQuadrant(bounds, Offset(400.5f, 150f)))   // right gutter
        assertNull(hitQuadrant(bounds, Offset(100f, -1f)))      // above grid
        assertNull(hitQuadrant(bounds, Offset(500f, 500f)))     // past everything
    }

    @Test
    fun emptyBoundsReturnsNull() {
        assertNull(hitQuadrant(emptyMap(), Offset.Zero))
    }

    // ---- matrixCellsToQuadrants: VM cell order -> grid quadrant order ----

    @Test
    fun cellsMapInOrderToGridQuadrants() {
        val t0 = com.orvane.app.data.db.TaskEntity(id = "0", title = "do first", createdAt = 0L, modifiedAt = 0L)
        val t1 = com.orvane.app.data.db.TaskEntity(id = "1", title = "schedule", createdAt = 0L, modifiedAt = 0L)
        val t2 = com.orvane.app.data.db.TaskEntity(id = "2", title = "delegate", createdAt = 0L, modifiedAt = 0L)
        val t3 = com.orvane.app.data.db.TaskEntity(id = "3", title = "later", createdAt = 0L, modifiedAt = 0L)
        val cells = mapOf(
            MatrixQuadrant.DO_FIRST to listOf(t0),
            MatrixQuadrant.SCHEDULE to listOf(t1),
            MatrixQuadrant.DELEGATE to listOf(t2),
            MatrixQuadrant.LATER to listOf(t3),
        )
        assertEquals(cells, matrixCellsToQuadrants(listOf(
            MatrixCell("Do first", listOf(t0)),
            MatrixCell("Schedule", listOf(t1)),
            MatrixCell("Delegate", listOf(t2)),
            MatrixCell("Later", listOf(t3)),
        )))
    }

    @Test
    fun missingCellsMapToEmptyLists() {
        assertTrue(matrixCellsToQuadrants(emptyList()).all { it.value.isEmpty() })
    }

    // ---- VM enum <-> repo enum alignment: commitMatrixMove relies on
    //      MatrixQuadrant.valueOf(name) / MatrixQuadrant.name — silent drift
    //      between the two enums would drop moves on the floor. ----

    @Test
    fun vmAndRepoQuadrantEnumsStayAligned() {
        val vmNames = MatrixQuadrant.entries.map { it.name }
        val repoNames =
            com.orvane.app.data.repo.TaskRepository.MatrixQuadrant.entries.map { it.name }
        assertTrue(vmNames == repoNames)
    }
}
