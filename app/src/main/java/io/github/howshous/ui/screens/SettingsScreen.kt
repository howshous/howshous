package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.clearSession
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.AccountViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: AccountViewModel = viewModel()
    val profile by viewModel.userProfile.collectAsState()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadUserProfile(uid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Profile Section
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profile", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Text("Name: ${profile?.firstName ?: "--"} ${profile?.lastName ?: "--"}")
                    Spacer(Modifier.height(8.dp))
                    Text("Email: ${profile?.email ?: "--"}")
                    Spacer(Modifier.height(8.dp))
                    Text("Role: ${profile?.role?.uppercase() ?: "--"}")
                    Spacer(Modifier.height(8.dp))
                    Text("Verified: ${if (profile?.verified == true) "✓ Yes" else "✗ No"}")
                }
            }

            // Action Buttons
            Button(
                onClick = { nav.navigate("edit_profile") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Edit Profile")
            }

            Button(
                onClick = { nav.navigate("change_password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Change Password")
            }

            Spacer(Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = {
                    scope.launch {
                        clearSession(context)
                        nav.navigate("login") {
                            popUpTo("tenant_dashboard") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout", color = Color.White)
            }
        }
    }
}
