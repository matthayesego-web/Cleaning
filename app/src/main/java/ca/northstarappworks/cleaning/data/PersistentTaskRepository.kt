package ca.northstarappworks.cleaning.data

import android.content.Context
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/**
 * Local source of truth for tasks and completion history.
 *
 * Firestore mirrors through this repository so the UI stays responsive offline,
 * while a paired household can replace the local snapshot whenever remote data
 * changes.
 */
class PersistentTaskRepository(context: Context) : TaskRepository {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val mutableTasks = MutableStateFlow(loadTasks())
    override val tasks: StateFlow<List<CleaningTask>> = mutableTasks.asStateFlow()

    private val mutableCompletions = MutableStateFlow(loadCompletions())
    override val completions: StateFlow<List<CompletionRecord>> = mutableCompletions.asStateFlow()

    override fun add(task: CleaningTask) {
        mutableTasks.value = listOf(task) + mutableTasks.value
        persistTasks()
    }

    override fun update(task: CleaningTask) {
        mutableTasks.value = mutableTasks.value.map { existing ->
            if (existing.id == task.id) task else existing
        }
        persistTasks()
    }

    override fun addCompletion(record: CompletionRecord) {
        mutableCompletions.value = listOf(record) + mutableCompletions.value
        persistCompletions()
    }

    override fun removeCompletion(recordId: String) {
        mutableCompletions.value = mutableCompletions.value.filterNot { it.id == recordId }
        persistCompletions()
    }

    override fun replaceTasks(tasks: List<CleaningTask>) {
        mutableTasks.value = tasks
        persistTasks()
    }

    override fun replaceCompletions(records: List<CompletionRecord>) {
        mutableCompletions.value = records.sortedByDescending { it.completedAt }
        persistCompletions()
    }

    private fun persistTasks() {
        val array = JSONArray()
        mutableTasks.value.forEach { task -> array.put(task.toJson()) }
        preferences.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private fun persistCompletions() {
        val array = JSONArray()
        mutableCompletions.value.forEach { completion -> array.put(completion.toJson()) }
        preferences.edit().putString(KEY_COMPLETIONS, array.toString()).apply()
    }

    private fun loadTasks(): List<CleaningTask> {
        val raw = preferences.getString(KEY_TASKS, null)
        if (raw.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toCleaningTask())
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun loadCompletions(): List<CompletionRecord> {
        val raw = preferences.getString(KEY_COMPLETIONS, null)
        if (raw.isNullOrBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toCompletionRecord())
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun CleaningTask.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("notes", notes)
        put("room", room)
        put("dueLabel", dueLabel)
        put("assignee", assignee.name)
        put("priority", priority.name)
        put("recurrence", recurrence.name)
        put("nextDueDate", nextDueDate.toString())
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
        recurrence = enumValueOrDefault(optString("recurrence"), Recurrence.ONE_OFF),
        nextDueDate = optNullableString("nextDueDate")
            ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
            ?: LocalDate.now(),
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

    private fun CompletionRecord.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("taskId", taskId)
        put("taskTitle", taskTitle)
        put("room", room)
        put("completedBy", completedBy.name)
        put("completedAt", completedAt.toString())
    }

    private fun JSONObject.toCompletionRecord(): CompletionRecord = CompletionRecord(
        id = getString("id"),
        taskId = getString("taskId"),
        taskTitle = optString("taskTitle", "Task"),
        room = optString("room", "Around the house"),
        completedBy = enumValueOrDefault(optString("completedBy"), Assignee.MATT),
        completedAt = optNullableString("completedAt")
            ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
            ?: Instant.now()
    )

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private companion object {
        const val PREFS_NAME = "our_home_tasks"
        const val KEY_TASKS = "tasks_json"
        const val KEY_COMPLETIONS = "completion_history_json"
    }
}
