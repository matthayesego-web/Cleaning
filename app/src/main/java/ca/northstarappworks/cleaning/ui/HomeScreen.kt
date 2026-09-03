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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class HomeTab { TODAY, HISTORY, HOUSEHOLD }
private enum class TaskFilter { ALL, MINE, PARTNER, SHARED }

private val roomOrder = listOf(
    "Kitchen",
    "Living room",
    "Bathroom",
    "Bedroom",
    "Hallway",
    "Entryway",
    "Around the house"
)

@Composable
fun OurHomeApp(homeViewModel: HomeViewModel = viewModel()) {
    val tasks by homeViewModel.tasks.collectAsState()
    val completions by homeViewModel.completions.collectAsState()
    val currentUser by homeViewModel.currentUser.collectAsState()

    var addingTask by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(HomeTab.TODAY) }

    Scaffold(
        containerColor = Cream,
        bottomBar = { HomeNavigation(selectedTab) { selectedTab = it } },
        floatingActionButton = {
            if (selectedTab == HomeTab.TODAY) {
                ExtendedFloatingActionButton(
                    onClick = { addingTask = true },
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
                onChecked = homeViewModel::setCompleted,
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

    if (addingTask) {
        AddTaskDialog(
            onDismiss = { addingTask = false },
            onAdd = { title, room, assignee, priority, recurrence ->
                homeViewModel.addTask(title, room, assignee, priority, recurrence)
                addingTask = false
            }
        )
    }
}

@Composable
private fun TodayScreen(
    tasks: List<CleaningTask>,
    completions: List<CompletionRecord>,
    currentUser: Assignee,
    onChecked: (CleaningTask, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    val today = LocalDate.now()
    val partner = if (currentUser == Assignee.MATT) Assignee.JESSIE else Assignee.MATT

    val dueTasks = tasks.filter { task ->
        !task.completed && !task.nextDueDate.isAfter(today)
    }
    val visibleTasks = dueTasks.filter { task ->
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
    val orderedRooms = buildList {
        roomOrder.filterTo(this) { groupedTasks.containsKey(it) }
        groupedTasks.keys.filterTo(this) { it !in this }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { WelcomeHeader(currentUser) }
        item { ProgressHero(done = completedToday.size, waiting = dueTasks.size) }
        item { FilterRow(filter, currentUser) { filter = it } }

        if (visibleTasks.isEmpty()) {
            item { EmptyState() }
        } else {
            orderedRooms.forEach { room ->
                val roomTasks = groupedTasks[room].orEmpty()
                item(key = "header-$room") {
                    RoomHeader(room = room, count = roomTasks.size)
                }
                items(roomTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onChecked = { checked -> onChecked(task, checked) },
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
private fun WelcomeHeader(currentUser: Assignee) {
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
        IconButton(onClick = {}) {
            Box {
                Icon(Icons.Default.NotificationsNone, "Notifications", tint = Ink)
                Box(Modifier.align(Alignment.TopEnd).size(7.dp).background(Peach, CircleShape))
            }
        }
        Avatar(currentUser.label.take(1), if (currentUser == Assignee.MATT) Forest else Color(0xFF8D6AAE))
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
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 0.dp),
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
private fun TaskCard(task: CleaningTask, onChecked: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val accent = when (task.priority) {
        Priority.URGENT -> Color(0xFFD35D56)
        Priority.IMPORTANT -> Peach
        Priority.NORMAL -> Mint
    }
    Card(
        modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) Color.White.copy(alpha = .58f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (task.completed) 0.dp else 3.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                task.completed,
                onChecked,
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
                    color = if (task.completed) MutedInk else Ink,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.room, style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    Text("  ·  ", color = Color(0xFFBCC4C1))
                    if (task.recurrence == Recurrence.ONE_OFF) {
                        Icon(Icons.Default.Schedule, null, tint = MutedInk, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(task.dueLabel, style = MaterialTheme.typography.bodySmall, color = MutedInk)
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
                if (!task.completed && task.priority != Priority.NORMAL) {
                    Spacer(Modifier.height(7.dp))
                    Box(Modifier.size(7.dp).background(accent, CircleShape))
                }
            }
            IconButton(onClick = {}, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.MoreHoriz, "Task options", tint = Color(0xFF9AA5A1))
            }
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
    val today = LocalDate.now()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
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
                Text("History & scoreboard", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("A tiny bit of friendly household competition.", color = MutedInk)
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
                ScorePerson(
                    name = "Matt",
                    initial = "M",
                    weekly = mattWeek,
                    allTime = mattTotal,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.width(1.dp).height(72.dp).background(Color.White.copy(alpha = .2f)))
                ScorePerson(
                    name = "Jessie",
                    initial = "J",
                    weekly = jessieWeek,
                    allTime = jessieTotal,
                    modifier = Modifier.weight(1f)
                )
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
                title = "Secure household pairing",
                body = "The sync layer will pair exactly two phones so tasks, assignments and the scoreboard stay in step.",
                status = "Next connection step"
            )
        }
        item {
            FeatureStatusCard(
                icon = Icons.Default.NotificationsNone,
                title = "Task-complete boops",
                body = "When the other person finishes a task, the notification will say what was completed and which room it belongs to.",
                status = "Built with pairing"
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
            selected == HomeTab.TODAY,
            { onSelected(HomeTab.TODAY) },
            { Icon(Icons.Default.Home, null) },
            label = { Text("Today") }
        )
        NavigationBarItem(
            selected == HomeTab.HISTORY,
            { onSelected(HomeTab.HISTORY) },
            { Icon(Icons.Default.Check, null) },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected == HomeTab.HOUSEHOLD,
            { onSelected(HomeTab.HOUSEHOLD) },
            { Icon(Icons.Default.Person, null) },
            label = { Text("Household") }
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
        Text("Nothing waiting here", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
        Text("Everything due today is handled.", color = MutedInk)
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Assignee, Priority, Recurrence) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("Around the house") }
    var assignee by remember { mutableStateOf(Assignee.EITHER) }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    var recurrence by remember { mutableStateOf(Recurrence.ONE_OFF) }
    val rooms = listOf(
        "Around the house",
        "Kitchen",
        "Living room",
        "Bathroom",
        "Bedroom",
        "Hallway",
        "Entryway"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text("Add something", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Text("One-off or repeating — keep it simple.", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.widthIn(max = 420.dp).heightIn(max = 520.dp)
            ) {
                item {
                    OutlinedTextField(
                        title,
                        { title = it },
                        label = { Text("What needs doing?") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { ChoiceSection("Room", rooms, room) { room = it } }
                item {
                    ChoiceSection(
                        "Assigned to",
                        Assignee.entries.map { it.label },
                        assignee.label
                    ) { label ->
                        assignee = Assignee.entries.first { it.label == label }
                    }
                }
                item {
                    ChoiceSection(
                        "Repeats",
                        Recurrence.entries.map { it.label },
                        recurrence.label
                    ) { label ->
                        recurrence = Recurrence.entries.first { it.label == label }
                    }
                }
                item {
                    ChoiceSection(
                        "Priority",
                        Priority.entries.map { it.label },
                        priority.label
                    ) { label ->
                        priority = Priority.entries.first { it.label == label }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                { onAdd(title, room, assignee, priority, recurrence) },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Forest)
            ) {
                Text("Add task", Modifier.padding(horizontal = 6.dp), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = MutedInk) } }
    )
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

private fun dayPart(): String = when (LocalTime.now().hour) {
    in 5..11 -> "morning"
    in 12..16 -> "afternoon"
    in 17..20 -> "evening"
    else -> "night"
}
