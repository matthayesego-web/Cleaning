package ca.northstarappworks.cleaning.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import ca.northstarappworks.cleaning.MainActivity
import ca.northstarappworks.cleaning.R

/** Builds the intentionally small household task-complete "boop". */
object TaskNotificationManager {
    private const val CHANNEL_ID = "task_completions"
    private const val CHANNEL_NAME = "Task completions"
    private const val REWARD_CHANNEL_ID = "reward_requests"
    private const val REWARD_CHANNEL_NAME = "Reward requests"

    fun showTaskCompleted(
        context: Context,
        completedBy: String,
        taskTitle: String,
        room: String,
        notificationId: Int = (completedBy + taskTitle + room).hashCode()
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

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
            .setSmallIcon(R.drawable.ic_task_complete)
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

    fun showRewardUseRequest(
        context: Context,
        requestedBy: String,
        rewardTitle: String,
        notificationId: Int = (requestedBy + rewardTitle).hashCode()
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService<NotificationManager>() ?: return
        ensureRewardChannel(manager)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, REWARD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_task_complete)
            .setContentTitle("$requestedBy wants to use a reward")
            .setContentText(rewardTitle)
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("$requestedBy wants to use “$rewardTitle”. Open Our Home to approve or choose Not now.")
            )
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun ensureRewardChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(REWARD_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                REWARD_CHANNEL_ID,
                REWARD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Requests to use a redeemed household reward."
                enableVibration(true)
            }
        )
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
