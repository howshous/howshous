package io.github.howshous.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.data.auth.AuthRepository
import io.github.howshous.ui.data.readRoleFlow
import io.github.howshous.ui.data.readUidFlow
import kotlinx.coroutines.launch

@Composable
fun LandlordDashboard(nav: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uid by readUidFlow(context).collectAsState(initial = "")
    val role by readRoleFlow(context).collectAsState(initial = "")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Landlord Dashboard", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            Text("UID: $uid", style = MaterialTheme.typography.bodySmall)
            Text("Role: $role", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        AuthRepository(context).logout()
                        nav.navigate("login_choice") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            ) {
                Text("Logout")
            }
        }
    }
}
