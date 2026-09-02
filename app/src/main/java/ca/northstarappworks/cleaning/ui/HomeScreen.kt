package ca.northstarappworks.cleaning.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.Priority
import java.time.LocalTime

@Composable
fun OurHomeApp(homeViewModel: HomeViewModel = viewModel()) {
    val tasks by homeViewModel.tasks.collectAsState()
    var addingTask by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addingTask = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add task") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Text("Good ${dayPart()}, Matt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Let’s make home feel good.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(22.dp))
                Text("Today · ${tasks.count { !it.completed }} tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            items(tasks, key = { it.id }) { task ->
                TaskCard(task = task, onChecked = { homeViewModel.setCompleted(task, it) })
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    if (addingTask) {
        AddTaskDialog(
            onDismiss = { addingTask = false },
            onAdd = { title, assignee, priority ->
                homeViewModel.addTask(title, assignee, priority)
                addingTask = false
            }
        )
    }
}

@Composable
private fun TaskCard(task: CleaningTask, onChecked: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = task.completed, onCheckedChange = onChecked)
            Column(modifier = Modifier.weight(1f).padding(top = 7.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                Text(
                    if (task.completed) "Completed by ${task.completedBy?.label}" else "${task.assignee.label} · ${task.priority.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.priority == Priority.URGENT && !task.completed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Assignee, Priority) -> Unit) {
    var title by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf(Assignee.EITHER) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(value = title, onValueChange = { title = it }, label = { Text("What needs doing?") }, singleLine = true)
                Text("Assigned to", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Assignee.entries.forEach { value ->
                        AssistChip(onClick = { assignee = value }, label = { Text(if (assignee == value) "✓ ${value.label}" else value.label) })
                    }
                }
                Text("Priority", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.forEach { value ->
                        AssistChip(onClick = { priority = value }, label = { Text(if (priority == value) "✓ ${value.label}" else value.label) })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onAdd(title, assignee, priority) }, enabled = title.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun dayPart(): String = when (LocalTime.now().hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    in 17..20 -> "evening"
    else -> "night"
}
