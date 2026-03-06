package io.github.howshous.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.github.howshous.R
import io.github.howshous.data.auth.AuthRepository
import io.github.howshous.ui.data.clearSession
import io.github.howshous.ui.data.readRoleFlow
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.data.saveRole
import io.github.howshous.ui.data.saveUid
import io.github.howshous.ui.theme.PrimaryTeal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun Splash(nav: NavController) {
    val context = LocalContext.current

    // Auto-navigate after a short delay, using persisted auth if available
    LaunchedEffect(Unit) {
        delay(1200) // splash duration

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            clearSession(context)
            nav.navigate("login_choice") {
                popUpTo("splash") { inclusive = true }
            }
            return@LaunchedEffect
        }

        val storedUid = readUidFlow(context).first()
        val storedRole = readRoleFlow(context).first()

        if (storedUid == currentUser.uid && storedRole.isNotBlank()) {
            nav.navigate("dashboard_router") {
                popUpTo("splash") { inclusive = true }
            }
            return@LaunchedEffect
        }

        val db = FirebaseFirestore.getInstance()
        try {
            val doc = db.collection("users").document(currentUser.uid).get().await()
            if (!doc.exists()) {
                AuthRepository(context).logout()
                nav.navigate("login_choice") {
                    popUpTo("splash") { inclusive = true }
                }
                return@LaunchedEffect
            }

            val role = doc.getString("role") ?: ""
            val isBanned = doc.getBoolean("isBanned") ?: false
            if (role.isBlank()) {
                clearSession(context)
                nav.navigate("login_choice") {
                    popUpTo("splash") { inclusive = true }
                }
                return@LaunchedEffect
            }
            if (isBanned) {
                AuthRepository(context).logout()
                nav.navigate("login_choice") {
                    popUpTo("splash") { inclusive = true }
                }
                return@LaunchedEffect
            }

            saveUid(context, currentUser.uid)
            saveRole(context, role)
            nav.navigate("dashboard_router") {
                popUpTo("splash") { inclusive = true }
            }
        } catch (e: Exception) {
            clearSession(context)
            nav.navigate("login_choice") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // UI layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryTeal),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_white),
            contentDescription = "HowsHous Logo",
            modifier = Modifier
                .size(200.dp)
        )
    }
}
