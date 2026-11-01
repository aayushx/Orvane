package com.orvane.app.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** v1.2.0 / E7: title editing acceptance rule (blank & unchanged rejected). */
class TaskTitleEditTest {

    @Test
    fun trimmedDraftIsPersisted() {
        assertEquals("Buy milk", TaskDetailViewModel.serializeTitle("  Buy milk  ", "Buy"))
    }

    @Test
    fun blankDraftsAreRejectedSoLastTitleIsKept() {
        assertNull(TaskDetailViewModel.serializeTitle("", "Buy milk"))
        assertNull(TaskDetailViewModel.serializeTitle("   ", "Buy milk"))
        assertNull(TaskDetailViewModel.serializeTitle("\n\t", "Buy milk"))
    }

    @Test
    fun unchangedDraftsAreRejected() {
        assertNull(TaskDetailViewModel.serializeTitle("Buy milk", "Buy milk"))
        // Whitespace-only difference counts as unchanged after trimming.
        assertNull(TaskDetailViewModel.serializeTitle(" Buy milk ", "Buy milk"))
    }
}
