package ca.northstarappworks.cleaning.data

import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryTaskRepository : TaskRepository {
    private val mutableTasks = MutableStateFlow(
        listOf(
            CleaningTask("welcome-1", "Empty dishwasher", room = "Kitchen", dueLabel = "Before dinner", assignee = Assignee.JESSIE),
            CleaningTask("welcome-2", "Sweep living room", room = "Living room", assignee = Assignee.EITHER),
            CleaningTask("welcome-3", "Clean bathroom", room = "Bathroom", dueLabel = "Tonight", assignee = Assignee.MATT, priority = Priority.URGENT),
            CleaningTask("welcome-4", "Wipe kitchen counters", room = "Kitchen", assignee = Assignee.MATT, completed = true, completedBy = Assignee.MATT)
        )
    )
    override val tasks: StateFlow<List<CleaningTask>> = mutableTasks.asStateFlow()

    override fun add(task: CleaningTask) {
        mutableTasks.value = listOf(task) + mutableTasks.value
    }

    override fun update(task: CleaningTask) {
        mutableTasks.value = mutableTasks.value.map { if (it.id == task.id) task else it }
    }
}
