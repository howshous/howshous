package io.github.howshous.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.data.firestore.AnalyticsRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.models.Listing
import io.github.howshous.ui.data.clearSession
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

    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isSeeding by remember { mutableStateOf(false) }

    val listingRepo = remember { ListingRepository() }
    val analyticsRepo = remember { AnalyticsRepository() }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            listings = listingRepo.getListingsForLandlord(uid)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Landlord Dashboard", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            Text("UID: $uid", style = MaterialTheme.typography.bodySmall)
            Text("Role: $role", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    if (uid.isBlank()) return@OutlinedButton
                    if (listings.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Create at least one listing first, then tap again.",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@OutlinedButton
                    }
                    isSeeding = true
                    scope.launch {
                        try {
                            analyticsRepo.seedTestEventsForLandlord(
                                landlordId = uid,
                                listings = listings.map { it.id to it.price },
                            )
                            snackbarHostState.showSnackbar(
                                "Test data added for ${listings.size} listing(s). Aggregates may take a few seconds.",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                "Failed: ${e.message}",
                                duration = SnackbarDuration.Short
                            )
                        } finally {
                            isSeeding = false
                        }
                    }
                },
                enabled = !isSeeding
            ) {
                if (isSeeding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSeeding) "Populating…" else "Populate test data")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        clearSession(context)
                        nav.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            ) {
                Text("Logout")
            }
        }
    }
}
