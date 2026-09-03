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
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
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
    "Kitchen",
    "Living room",
    "Bathroom",
    "Bedroom",
    "Hallway",
    "Entryway",
    "Around the house"
)

private val rooms = listOf(
    "Around the house",
    "Kitchen",
    "Living room",
    "Bathroom",
    "Bedroom",
    "Hallway",
    "Entryway"
)

@Composable
fun OurHomeApp(homeViewModel: HomeViewModel = viewModel()) {
    val tasks by homeViewModel.tasks.collectAsState()
    val completions by homeViewModel.completions.collectAsState()
    val currentUser by homeViewModel.currentUser.collectAsState()
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
                    containerColor = Forest,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
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
                onEdit = { task -> editorRequest = EditorRequest(task, task.nextDueDate) },
                onDelete = homeViewModel::deleteTask,
                onBellClick = { selectedTab = HomeTab.HISTORY },
                modifier = Modifier.padding(padding)
            )

            HomeTab.WEEK -> WeekScreen(
                tasks = tasks,
                completions = completions,
                today = today,
                onAddForDate = { date -> editorRequest = EditorRequest(dueDate = date) },
                onEdit = { task -> editorRequest = EditorRequest(task, task.nextDueDate) },
                onDelete = homeViewModel::deleteTask,
                modifier = Modifier.padding(padding)
            )

            HomeTab.HISTORY -> HistoryScreen(
                completions = completions,
                modifier = Modifier.padding(padding)
            )

            HomeTab.HOUSEHOLD -> HouseholdScreen(
                currentUser = currentUser,
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
            onSave = { title, room, assignee, priority, recurrence, dueDate ->
                val existing = request.task
                if (existing == null) {
                    homeViewModel.addTask(
                        title = title,
                        room = room,
                        assignee = assignee,
                        priority = priority,
                        recurrence = recurrence,
                        dueDate = dueDate
                    )
                } else {
                    homeViewModel.updateTask(
                        task = existing,
                        title = title,
                        room = room,
                        assignee = assignee,
                        priority = priority,
                        recurrence = recurrence,
                        dueDate = dueDate
                    )
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
    onEdit: (CleaningTask) -> Unit,
    onDelete: (CleaningTask) -> Unit,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    val partner = if (currentUser == Assignee.MATT) Assignee.JESSIE else Assignee.MATT

    // Today is deliberately exact: overdue tasks do not snowball into a giant list.
    // Recurring tasks calculate whether they belong on the new date automatically.
    val todayTasks = tasks.filter { task ->
        !task.completed && task.occursOn(today)
    }
    val visibleTasks = todayTasks.filter { task ->
        when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.MINE -> task.assignee == currentUser
            TaskFilter.PARTNER -> task.assignee == partner
            TaskFilter.SHARED -> task.assignee == Assignee.EITHER
        }
    }
    val completedToday = completions.filter { completion ->
        completion.completedAt.atZone(ZoneId.systemDefault()).toLocalDate() == today
    }

    val groupedTasks = visibleTasks.groupBy { it.room }
    val orderedRooms = orderedRooms(groupedTasks.keys)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WelcomeHeader(
                currentUser = currentUser,
                hasActivity = completedToday.isNotEmpty(),
                onBellClick = onBellClick
            )
        }
        item { ProgressHero(done = completedToday.size, waiting = todayTasks.size) }
        item { FilterRow(filter, currentUser) { filter = it } }

        if (visibleTasks.isEmpty()) {
            item { EmptyState() }
        } else {
            orderedRooms.forEach { room ->
                val roomTasks = groupedTasks[room].orEmpty().sortedBy { it.title.lowercase() }
                item(key = "today-header-$room") {
                    RoomHeader(room = room, count = roomTasks.size)
                }
                items(roomTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onChecked = { checked -> onChecked(task, checked) },
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }

        if (completedToday.isNotEmpty()) {
            item { CompletedTodayCard(completedToday.take(4)) }
        }
    }
}

@Composable
private fun WelcomeHeader(
    currentUser: Assignee,
    hasActivity: Boolean,
    onBellClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Good ${dayPart()}, ${currentUser.label}",
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, null, tint = Forest, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("Our Home · Today", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
        }
        IconButton(onClick = onBellClick) {
            Box {
                Icon(Icons.Default.NotificationsNone, "Recent activity", tint = Ink)
                if (hasActivity) {
                    Box(Modifier.align(Alignment.TopEnd).size(7.dp).background(Peach, CircleShape))
                }
            }
        }
        Avatar(
            currentUser.label.take(1),
            if (currentUser == Assignee.MATT) Forest else Color(0xFF8D6AAE)
        )
    }
}

@Composable
private fun ProgressHero(done: Int, waiting: Int) {
    val total = done + waiting
    val progress = if (total == 0) 1f else done.toFloat() / total.toFloat()
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = Forest.copy(alpha = .16f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(ForestDeep, Forest)))
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = .14f), shape = RoundedCornerShape(12.dp)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = Peach,
                        modifier = Modifier.padding(9.dp).size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Today’s little wins",
                        color = Color.White.copy(alpha = .72f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        when {
                            total == 0 -> "Nothing waiting today"
                            waiting == 0 -> "Home goal complete!"
                            else -> "You’re making great progress"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Text("$done/$total", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = .16f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(Peach, Color(0xFFFFD0A6))))
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "$done completed today · $waiting still waiting",
                color = Color.White.copy(alpha = .78f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FilterRow(
    selected: TaskFilter,
    currentUser: Assignee,
    onFilter: (TaskFilter) -> Unit
) {
    val partner = if (currentUser == Assignee.MATT) Assignee.JESSIE else Assignee.MATT
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
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
                color = if (active) Forest else Color.White,
                shadowElevation = if (active) 0.dp else 2.dp
            ) {
                Text(
                    label,
                    Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    color = if (active) Color.White else MutedInk,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RoomHeader(room: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = Mint) {
            Icon(
                Icons.Default.HomeWork,
                null,
                tint = ForestDeep,
                modifier = Modifier.padding(7.dp).size(17.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(room, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.weight(1f))
        Text("$count", color = MutedInk, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TaskCard(
    task: CleaningTask,
    onChecked: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (task.priority) {
        Priority.URGENT -> Color(0xFFD35D56)
        Priority.IMPORTANT -> Peach
        Priority.NORMAL -> Mint
    }

    Card(
        modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = false,
                onCheckedChange = onChecked,
                colors = CheckboxDefaults.colors(checkedColor = Forest, uncheckedColor = Color(0xFFB6C0BC))
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Ink
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.room, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    Text("  ·  ", color = Color(0xFFBCC4C1))
                    if (task.recurrence == Recurrence.ONE_OFF) {
                        Icon(Icons.Default.Schedule, null, tint = MutedInk, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(task.nextDueDate.format(DateTimeFormatter.ofPattern("EEE")), style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    } else {
                        Icon(Icons.Default.Repeat, null, tint = Forest, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(task.recurrence.label, style = MaterialTheme.typography.bodySmall, color = ForestDeep)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val avatarColour = when (task.assignee) {
                    Assignee.JESSIE -> Color(0xFF8D6AAE)
                    Assignee.MATT -> Forest
                    Assignee.EITHER -> Color(0xFF73847E)
                }
                Avatar(task.assignee.label.take(1), avatarColour)
                if (task.priority != Priority.NORMAL) {
                    Spacer(Modifier.height(7.dp))
                    Box(Modifier.size(7.dp).background(accent, CircleShape))
                }
            }
            TaskActionsButton(task.title, onEdit, onDelete)
        }
    }
}

@Composable
private fun TaskActionsButton(
    taskTitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.MoreVert, "Task options", tint = Color(0xFF9AA5A1))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Edit task") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete task", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    confirmDelete = true
                }
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(26.dp),
            title = { Text("Delete task?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("“$taskTitle” will be removed from the shared schedule. Past completion history stays on the scoreboard.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
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
    modifier: Modifier = Modifier
) {
    var weekStart by remember(today) { mutableStateOf(today.startOfWeek()) }
    var selectedDate by remember(today) { mutableStateOf(today) }
    val days = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }

    val completedIds = completions
        .filter { it.completedAt.atZone(ZoneId.systemDefault()).toLocalDate() == selectedDate }
        .map { it.taskId }
        .toSet()

    val scheduled = tasks
        .filter { task -> task.occursOn(selectedDate) || task.id in completedIds }
        .sortedWith(
            compareBy<CleaningTask> { roomSortIndex(it.room) }
                .thenBy { it.room }
                .thenBy { it.title.lowercase() }
        )
    val grouped = scheduled.groupBy { it.room }
    val orderedRooms = orderedRooms(grouped.keys)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weekly control panel", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("Plan the week without crowding your Today list.", color = MutedInk)
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = Mint) {
                        Icon(Icons.Default.CalendarMonth, null, tint = ForestDeep, modifier = Modifier.padding(11.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                WeekControls(
                    weekStart = weekStart,
                    today = today,
                    onPrevious = {
                        weekStart = weekStart.minusWeeks(1)
                        selectedDate = selectedDate.minusWeeks(1)
                    },
                    onNext = {
                        weekStart = weekStart.plusWeeks(1)
                        selectedDate = selectedDate.plusWeeks(1)
                    },
                    onThisWeek = {
                        weekStart = today.startOfWeek()
                        selectedDate = today
                    }
                )
                Spacer(Modifier.height(12.dp))
                DayStrip(days, selectedDate, today) { selectedDate = it }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Mint)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                        Text(
                            if (scheduled.isEmpty()) "No tasks planned" else "${scheduled.size} planned · sorted by room",
                            color = MutedInk,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { onAddForDate(selectedDate) },
                        colors = ButtonDefaults.buttonColors(containerColor = Forest),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }

        if (scheduled.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = CircleShape, color = Mint) {
                        Icon(Icons.Default.EventAvailable, null, tint = Forest, modifier = Modifier.padding(18.dp).size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("A clear day", fontWeight = FontWeight.Bold, color = Ink)
                    Text("Add something only if it needs doing.", color = MutedInk)
                }
            }
        } else {
            orderedRooms.forEach { room ->
                val roomTasks = grouped[room].orEmpty()
                item(key = "week-header-$room") { RoomHeader(room, roomTasks.size) }
                items(roomTasks, key = { "week-${selectedDate}-${it.id}" }) { task ->
                    WeekTaskCard(
                        task = task,
                        done = task.id in completedIds,
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
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
        IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "Previous week") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}",
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            if (weekStart != today.startOfWeek()) {
                Text(
                    "Back to this week",
                    modifier = Modifier.clickable(onClick = onThisWeek),
                    color = Forest,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next week") }
    }
}

@Composable
private fun DayStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    today: LocalDate,
    onSelected: (LocalDate) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { date ->
            val active = date == selected
            Surface(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { onSelected(date) },
                shape = RoundedCornerShape(16.dp),
                color = if (active) Forest else Color.White,
                shadowElevation = if (active) 0.dp else 1.dp
            ) {
                Column(
                    Modifier.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEE")).take(1),
                        color = if (active) Color.White.copy(alpha = .8f) else MutedInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        date.dayOfMonth.toString(),
                        color = if (active) Color.White else Ink,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (date == today) {
                        Box(
                            Modifier
                                .padding(top = 3.dp)
                                .size(4.dp)
                                .background(if (active) Peach else Forest, CircleShape)
                        )
                    } else {
                        Spacer(Modifier.height(7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTaskCard(
    task: CleaningTask,
    done: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (done) Mint.copy(alpha = .72f) else Color.White),
        elevation = CardDefaults.cardElevation(if (done) 0.dp else 2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = if (done) Forest else Mint) {
                Icon(
                    if (done) Icons.Default.Check else Icons.Default.EventRepeat,
                    null,
                    tint = if (done) Color.White else Forest,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    color = if (done) MutedInk else Ink,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
                Text(
                    "${task.assignee.label} · ${task.recurrence.label}",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (done) {
                Text("Done", color = ForestDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            TaskActionsButton(task.title, onEdit, onDelete)
        }
    }
}

@Composable
private fun CompletedTodayCard(completions: List<CompletionRecord>) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Mint) {
                    Icon(Icons.Default.Check, null, tint = Forest, modifier = Modifier.padding(8.dp).size(17.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text("Finished today", fontWeight = FontWeight.ExtraBold, color = Ink)
            }
            completions.forEach { completion ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(completion.taskTitle, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(
                            "${completion.room} · ${completion.completedBy.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                    Icon(Icons.Default.CheckCircle, null, tint = Forest, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(completions: List<CompletionRecord>, modifier: Modifier = Modifier) {
    val today = rememberToday()
    val weekStart = today.startOfWeek()
    val weekCompletions = completions.filter { record ->
        val date = record.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        !date.isBefore(weekStart) && !date.isAfter(today)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("History & activity", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Completed tasks, recent boops and the friendly scoreboard.", color = MutedInk)
            }
        }
        item { ScoreboardCard(completions, weekCompletions) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent wins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("${completions.size} total", color = MutedInk)
            }
        }
        if (completions.isEmpty()) {
            item { EmptyHistoryState() }
        } else {
            items(completions.sortedByDescending { it.completedAt }, key = { it.id }) { completion ->
                CompletionCard(completion)
            }
        }
    }
}

@Composable
private fun ScoreboardCard(
    allCompletions: List<CompletionRecord>,
    weekCompletions: List<CompletionRecord>
) {
    val mattWeek = weekCompletions.count { it.completedBy == Assignee.MATT }
    val jessieWeek = weekCompletions.count { it.completedBy == Assignee.JESSIE }
    val mattTotal = allCompletions.count { it.completedBy == Assignee.MATT }
    val jessieTotal = allCompletions.count { it.completedBy == Assignee.JESSIE }

    Card(
        Modifier.fillMaxWidth().shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = Forest.copy(alpha = .13f)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(ForestDeep, Forest)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, null, tint = Peach, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("This week", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Completed tasks", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScorePerson("Matt", "M", mattWeek, mattTotal, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(72.dp).background(Color.White.copy(alpha = .2f)))
                ScorePerson("Jessie", "J", jessieWeek, jessieTotal, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScorePerson(
    name: String,
    initial: String,
    weekly: Int,
    allTime: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = .14f)) {
            Text(initial, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(7.dp))
        Text("$weekly", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp)
        Text(name, color = Color.White.copy(alpha = .9f), fontWeight = FontWeight.Bold)
        Text("$allTime all-time", color = Color.White.copy(alpha = .62f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CompletionCard(completion: CompletionRecord) {
    val whenText = completion.completedAt
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"))
    val colour = if (completion.completedBy == Assignee.MATT) Forest else Color(0xFF8D6AAE)

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(completion.completedBy.label.take(1), colour)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(completion.taskTitle, fontWeight = FontWeight.Bold, color = Ink)
                Text("${completion.room} · ${completion.completedBy.label}", color = MutedInk, style = MaterialTheme.typography.bodySmall)
                Text(whenText, color = Color(0xFF9AA5A1), style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.CheckCircle, null, tint = Forest, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, null, tint = Forest, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(10.dp))
            Text("No completed tasks yet", fontWeight = FontWeight.Bold, color = Ink)
            Text("The scoreboard starts with your first checkmark.", color = MutedInk)
        }
    }
}

@Composable
private fun HouseholdScreen(
    currentUser: Assignee,
    onCurrentUserChanged: (Assignee) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Household", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("Keep Matt and Jessie correctly attributed.", color = MutedInk)
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This phone belongs to", fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("Completed tasks count toward the person selected here.", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                    ChoiceSection(
                        label = "Household member",
                        values = listOf(Assignee.MATT.label, Assignee.JESSIE.label),
                        selected = currentUser.label
                    ) { label ->
                        onCurrentUserChanged(if (label == Assignee.MATT.label) Assignee.MATT else Assignee.JESSIE)
                    }
                }
            }
        }
        item {
            FeatureStatusCard(
                icon = Icons.Default.Sync,
                title = "Secure household sync",
                body = "Your household is ready to share tasks, assignments, history and the weekly plan across two phones when Jessie joins with the pairing code.",
                status = "Ready"
            )
        }
        item {
            FeatureStatusCard(
                icon = Icons.Default.NotificationsNone,
                title = "Task-complete boops",
                body = "On the free Firebase setup, the other phone receives completion activity while household sync is active and catches up when the app reconnects.",
                status = "Spark / free tier"
            )
        }
    }
}

@Composable
private fun FeatureStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    status: String
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = RoundedCornerShape(14.dp), color = Mint) {
                Icon(icon, null, tint = ForestDeep, modifier = Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = Ink)
                Spacer(Modifier.height(3.dp))
                Text(body, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(9.dp))
                Surface(shape = CircleShape, color = Color(0xFFF1F1ED)) {
                    Text(status, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = ForestDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Avatar(initial: String, colour: Color) {
    Box(
        Modifier.size(38.dp).background(colour.copy(alpha = .13f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = colour, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun HomeNavigation(selected: HomeTab, onSelected: (HomeTab) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = selected == HomeTab.TODAY,
            onClick = { onSelected(HomeTab.TODAY) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Today") }
        )
        NavigationBarItem(
            selected = selected == HomeTab.WEEK,
            onClick = { onSelected(HomeTab.WEEK) },
            icon = { Icon(Icons.Default.CalendarMonth, null) },
            label = { Text("Week") }
        )
        NavigationBarItem(
            selected = selected == HomeTab.HISTORY,
            onClick = { onSelected(HomeTab.HISTORY) },
            icon = { Icon(Icons.Default.Check, null) },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = selected == HomeTab.HOUSEHOLD,
            onClick = { onSelected(HomeTab.HOUSEHOLD) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Home") }
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = Mint) {
            Icon(Icons.Default.Check, null, tint = Forest, modifier = Modifier.padding(18.dp).size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Nothing waiting today", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
        Text("Tomorrow gets a fresh, uncluttered list.", color = MutedInk)
    }
}

@Composable
private fun TaskEditorDialog(
    task: CleaningTask?,
    initialDueDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, Assignee, Priority, Recurrence, LocalDate) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var room by remember(task?.id) { mutableStateOf(task?.room ?: "Around the house") }
    var assignee by remember(task?.id) { mutableStateOf(task?.assignee ?: Assignee.EITHER) }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: Priority.NORMAL) }
    var recurrence by remember(task?.id) { mutableStateOf(task?.recurrence ?: Recurrence.ONE_OFF) }
    var dueDate by remember(task?.id, initialDueDate) { mutableStateOf(initialDueDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(if (task == null) "Add something" else "Edit task", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text("Choose the day it belongs on — Today stays clean.", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.widthIn(max = 430.dp).heightIn(max = 570.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("What needs doing?") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { DateChoice(dueDate) { dueDate = it } }
                item { ChoiceSection("Room", rooms, room) { room = it } }
                item {
                    ChoiceSection(
                        "Assigned to",
                        Assignee.entries.map { it.label },
                        assignee.label
                    ) { label -> assignee = Assignee.entries.first { it.label == label } }
                }
                item {
                    ChoiceSection(
                        "Repeats",
                        Recurrence.entries.map { it.label },
                        recurrence.label
                    ) { label -> recurrence = Recurrence.entries.first { it.label == label } }
                }
                item {
                    ChoiceSection(
                        "Priority",
                        Priority.entries.map { it.label },
                        priority.label
                    ) { label -> priority = Priority.entries.first { it.label == label } }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, room, assignee, priority, recurrence, dueDate) },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                Text(if (task == null) "Add task" else "Save changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = MutedInk) } }
    )
}

@Composable
private fun DateChoice(selectedDate: LocalDate, onSelected: (LocalDate) -> Unit) {
    val weekStart = selectedDate.startOfWeek()
    val dates = (0L..6L).map(weekStart::plusDays)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Day", fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSelected(selectedDate.minusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronLeft, "Previous week")
            }
            Text(
                selectedDate.format(DateTimeFormatter.ofPattern("MMM d")),
                color = ForestDeep,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            IconButton(onClick = { onSelected(selectedDate.plusWeeks(1)) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, "Next week")
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(dates) { date ->
                val active = date == selectedDate
                Surface(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable { onSelected(date) },
                    color = if (active) Forest else Color(0xFFF1F1ED),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEE")),
                            color = if (active) Color.White.copy(alpha = .8f) else MutedInk,
                            fontSize = 11.sp
                        )
                        Text(
                            date.dayOfMonth.toString(),
                            color = if (active) Color.White else Ink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceSection(
    label: String,
    values: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, fontWeight = FontWeight.Bold, color = Ink)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(values) { value ->
                val active = value == selected
                Surface(
                    Modifier.clip(CircleShape).clickable { onSelect(value) },
                    color = if (active) Mint else Color(0xFFF1F1ED),
                    shape = CircleShape
                ) {
                    Text(
                        if (active) "✓ $value" else value,
                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (active) ForestDeep else MutedInk,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
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
            val nextMidnight = now.toLocalDate()
                .plusDays(1)
                .atStartOfDay(now.zone)
                .plusSeconds(1)
            val waitMillis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L)
            delay(waitMillis)
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
        Recurrence.WEEKLY -> ChronoUnit.DAYS.between(nextDueDate, date) % 7L == 0L
        Recurrence.MONTHLY -> {
            val anchorMonth = YearMonth.from(nextDueDate)
            val targetMonth = YearMonth.from(date)
            if (targetMonth.isBefore(anchorMonth)) {
                false
            } else {
                date.dayOfMonth == minOf(nextDueDate.dayOfMonth, date.lengthOfMonth())
            }
        }
    }
}

private fun LocalDate.startOfWeek(): LocalDate = minusDays((dayOfWeek.value - 1).toLong())

private fun orderedRooms(keys: Set<String>): List<String> = buildList {
    roomOrder.filterTo(this) { it in keys }
    keys.filterTo(this) { it !in this }.sort()
}

private fun roomSortIndex(room: String): Int {
    val index = roomOrder.indexOf(room)
    return if (index >= 0) index else roomOrder.size
}

private fun dayPart(): String = when (LocalTime.now().hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    in 17..20 -> "evening"
    else -> "night"
}
