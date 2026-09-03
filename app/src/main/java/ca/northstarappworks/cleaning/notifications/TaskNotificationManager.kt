package ca.northstarappworks.cleaning.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import ca.northstarappworks.cleaning.MainActivity
import ca.northstarappworks.cleaning.R

/**
 * Builds the intentionally small "boop" notification used when the other
 * household member completes a task. The eventual sync/push receiver only has
 * to pass the remote completion payload into this class.
 */
object TaskNotificationManager {
    private const val CHANNEL_ID = "task_completions"
    private const val CHANNEL_NAME = "Task completions"

    fun showTaskCompleted(
        context: Context,
        completedBy: String,
        taskTitle: String,
        room: String,
        notificationId: Int = (completedBy + taskTitle + room).hashCode()
    ) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        ensureChannel(manager)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$completedBy finished a task")
            .setContentText("$taskTitle · $room")
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("$completedBy completed $taskTitle in $room.")
            )
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "A small alert when Matt or Jessie completes a household task."
                enableVibration(true)
            }
        )
    }
}
