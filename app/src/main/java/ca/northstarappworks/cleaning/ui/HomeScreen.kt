package ca.northstarappworks.cleaning.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.Priority
import java.time.LocalTime

private enum class TaskFilter(val label: String) { ALL("All"), MINE("Mine"), JESSIE("Jessie"), SHARED("Shared") }

@Composable
fun OurHomeApp(homeViewModel: HomeViewModel = viewModel()) {
    val tasks by homeViewModel.tasks.collectAsState()
    var addingTask by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    val visibleTasks = tasks.filter {
        when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.MINE -> it.assignee == Assignee.MATT
            TaskFilter.JESSIE -> it.assignee == Assignee.JESSIE
            TaskFilter.SHARED -> it.assignee == Assignee.EITHER
        }
    }

    Scaffold(
        containerColor = Cream,
        bottomBar = { HomeNavigation() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addingTask = true },
                containerColor = Forest,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New task", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { WelcomeHeader() }
            item { ProgressHero(tasks) }
            item { FilterRow(filter) { filter = it } }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text("${visibleTasks.count { !it.completed }} left", color = MutedInk, fontWeight = FontWeight.Medium)
                }
            }
            items(visibleTasks, key = { it.id }) { task ->
                TaskCard(task, { homeViewModel.setCompleted(task, it) }, Modifier.padding(horizontal = 20.dp))
            }
            if (visibleTasks.isEmpty()) item { EmptyState() }
        }
    }

    if (addingTask) AddTaskDialog(
        onDismiss = { addingTask = false },
        onAdd = { title, room, assignee, priority ->
            homeViewModel.addTask(title, room, assignee, priority)
            addingTask = false
        }
    )
}

@Composable
private fun WelcomeHeader() {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Good ${dayPart()}, Matt", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sync, null, tint = Forest, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("Duck House · All caught up", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
        }
        IconButton(onClick = {}) {
            Box {
                Icon(Icons.Default.NotificationsNone, "Notifications", tint = Ink)
                Box(Modifier.align(Alignment.TopEnd).size(7.dp).background(Peach, CircleShape))
            }
        }
        Avatar("M", Forest)
    }
}

@Composable
private fun ProgressHero(tasks: List<CleaningTask>) {
    val done = tasks.count { it.completed }
    val total = tasks.size.coerceAtLeast(1)
    val progress = done.toFloat() / total
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Forest.copy(alpha = .16f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(Modifier.background(Brush.linearGradient(listOf(ForestDeep, Forest))).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Peach, modifier = Modifier.padding(9.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Today’s little wins", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelLarge)
                    Text(if (done == tasks.size && tasks.isNotEmpty()) "Home goal complete!" else "You’re making great progress", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text("$done/${tasks.size}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f))) {
                Box(Modifier.fillMaxWidth(progress).height(10.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Peach, Color(0xFFFFD0A6)))))
            }
            Spacer(Modifier.height(10.dp))
            Text("$done completed · ${tasks.count { !it.completed }} still waiting", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FilterRow(selected: TaskFilter, onFilter: (TaskFilter) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(TaskFilter.entries) { filter ->
            val active = selected == filter
            Surface(
                Modifier.clip(CircleShape).clickable { onFilter(filter) },
                shape = CircleShape,
                color = if (active) Forest else Color.White,
                shadowElevation = if (active) 0.dp else 2.dp
            ) {
                Text(filter.label, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = if (active) Color.White else MutedInk, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TaskCard(task: CleaningTask, onChecked: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val accent = when (task.priority) {
        Priority.URGENT -> Color(0xFFD35D56)
        Priority.IMPORTANT -> Peach
        Priority.NORMAL -> Mint
    }
    Card(
        modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (task.completed) Color.White.copy(alpha = .58f) else Color.White),
        elevation = CardDefaults.cardElevation(if (task.completed) 0.dp else 3.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(task.completed, onChecked, colors = CheckboxDefaults.colors(checkedColor = Forest, uncheckedColor = Color(0xFFB6C0BC)))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (task.completed) MutedInk else Ink, textDecoration = if (task.completed) TextDecoration.LineThrough else null)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.room, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    Text("  ·  ", color = Color(0xFFBCC4C1))
                    Icon(Icons.Default.Schedule, null, tint = MutedInk, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (task.completed) "Done by ${task.completedBy?.label}" else task.dueLabel, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val avatarColour = when (task.assignee) { Assignee.JESSIE -> Color(0xFF8D6AAE); Assignee.MATT -> Forest; Assignee.EITHER -> Color(0xFF73847E) }
                Avatar(task.assignee.label.take(1), avatarColour)
                if (!task.completed && task.priority != Priority.NORMAL) {
                    Spacer(Modifier.height(7.dp)); Box(Modifier.size(7.dp).background(accent, CircleShape))
                }
            }
            IconButton(onClick = {}, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.MoreHoriz, "Task options", tint = Color(0xFF9AA5A1)) }
        }
    }
}

@Composable
private fun Avatar(initial: String, colour: Color) {
    Box(Modifier.size(38.dp).background(colour.copy(alpha = .13f), CircleShape), contentAlignment = Alignment.Center) {
        Text(initial, color = colour, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun HomeNavigation() {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(true, {}, { Icon(Icons.Default.Home, null) }, label = { Text("Today") })
        NavigationBarItem(false, {}, { Icon(Icons.Default.Check, null) }, label = { Text("History") })
        NavigationBarItem(false, {}, { Icon(Icons.Default.Person, null) }, label = { Text("Household") })
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Mint) { Icon(Icons.Default.Check, null, tint = Forest, modifier = Modifier.padding(18.dp).size(28.dp)) }
        Spacer(Modifier.height(14.dp)); Text("Nothing waiting here", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("That’s a lovely feeling.", color = MutedInk)
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String, Assignee, Priority) -> Unit) {
    var title by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("Around the house") }
    var assignee by remember { mutableStateOf(Assignee.EITHER) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    val rooms = listOf("Around the house", "Kitchen", "Living room", "Bathroom", "Bedroom")
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Column { Text("Add something", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp); Text("Keep it quick and simple.", color = MutedInk, style = MaterialTheme.typography.bodyMedium) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.widthIn(max = 420.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("What needs doing?") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                ChoiceSection("Room", rooms, room) { room = it }
                ChoiceSection("Assigned to", Assignee.entries.map { it.label }, assignee.label) { label -> assignee = Assignee.entries.first { it.label == label } }
                ChoiceSection("Priority", Priority.entries.map { it.label }, priority.label) { label -> priority = Priority.entries.first { it.label == label } }
            }
        },
        confirmButton = { Button({ onAdd(title, room, assignee, priority) }, enabled = title.isNotBlank(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Forest)) { Text("Add task", Modifier.padding(horizontal = 6.dp), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = MutedInk) } }
    )
}

@Composable
private fun ChoiceSection(label: String, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontWeight = FontWeight.Bold, color = Ink)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(values) { value ->
                val active = value == selected
                Surface(Modifier.clip(CircleShape).clickable { onSelect(value) }, color = if (active) Mint else Color(0xFFF1F1ED), shape = CircleShape) {
                    Text(if (active) "✓ $value" else value, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (active) ForestDeep else MutedInk, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun dayPart(): String = when (LocalTime.now().hour) { in 5..11 -> "morning"; in 12..16 -> "afternoon"; in 17..20 -> "evening"; else -> "night" }
