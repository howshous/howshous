package io.github.howshous.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val GENERAL = "general"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            GENERAL,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General app notifications"
        }
        manager.createNotificationChannel(channel)
    }
}

