package ca.northstarappworks.cleaning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.northstarappworks.cleaning.data.PersistentTaskRepository
import ca.northstarappworks.cleaning.data.TaskRepository
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.Priority
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository = PersistentTaskRepository(application)

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
