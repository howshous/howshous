package io.github.howshous.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.howshous.MainActivity
import io.github.howshous.R
import io.github.howshous.data.models.Notification

object LocalNotificationHelper {
    fun show(context: Context, notif: Notification) {
        NotificationChannels.ensureCreated(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("actionUrl", notif.actionUrl)
            putExtra("notificationId", notif.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notif.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(context, NotificationChannels.GENERAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notif.title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(notif.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notif.message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notif.id.hashCode(), n)
    }
}

