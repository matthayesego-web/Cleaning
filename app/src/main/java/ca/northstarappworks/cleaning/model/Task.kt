package ca.northstarappworks.cleaning.model

import java.time.Instant
import java.time.LocalDate

enum class Assignee(val label: String) {
    MATT("Matt"),
    JESSIE("Jessie"),
    EITHER("Either")
}

enum class Priority(val label: String) {
    NORMAL("Normal"),
    IMPORTANT("Important"),
    URGENT("Urgent")
}

enum class Recurrence(val label: String) {
    ONE_OFF("One-off"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class CleaningTask(
    val id: String,
    val title: String,
    val notes: String = "",
    val room: String = "Around the house",
    val dueLabel: String = "Today",
    val assignee: Assignee = Assignee.EITHER,
    val priority: Priority = Priority.NORMAL,
    val recurrence: Recurrence = Recurrence.ONE_OFF,
    val nextDueDate: LocalDate = LocalDate.now(),
    val completed: Boolean = false,
    val completedBy: Assignee? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)
