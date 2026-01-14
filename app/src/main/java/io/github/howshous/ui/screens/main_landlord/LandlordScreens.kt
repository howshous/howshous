package io.github.howshous.ui.screens.main_landlord

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
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.DueYellow
import io.github.howshous.ui.theme.OverdueRed
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.VacancyBlue
import io.github.howshous.ui.viewmodels.HomeViewModel
import io.github.howshous.ui.viewmodels.LandlordListingsViewModel
import io.github.howshous.ui.viewmodels.ChatViewModel
import io.github.howshous.ui.viewmodels.NotificationViewModel
import io.github.howshous.ui.viewmodels.AccountViewModel
import io.github.howshous.ui.util.SampleListingsGenerator
import io.github.howshous.ui.components.ListingCard
import kotlinx.coroutines.launch

@Composable
fun LandlordHome(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: HomeViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val vacantCount by viewModel.vacantCount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    val issues by viewModel.landlordIssues.collectAsState()

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
            KPICard("$activeCount", "Active", PricePointGreen, Modifier.weight(1f))
            KPICard("$vacantCount", "Vacant", VacancyBlue, Modifier.weight(1f))
            KPICard("$overdueCount", "Overdue", OverdueRed, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        
        if (issues.isEmpty()) {
            Text("No recent issues", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(issues) { issue ->
                    IssueCard(issue = issue, onClick = {
                        nav.navigate("issue_detail/${issue.id}")
                    })
                }
            }
        }
    }
}

@Composable
fun IssueCard(issue: io.github.howshous.data.models.Issue, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (issue.status == "pending") Color(0xFFFFF9C4) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🚨 ${issue.issueType}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (issue.status == "pending") OverdueRed else PricePointGreen
                )
                Text(
                    issue.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (issue.status == "pending") OverdueRed else PricePointGreen
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                issue.description.take(100) + if (issue.description.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Reported: ${issue.reportedAt?.toDate()?.toString()?.take(16) ?: "Unknown"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadListingsForLandlord(uid)
    }

    LaunchedEffect(listingCreated) {
        if (listingCreated && uid.isNotEmpty()) {
            viewModel.loadListingsForLandlord(uid)
            navBackStackEntry?.savedStateHandle?.set("listingCreated", false)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight)
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("My Listings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { nav.navigate("create_listing") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Add Listing")
            }
            
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val createdIds = SampleListingsGenerator.generateSampleListings()
                            if (createdIds.isNotEmpty()) {
                                snackbarHostState.showSnackbar("Generated ${createdIds.size} sample listings!")
                                // Refresh listings
                                if (uid.isNotEmpty()) viewModel.loadListingsForLandlord(uid)
                            } else {
                                snackbarHostState.showSnackbar("Failed to generate sample listings")
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Sample Data")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (listings.isEmpty()) {
            Text("No listings yet", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(listings) { listing ->
                    ListingCard(
                        listing = listing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        showStatus = true
                    )
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
