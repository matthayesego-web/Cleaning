package ca.northstarappworks.cleaning.sync

import android.content.Context
import ca.northstarappworks.cleaning.data.HouseholdPreferences
import ca.northstarappworks.cleaning.data.TaskRepository
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
import ca.northstarappworks.cleaning.notifications.TaskNotificationManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import kotlin.random.Random

enum class HouseholdSyncStatus { NOT_PAIRED, CONNECTING, PAIRED, ERROR }

data class HouseholdSyncUiState(
    val status: HouseholdSyncStatus,
    val pairingCode: String? = null,
    val message: String? = null
)

class HouseholdSyncManager(
    context: Context,
    private val repository: TaskRepository,
    private val preferences: HouseholdPreferences
) {
    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val mutableUiState = MutableStateFlow(
        if (preferences.householdId.value == null) {
            HouseholdSyncUiState(HouseholdSyncStatus.NOT_PAIRED)
        } else {
            HouseholdSyncUiState(
                status = HouseholdSyncStatus.CONNECTING,
                pairingCode = preferences.pairingCode.value,
                message = "Connecting your household…"
            )
        }
    )
    val uiState: StateFlow<HouseholdSyncUiState> = mutableUiState.asStateFlow()

    private val listenerRegistrations = mutableListOf<ListenerRegistration>()
    private val seenCompletionIds = mutableSetOf<String>()
    private var completionListenerPrimed = false

    init {
        preferences.householdId.value?.let { householdId -> withSignedInUser { startSync(householdId) } }
    }

    fun createHousehold() {
        mutableUiState.value = HouseholdSyncUiState(HouseholdSyncStatus.CONNECTING, message = "Creating Our Home…")
        withSignedInUser { user ->
            val code = generatePairingCode()
            val householdRef = firestore.collection(HOUSEHOLDS).document()
            val inviteRef = firestore.collection(INVITES).document(code)
            val memberRef = householdRef.collection(MEMBERS).document(user.uid)
            val role = preferences.currentUser.value

            val batch = firestore.batch()
            batch.set(householdRef, mapOf(
                "pairingCode" to code,
                "memberUids" to listOf(user.uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(inviteRef, mapOf(
                "householdId" to householdRef.id,
                "ownerUid" to user.uid,
                "createdAt" to FieldValue.serverTimestamp()
            ))
            batch.set(memberRef, memberMap(role), SetOptions.merge())

            batch.commit()
                .addOnSuccessListener {
                    preferences.setHousehold(householdRef.id, code)
                    uploadLocalSnapshot(householdRef) { startSync(householdRef.id) }
                }
                .addOnFailureListener { fail(it, "Couldn't create the household") }
        }
    }

    fun joinHousehold(rawCode: String) {
        val code = rawCode.normalizedPairingCode()
        if (code.length != PAIRING_CODE_LENGTH) {
            mutableUiState.value = HouseholdSyncUiState(
                HouseholdSyncStatus.ERROR,
                pairingCode = code,
                message = "Enter the $PAIRING_CODE_LENGTH-character household code."
            )
            return
        }

        mutableUiState.value = HouseholdSyncUiState(
            HouseholdSyncStatus.CONNECTING,
            pairingCode = code,
            message = "Joining Our Home…"
        )

        withSignedInUser { user ->
            firestore.collection(INVITES).document(code).get()
                .addOnSuccessListener { invite ->
                    val householdId = invite.getString("householdId")
                    if (!invite.exists() || householdId.isNullOrBlank()) {
                        mutableUiState.value = HouseholdSyncUiState(
                            HouseholdSyncStatus.ERROR,
                            pairingCode = code,
                            message = "That household code wasn't found."
                        )
                        return@addOnSuccessListener
                    }

                    val householdRef = firestore.collection(HOUSEHOLDS).document(householdId)
                    val memberRef = householdRef.collection(MEMBERS).document(user.uid)
                    val role = preferences.currentUser.value

                    firestore.runTransaction { transaction ->
                        val household = transaction.get(householdRef)
                        if (!household.exists()) throw IllegalStateException("Household no longer exists")

                        val members = household.get("memberUids") as? List<*> ?: emptyList<Any>()
                        val existingUids = members.mapNotNull { it as? String }
                        if (user.uid !in existingUids && existingUids.size >= MAX_MEMBERS) {
                            throw IllegalStateException("This household already has two phones")
                        }
                        if (user.uid !in existingUids) {
                            transaction.update(householdRef, mapOf(
                                "memberUids" to FieldValue.arrayUnion(user.uid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            ))
                        }
                        transaction.set(memberRef, memberMap(role), SetOptions.merge())
                        null
                    }.addOnSuccessListener {
                        preferences.setHousehold(householdId, code)
                        repository.replaceTasks(emptyList())
                        repository.replaceCompletions(emptyList())
                        startSync(householdId)
                    }.addOnFailureListener { fail(it, "Couldn't join the household") }
                }
                .addOnFailureListener { fail(it, "Couldn't look up that household") }
        }
    }

    fun publishTask(task: CleaningTask) {
        val householdId = preferences.householdId.value ?: return
        firestore.collection(HOUSEHOLDS).document(householdId)
            .collection(TASKS).document(task.id)
            .set(task.toRemoteMap(), SetOptions.merge())
    }

    fun publishCompletion(record: CompletionRecord) {
        val householdId = preferences.householdId.value ?: return
        firestore.collection(HOUSEHOLDS).document(householdId)
            .collection(COMPLETIONS).document(record.id)
            .set(record.toRemoteMap())
    }

    fun deleteCompletion(recordId: String) {
        val householdId = preferences.householdId.value ?: return
        firestore.collection(HOUSEHOLDS).document(householdId)
            .collection(COMPLETIONS).document(recordId).delete()
    }

    fun updateMemberIdentity(assignee: Assignee) {
        if (assignee == Assignee.EITHER) return
        val householdId = preferences.householdId.value ?: return
        val user = auth.currentUser ?: return
        firestore.collection(HOUSEHOLDS).document(householdId)
            .collection(MEMBERS).document(user.uid)
            .set(memberMap(assignee), SetOptions.merge())
    }

    private fun withSignedInUser(onReady: (FirebaseUser) -> Unit) {
        auth.currentUser?.let(onReady) ?: auth.signInAnonymously()
            .addOnSuccessListener { result ->
                result.user?.let(onReady)
                    ?: fail(IllegalStateException("Anonymous sign-in returned no user"), "Couldn't connect")
            }
            .addOnFailureListener { fail(it, "Couldn't sign in to household sync") }
    }

    private fun startSync(householdId: String) {
        stopListeners()
        completionListenerPrimed = false
        seenCompletionIds.clear()
        val householdRef = firestore.collection(HOUSEHOLDS).document(householdId)

        listenerRegistrations += householdRef.collection(TASKS).addSnapshotListener { snapshot, error ->
            if (error != null) {
                fail(error, "Task sync paused")
                return@addSnapshotListener
            }
            val tasks = snapshot?.documents.orEmpty()
                .mapNotNull { it.toCleaningTaskOrNull() }
                .sortedWith(compareBy<CleaningTask> { it.nextDueDate }.thenBy { it.room }.thenBy { it.title })
            repository.replaceTasks(tasks)
        }

        listenerRegistrations += householdRef.collection(COMPLETIONS).addSnapshotListener { snapshot, error ->
            if (error != null) {
                fail(error, "History sync paused")
                return@addSnapshotListener
            }
            snapshot ?: return@addSnapshotListener
            val records = snapshot.documents
                .mapNotNull { it.toCompletionRecordOrNull() }
                .sortedByDescending { it.completedAt }
            repository.replaceCompletions(records)

            if (!completionListenerPrimed) {
                val previousCheckpoint = preferences.lastSeenCompletionAt()
                seenCompletionIds += records.map { it.id }
                if (previousCheckpoint != null) {
                    records.asSequence()
                        .filter { it.completedAt.isAfter(previousCheckpoint) }
                        .filter { it.completedBy != preferences.currentUser.value }
                        .sortedBy { it.completedAt }
                        .forEach(::showCompletionBoop)
                }
                completionListenerPrimed = true
                updateCompletionCheckpoint(records)
            } else {
                snapshot.documentChanges.asSequence()
                    .filter { it.type == DocumentChange.Type.ADDED }
                    .mapNotNull { it.document.toCompletionRecordOrNull() }
                    .filter { seenCompletionIds.add(it.id) }
                    .filter { it.completedBy != preferences.currentUser.value }
                    .forEach(::showCompletionBoop)
                updateCompletionCheckpoint(records)
            }
        }

        mutableUiState.value = HouseholdSyncUiState(
            status = HouseholdSyncStatus.PAIRED,
            pairingCode = preferences.pairingCode.value,
            message = "Matt and Jessie can now share this household."
        )
    }

    private fun showCompletionBoop(record: CompletionRecord) {
        TaskNotificationManager.showTaskCompleted(
            context = appContext,
            completedBy = record.completedBy.label,
            taskTitle = record.taskTitle,
            room = record.room,
            notificationId = record.id.hashCode()
        )
    }

    private fun updateCompletionCheckpoint(records: List<CompletionRecord>) {
        records.maxOfOrNull { it.completedAt }?.let(preferences::setLastSeenCompletionAt)
    }

    private fun stopListeners() {
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()
    }

    private fun uploadLocalSnapshot(householdRef: DocumentReference, onFinished: () -> Unit) {
        val localTasks = repository.tasks.value.filterNot { it.id.startsWith("welcome-") }
        val localCompletions = repository.completions.value
        if (localTasks.isEmpty() && localCompletions.isEmpty()) {
            repository.replaceTasks(emptyList())
            repository.replaceCompletions(emptyList())
            onFinished()
            return
        }

        val batch = firestore.batch()
        localTasks.forEach { batch.set(householdRef.collection(TASKS).document(it.id), it.toRemoteMap()) }
        localCompletions.forEach {
            batch.set(householdRef.collection(COMPLETIONS).document(it.id), it.toRemoteMap())
        }
        batch.commit()
            .addOnSuccessListener { onFinished() }
            .addOnFailureListener { fail(it, "Household created, but local tasks couldn't upload") }
    }

    private fun memberMap(role: Assignee): Map<String, Any> = mapOf(
        "role" to role.name,
        "name" to role.label,
        "joinedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun fail(error: Throwable, friendlyMessage: String) {
        mutableUiState.value = HouseholdSyncUiState(
            status = HouseholdSyncStatus.ERROR,
            pairingCode = preferences.pairingCode.value,
            message = "$friendlyMessage: ${error.message ?: "unknown error"}"
        )
    }

    private fun CleaningTask.toRemoteMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "notes" to notes,
        "room" to room,
        "dueLabel" to dueLabel,
        "assignee" to assignee.name,
        "priority" to priority.name,
        "recurrence" to recurrence.name,
        "intervalDays" to intervalDays,
        "nextDueDate" to nextDueDate.toString(),
        "completed" to completed,
        "completedBy" to completedBy?.name,
        "completedAt" to completedAt?.let { Timestamp(Date.from(it)) },
        "createdAt" to Timestamp(Date.from(createdAt)),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun CompletionRecord.toRemoteMap(): Map<String, Any?> = mapOf(
        "taskId" to taskId,
        "taskTitle" to taskTitle,
        "room" to room,
        "completedBy" to completedBy.name,
        "scheduledDueDate" to scheduledDueDate?.toString(),
        "completedAt" to Timestamp(Date.from(completedAt))
    )

    private fun DocumentSnapshot.toCleaningTaskOrNull(): CleaningTask? = runCatching {
        CleaningTask(
            id = id,
            title = getString("title") ?: return null,
            notes = getString("notes").orEmpty(),
            room = getString("room") ?: "Around the house",
            dueLabel = getString("dueLabel") ?: "Today",
            assignee = enumOrDefault(getString("assignee"), Assignee.EITHER),
            priority = enumOrDefault(getString("priority"), Priority.NORMAL),
            recurrence = enumOrDefault(getString("recurrence"), Recurrence.ONE_OFF),
            intervalDays = (getLong("intervalDays")?.toInt() ?: 2).coerceIn(2, 365),
            nextDueDate = getString("nextDueDate")?.let(LocalDate::parse) ?: LocalDate.now(),
            completed = getBoolean("completed") ?: false,
            completedBy = getString("completedBy")?.let { enumOrDefault(it, Assignee.EITHER) },
            completedAt = getTimestamp("completedAt")?.toDate()?.toInstant(),
            createdAt = getTimestamp("createdAt")?.toDate()?.toInstant() ?: Instant.now()
        )
    }.getOrNull()

    private fun DocumentSnapshot.toCompletionRecordOrNull(): CompletionRecord? = runCatching {
        CompletionRecord(
            id = id,
            taskId = getString("taskId") ?: return null,
            taskTitle = getString("taskTitle") ?: "Task",
            room = getString("room") ?: "Around the house",
            completedBy = enumOrDefault(getString("completedBy"), Assignee.MATT),
            scheduledDueDate = getString("scheduledDueDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            completedAt = getTimestamp("completedAt")?.toDate()?.toInstant() ?: Instant.now()
        )
    }.getOrNull()

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private fun generatePairingCode(): String = buildString {
        repeat(PAIRING_CODE_LENGTH) { append(PAIRING_ALPHABET[Random.nextInt(PAIRING_ALPHABET.length)]) }
    }

    private fun String.normalizedPairingCode(): String = uppercase().filter { it.isLetterOrDigit() }

    companion object {
        private const val HOUSEHOLDS = "households"
        private const val INVITES = "invites"
        private const val MEMBERS = "members"
        private const val TASKS = "tasks"
        private const val COMPLETIONS = "completions"
        private const val MAX_MEMBERS = 2
        const val PAIRING_CODE_LENGTH = 6
        private const val PAIRING_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
