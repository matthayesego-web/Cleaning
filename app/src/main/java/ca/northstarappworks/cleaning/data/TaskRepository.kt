package ca.northstarappworks.cleaning.data

import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    val tasks: StateFlow<List<CleaningTask>>
    val completions: StateFlow<List<CompletionRecord>>

    fun add(task: CleaningTask)
    fun update(task: CleaningTask)
    fun addCompletion(record: CompletionRecord)
    fun removeCompletion(recordId: String)
    fun replaceTasks(tasks: List<CleaningTask>)
    fun replaceCompletions(records: List<CompletionRecord>)
}
