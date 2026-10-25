package com.tasky.app.data.repo

import com.tasky.app.data.db.HabitEntity
import com.tasky.app.data.db.HabitFreqType
import com.tasky.app.data.db.TaskPriority
import com.tasky.app.core.util.Time
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Seeds friendly example content during first-run onboarding. */
@Singleton
class SampleDataSeeder @Inject constructor(
    private val tasks: TaskRepository,
    private val habits: HabitRepository,
) {

    /** Idempotent-enough: only called once from onboarding. */
    suspend fun seed() = withContext(Dispatchers.IO) {
        // ----- lists -----
        val workList = tasks.createList("Work", 0xFF006C4C.toInt())
        val homeList = tasks.createList("Home", 0xFF7A5466.toInt())

        tasks.quickAdd("Set up Tasky on all my devices")
        tasks.quickAdd("Pay electricity bill tomorrow 9am !High", listId = homeList)
        tasks.quickAdd("Plan sprint demo friday 2pm !Medium #work", listId = workList)
        tasks.quickAdd("Grocery run sunday #home", listId = homeList)
        tasks.quickAdd("Read 20 pages of the current book", listId = null)

        // A task with subtasks to show the progress bar.
        val parent = tasks.quickAdd("Prepare quarterly review monday 10am !High", listId = workList)
        if (parent != null) {
            tasks.quickAdd("Pull metrics from last quarter", listId = workList, isSubtaskOf = parent.id)
            tasks.quickAdd("Draft slide outline", listId = workList, isSubtaskOf = parent.id)
            tasks.quickAdd("Rehearse 10 minutes", listId = workList, isSubtaskOf = parent.id)
        }

        // ----- habits -----
        habits.upsert(
            HabitEntity(
                id = habits.newId(),
                name = "Morning stretch",
                emoji = "🧘",
                freqType = HabitFreqType.DAILY,
                reminderEnabled = true,
                reminderHour = 7,
                reminderMinute = 30,
                createdAt = System.currentTimeMillis(),
            ),
        )
        habits.upsert(
            HabitEntity(
                id = habits.newId(),
                name = "Evening walk",
                emoji = "🏃",
                freqType = HabitFreqType.WEEKDAYS,
                weekdays = listOf("MO", "TU", "WE", "TH", "FR"),
                reminderEnabled = true,
                reminderHour = 18,
                reminderMinute = 0,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}
