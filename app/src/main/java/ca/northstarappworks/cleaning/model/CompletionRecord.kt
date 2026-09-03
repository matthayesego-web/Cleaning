package ca.northstarappworks.cleaning.model

import java.time.Instant

data class CompletionRecord(
    val id: String,
    val taskId: String,
    val taskTitle: String,
    val room: String,
    val completedBy: Assignee,
    val completedAt: Instant = Instant.now()
)
