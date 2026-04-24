package io.github.howshous.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.data.firestore.NotificationRepository
import io.github.howshous.data.models.Notification
import io.github.howshous.ui.data.readLastNotifiedAtMs
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.data.saveLastNotifiedAtMs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = readUidFlow(applicationContext).first()
        if (uid.isBlank()) return Result.success()

        val lastMs = readLastNotifiedAtMs(applicationContext)
        val lastTs = Timestamp(lastMs / 1000, ((lastMs % 1000) * 1_000_000).toInt())
        val notifRepo = NotificationRepository()

        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", uid)
                .whereGreaterThan("timestamp", lastTs)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .limit(10)
                .get()
                .await()

            var maxSeenMs = lastMs
            snap.documents.forEach { doc ->
                val notif = doc.toObject(Notification::class.java)?.copy(id = doc.id) ?: return@forEach
                if (notif.read || notif.notified) return@forEach
                LocalNotificationHelper.show(applicationContext, notif)
                notifRepo.markNotified(notif.id)
                val ms = (notif.timestamp?.seconds ?: 0L) * 1000L
                if (ms >= maxSeenMs) maxSeenMs = ms + 1
            }

            saveLastNotifiedAtMs(applicationContext, maxSeenMs)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

