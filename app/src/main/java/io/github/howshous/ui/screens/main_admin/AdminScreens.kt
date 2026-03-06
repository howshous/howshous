package io.github.howshous.ui.screens.main_admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.data.models.Listing
import io.github.howshous.data.models.UserProfile
import io.github.howshous.ui.components.ListingCard
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import kotlinx.coroutines.launch

@Composable
fun AdminHome() {
    val listingRepo = remember { ListingRepository() }
    val userRepo = remember { UserRepository() }
    var isLoading by remember { mutableStateOf(true) }
    var pending by remember { mutableStateOf(0) }
    var totalListings by remember { mutableStateOf(0) }
    var bannedUsers by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        isLoading = true
        val review = listingRepo.getListingsUnderReview()
        val all = listingRepo.getAllListingsForAdmin()
        val users = userRepo.getAllUsers()
        pending = review.size
        totalListings = all.size
        bannedUsers = users.count { it.isBanned }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Admin Dashboard", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            KPI("Pending Reviews", pending.toString())
            KPI("Total Listings", totalListings.toString())
            KPI("Banned Accounts", bannedUsers.toString())
        }
    }
}

@Composable
private fun KPI(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AdminReviewQueue(nav: NavController) {
    val listingRepo = remember { ListingRepository() }
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        scope.launch {
            isLoading = true
            listings = listingRepo.getListingsUnderReview()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Listings Under Review", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (listings.isEmpty()) {
            Text("No listings awaiting review.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listings) { listing ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            ListingCard(
                                listing = listing,
                                onClick = { nav.navigate("listing/${listing.id}") },
                                showStatus = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminListings(nav: NavController) {
    val listingRepo = remember { ListingRepository() }
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        scope.launch {
            isLoading = true
            listings = listingRepo.getAllListingsForAdmin()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("All Listings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listings) { listing ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            ListingCard(
                                listing = listing,
                                onClick = { nav.navigate("listing/${listing.id}") },
                                showStatus = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsers() {
    val context = LocalContext.current
    val adminUid by readUidFlow(context).collectAsState(initial = "")
    val userRepo = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        scope.launch {
            isLoading = true
            users = userRepo.getAllUsers()
                .filter { it.role == "tenant" || it.role == "landlord" || it.role == "administrator" }
                .sortedBy { it.role }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Account Moderation", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users) { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${user.firstName} ${user.lastName}".trim(), style = MaterialTheme.typography.titleSmall)
                            Text(user.email, style = MaterialTheme.typography.bodySmall)
                            Text("Role: ${user.role}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            val isBanned = user.isBanned
                            Button(
                                onClick = {
                                    scope.launch {
                                        userRepo.setUserBanStatus(
                                            uid = user.uid,
                                            banned = !isBanned,
                                            adminUid = adminUid,
                                            reason = if (!isBanned) "Violated platform policy" else ""
                                        )
                                        refresh()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBanned) Color(0xFF1B8D45) else Color(0xFFB00020)
                                )
                            ) {
                                Text(if (isBanned) "Unban Account" else "Ban Account", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAccount() {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val userRepo = remember { UserRepository() }
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            profile = userRepo.getUserProfile(uid)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Admin Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("${profile?.firstName ?: ""} ${profile?.lastName ?: ""}".trim())
                Text(profile?.email ?: "")
                Text("Role: ${profile?.role ?: "administrator"}")
            }
        }
    }
}
