package ca.northstarappworks.cleaning.data

import ca.northstarappworks.cleaning.model.CleaningTask
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    val tasks: StateFlow<List<CleaningTask>>
    fun add(task: CleaningTask)
    fun update(task: CleaningTask)
}
