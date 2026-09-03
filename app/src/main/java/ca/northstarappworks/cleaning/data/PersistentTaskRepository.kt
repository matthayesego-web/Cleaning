package ca.northstarappworks.cleaning.data

import android.content.Context
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Small, dependency-free local store for the first usable build.
 *
 * Tasks survive app restarts and phone reboots. The repository is deliberately
 * behind TaskRepository so cloud/household sync can be layered on next without
 * changing the UI or ViewModel contract.
 */
class PersistentTaskRepository(context: Context) : TaskRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val mutableTasks = MutableStateFlow(loadTasks())
    override val tasks: StateFlow<List<CleaningTask>> = mutableTasks.asStateFlow()

    override fun add(task: CleaningTask) {
        mutableTasks.value = listOf(task) + mutableTasks.value
        persist()
    }

    override fun update(task: CleaningTask) {
        mutableTasks.value = mutableTasks.value.map { existing ->
            if (existing.id == task.id) task else existing
        }
        persist()
    }

    private fun persist() {
        val array = JSONArray()
        mutableTasks.value.forEach { task -> array.put(task.toJson()) }
        preferences.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private fun loadTasks(): List<CleaningTask> {
        val raw = preferences.getString(KEY_TASKS, null)
        if (raw.isNullOrBlank()) return starterTasks()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toCleaningTask())
                }
            }
        }.getOrElse {
            // Never strand the user on a broken local payload. Keep the app
            // usable and allow a fresh set of tasks to be saved normally.
            starterTasks()
        }
    }

    private fun CleaningTask.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("notes", notes)
        put("room", room)
        put("dueLabel", dueLabel)
        put("assignee", assignee.name)
        put("priority", priority.name)
        put("completed", completed)
        put("completedBy", completedBy?.name ?: JSONObject.NULL)
        put("completedAt", completedAt?.toString() ?: JSONObject.NULL)
        put("createdAt", createdAt.toString())
    }

    private fun JSONObject.toCleaningTask(): CleaningTask = CleaningTask(
        id = getString("id"),
        title = getString("title"),
        notes = optString("notes", ""),
        room = optString("room", "Around the house"),
        dueLabel = optString("dueLabel", "Today"),
        assignee = enumValueOrDefault(optString("assignee"), Assignee.EITHER),
        priority = enumValueOrDefault(optString("priority"), Priority.NORMAL),
        completed = optBoolean("completed", false),
        completedBy = optNullableString("completedBy")?.let {
            enumValueOrDefault(it, Assignee.EITHER)
        },
        completedAt = optNullableString("completedAt")?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull()
        },
        createdAt = optNullableString("createdAt")
            ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
            ?: Instant.now()
    )

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private fun starterTasks() = listOf(
        CleaningTask(
            id = "welcome-1",
            title = "Empty dishwasher",
            room = "Kitchen",
            dueLabel = "Before dinner",
            assignee = Assignee.JESSIE
        ),
        CleaningTask(
            id = "welcome-2",
            title = "Sweep living room",
            room = "Living room",
            assignee = Assignee.EITHER
        ),
        CleaningTask(
            id = "welcome-3",
            title = "Clean bathroom",
            room = "Bathroom",
            dueLabel = "Tonight",
            assignee = Assignee.MATT,
            priority = Priority.URGENT
        ),
        CleaningTask(
            id = "welcome-4",
            title = "Wipe kitchen counters",
            room = "Kitchen",
            assignee = Assignee.MATT,
            completed = true,
            completedBy = Assignee.MATT
        )
    )

    private companion object {
        const val PREFS_NAME = "our_home_tasks"
        const val KEY_TASKS = "tasks_json"
    }
}
