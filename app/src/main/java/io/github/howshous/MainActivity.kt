package io.github.howshous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import io.github.howshous.ui.components.navigation.HowsHousApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase FIRST
        FirebaseApp.initializeApp(this)

        // Connect to emulators BEFORE any Firebase usage
        val host = "10.0.2.2"
        FirebaseAuth.getInstance().useEmulator(host, 9100)
        FirebaseFirestore.getInstance().useEmulator(host, 8085)
        FirebaseStorage.getInstance().useEmulator(host, 9190)

        setContent {
            HowsHousApp()
        }
    }
}