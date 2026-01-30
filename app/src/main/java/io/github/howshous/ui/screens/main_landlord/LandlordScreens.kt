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
import kotlinx.coroutines.delay
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
    val listingRepository = remember { io.github.howshous.data.firestore.ListingRepository() }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) viewModel.loadListingsForLandlord(uid)
    }

    LaunchedEffect(listingCreated) {
        if (listingCreated && uid.isNotEmpty()) {
            viewModel.loadListingsForLandlord(uid)
            navBackStackEntry?.savedStateHandle?.set("listingCreated", false)
        }
    }

    var metricsMap by remember { mutableStateOf<Map<String, io.github.howshous.data.firestore.ListingMetrics>>(emptyMap()) }
    val metricsRepo = remember { io.github.howshous.data.firestore.ListingMetricsRepository() }
    var metricsLoading by remember { mutableStateOf(false) }
    var metricsRefreshTrigger by remember { mutableStateOf(0) }
    var isSeeding by remember { mutableStateOf(false) }
    val analyticsRepo = remember { io.github.howshous.data.firestore.AnalyticsRepository() }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Listings, 1 = Performance

    LaunchedEffect(listings, metricsRefreshTrigger) {
        if (listings.isNotEmpty()) {
            metricsLoading = true
            metricsMap = metricsRepo.getMetricsForListings(listings.map { it.id })
            metricsLoading = false
        } else {
            metricsMap = emptyMap()
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
                .padding(horizontal = 16.dp)
        ) {
            Text("My Listings", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Listings") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Performance") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { nav.navigate("create_listing") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Add Listing") }
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        if (uid.isBlank()) {
                                            snackbarHostState.showSnackbar("Missing user id.")
                                            return@launch
                                        }
                                        val createdIds = SampleListingsGenerator.generateSampleListings(uid)
                                        if (createdIds.isNotEmpty()) {
                                            snackbarHostState.showSnackbar("Generated ${createdIds.size} sample listings!")
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) { Text("Sample Data") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isLoading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (listings.isEmpty()) {
                        Text("No listings yet. Add a listing or use Sample Data.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listings) { listing ->
                                ListingCard(
                                    listing = listing,
                                    modifier = Modifier.fillMaxWidth(),
                                    showStatus = true,
                                    showViews = true
                                )
                            }
                        }
                    }
                }
                1 -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("How to use analytics", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "• Views: How many times tenants opened your listing\n• Saves: How many tenants favorited it\n• Messages: How many tenants contacted you\n• Conversion rates show how well your listing converts interest into action",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (uid.isBlank()) return@OutlinedButton
                            if (listings.isEmpty()) {
                                scope.launch { snackbarHostState.showSnackbar("Create at least one listing first.") }
                                return@OutlinedButton
                            }
                            isSeeding = true
                            scope.launch {
                                try {
                                    analyticsRepo.seedTestEventsForLandlord(
                                        landlordId = uid,
                                        listings = listings.map { it.id to it.price },
                                    )
                                    snackbarHostState.showSnackbar("Test data added for ${listings.size} listing(s). Wait a few seconds, then tap Refresh.")
                                    delay(3500)
                                    metricsRefreshTrigger++
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed: ${e.message}")
                                } finally {
                                    isSeeding = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSeeding
                    ) {
                        if (isSeeding) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isSeeding) "Populating…" else "Populate test data")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    if (uid.isBlank()) {
                                        snackbarHostState.showSnackbar("Missing user id.")
                                        return@launch
                                    }
                                    val updated = listingRepository.backfillUniqueViewCountsForLandlord(uid)
                                    snackbarHostState.showSnackbar("Recounted views for $updated listings.")
                                    viewModel.loadListingsForLandlord(uid)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Recount") }
                        OutlinedButton(
                            onClick = {
                                metricsRefreshTrigger++
                                scope.launch {
                                    snackbarHostState.showSnackbar("Metrics refreshed.")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Refresh") }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (listings.isEmpty()) {
                        Text("Add listings first, then use \"Populate test data\".", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else if (metricsLoading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Summary card showing overall performance
                        val totalViews = metricsMap.values.sumOf { it.views30d }
                        val totalSaves = metricsMap.values.sumOf { it.saves30d }
                        val totalMessages = metricsMap.values.sumOf { it.messages30d }
                        val avgSaveRate = if (totalViews > 0) (totalSaves.toFloat() / totalViews * 100) else 0f
                        val avgMessageRate = if (totalViews > 0) (totalMessages.toFloat() / totalViews * 100) else 0f
                        
                        if (metricsMap.isNotEmpty() && totalViews > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Overall Performance (30d)", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("$totalViews", style = MaterialTheme.typography.titleMedium, color = PricePointGreen)
                                            Text("Total Views", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Column {
                                            Text("$totalSaves", style = MaterialTheme.typography.titleMedium)
                                            Text("Total Saves", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Column {
                                            Text("$totalMessages", style = MaterialTheme.typography.titleMedium)
                                            Text("Total Messages", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Avg Save Rate: ${String.format("%.1f", avgSaveRate)}%", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                        Text("Avg Message Rate: ${String.format("%.1f", avgMessageRate)}%", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(listings) { listing ->
                                val m = metricsMap[listing.id]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(listing.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                            if (m != null && m.views30d > 0) {
                                                val saveRate = (m.saves30d.toFloat() / m.views30d * 100).toInt()
                                                val messageRate = (m.messages30d.toFloat() / m.views30d * 100).toInt()
                                                Text(
                                                    "$${listing.price}/mo",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        if (m != null) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("${m.views7d} / ${m.views30d}", style = MaterialTheme.typography.labelMedium, color = PricePointGreen)
                                                    Text("Views", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("${m.saves7d} / ${m.saves30d}", style = MaterialTheme.typography.labelMedium)
                                                    Text("Saves", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("${m.messages7d} / ${m.messages30d}", style = MaterialTheme.typography.labelMedium)
                                                    Text("Messages", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                }
                                            }
                                            if (m.views30d > 0) {
                                                Spacer(Modifier.height(8.dp))
                                                Divider()
                                                Spacer(Modifier.height(8.dp))
                                                val saveRate = (m.saves30d.toFloat() / m.views30d * 100)
                                                val messageRate = (m.messages30d.toFloat() / m.views30d * 100)
                                                val messageFromSaveRate = if (m.saves30d > 0) (m.messages30d.toFloat() / m.saves30d * 100) else 0f
                                                
                                                Text("Conversion Rates (30d)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                Spacer(Modifier.height(4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${String.format("%.1f", saveRate)}%", style = MaterialTheme.typography.bodyMedium, color = if (saveRate >= 10) PricePointGreen else if (saveRate >= 5) DueYellow else OverdueRed)
                                                        Text("Views → Saves", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${String.format("%.1f", messageRate)}%", style = MaterialTheme.typography.bodyMedium, color = if (messageRate >= 5) PricePointGreen else if (messageRate >= 2) DueYellow else OverdueRed)
                                                        Text("Views → Messages", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("${String.format("%.1f", messageFromSaveRate)}%", style = MaterialTheme.typography.bodyMedium, color = if (messageFromSaveRate >= 50) PricePointGreen else if (messageFromSaveRate >= 25) DueYellow else OverdueRed)
                                                        Text("Saves → Messages", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    }
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                
                                                // Actionable insights
                                                val insights = mutableListOf<String>()
                                                if (m.views30d < 10) {
                                                    insights.add("Low visibility - consider improving photos or description")
                                                } else if (saveRate < 5) {
                                                    insights.add("Low save rate - photos or price may need adjustment")
                                                } else if (messageRate < 2) {
                                                    insights.add("Low message rate - description may need more details")
                                                } else if (messageFromSaveRate < 25 && m.saves30d > 5) {
                                                    insights.add("Many saves but few messages - consider highlighting urgency")
                                                } else if (saveRate >= 10 && messageRate >= 5) {
                                                    insights.add("Strong performance - listing is converting well!")
                                                }
                                                
                                                if (insights.isNotEmpty()) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                                    ) {
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text("💡 Insights", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                            Spacer(Modifier.height(4.dp))
                                                            insights.forEach { insight ->
                                                                Text("• $insight", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.height(4.dp))
                                            Text("No data yet. Populate test data, wait, then tap Refresh.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }
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
