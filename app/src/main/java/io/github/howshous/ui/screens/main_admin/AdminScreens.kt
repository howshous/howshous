package io.github.howshous.ui.screens.main_admin

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import io.github.howshous.data.firestore.BanAppealRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.data.models.BanAppeal
import io.github.howshous.data.models.Listing
import io.github.howshous.R
import io.github.howshous.data.models.UserProfile
import io.github.howshous.ui.components.ListingCard
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.AlertOrange
import io.github.howshous.ui.theme.LandlordBlueAlt
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.TenantGreenAlt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHome(nav: NavController) {
    val listingRepo = remember { ListingRepository() }
    val userRepo = remember { UserRepository() }
    val appealRepo = remember { BanAppealRepository() }
    var isLoading by remember { mutableStateOf(true) }
    var pending by remember { mutableIntStateOf(0) }
    var totalListings by remember { mutableIntStateOf(0) }
    var bannedUsers by remember { mutableIntStateOf(0) }
    var pendingAppeals by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isLoading = true
        val review = listingRepo.getListingsUnderReview()
        val all = listingRepo.getAllListingsForAdmin()
        val users = userRepo.getAllUsers()
        val appeals = appealRepo.getPendingCount()
        pending = review.size
        totalListings = all.size
        bannedUsers = users.count { it.role == "banned" }
        pendingAppeals = appeals
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
            KPI(
                label = "Pending Reviews",
                value = pending.toString(),
                onClick = { nav.navigate("admin_review") }
            )
            KPI(
                label = "Total Listings",
                value = totalListings.toString(),
                onClick = { nav.navigate("admin_listings") }
            )
            KPI("Banned Accounts", bannedUsers.toString())
            KPI(
                label = "Pending Appeals",
                value = pendingAppeals.toString(),
                onClick = { nav.navigate("admin_appeals") }
            )
        }
    }
}

@Composable
private fun KPI(label: String, value: String, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }

    Card(modifier = modifier) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReviewQueue(nav: NavController) {
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var stateFilter by remember { mutableStateOf("under_review") }
    var showFilters by remember { mutableStateOf(true) }
    val stateOptions = remember {
        listOf(
            "all",
            "under_review",
            "rejected",
            "delisted",
            "active",
            "inactive"
        )
    }

    DisposableEffect(Unit) {
        val reg: ListenerRegistration = FirebaseFirestore.getInstance()
            .collection("listings")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    listings = snap.documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.copy(id = doc.id)
                    }
                    isLoading = false
                }
            }
        onDispose { reg.remove() }
    }

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
        } else {
            val normalizedQuery = searchQuery.trim().lowercase()
            val filtered = if (normalizedQuery.isBlank()) {
                listings
            } else {
                listings.filter { listing ->
                    val title = listing.title.lowercase()
                    val location = listing.location.lowercase()
                    val status = listing.status.lowercase()
                    title.contains(normalizedQuery) ||
                        location.contains(normalizedQuery) ||
                        status.contains(normalizedQuery) ||
                        listing.landlordId.lowercase().contains(normalizedQuery)
                }
            }
            val stateFiltered = filtered.filter { matchesListingState(it, stateFilter) }

            Text("Search & Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (showFilters) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search title, location, status") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val current = stateOptions.indexOf(stateFilter).takeIf { it >= 0 } ?: 0
                            stateFilter = stateOptions[(current + 1) % stateOptions.size]
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D45))
                    ) {
                        Text("State: ${listingStateLabel(stateFilter)}", color = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            CollapseChevron(expanded = showFilters, onToggle = { showFilters = !showFilters })

            if (stateFiltered.isEmpty()) {
                Text("No listings match the current filters.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(stateFiltered) { listing ->
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListings(nav: NavController) {
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var stateFilter by remember { mutableStateOf("all") }
    var showFilters by remember { mutableStateOf(true) }
    val stateOptions = remember {
        listOf(
            "all",
            "under_review",
            "rejected",
            "delisted",
            "active",
            "inactive"
        )
    }

    DisposableEffect(Unit) {
        val reg: ListenerRegistration = FirebaseFirestore.getInstance()
            .collection("listings")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    listings = snap.documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.copy(id = doc.id)
                    }
                    isLoading = false
                }
            }
        onDispose { reg.remove() }
    }

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
            Text("Search & Filters", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (showFilters) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search title, location, status") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val current = stateOptions.indexOf(stateFilter).takeIf { it >= 0 } ?: 0
                            stateFilter = stateOptions[(current + 1) % stateOptions.size]
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D45))
                    ) {
                        Text("State: ${listingStateLabel(stateFilter)}", color = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            CollapseChevron(expanded = showFilters, onToggle = { showFilters = !showFilters })

            val normalizedQuery = searchQuery.trim().lowercase()
            val filtered = if (normalizedQuery.isBlank()) {
                listings
            } else {
                listings.filter { listing ->
                    val title = listing.title.lowercase()
                    val location = listing.location.lowercase()
                    val status = listing.status.lowercase()
                    title.contains(normalizedQuery) ||
                        location.contains(normalizedQuery) ||
                        status.contains(normalizedQuery) ||
                        listing.landlordId.lowercase().contains(normalizedQuery)
                }
            }

            val stateFiltered = filtered.filter { matchesListingState(it, stateFilter) }
            val delisted = stateFiltered.filter {
                it.status == "delisted"
            }
            val activeListings = stateFiltered.filterNot {
                it.status == "delisted"
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeListings.isEmpty()) {
                    item { Text("No listings match the current filters.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(activeListings) { listing ->
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

                if (delisted.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Delisted Listings", style = MaterialTheme.typography.titleMedium)
                    }
                    items(delisted) { listing ->
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
}

@ExperimentalMaterialApi
@Composable
fun AdminUsers(nav: NavController) {
    val context = LocalContext.current
    val adminUid by readUidFlow(context).collectAsState(initial = "")
    val userRepo = remember { UserRepository() }
    val scope = rememberCoroutineScope()
    var allUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("all") }
    var statusFilter by remember { mutableStateOf("all") }
    var showFilters by remember { mutableStateOf(true) }
    var showBanDialog by remember { mutableStateOf(false) }
    var banReason by remember { mutableStateOf("") }
    var banReasonError by remember { mutableStateOf("") }
    var pendingUser by remember { mutableStateOf<UserProfile?>(null) }
    val roleOptions = remember { listOf("all", "landlord", "tenant") }
    val statusOptions = remember { listOf("all", "active", "banned") }

    fun refreshUsers() {
        isRefreshing = true
        FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener { snap ->
                allUsers = snap.documents.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                }.filter { it.role == "tenant" || it.role == "landlord" || it.role == "banned" }
                    .sortedBy { it.role }
                isLoading = false
                isRefreshing = false
            }
            .addOnFailureListener {
                isRefreshing = false
            }
    }

    DisposableEffect(Unit) {
        val reg: ListenerRegistration = FirebaseFirestore.getInstance()
            .collection("users")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    allUsers = snap.documents.mapNotNull { doc ->
                        doc.toObject(UserProfile::class.java)?.copy(uid = doc.id)
                    }.filter { it.role == "tenant" || it.role == "landlord" || it.role == "banned" }
                        .sortedBy { it.role }
                    isLoading = false
                }
            }
        onDispose { reg.remove() }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { refreshUsers() }
    )

    val normalizedQuery = searchQuery.trim().lowercase()
    val searchedUsers = if (normalizedQuery.isBlank()) {
        allUsers
    } else {
        allUsers.filter { user ->
            val fullName = "${user.firstName} ${user.lastName}".trim().lowercase()
            fullName.contains(normalizedQuery) ||
                user.email.lowercase().contains(normalizedQuery) ||
                user.role.lowercase().contains(normalizedQuery)
        }
    }
    val roleFiltered = when (roleFilter) {
        "tenant" -> searchedUsers.filter { it.originalRole == "tenant" || (it.originalRole.isBlank() && it.role == "tenant") }
        "landlord" -> searchedUsers.filter { it.originalRole == "landlord" || (it.originalRole.isBlank() && it.role == "landlord") }
        else -> searchedUsers
    }
    val statusFiltered = when (statusFilter) {
        "banned" -> roleFiltered.filter { it.role == "banned" }
        "active" -> roleFiltered.filter { it.role != "banned" }
        else -> roleFiltered
    }
    val landlords = statusFiltered.filter { it.originalRole == "landlord" || (it.originalRole.isBlank() && it.role == "landlord") }
    val tenants = statusFiltered.filter { it.originalRole == "tenant" || (it.originalRole.isBlank() && it.role == "tenant") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
            .padding(16.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Account Moderation", style = MaterialTheme.typography.headlineSmall)
                Button(
                    onClick = { nav.navigate("admin_appeals") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D45))
                ) {
                    Text("Appeals", color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Text("Search & Filters", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (showFilters) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search name, email, or role") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val current = roleOptions.indexOf(roleFilter).takeIf { it >= 0 } ?: 0
                                roleFilter = roleOptions[(current + 1) % roleOptions.size]
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B8D45)
                            )
                        ) { Text("Role: ${roleFilter.replaceFirstChar { it.uppercase() }}", color = Color.White) }
                        Button(
                            onClick = {
                                val current = statusOptions.indexOf(statusFilter).takeIf { it >= 0 } ?: 0
                                statusFilter = statusOptions[(current + 1) % statusOptions.size]
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B8D45)
                            )
                        ) { Text("Status: ${statusFilter.replaceFirstChar { it.uppercase() }}", color = Color.White) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                CollapseChevron(expanded = showFilters, onToggle = { showFilters = !showFilters })
                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Landlords", style = MaterialTheme.typography.titleMedium)
                    }
                    if (landlords.isEmpty()) {
                        item { Text("No landlords match the current filters.", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(landlords) { user ->
                            val isBanned = user.role == "banned"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBanned) Color(0xFFFFEBEE) else Color.White
                                ),
                                border = if (isBanned) {
                                    BorderStroke(1.dp, Color(0xFFB00020))
                                } else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (user.profileImageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = user.profileImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE0E0E0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.i_user),
                                                    contentDescription = null,
                                                    tint = Color(0xFF9E9E9E)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${user.firstName} ${user.lastName}".trim(), style = MaterialTheme.typography.titleSmall)
                                            Text(user.email, style = MaterialTheme.typography.bodySmall)
                                        }
                                        val displayStatus = if (isBanned) "BANNED" else user.role
                                        val statusColor = when (user.role) {
                                            "banned" -> Color(0xFFB00020)
                                            "landlord" -> LandlordBlueAlt
                                            "tenant" -> TenantGreenAlt
                                            else -> Color.Gray
                                        }
                                        Text(
                                            displayStatus.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (isBanned) {
                                                scope.launch {
                                                    userRepo.setUserBanStatus(
                                                        uid = user.uid,
                                                        banned = false,
                                                        adminUid = adminUid,
                                                        reason = ""
                                                    )
                                                }
                                            } else {
                                                pendingUser = user
                                                banReason = ""
                                                banReasonError = ""
                                                showBanDialog = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isBanned) AlertOrange else Color(0xFFB00020)
                                        )
                                    ) {
                                        Text(if (isBanned) "Unban Account" else "Ban Account", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Tenants", style = MaterialTheme.typography.titleMedium)
                    }
                    if (tenants.isEmpty()) {
                        item { Text("No tenants match the current filters.", style = MaterialTheme.typography.bodyMedium) }
                    } else {
                        items(tenants) { user ->
                            val isBanned = user.role == "banned"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBanned) Color(0xFFFFEBEE) else Color.White
                                ),
                                border = if (isBanned) {
                                    BorderStroke(1.dp, Color(0xFFB00020))
                                } else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (user.profileImageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = user.profileImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE0E0E0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.i_user),
                                                    contentDescription = null,
                                                    tint = Color(0xFF9E9E9E)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${user.firstName} ${user.lastName}".trim(), style = MaterialTheme.typography.titleSmall)
                                            Text(user.email, style = MaterialTheme.typography.bodySmall)
                                        }
                                        val displayStatus = if (isBanned) "BANNED" else user.role
                                        val statusColor = when (user.role) {
                                            "banned" -> Color(0xFFB00020)
                                            "landlord" -> LandlordBlueAlt
                                            "tenant" -> TenantGreenAlt
                                            else -> Color.Gray
                                        }
                                        Text(
                                            displayStatus.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            if (isBanned) {
                                                scope.launch {
                                                    userRepo.setUserBanStatus(
                                                        uid = user.uid,
                                                        banned = false,
                                                        adminUid = adminUid,
                                                        reason = ""
                                                    )
                                                }
                                            } else {
                                                pendingUser = user
                                                banReason = ""
                                                banReasonError = ""
                                                showBanDialog = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isBanned) AlertOrange else Color(0xFFB00020)
                                        )
                                    ) {
                                        Text(if (isBanned) "Unban Account" else "Ban Account", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

    if (showBanDialog && pendingUser != null) {
        AlertDialog(
            onDismissRequest = {
                showBanDialog = false
                banReasonError = ""
            },
            title = { Text("Ban Account") },
            text = {
                Column {
                    Text(
                        "Provide a clear reason for banning this account.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = {
                            banReason = it
                            banReasonError = ""
                        },
                        label = { Text("Ban reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (banReasonError.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(banReasonError, color = Color(0xFFB00020), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (banReason.trim().isBlank()) {
                            banReasonError = "Ban reason is required."
                            return@TextButton
                        }
                        val target = pendingUser ?: return@TextButton
                        scope.launch {
                            userRepo.setUserBanStatus(
                                uid = target.uid,
                                banned = true,
                                adminUid = adminUid,
                                reason = banReason.trim()
                            )
                            showBanDialog = false
                        }
                    }
                ) { Text("Confirm Ban", color = Color(0xFFB00020)) }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppeals(nav: NavController) {
    val context = LocalContext.current
    val adminUid by readUidFlow(context).collectAsState(initial = "")
    val appealRepo = remember { BanAppealRepository() }
    val userRepo = remember { UserRepository() }
    val scope = rememberCoroutineScope()

    var appeals by remember { mutableStateOf<List<BanAppeal>>(emptyList()) }
    var userMap by remember { mutableStateOf<Map<String, UserProfile>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        scope.launch {
            isLoading = true
            val loadedAppeals = appealRepo.getAllAppeals()
            val users = userRepo.getAllUsers()
            appeals = loadedAppeals
            userMap = users.associateBy { it.uid }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text("Ban Appeals", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(12.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (appeals.isEmpty()) {
            Text("No appeals submitted.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(appeals) { appeal ->
                    val user = userMap[appeal.userId]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim(),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(user?.email ?: "Unknown email", style = MaterialTheme.typography.bodySmall)
                            Text("Role: ${user?.role ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                            if (user != null && !user.banReason.isNullOrBlank()) {
                                Text("Ban reason: ${user.banReason}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Appeal: ${appeal.message}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(6.dp))
                            Text("Status: ${appeal.status}", style = MaterialTheme.typography.bodySmall)

                            if (appeal.status == "pending") {
                                var notes by remember(appeal.id) { mutableStateOf("") }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("Review notes (optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                appealRepo.reviewAppeal(
                                                    appealId = appeal.id,
                                                    adminUid = adminUid,
                                                    approved = true,
                                                    notes = notes
                                                )
                                                refresh()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D45))
                                    ) {
                                        Text("Approve")
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                appealRepo.reviewAppeal(
                                                    appealId = appeal.id,
                                                    adminUid = adminUid,
                                                    approved = false,
                                                    notes = notes
                                                )
                                                refresh()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            } else if (appeal.reviewNotes.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Admin notes: ${appeal.reviewNotes}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

private fun listingStateLabel(state: String): String {
    return when (state) {
        "all" -> "All"
        "under_review" -> "Under Review"
        "rejected" -> "Rejected"
        "delisted" -> "Delisted"
        "active" -> "Active"
        "inactive" -> "Inactive"
        else -> state.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

private fun matchesListingState(listing: Listing, filter: String): Boolean {
    if (filter == "all") return true
    return listing.status == filter
}

@Composable
private fun CollapseChevron(expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "collapseChevron"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier
                .size(36.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
