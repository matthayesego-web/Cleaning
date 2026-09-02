package ca.northstarappworks.cleaning.ui

import androidx.lifecycle.ViewModel
import ca.northstarappworks.cleaning.data.InMemoryTaskRepository
import ca.northstarappworks.cleaning.data.TaskRepository
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.Priority
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.UUID

class HomeViewModel(
    private val repository: TaskRepository = InMemoryTaskRepository()
) : ViewModel() {
    val tasks: StateFlow<List<CleaningTask>> = repository.tasks

    fun addTask(title: String, room: String, assignee: Assignee, priority: Priority) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        repository.add(
            CleaningTask(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                room = room,
                assignee = assignee,
                priority = priority
            )
        )
    }

    fun setCompleted(task: CleaningTask, completed: Boolean) {
        repository.update(
            task.copy(
                completed = completed,
                completedBy = if (completed) Assignee.MATT else null,
                completedAt = if (completed) Instant.now() else null
            )
        )
    }
}
