package ca.northstarappworks.cleaning.data

import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryTaskRepository : TaskRepository {
    private val mutableTasks = MutableStateFlow(
        listOf(
            CleaningTask(
                "welcome-1",
                "Empty dishwasher",
                room = "Kitchen",
                dueLabel = "Before dinner",
                assignee = Assignee.JESSIE,
                recurrence = Recurrence.DAILY
            ),
            CleaningTask(
                "welcome-2",
                "Sweep living room",
                room = "Living room",
                assignee = Assignee.EITHER,
                recurrence = Recurrence.WEEKLY
            ),
            CleaningTask(
                "welcome-3",
                "Clean bathroom",
                room = "Bathroom",
                dueLabel = "Tonight",
                assignee = Assignee.MATT,
                priority = Priority.URGENT,
                recurrence = Recurrence.WEEKLY
            )
        )
    )
    override val tasks: StateFlow<List<CleaningTask>> = mutableTasks.asStateFlow()

    private val mutableCompletions = MutableStateFlow<List<CompletionRecord>>(emptyList())
    override val completions: StateFlow<List<CompletionRecord>> = mutableCompletions.asStateFlow()

    override fun add(task: CleaningTask) {
        mutableTasks.value = listOf(task) + mutableTasks.value
    }

    override fun update(task: CleaningTask) {
        mutableTasks.value = mutableTasks.value.map { if (it.id == task.id) task else it }
    }

    override fun addCompletion(record: CompletionRecord) {
        mutableCompletions.value = listOf(record) + mutableCompletions.value
    }

    override fun removeCompletion(recordId: String) {
        mutableCompletions.value = mutableCompletions.value.filterNot { it.id == recordId }
    }
}
