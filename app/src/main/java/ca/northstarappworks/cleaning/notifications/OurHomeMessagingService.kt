package ca.northstarappworks.cleaning.notifications

import ca.northstarappworks.cleaning.data.HouseholdPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives true push notifications once the Firebase sender is deployed. */
class OurHomeMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val completedBy = data["completedBy"] ?: return
        val taskTitle = data["taskTitle"] ?: return
        val room = data["room"] ?: "Around the house"

        val preferences = HouseholdPreferences(applicationContext)
        if (completedBy.equals(preferences.currentUser.value.label, ignoreCase = true)) return

        TaskNotificationManager.showTaskCompleted(
            context = applicationContext,
            completedBy = completedBy,
            taskTitle = taskTitle,
            room = room,
            notificationId = (data["completionId"] ?: message.messageId ?: taskTitle).hashCode()
        )
    }

    override fun onNewToken(token: String) {
        val preferences = HouseholdPreferences(applicationContext)
        val householdId = preferences.householdId.value ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("households")
            .document(householdId)
            .collection("members")
            .document(user.uid)
            .set(
                mapOf(
                    "fcmToken" to token,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }
}
