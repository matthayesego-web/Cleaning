package ca.northstarappworks.cleaning.data

import android.content.Context
import ca.northstarappworks.cleaning.model.Assignee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

/** Stores this phone's household identity, pairing information and sync checkpoints. */
class HouseholdPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val mutableCurrentUser = MutableStateFlow(loadCurrentUser())
    val currentUser: StateFlow<Assignee> = mutableCurrentUser.asStateFlow()

    private val mutableHouseholdId = MutableStateFlow(preferences.getString(KEY_HOUSEHOLD_ID, null))
    val householdId: StateFlow<String?> = mutableHouseholdId.asStateFlow()

    private val mutablePairingCode = MutableStateFlow(preferences.getString(KEY_PAIRING_CODE, null))
    val pairingCode: StateFlow<String?> = mutablePairingCode.asStateFlow()

    fun setCurrentUser(assignee: Assignee) {
        if (assignee == Assignee.EITHER) return
        mutableCurrentUser.value = assignee
        preferences.edit().putString(KEY_CURRENT_USER, assignee.name).apply()
    }

    fun setHousehold(householdId: String, pairingCode: String) {
        mutableHouseholdId.value = householdId
        mutablePairingCode.value = pairingCode
        preferences.edit()
            .putString(KEY_HOUSEHOLD_ID, householdId)
            .putString(KEY_PAIRING_CODE, pairingCode)
            .remove(KEY_LAST_SEEN_COMPLETION_AT)
            .apply()
    }

    fun clearHousehold() {
        mutableHouseholdId.value = null
        mutablePairingCode.value = null
        preferences.edit()
            .remove(KEY_HOUSEHOLD_ID)
            .remove(KEY_PAIRING_CODE)
            .remove(KEY_LAST_SEEN_COMPLETION_AT)
            .apply()
    }

    fun lastSeenCompletionAt(): Instant? = preferences
        .getString(KEY_LAST_SEEN_COMPLETION_AT, null)
        ?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }

    fun setLastSeenCompletionAt(instant: Instant) {
        preferences.edit().putString(KEY_LAST_SEEN_COMPLETION_AT, instant.toString()).apply()
    }

    private fun loadCurrentUser(): Assignee {
        val stored = preferences.getString(KEY_CURRENT_USER, null)
        return Assignee.entries.firstOrNull { it.name == stored && it != Assignee.EITHER }
            ?: Assignee.MATT
    }

    private companion object {
        const val PREFS_NAME = "our_home_household"
        const val KEY_CURRENT_USER = "current_user"
        const val KEY_HOUSEHOLD_ID = "household_id"
        const val KEY_PAIRING_CODE = "pairing_code"
        const val KEY_LAST_SEEN_COMPLETION_AT = "last_seen_completion_at"
    }
}
