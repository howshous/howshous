package io.github.howshous.ui.screens.main_tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.HomeViewModel
import io.github.howshous.ui.viewmodels.TenantSearchViewModel
import io.github.howshous.ui.viewmodels.ChatViewModel
import io.github.howshous.ui.viewmodels.NotificationViewModel
import io.github.howshous.ui.viewmodels.AccountViewModel
import kotlinx.coroutines.launch
import io.github.howshous.ui.components.SearchBar
import io.github.howshous.ui.components.NoListingsEmptyState

@Composable
fun TenantHome(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: HomeViewModel = viewModel()
    val contractsViewModel: io.github.howshous.ui.viewmodels.ContractsViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val listings by viewModel.tenantRecentListings.collectAsState()
    var hasContract by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val contractRepository = remember { io.github.howshous.data.firestore.ContractRepository() }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            viewModel.loadTenantHome(uid)
            scope.launch {
                hasContract = contractRepository.hasSignedContract(uid)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Three buttons at the top (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // View Contracts Button
            Button(
                onClick = { nav.navigate("view_contracts") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("View Contracts", style = MaterialTheme.typography.labelSmall)
            }

            // Emergency Button
            Button(
                onClick = { nav.navigate("emergency") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252)
                )
            ) {
                Text("Emergency", style = MaterialTheme.typography.labelSmall)
            }

            // Report Issue Button (only if has contract)
            if (hasContract) {
                Button(
                    onClick = { nav.navigate("report_issue") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("Report Issue", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                // Spacer to maintain layout when button is hidden
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Rest of the home content (scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyColumn {
                    item {
                        Text("Recent Listings", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(listings) { listing ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { nav.navigate("listing/${listing.id}") }
                        ) {
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
                                Text("₱${listing.price}/month", style = MaterialTheme.typography.labelLarge, color = Color.Green)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantSearch(nav: NavController) {
    val viewModel: TenantSearchViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val listings by viewModel.filteredListings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
        Text("Search Listings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { nav.navigate("tenant_ai_helper") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Ask AI for Boarding House Help")
        }

        Spacer(Modifier.height(12.dp))

        SearchBar(
            query = query,
            onQueryChange = { viewModel.searchByLocation(it) },
            placeholder = "Search by location..."
        )
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (listings.isEmpty()) {
            NoListingsEmptyState()
        } else {
            LazyColumn {
                items(listings) { listing ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { nav.navigate("listing/${listing.id}") }
                    ) {
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
                            Text("₱${listing.price}/month", style = MaterialTheme.typography.labelLarge, color = Color.Green)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TenantChatList(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ChatViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val chats by viewModel.chats.collectAsState()
    var listingsMap by remember { mutableStateOf<Map<String, io.github.howshous.data.models.Listing>>(emptyMap()) }
    val listingRepository = remember { io.github.howshous.data.firestore.ListingRepository() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadChatsForUser(uid)
    }

    LaunchedEffect(chats) {
        if (chats.isNotEmpty()) {
            scope.launch {
                val map = mutableMapOf<String, io.github.howshous.data.models.Listing>()
                chats.forEach { chat ->
                    if (chat.listingId.isNotEmpty() && !map.containsKey(chat.listingId)) {
                        val listing = listingRepository.getListing(chat.listingId)
                        listing?.let { map[chat.listingId] = it }
                    }
                }
                listingsMap = map
            }
        }
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
                    val listing = listingsMap[chat.listingId]
                    val listingTitle = listing?.title ?: "Listing #${chat.listingId}"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { nav.navigate("chat/${chat.id}") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(listingTitle, style = MaterialTheme.typography.titleSmall)
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
fun TenantNotifications(nav: NavController) {
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
                            containerColor = if (notif.read) SurfaceLight else Color(0xFFE8F5E9)
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
fun TenantAccount(nav: NavController) {
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
