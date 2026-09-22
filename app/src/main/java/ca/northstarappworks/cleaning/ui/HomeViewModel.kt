package ca.northstarappworks.cleaning.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import ca.northstarappworks.cleaning.data.HouseholdPreferences
import ca.northstarappworks.cleaning.data.PersistentRewardRepository
import ca.northstarappworks.cleaning.data.PersistentTaskRepository
import ca.northstarappworks.cleaning.data.TaskRepository
import ca.northstarappworks.cleaning.model.Assignee
import ca.northstarappworks.cleaning.model.CleaningTask
import ca.northstarappworks.cleaning.model.CompletionRecord
import ca.northstarappworks.cleaning.model.Priority
import ca.northstarappworks.cleaning.model.Recurrence
import ca.northstarappworks.cleaning.model.RewardCoupon
import ca.northstarappworks.cleaning.model.RewardCouponStatus
import ca.northstarappworks.cleaning.model.RewardDefinition
import ca.northstarappworks.cleaning.sync.HouseholdSyncManager
import ca.northstarappworks.cleaning.sync.HouseholdSyncUiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository = PersistentTaskRepository(application)
    private val rewardRepository = PersistentRewardRepository(application)
    private val householdPreferences = HouseholdPreferences(application)
    private val syncManager = HouseholdSyncManager(application, repository, rewardRepository, householdPreferences)
    private val firestore = FirebaseFirestore.getInstance()

    val tasks: StateFlow<List<CleaningTask>> = repository.tasks
    val completions: StateFlow<List<CompletionRecord>> = repository.completions
    val customRewards: StateFlow<List<RewardDefinition>> = rewardRepository.customRewards
    val rewardCoupons: StateFlow<List<RewardCoupon>> = rewardRepository.coupons
    val currentUser: StateFlow<Assignee> = householdPreferences.currentUser
    val syncUiState: StateFlow<HouseholdSyncUiState> = syncManager.uiState

    val hadHouseholdAtLaunch: Boolean = householdPreferences.householdId.value != null

    fun createHousehold() = syncManager.createHousehold()
    fun joinHousehold(code: String) = syncManager.joinHousehold(code)

    fun addTask(
        title: String,
        room: String,
        assignee: Assignee,
        priority: Priority,
        recurrence: Recurrence,
        intervalDays: Int,
        dueDate: LocalDate = LocalDate.now()
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        val task = CleaningTask(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            room = room,
            assignee = assignee,
            priority = priority,
            recurrence = recurrence,
            intervalDays = intervalDays.coerceIn(2, 365),
            nextDueDate = dueDate
        )
        repository.add(task)
        syncManager.publishTask(task)
    }

    fun updateTask(
        task: CleaningTask,
        title: String,
        room: String,
        assignee: Assignee,
        priority: Priority,
        recurrence: Recurrence,
        intervalDays: Int,
        dueDate: LocalDate
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        val updated = task.copy(
            title = cleanTitle,
            room = room,
            assignee = assignee,
            priority = priority,
            recurrence = recurrence,
            intervalDays = intervalDays.coerceIn(2, 365),
            nextDueDate = dueDate
        )
        repository.update(updated)
        syncManager.publishTask(updated)
    }

    fun deleteTask(task: CleaningTask) {
        repository.delete(task.id)
        val householdId = householdPreferences.householdId.value ?: return
        firestore.collection("households").document(householdId)
            .collection("tasks").document(task.id).delete()
    }

    fun addCustomReward(
        title: String,
        description: String,
        cost: Int,
        owner: Assignee
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || owner == Assignee.EITHER) return
        val reward = RewardDefinition(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            description = description.trim(),
            cost = cost.coerceIn(1, 5000),
            owner = owner,
            custom = true
        )
        rewardRepository.addReward(reward)
        syncManager.publishReward(reward)
    }

    fun deleteCustomReward(reward: RewardDefinition) {
        if (!reward.custom) return
        rewardRepository.deleteReward(reward.id)
        syncManager.deleteReward(reward.id)
    }

    fun redeemReward(reward: RewardDefinition) {
        val owner = currentUser.value
        if (reward.owner != owner) return
        val earned = repository.completions.value.count { it.completedBy == owner } * POINTS_PER_TASK
        val spent = rewardRepository.coupons.value
            .filter { it.owner == owner }
            .sumOf { it.cost }
        if (earned - spent < reward.cost) return

        val coupon = RewardCoupon(
            id = UUID.randomUUID().toString(),
            rewardId = reward.id,
            title = reward.title,
            cost = reward.cost,
            owner = owner
        )
        rewardRepository.addCoupon(coupon)
        syncManager.publishCoupon(coupon)
    }

    fun requestRewardUse(coupon: RewardCoupon) {
        if (coupon.owner != currentUser.value || coupon.status != RewardCouponStatus.AVAILABLE) return
        val updated = coupon.copy(
            status = RewardCouponStatus.PENDING,
            requestedAt = Instant.now(),
            resolvedAt = null
        )
        rewardRepository.updateCoupon(updated)
        syncManager.publishCoupon(updated)
    }

    fun approveRewardUse(coupon: RewardCoupon) {
        if (coupon.owner == currentUser.value || coupon.status != RewardCouponStatus.PENDING) return
        val updated = coupon.copy(
            status = RewardCouponStatus.USED,
            resolvedAt = Instant.now()
        )
        rewardRepository.updateCoupon(updated)
        syncManager.publishCoupon(updated)
    }

    fun declineRewardUse(coupon: RewardCoupon) {
        if (coupon.owner == currentUser.value || coupon.status != RewardCouponStatus.PENDING) return
        val updated = coupon.copy(
            status = RewardCouponStatus.AVAILABLE,
            requestedAt = null,
            resolvedAt = Instant.now()
        )
        rewardRepository.updateCoupon(updated)
        syncManager.publishCoupon(updated)
    }

    fun setCurrentUser(assignee: Assignee) {
        householdPreferences.setCurrentUser(assignee)
        syncManager.updateMemberIdentity(assignee)
    }

    fun setCompleted(task: CleaningTask, completed: Boolean) {
        if (completed) completeTask(task) else reopenTask(task)
    }

    fun undoCompletion(record: CompletionRecord) {
        val latestForTask = repository.completions.value
            .filter { it.taskId == record.taskId }
            .maxByOrNull { it.completedAt }
            ?: return
        if (latestForTask.id != record.id) return

        val task = repository.tasks.value.firstOrNull { it.id == record.taskId } ?: return
        repository.removeCompletion(record.id)
        syncManager.deleteCompletion(record.id)

        val restoredDueDate = record.scheduledDueDate
            ?: record.completedAt.atZone(ZoneId.systemDefault()).toLocalDate()
        val reopened = task.copy(
            completed = false,
            completedBy = null,
            completedAt = null,
            nextDueDate = restoredDueDate
        )
        repository.update(reopened)
        syncManager.publishTask(reopened)
    }

    private fun completeTask(task: CleaningTask) {
        val completedBy = currentUser.value
        val completedAt = Instant.now()
        val record = CompletionRecord(
            id = UUID.randomUUID().toString(),
            taskId = task.id,
            taskTitle = task.title,
            room = task.room,
            completedBy = completedBy,
            scheduledDueDate = task.nextDueDate,
            completedAt = completedAt
        )

        repository.addCompletion(record)
        syncManager.publishCompletion(record)

        val updatedTask = if (task.recurrence == Recurrence.ONE_OFF) {
            task.copy(completed = true, completedBy = completedBy, completedAt = completedAt)
        } else {
            task.copy(
                completed = false,
                completedBy = null,
                completedAt = null,
                nextDueDate = nextOccurrence(task)
            )
        }

        repository.update(updatedTask)
        syncManager.publishTask(updatedTask)
    }

    private fun reopenTask(task: CleaningTask) {
        repository.completions.value
            .filter { it.taskId == task.id }
            .maxByOrNull { it.completedAt }
            ?.let(::undoCompletion)
    }

    private fun nextOccurrence(task: CleaningTask): LocalDate {
        val today = LocalDate.now()
        var next = advance(task.nextDueDate, task.recurrence, task.intervalDays)
        while (!next.isAfter(today) && task.recurrence != Recurrence.ONE_OFF) {
            next = advance(next, task.recurrence, task.intervalDays)
        }
        return next
    }

    private fun advance(date: LocalDate, recurrence: Recurrence, intervalDays: Int): LocalDate = when (recurrence) {
        Recurrence.ONE_OFF -> date
        Recurrence.DAILY -> date.plusDays(1)
        Recurrence.CUSTOM_DAYS -> date.plusDays(intervalDays.coerceIn(2, 365).toLong())
        Recurrence.WEEKLY -> date.plusWeeks(1)
        Recurrence.MONTHLY -> date.plusMonths(1)
    }

    companion object {
        const val POINTS_PER_TASK = 10
    }
}
