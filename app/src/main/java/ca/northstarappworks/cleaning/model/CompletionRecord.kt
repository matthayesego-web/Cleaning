package ca.northstarappworks.cleaning.model

import java.time.Instant
import java.time.LocalDate

data class CompletionRecord(
    val id: String,
    val taskId: String,
    val taskTitle: String,
    val room: String,
    val completedBy: Assignee,
    val scheduledDueDate: LocalDate? = null,
    val completedAt: Instant = Instant.now()
)
