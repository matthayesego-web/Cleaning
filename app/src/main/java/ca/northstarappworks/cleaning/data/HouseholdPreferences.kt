package ca.northstarappworks.cleaning.data

import android.content.Context
import ca.northstarappworks.cleaning.model.Assignee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stores which household member is using this phone. */
class HouseholdPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val mutableCurrentUser = MutableStateFlow(loadCurrentUser())
    val currentUser: StateFlow<Assignee> = mutableCurrentUser.asStateFlow()

    fun setCurrentUser(assignee: Assignee) {
        if (assignee == Assignee.EITHER) return
        mutableCurrentUser.value = assignee
        preferences.edit().putString(KEY_CURRENT_USER, assignee.name).apply()
    }

    private fun loadCurrentUser(): Assignee {
        val stored = preferences.getString(KEY_CURRENT_USER, null)
        return Assignee.entries.firstOrNull { it.name == stored && it != Assignee.EITHER }
            ?: Assignee.MATT
    }

    private companion object {
        const val PREFS_NAME = "our_home_household"
        const val KEY_CURRENT_USER = "current_user"
    }
}
