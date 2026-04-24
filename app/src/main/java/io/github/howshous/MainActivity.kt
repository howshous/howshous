package io.github.howshous

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.github.howshous.data.firestore.NotificationRepository
import io.github.howshous.notifications.LocalNotificationHelper
import io.github.howshous.notifications.NotificationSyncWorker
import io.github.howshous.ui.components.navigation.HowsHousApp
import io.github.howshous.ui.data.readLastNotifiedAtMs
import io.github.howshous.ui.data.readPhoneNotifsEnabledFlow
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.data.saveLastNotifiedAtMs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase FIRST
        FirebaseApp.initializeApp(this)

        // Only use emulators in debug builds.
        if (BuildConfig.USE_FIREBASE_EMULATORS) {
            val host = "10.0.2.2"
            FirebaseAuth.getInstance().useEmulator(host, 9100)
            FirebaseFirestore.getInstance().useEmulator(host, 8085)
            FirebaseStorage.getInstance().useEmulator(host, 9190)
        }

        // Android 13+ requires runtime permission to show notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Free-plan "phone notifications": foreground listener + periodic background poll (15 min min).
        val notifRepo = NotificationRepository()
        lifecycleScope.launch {
            var listener: com.google.firebase.firestore.ListenerRegistration? = null
            combine(
                readUidFlow(this@MainActivity),
                readPhoneNotifsEnabledFlow(this@MainActivity)
            ) { uid, enabled -> uid to enabled }.collectLatest { (uid, enabled) ->
                listener?.remove()
                listener = null

                if (!enabled || uid.isBlank()) {
                    WorkManager.getInstance(this@MainActivity).cancelUniqueWork("notification_sync")
                    return@collectLatest
                }

                val work = PeriodicWorkRequestBuilder<NotificationSyncWorker>(15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
                    "notification_sync",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    work
                )

                listener = notifRepo.listenNotificationsForUser(uid, limit = 20) { notifs ->
                    lifecycleScope.launch {
                        var maxSeen = readLastNotifiedAtMs(this@MainActivity)
                        notifs.asReversed().forEach { n ->
                            val ms = (n.timestamp?.seconds ?: 0L) * 1000L
                            if (!n.read && !n.notified && ms >= maxSeen) {
                                LocalNotificationHelper.show(this@MainActivity, n)
                                notifRepo.markNotified(n.id)
                                if (ms >= maxSeen) maxSeen = ms + 1
                            }
                        }
                        saveLastNotifiedAtMs(this@MainActivity, maxSeen)
                    }
                }
            }
        }

        setContent {
            HowsHousApp()
        }
    }
}