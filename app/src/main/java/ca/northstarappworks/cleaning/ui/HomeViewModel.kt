package ca.northstarappworks.cleaning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.northstarappworks.cleaning.data.HouseholdPreferences
import ca.northstarappworks.cleaning.data.PersistentTaskRepository
import ca.northstarappworks.cleaning.data.TaskRepository
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository = PersistentTaskRepository(application)
    private val householdPreferences = HouseholdPreferences(application)

    val tasks: StateFlow<List<CleaningTask>> = repository.tasks
    val completions: StateFlow<List<CompletionRecord>> = repository.completions
    val currentUser: StateFlow<Assignee> = householdPreferences.currentUser

    fun addTask(
        title: String,
        room: String,
        assignee: Assignee,
        priority: Priority,
        recurrence: Recurrence
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        repository.add(
            CleaningTask(
                id = UUID.randomUUID().toString(),
                title = cleanTitle,
                room = room,
                assignee = assignee,
                priority = priority,
                recurrence = recurrence,
                nextDueDate = LocalDate.now()
            )
        )
    }

    fun setCurrentUser(assignee: Assignee) {
        householdPreferences.setCurrentUser(assignee)
    }

    fun setCompleted(task: CleaningTask, completed: Boolean) {
        if (completed) {
            completeTask(task)
        } else {
            reopenTask(task)
        }
    }

    private fun completeTask(task: CleaningTask) {
        val completedBy = currentUser.value
        val completedAt = Instant.now()
        val record = CompletionRecord(
            id = UUID.randomUUID().toString(),
            taskId = task.id,
            taskTitle = task.title,
            room = task.room,
            completedBy = completedBy,
            completedAt = completedAt
        )

        repository.addCompletion(record)

        if (task.recurrence == Recurrence.ONE_OFF) {
            repository.update(
                task.copy(
                    completed = true,
                    completedBy = completedBy,
                    completedAt = completedAt
                )
            )
        } else {
            repository.update(
                task.copy(
                    completed = false,
                    completedBy = null,
                    completedAt = null,
                    nextDueDate = nextOccurrence(task.nextDueDate, task.recurrence)
                )
            )
        }
    }

    private fun reopenTask(task: CleaningTask) {
        repository.completions.value
            .filter { it.taskId == task.id }
            .maxByOrNull { it.completedAt }
            ?.let { repository.removeCompletion(it.id) }

        repository.update(
            task.copy(
                completed = false,
                completedBy = null,
                completedAt = null
            )
        )
    }

    private fun nextOccurrence(currentDueDate: LocalDate, recurrence: Recurrence): LocalDate {
        val today = LocalDate.now()
        var next = when (recurrence) {
            Recurrence.ONE_OFF -> currentDueDate
            Recurrence.DAILY -> currentDueDate.plusDays(1)
            Recurrence.WEEKLY -> currentDueDate.plusWeeks(1)
            Recurrence.MONTHLY -> currentDueDate.plusMonths(1)
        }

        while (!next.isAfter(today) && recurrence != Recurrence.ONE_OFF) {
            next = when (recurrence) {
                Recurrence.ONE_OFF -> next
                Recurrence.DAILY -> next.plusDays(1)
                Recurrence.WEEKLY -> next.plusWeeks(1)
                Recurrence.MONTHLY -> next.plusMonths(1)
            }
        }
        return next
    }
}
