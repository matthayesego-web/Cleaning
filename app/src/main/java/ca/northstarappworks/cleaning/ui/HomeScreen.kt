package ca.northstarappworks.cleaning.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
import ca.northstarappworks.cleaning.sync.HouseholdSyncStatus
import ca.northstarappworks.cleaning.sync.HouseholdSyncUiState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class HomeTab { TODAY, WEEK, HISTORY, HOUSEHOLD }
private enum class TaskFilter { ALL, MINE, PARTNER, SHARED }

private data class EditorRequest(
    val task: CleaningTask? = null,
    val dueDate: LocalDate = LocalDate.now()
)

private val roomOrder = listOf(
    "Kitchen", "Living room", "Bathroom", "Bedroom", "Hallway", "Entryway", "Around the house"
)

private val rooms = listOf(
    "Around the house", "Kitchen", "Living room", "Bathroom", "Bedroom", "Hallway", "Entryway"
)

@Composable
fun OurHomeApp(homeViewModel: HomeViewModel = viewModel()) {
    val tasks by homeViewModel.tasks.collectAsState()
    val completions by homeViewModel.completions.collectAsState()
    val currentUser by homeViewModel.currentUser.collectAsState()
    val syncState by homeViewModel.syncUiState.collectAsState()
    val today = rememberToday()

    var selectedTab by remember { mutableStateOf(HomeTab.TODAY) }
    var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }

    Scaffold(
        containerColor = Cream,
        bottomBar = { HomeNavigation(selectedTab) { selectedTab = it } },
        floatingActionButton = {
            if (selectedTab == HomeTab.TODAY) {
                ExtendedFloatingActionButton(
                    onClick = { editorRequest = EditorRequest(dueDate = today) },
                    containerColor = ForestDeep,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(19.dp),
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New task", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            HomeTab.TODAY -> TodayScreen(
                tasks = tasks,
                completions = completions,
                currentUser = currentUser,
                today = today,
                onChecked = homeViewModel::setCompleted,
                onUndo = homeViewModel::undoCompletion,
                onEdit = { editorRequest = EditorRequest(it, it.nextDueDate) },
                onDelete = homeViewModel::deleteTask,
                onBellClick = { selectedTab = HomeTab.HISTORY },
                modifier = Modifier.padding(padding)
            )

            HomeTab.WEEK -> WeekScreen(
                tasks = tasks,
                completions = completions,
                today = today,
                onAddForDate = { editorRequest = EditorRequest(dueDate = it) },
                onEdit = { editorRequest = EditorRequest(it, it.nextDueDate) },
                onDelete = homeViewModel::deleteTask,
                onUndo = homeViewModel::undoCompletion,
                modifier = Modifier.padding(padding)
            )

            HomeTab.HISTORY -> HistoryScreen(
                completions = completions,
                modifier = Modifier.padding(padding)
            )

            HomeTab.HOUSEHOLD -> HouseholdScreen(
                currentUser = currentUser,
                syncState = syncState,
                onCurrentUserChanged = homeViewModel::setCurrentUser,
                modifier = Modifier.padding(padding)
            )
        }
    }

    editorRequest?.let { request ->
        TaskEditorDialog(
            task = request.task,
            initialDueDate = request.dueDate,
            onDismiss = { editorRequest = null },
            onSave = { title, room, assignee, priority, recurrence, intervalDays, dueDate ->
                val existing = request.task
                if (existing == null) {
                    homeViewModel.addTask(title, room, assignee, priority, recurrence, intervalDays, dueDate)
                } else {
                    homeViewModel.updateTask(existing, title, room, assignee, priority, recurrence, intervalDays, dueDate)
                }
                editorRequest = null
            }
        )
    }
}

@Composable
private fun TodayScreen(
    tasks: List<CleaningTask>,
    completions: List<CompletionRecord>,
    currentUser: Assignee,
    today: LocalDate,
    onChecked: (CleaningTask, Boolean) -> Unit,
    onUndo: (CompletionRecord) -> Unit,
    onEdit: (CleaningTask) -> Unit,
    onDelete: (CleaningTask) -> Unit,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    val partner = if (currentUser == Assignee.MATT) Assignee.JESSIE else Assignee.MATT
    val actionableTasks = tasks.filter { !it.completed && (it.occursOn(today) || it.isCarryoverFor(today)) }
    val visibleTasks = actionableTasks.filter { task ->
        when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.MINE -> task.assignee == currentUser
            TaskFilter.PARTNER -> task.assignee == partner
            TaskFilter.SHARED -> task.assignee == Assignee.EITHER
        }
    }
    val completedToday = completions
        .filter { it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today }
        .sortedByDescending { it.completedAt }
    val carryoverCount = actionableTasks.count { it.isCarryoverFor(today) }
    val grouped = visibleTasks.groupBy { it.room }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WelcomeHeader(currentUser, today, completedToday.isNotEmpty(), onBellClick)
        }
        item {
            ProgressHero(completedToday.size, actionableTasks.size, carryoverCount)
        }
        item { FilterRow(filter, currentUser) { filter = it } }

        if (visibleTasks.isEmpty()) {
            item { EmptyState() }
        } else {
            orderedRooms(grouped.keys).forEach { room ->
                val roomTasks = grouped[room].orEmpty().sortedWith(
                    compareByDescending<CleaningTask> { it.isCarryoverFor(today) }
                        .thenByDescending { it.priority.ordinal }
                        .thenBy { it.title.lowercase() }
                )
                item(key = "today-header-$room") { RoomHeader(room, roomTasks.size) }
                items(roomTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        carriedOver = task.isCarryoverFor(today),
                        onChecked = { onChecked(task, it) },
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        if (completedToday.isNotEmpty()) {
            item { CompletedTodayCard(completedToday.take(6), onUndo) }
        }
    }
}

@Composable
private fun WelcomeHeader(
    currentUser: Assignee,
    today: LocalDate,
    hasActivity: Boolean,
    onBellClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 22.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Good ${dayPart()}, ${currentUser.label}", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(3.dp))
                Surface(shape = CircleShape, color = MintSoft, border = BorderStroke(1.dp, Hairline)) {
                    Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = Forest, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            color = MutedInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Surface(shape = CircleShape, color = Paper, border = BorderStroke(1.dp, Hairline), shadowElevation = 2.dp) {
                IconButton(onClick = onBellClick) {
                    Box {
                        Icon(Icons.Default.NotificationsNone, "Recent activity", tint = Ink)
                        if (hasActivity) {
                            Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(Peach, CircleShape))
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Avatar(currentUser.label.take(1), if (currentUser == Assignee.MATT) Forest else Color(0xFF80679A), 44)
        }
    }
}

@Composable
private fun ProgressHero(done: Int, waiting: Int, carried: Int) {
    val total = done + waiting
    val progress = if (total == 0) 1f else done.toFloat() / total
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .shadow(22.dp, RoundedCornerShape(30.dp), ambientColor = ForestDeep.copy(alpha = .17f)),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(ForestDeep, Forest, ForestSoft))).padding(22.dp)) {
            Box(Modifier.offset(x = 230.dp, y = (-44).dp).size(120.dp).background(Color.White.copy(alpha = .05f), CircleShape))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Champagne, modifier = Modifier.padding(10.dp).size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("TODAY AT HOME", color = Color.White.copy(alpha = .65f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Text(
                            when {
                                total == 0 -> "Everything is clear"
                                waiting == 0 -> "Home goal complete"
                                done == 0 -> "A fresh start"
                                else -> "Beautiful progress"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(done.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 29.sp)
                        Text("of $total", color = Color.White.copy(alpha = .62f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = .13f))) {
                    Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Champagne, Peach))))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (waiting == 0) "Nothing waiting" else "$waiting waiting", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall)
                    if (carried > 0) {
                        Text("  •  ", color = Color.White.copy(alpha = .35f))
                        Surface(color = Color.White.copy(alpha = .10f), shape = CircleShape) {
                            Text("$carried carried over", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = Champagne, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: TaskFilter, currentUser: Assignee, onFilter: (TaskFilter) -> Unit) {
    val partner = if (currentUser == Assignee.MATT) Assignee.JESSIE else Assignee.MATT
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TaskFilter.entries) { filter ->
            val active = selected == filter
            val label = when (filter) {
                TaskFilter.ALL -> "All"
                TaskFilter.MINE -> "Mine"
                TaskFilter.PARTNER -> partner.label
                TaskFilter.SHARED -> "Shared"
            }
            Surface(
                Modifier.clip(CircleShape).clickable { onFilter(filter) },
                shape = CircleShape,
                color = if (active) ForestDeep else Paper,
                border = if (active) null else BorderStroke(1.dp, Hairline),
                shadowElevation = if (active) 3.dp else 0.dp
            ) {
                Text(label, Modifier.padding(horizontal = 17.dp, vertical = 9.dp), color = if (active) Color.White else MutedInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RoomHeader(room: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(11.dp), color = roomTint(room), border = BorderStroke(1.dp, roomAccent(room).copy(alpha = .10f))) {
            Icon(roomIcon(room), null, tint = roomAccent(room), modifier = Modifier.padding(8.dp).size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(room, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.weight(1f))
        Surface(shape = CircleShape, color = MintSoft) {
            Text(count.toString(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = MutedInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TaskCard(
    task: CleaningTask,
    carriedOver: Boolean,
    onChecked: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityAccent = when (task.priority) {
        Priority.URGENT -> Color(0xFFC85550)
        Priority.IMPORTANT -> Peach
        Priority.NORMAL -> ForestSoft
    }
    val avatarColour = when (task.assignee) {
        Assignee.JESSIE -> Color(0xFF80679A)
        Assignee.MATT -> Forest
        Assignee.EITHER -> Color(0xFF73847E)
    }

    Card(
        modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, if (carriedOver) Champagne.copy(alpha = .8f) else Hairline),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(start = 4.dp, top = 13.dp, end = 8.dp, bottom = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(48.dp).clip(CircleShape).background(priorityAccent.copy(alpha = if (task.priority == Priority.NORMAL) .18f else .85f)))
            Spacer(Modifier.width(5.dp))
            Checkbox(
                checked = false,
                onCheckedChange = onChecked,
                colors = CheckboxDefaults.colors(checkedColor = Forest, uncheckedColor = Color(0xFFAAB6B1), checkmarkColor = Color.White)
            )
            Spacer(Modifier.width(3.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (carriedOver) {
                        Surface(shape = CircleShape, color = Color(0xFFFFF1DE)) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Update, null, tint = Color(0xFF97612B), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Carried over", color = Color(0xFF7E542B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Icon(if (task.recurrence == Recurrence.ONE_OFF) Icons.Default.Schedule else Icons.Default.Repeat, null, tint = if (task.recurrence == Recurrence.ONE_OFF) MutedInk else Forest, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(task.recurrenceText(), style = MaterialTheme.typography.bodySmall, color = if (task.recurrence == Recurrence.ONE_OFF) MutedInk else ForestDeep)
                    }
                }
            }
            Avatar(task.assignee.label.take(1), avatarColour, 36)
            TaskActionsButton(task.title, onEdit, onDelete)
        }
    }
}

@Composable
private fun TaskActionsButton(taskTitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.MoreVert, "Task options", tint = Color(0xFF8F9B96))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Edit task") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { expanded = false; onEdit() })
            DropdownMenuItem(
                text = { Text("Delete task", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; confirmDelete = true }
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = Paper,
            title = { Text("Delete task?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("“$taskTitle” will leave the shared schedule. Past completion history stays on the scoreboard.") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WeekScreen(
    tasks: List<CleaningTask>,
    completions: List<CompletionRecord>,
    today: LocalDate,
    onAddForDate: (LocalDate) -> Unit,
    onEdit: (CleaningTask) -> Unit,
    onDelete: (CleaningTask) -> Unit,
    onUndo: (CompletionRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    var weekStart by remember(today) { mutableStateOf(today.startOfWeek()) }
    var selectedDate by remember(today) { mutableStateOf(today) }
    val days = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }
    val completedForDate = completions
        .filter { it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate }
        .associateBy { it.taskId }
    val completedIds = completedForDate.keys
    val scheduled = tasks
        .filter { it.occursOn(selectedDate) || it.id in completedIds }
        .sortedWith(compareBy<CleaningTask> { roomSortIndex(it.room) }.thenBy { it.room }.thenBy { it.title.lowercase() })
    val grouped = scheduled.groupBy { it.room }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weekly plan", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("The calm place to shape your week.", color = MutedInk)
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = MintSoft, border = BorderStroke(1.dp, Hairline)) {
                        Icon(Icons.Default.CalendarMonth, null, tint = ForestDeep, modifier = Modifier.padding(12.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                WeekControls(
                    weekStart,
                    today,
                    onPrevious = { weekStart = weekStart.minusWeeks(1); selectedDate = selectedDate.minusWeeks(1) },
                    onNext = { weekStart = weekStart.plusWeeks(1); selectedDate = selectedDate.plusWeeks(1) },
                    onThisWeek = { weekStart = today.startOfWeek(); selectedDate = today }
                )
                Spacer(Modifier.height(12.dp))
                DayStrip(days, selectedDate, today, tasks) { selectedDate = it }
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Hairline),
                colors = CardDefaults.cardColors(containerColor = Paper),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(13.dp), color = Mint) {
                        Text(selectedDate.dayOfMonth.toString(), Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = ForestDeep, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE")), fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 17.sp)
                        Text(if (scheduled.isEmpty()) "No tasks planned" else "${scheduled.size} planned · by room", color = MutedInk, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { onAddForDate(selectedDate) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestDeep),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(3.dp)); Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (scheduled.isEmpty()) {
            item { ClearDayState() }
        } else {
            orderedRooms(grouped.keys).forEach { room ->
                val roomTasks = grouped[room].orEmpty()
                item(key = "week-header-$room") { RoomHeader(room, roomTasks.size) }
                items(roomTasks, key = { "week-${selectedDate}-${it.id}" }) { task ->
                    val completion = completedForDate[task.id]
                    WeekTaskCard(
                        task = task,
                        completion = completion,
                        missed = selectedDate.isBefore(today) && completion == null,
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
                        onUndo = { completion?.let(onUndo) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekControls(
    weekStart: LocalDate,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onThisWeek: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Paper, border = BorderStroke(1.dp, Hairline)) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.ChevronLeft, "Previous week") }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}", fontWeight = FontWeight.ExtraBold, color = Ink)
            if (weekStart != today.startOfWeek()) {
                Text("Back to this week", modifier = Modifier.clickable(onClick = onThisWeek), color = Forest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            } else Text("This week", color = MutedInk, fontSize = 12.sp)
        }
        Surface(shape = CircleShape, color = Paper, border = BorderStroke(1.dp, Hairline)) {
            IconButton(onClick = onNext, modifier = Modifier.size(38.dp)) { Icon(Icons.Default.ChevronRight, "Next week") }
        }
    }
}

@Composable
private fun DayStrip(days: List<LocalDate>, selected: LocalDate, today: LocalDate, tasks: List<CleaningTask>, onSelected: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { date ->
            val active = date == selected
            val count = tasks.count { !it.completed && it.occursOn(date) }
            Surface(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(17.dp)).clickable { onSelected(date) },
                shape = RoundedCornerShape(17.dp),
                color = if (active) ForestDeep else Paper,
                border = if (active) null else BorderStroke(1.dp, Hairline),
                shadowElevation = if (active) 5.dp else 0.dp
            ) {
                Column(Modifier.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEE")).take(1), color = if (active) Color.White.copy(alpha = .68f) else MutedInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(date.dayOfMonth.toString(), color = if (active) Color.White else Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    when {
                        date == today -> Box(Modifier.padding(top = 4.dp).size(5.dp).background(if (active) Champagne else Forest, CircleShape))
                        count > 0 -> Text(count.toString(), color = if (active) Color.White.copy(alpha = .55f) else Color(0xFF9AA6A1), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        else -> Spacer(Modifier.height(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTaskCard(
    task: CleaningTask,
    completion: CompletionRecord?,
    missed: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val done = completion != null
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, if (missed) Champagne.copy(alpha = .75f) else Hairline),
        colors = CardDefaults.cardColors(containerColor = if (done) MintSoft else Paper),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(13.dp), color = when { done -> Forest; missed -> Color(0xFFFFF0DD); else -> Mint }) {
                Icon(
                    when { done -> Icons.Default.Check; missed -> Icons.Default.Update; else -> Icons.Default.EventRepeat },
                    null,
                    tint = when { done -> Color.White; missed -> Color(0xFF97612B); else -> Forest },
                    modifier = Modifier.padding(9.dp).size(17.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, color = if (done) MutedInk else Ink, textDecoration = if (done) TextDecoration.LineThrough else null, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${task.assignee.label} · ${task.recurrenceText()}", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            when {
                done -> TextButton(onClick = onUndo, contentPadding = PaddingValues(horizontal = 7.dp)) {
                    Icon(Icons.Default.Replay, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                missed -> StatusPill("Carried", Color(0xFF7E542B), Color(0xFFFFF0DD))
            }
            TaskActionsButton(task.title, onEdit, onDelete)
        }
    }
}

@Composable
private fun CompletedTodayCard(completions: List<CompletionRecord>, onUndo: (CompletionRecord) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Hairline),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(11.dp), color = Mint) {
                    Icon(Icons.Default.Check, null, tint = Forest, modifier = Modifier.padding(8.dp).size(16.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text("Finished today", fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text(completions.size.toString(), color = MutedInk, fontWeight = FontWeight.Bold)
            }
            completions.forEach { completion ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(completion.taskTitle, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("${completion.room} · ${completion.completedBy.label}", style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    }
                    TextButton(onClick = { onUndo(completion) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Undo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, content: Color, background: Color) {
    Surface(shape = CircleShape, color = background) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = content, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun ClearDayState() {
    Column(Modifier.fillMaxWidth().padding(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MintSoft, border = BorderStroke(1.dp, Hairline)) {
            Icon(Icons.Default.EventAvailable, null, tint = Forest, modifier = Modifier.padding(18.dp).size(28.dp))
        }
        Spacer(Modifier.height(12.dp)); Text("A clear day", fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 18.sp); Text("Leave a little room for life.", color = MutedInk)
    }
}

@Composable
private fun HistoryScreen(completions: List<CompletionRecord>, modifier: Modifier = Modifier) {
    val today = rememberToday()
    val weekStart = today.startOfWeek()
    val weekCompletions = completions.filter {
        val date = it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        !date.isBefore(weekStart) && !date.isAfter(today)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Column { Text("Activity", style = MaterialTheme.typography.headlineMedium, color = Ink); Text("The little wins that keep home moving.", color = MutedInk) } }
        item { ScoreboardCard(completions, weekCompletions) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent completions", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = MintSoft) {
                    Text("${completions.size} total", Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MutedInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (completions.isEmpty()) item { EmptyHistoryState() }
        else items(completions.sortedByDescending { it.completedAt }, key = { it.id }) { CompletionCard(it) }
    }
}

@Composable
private fun ScoreboardCard(allCompletions: List<CompletionRecord>, weekCompletions: List<CompletionRecord>) {
    val mattWeek = weekCompletions.count { it.completedBy == Assignee.MATT }
    val jessieWeek = weekCompletions.count { it.completedBy == Assignee.JESSIE }
    val mattTotal = allCompletions.count { it.completedBy == Assignee.MATT }
    val jessieTotal = allCompletions.count { it.completedBy == Assignee.JESSIE }
    Card(
        Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(29.dp), ambientColor = Forest.copy(alpha = .13f)),
        shape = RoundedCornerShape(29.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(ForestDeep, Forest))).padding(20.dp)) {
            Box(Modifier.offset(x = 235.dp, y = (-45).dp).size(110.dp).background(Color.White.copy(alpha = .05f), CircleShape))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = .12f)) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Champagne, modifier = Modifier.padding(9.dp).size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp)); Column { Text("This week", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp); Text("A friendly little scoreboard", color = Color.White.copy(alpha = .66f), style = MaterialTheme.typography.bodySmall) }
                }
                Spacer(Modifier.height(19.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ScorePerson("Matt", "M", mattWeek, mattTotal, Modifier.weight(1f)); Box(Modifier.width(1.dp).height(74.dp).background(Color.White.copy(alpha = .16f))); ScorePerson("Jessie", "J", jessieWeek, jessieTotal, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScorePerson(name: String, initial: String, weekly: Int, allTime: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = .12f)) { Text(initial, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(7.dp)); Text(weekly.toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp); Text(name, color = Color.White.copy(alpha = .92f), fontWeight = FontWeight.Bold); Text("$allTime all-time", color = Color.White.copy(alpha = .58f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CompletionCard(completion: CompletionRecord) {
    val whenText = completion.completedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"))
    val colour = if (completion.completedBy == Assignee.MATT) Forest else Color(0xFF80679A)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), border = BorderStroke(1.dp, Hairline), colors = CardDefaults.cardColors(containerColor = Paper), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(completion.completedBy.label.take(1), colour, 38); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(completion.taskTitle, fontWeight = FontWeight.Bold, color = Ink)
                Text("${completion.room} · ${completion.completedBy.label}", color = MutedInk, style = MaterialTheme.typography.bodySmall)
                Text(whenText, color = Color(0xFF98A39F), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.CheckCircle, null, tint = Forest, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Hairline), colors = CardDefaults.cardColors(containerColor = Paper)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, null, tint = Forest, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(10.dp)); Text("No completed tasks yet", fontWeight = FontWeight.ExtraBold, color = Ink); Text("Your first checkmark starts the story.", color = MutedInk)
        }
    }
}

@Composable
private fun HouseholdScreen(
    currentUser: Assignee,
    syncState: HouseholdSyncUiState,
    onCurrentUserChanged: (Assignee) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Column { Text("Our household", style = MaterialTheme.typography.headlineMedium, color = Ink); Text("A small, private space for the people at home.", color = MutedInk) } }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), border = BorderStroke(1.dp, Hairline), colors = CardDefaults.cardColors(containerColor = Paper), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(currentUser.label.take(1), if (currentUser == Assignee.MATT) Forest else Color(0xFF80679A), 46); Spacer(Modifier.width(12.dp)); Column { Text("This phone belongs to", color = MutedInk, fontSize = 12.sp); Text(currentUser.label, fontWeight = FontWeight.ExtraBold, color = Ink, fontSize = 19.sp) }
                    }
                    ChoiceSection("Household member", listOf(Assignee.MATT.label, Assignee.JESSIE.label), currentUser.label) { label -> onCurrentUserChanged(if (label == Assignee.MATT.label) Assignee.MATT else Assignee.JESSIE) }
                }
            }
        }
        item { HouseholdConnectionCard(syncState) }
        item { FeatureStatusCard(Icons.Default.NotificationsNone, "Completion boops", "When one adult finishes a task, the other phone can surface the completion through household sync without notifying the phone that completed it.", "Private household activity") }
    }
}

@Composable
private fun HouseholdConnectionCard(syncState: HouseholdSyncUiState) {
    val paired = syncState.status == HouseholdSyncStatus.PAIRED
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = if (paired) MintSoft else Paper), border = BorderStroke(1.dp, if (paired) Forest.copy(alpha = .12f) else Hairline), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = if (paired) Mint else Color(0xFFF1F2EF)) {
                Icon(if (paired) Icons.Default.Sync else Icons.Default.Link, null, tint = if (paired) Forest else MutedInk, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (paired) "Household connected" else "Household connection", fontWeight = FontWeight.ExtraBold, color = Ink)
                Text(syncState.message ?: if (paired) "Tasks and activity are shared." else "Pair the second adult phone when you’re ready.", color = MutedInk, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                syncState.pairingCode?.let { code ->
                    Spacer(Modifier.height(8.dp)); Surface(shape = CircleShape, color = Paper, border = BorderStroke(1.dp, Hairline)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text("PAIRING CODE  ", color = MutedInk, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp); Text(code, color = ForestDeep, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                    }
                }
            }
            StatusPill(if (paired) "Ready" else "Setup", if (paired) ForestDeep else MutedInk, if (paired) Mint else MintSoft)
        }
    }
}

@Composable
private fun FeatureStatusCard(icon: ImageVector, title: String, body: String, status: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Hairline), colors = CardDefaults.cardColors(containerColor = Paper), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(14.dp), color = MintSoft) { Icon(icon, null, tint = ForestDeep, modifier = Modifier.padding(10.dp).size(20.dp)) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.ExtraBold, color = Ink); Spacer(Modifier.height(3.dp)); Text(body, color = MutedInk, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(9.dp)); Surface(shape = CircleShape, color = MintSoft) { Text(status, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = ForestDeep, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) } }
        }
    }
}

@Composable
private fun Avatar(initial: String, colour: Color, size: Int = 38) {
    Box(Modifier.size(size.dp).background(colour.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
        Text(initial, color = colour, fontWeight = FontWeight.ExtraBold, fontSize = (size * .38f).sp)
    }
}

@Composable
private fun HomeNavigation(selected: HomeTab, onSelected: (HomeTab) -> Unit) {
    NavigationBar(containerColor = Paper, tonalElevation = 5.dp) {
        val colours = NavigationBarItemDefaults.colors(selectedIconColor = ForestDeep, selectedTextColor = ForestDeep, indicatorColor = Mint, unselectedIconColor = Color(0xFF87948F), unselectedTextColor = Color(0xFF87948F))
        NavigationBarItem(selected == HomeTab.TODAY, { onSelected(HomeTab.TODAY) }, { Icon(Icons.Default.Home, null) }, label = { Text("Today", fontWeight = FontWeight.SemiBold) }, colors = colours)
        NavigationBarItem(selected == HomeTab.WEEK, { onSelected(HomeTab.WEEK) }, { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Week", fontWeight = FontWeight.SemiBold) }, colors = colours)
        NavigationBarItem(selected == HomeTab.HISTORY, { onSelected(HomeTab.HISTORY) }, { Icon(Icons.Default.CheckCircleOutline, null) }, label = { Text("Activity", fontWeight = FontWeight.SemiBold) }, colors = colours)
        NavigationBarItem(selected == HomeTab.HOUSEHOLD, { onSelected(HomeTab.HOUSEHOLD) }, { Icon(Icons.Default.People, null) }, label = { Text("Home", fontWeight = FontWeight.SemiBold) }, colors = colours)
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MintSoft, border = BorderStroke(1.dp, Hairline)) { Icon(Icons.Default.Check, null, tint = Forest, modifier = Modifier.padding(18.dp).size(28.dp)) }
        Spacer(Modifier.height(14.dp)); Text("Nothing waiting today", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ink); Text("A little breathing room is a win too.", color = MutedInk)
    }
}

@Composable
private fun TaskEditorDialog(
    task: CleaningTask?,
    initialDueDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, Assignee, Priority, Recurrence, Int, LocalDate) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var room by remember(task?.id) { mutableStateOf(task?.room ?: "Around the house") }
    var assignee by remember(task?.id) { mutableStateOf(task?.assignee ?: Assignee.EITHER) }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: Priority.NORMAL) }
    var recurrence by remember(task?.id) { mutableStateOf(task?.recurrence ?: Recurrence.ONE_OFF) }
    var intervalText by remember(task?.id) { mutableStateOf((task?.intervalDays ?: 2).toString()) }
    var dueDate by remember(task?.id, initialDueDate) { mutableStateOf(initialDueDate) }
    val intervalDays = intervalText.toIntOrNull()?.coerceIn(2, 365) ?: 2

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = Paper,
        title = { Column { Text(if (task == null) "Add a task" else "Edit task", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Ink); Text("Give it a day, room and owner.", color = MutedInk, style = MaterialTheme.typography.bodyMedium) } },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(15.dp), modifier = Modifier.widthIn(max = 430.dp).heightIn(max = 590.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What needs doing?") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Forest, unfocusedBorderColor = Hairline, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                }
                item { DateChoice(dueDate) { dueDate = it } }
                item { ChoiceSection("Room", rooms, room) { room = it } }
                item { ChoiceSection("Assigned to", Assignee.entries.map { it.label }, assignee.label) { label -> assignee = Assignee.entries.first { it.label == label } } }
                item { ChoiceSection("Repeats", Recurrence.entries.map { it.label }, recurrence.label) { label -> recurrence = Recurrence.entries.first { it.label == label } } }
                if (recurrence == Recurrence.CUSTOM_DAYS) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MintSoft), border = BorderStroke(1.dp, Hairline)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Mint) { Icon(Icons.Default.Repeat, null, tint = ForestDeep, modifier = Modifier.padding(9.dp).size(18.dp)) }
                                Spacer(Modifier.width(10.dp)); Text("Every", color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = intervalText,
                                    onValueChange = { value -> intervalText = value.filter(Char::isDigit).take(3) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(82.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Forest, unfocusedBorderColor = Hairline, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                                )
                                Spacer(Modifier.width(8.dp)); Text("days", color = Ink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item { Text("Choose 2–365 days. The schedule stays anchored to the day above.", color = MutedInk, style = MaterialTheme.typography.bodySmall) }
                }
                item { ChoiceSection("Priority", Priority.entries.map { it.label }, priority.label) { label -> priority = Priority.entries.first { it.label == label } } }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, room, assignee, priority, recurrence, intervalDays, dueDate) },
                enabled = title.isNotBlank() && (recurrence != Recurrence.CUSTOM_DAYS || (intervalText.toIntOrNull() ?: 0) in 2..365),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestDeep)
            ) { Text(if (task == null) "Add task" else "Save changes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedInk) } }
    )
}

@Composable
private fun DateChoice(selectedDate: LocalDate, onSelected: (LocalDate) -> Unit) {
    val weekStart = selectedDate.startOfWeek()
    val dates = (0L..6L).map(weekStart::plusDays)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Day", fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSelected(selectedDate.minusWeeks(1)) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ChevronLeft, "Previous week") }
            Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM d")), color = ForestDeep, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            IconButton(onClick = { onSelected(selectedDate.plusWeeks(1)) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.ChevronRight, "Next week") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(dates) { date ->
                val active = date == selectedDate
                Surface(Modifier.clip(RoundedCornerShape(14.dp)).clickable { onSelected(date) }, color = if (active) ForestDeep else MintSoft, border = if (active) null else BorderStroke(1.dp, Hairline), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.format(DateTimeFormatter.ofPattern("EEE")), color = if (active) Color.White.copy(alpha = .75f) else MutedInk, fontSize = 11.sp)
                        Text(date.dayOfMonth.toString(), color = if (active) Color.White else Ink, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceSection(label: String, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontWeight = FontWeight.Bold, color = Ink)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(values) { value ->
                val active = value == selected
                Surface(Modifier.clip(CircleShape).clickable { onSelect(value) }, color = if (active) Mint else MintSoft, border = BorderStroke(1.dp, if (active) Forest.copy(alpha = .15f) else Hairline), shape = CircleShape) {
                    Text(if (active) "✓ $value" else value, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (active) ForestDeep else MutedInk, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusSeconds(1)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
            today = LocalDate.now()
        }
    }
    return today
}

private fun CleaningTask.occursOn(date: LocalDate): Boolean {
    if (date.isBefore(nextDueDate)) return false
    return when (recurrence) {
        Recurrence.ONE_OFF -> date == nextDueDate
        Recurrence.DAILY -> true
        Recurrence.CUSTOM_DAYS -> ChronoUnit.DAYS.between(nextDueDate, date) % intervalDays.coerceIn(2, 365).toLong() == 0L
        Recurrence.WEEKLY -> ChronoUnit.DAYS.between(nextDueDate, date) % 7L == 0L
        Recurrence.MONTHLY -> {
            val anchorMonth = YearMonth.from(nextDueDate)
            val targetMonth = YearMonth.from(date)
            !targetMonth.isBefore(anchorMonth) && date.dayOfMonth == minOf(nextDueDate.dayOfMonth, date.lengthOfMonth())
        }
    }
}

private fun CleaningTask.isCarryoverFor(date: LocalDate): Boolean = !completed && nextDueDate.isBefore(date) && !occursOn(date)

private fun CleaningTask.recurrenceText(): String = when (recurrence) {
    Recurrence.ONE_OFF -> "Today"
    Recurrence.DAILY -> "Daily"
    Recurrence.CUSTOM_DAYS -> "Every ${intervalDays.coerceIn(2, 365)} days"
    Recurrence.WEEKLY -> "Weekly"
    Recurrence.MONTHLY -> "Monthly"
}

private fun LocalDate.startOfWeek(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())

private fun orderedRooms(keys: Set<String>): List<String> = buildList {
    roomOrder.filterTo(this) { it in keys }
    keys.filterTo(this) { it !in this }.sort()
}

private fun roomSortIndex(room: String): Int = roomOrder.indexOf(room).let { if (it >= 0) it else roomOrder.size }

private fun roomIcon(room: String): ImageVector = when (room) {
    "Kitchen" -> Icons.Default.Kitchen
    "Living room" -> Icons.Default.Weekend
    "Bathroom" -> Icons.Default.Bathtub
    "Bedroom" -> Icons.Default.Bed
    "Hallway" -> Icons.Default.MeetingRoom
    "Entryway" -> Icons.Default.DoorFront
    else -> Icons.Default.HomeWork
}

private fun roomAccent(room: String): Color = when (room) {
    "Kitchen" -> Color(0xFF8A633C)
    "Living room" -> Color(0xFF5C6F91)
    "Bathroom" -> Color(0xFF4F7F88)
    "Bedroom" -> Color(0xFF80679A)
    "Hallway" -> Color(0xFF66766F)
    "Entryway" -> Color(0xFF967047)
    else -> Forest
}

private fun roomTint(room: String): Color = when (room) {
    "Kitchen" -> Color(0xFFF6EEE4)
    "Living room" -> Color(0xFFEDF0F7)
    "Bathroom" -> Color(0xFFE8F1F2)
    "Bedroom" -> Lilac
    "Hallway" -> Color(0xFFEEF1EF)
    "Entryway" -> Color(0xFFF6EFE7)
    else -> MintSoft
}

private fun dayPart(): String = when (LocalTime.now().hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    in 17..20 -> "evening"
    else -> "night"
}
