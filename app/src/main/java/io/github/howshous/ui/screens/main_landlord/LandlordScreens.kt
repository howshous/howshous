package io.github.howshous.ui.screens.main_landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.DueYellow
import io.github.howshous.ui.theme.OverdueRed
import io.github.howshous.ui.theme.VacancyBlue
import io.github.howshous.ui.viewmodels.HomeViewModel
import io.github.howshous.ui.viewmodels.LandlordListingsViewModel
import io.github.howshous.ui.viewmodels.ChatViewModel
import io.github.howshous.ui.viewmodels.NotificationViewModel
import io.github.howshous.ui.viewmodels.AccountViewModel

@Composable
fun LandlordHome(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: HomeViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val vacantCount by viewModel.vacantCount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadLandlordHome(uid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // KPI Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KPICard("$activeCount", "Active", Color.Green, Modifier.weight(1f))
            KPICard("$vacantCount", "Vacant", VacancyBlue, Modifier.weight(1f))
            KPICard("$overdueCount", "Overdue", OverdueRed, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun KPICard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(4.dp)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun LandlordListings(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: LandlordListingsViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val listings by viewModel.listings.collectAsState()
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val listingCreatedFlow = navBackStackEntry?.savedStateHandle?.getStateFlow("listingCreated", false)
    val listingCreated by listingCreatedFlow?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadListingsForLandlord(uid)
    }

    LaunchedEffect(listingCreated) {
        if (listingCreated && uid.isNotEmpty()) {
            viewModel.loadListingsForLandlord(uid)
            navBackStackEntry?.savedStateHandle?.set("listingCreated", false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("My Listings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { nav.navigate("create_listing") },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Add Listing")
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (listings.isEmpty()) {
            Text("No listings yet", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(listings) { listing ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (listing.photos.isNotEmpty()) {
                                AsyncImage(
                                    model = listing.photos[0],
                                    contentDescription = listing.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(listing.title, style = MaterialTheme.typography.titleSmall)
                            Text(listing.location, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("₹${listing.price}/month", style = MaterialTheme.typography.labelLarge, color = Color.Green)
                                Text(listing.status, style = MaterialTheme.typography.labelSmall, color = if (listing.status == "active") Color.Green else Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun LandlordChatList(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ChatViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val chats by viewModel.chats.collectAsState()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadChatsForUser(uid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Messages", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (chats.isEmpty()) {
            Text("No conversations yet", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(chats) { chat ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text("Listing #${chat.listingId}", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(chat.lastMessage.take(50), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordNotifications(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: NotificationViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadNotificationsForUser(uid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Notifications", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (notifications.isEmpty()) {
            Text("No notifications", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.read) SurfaceLight else Color(0xFFFFF9C4)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(notif.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LandlordAccount(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: AccountViewModel = viewModel()
    val profile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadUserProfile(uid)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Account", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (profile != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (profile!!.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = profile!!.profileImageUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("${profile!!.firstName} ${profile!!.lastName}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(profile!!.email, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Verified: ${if (profile!!.verified) "✓" else "✗"}", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Text("Profile not found", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
