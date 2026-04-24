package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.howshous.R
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.ListingReviewRepository
import io.github.howshous.data.firestore.NotificationRepository
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.data.models.UserProfile
import androidx.compose.ui.geometry.Offset
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readRoleFlow
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.data.ensureSessionId
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.ReviewGreen
import io.github.howshous.ui.theme.ReviewRed
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.ListingViewModel
import io.github.howshous.ui.components.ReviewSummaryRow
import io.github.howshous.ui.components.ReviewSummaryButton
import io.github.howshous.ui.components.ReviewSubmissionSheet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(nav: NavController, listingId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val role by readRoleFlow(context).collectAsState(initial = "")
    val viewModel: ListingViewModel = viewModel()
    val listingRepository = remember { ListingRepository() }
    val reviewRepository = remember { ListingReviewRepository() }
    val notificationRepository = remember { NotificationRepository() }
    val scope = rememberCoroutineScope()
    val listing by viewModel.listing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    var sessionId by remember { mutableStateOf<String?>(null) }
    var landlordProfile by remember { mutableStateOf<UserProfile?>(null) }
    val userRepository = remember { UserRepository() }
    var showLandDeedViewer by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoUrl by remember { mutableStateOf("") }
    var rejectionReasons by remember { mutableStateOf(setOf<String>()) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectDialogError by remember { mutableStateOf("") }
    var showReviewsSheet by remember { mutableStateOf(false) }
    var isReviewsLoading by remember { mutableStateOf(false) }
    var reviews by remember { mutableStateOf(emptyList<io.github.howshous.data.models.ListingReview>()) }
    var showReviewSubmissionSheet by remember { mutableStateOf(false) }
    var tenantReview by remember { mutableStateOf<io.github.howshous.data.models.ListingReview?>(null) }
    var isReviewSubmitting by remember { mutableStateOf(false) }
    val adminRejectReasonOptions = remember {
        listOf(
            "Contract terms are missing or invalid",
            "Contract terms are unfair or non-compliant",
            "Listing details are inaccurate",
            "Ownership proof is insufficient",
            "Photos are unclear or misleading",
            "Other policy violation"
        )
    }

    LaunchedEffect(listingId, uid) {
        viewModel.observeListing(listingId)
        if (uid.isNotBlank()) {
            viewModel.loadSavedState(listingId, uid)
        }
    }

    LaunchedEffect(listing) {
        val landlordId = listing?.landlordId ?: ""
        if (landlordId.isNotBlank()) {
            landlordProfile = userRepository.getUserProfile(landlordId)
        } else {
            landlordProfile = null
        }
    }

    LaunchedEffect(Unit) {
        sessionId = ensureSessionId(context)
    }

    LaunchedEffect(listingId, uid, role, listing, sessionId) {
        val currentListing = listing
        val currentSessionId = sessionId
        if ((role == "tenant" || role == "administrator") && uid.isNotBlank() && currentListing != null && currentSessionId != null) {
            // Only count as a view if the detail screen stays visible for a short time window.
            // This helps avoid counting very quick scroll-past or accidental opens.
            kotlinx.coroutines.delay(2500)
            viewModel.recordUniqueViewWithAnalytics(
                listingId = listingId,
                viewerId = uid,
                sessionId = currentSessionId,
                price = currentListing.price
            )
        }
    }

    LaunchedEffect(showReviewsSheet, listingId) {
        if (!showReviewsSheet) return@LaunchedEffect
        if (listingId.isBlank()) return@LaunchedEffect
        isReviewsLoading = true
        reviews = reviewRepository.getReviewsForListing(listingId)
        
        // Load tenant's review if they are logged in
        if (role == "tenant" && uid.isNotBlank()) {
            tenantReview = reviewRepository.getTenantReviewForListing(listingId, uid)
        }
        isReviewsLoading = false
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DebouncedIconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Listing Details", style = MaterialTheme.typography.titleMedium)
            }
            if ((role == "tenant" || role == "administrator") && listing != null && uid.isNotBlank()) {
                IconButton(
                    onClick = {
                        viewModel.toggleSave(
                            listingId = listingId,
                            userId = uid,
                            sessionId = sessionId
                        )
                    }
                ) {
                    if (isSaved) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Unsave",
                            tint = PricePointGreen
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = "Save"
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else if (listing != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Photos
                item {
                    if (listing!!.photos.isNotEmpty()) {
                        val listState = rememberLazyListState()
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val itemWidth = maxWidth
                            LazyRow(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(listing!!.photos) { _, photoUrl ->
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = listing!!.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .height(200.dp)
                                            .clickable {
                                                selectedPhotoUrl = photoUrl
                                                showPhotoViewer = true
                                            }
                                    )
                                }
                            }
                            if (listing!!.photos.size > 1) {
                                val currentIndex = listState.firstVisibleItemIndex
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(listing!!.photos.size) { index ->
                                        val color = if (index == currentIndex) PricePointGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(color, shape = MaterialTheme.shapes.small)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Title
                item {
                    Text(listing!!.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                }

                // Location & Price
                item {
                    Text(listing!!.location, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("₱${listing!!.price}/month", style = MaterialTheme.typography.titleMedium, color = PricePointGreen)
                    Text("Deposit: ₱${listing!!.deposit}", style = MaterialTheme.typography.bodySmall)
                    val maxOccupancy = listing!!.capacity.coerceAtLeast(1)
                    val currentOccupancy = listing!!.currentOccupancy.coerceAtLeast(0).coerceAtMost(maxOccupancy)
                    val remainingSlots = (maxOccupancy - currentOccupancy).coerceAtLeast(0)
                    Text("Maximum occupancy: $maxOccupancy", style = MaterialTheme.typography.bodySmall)
                    Text("Current occupancy: $currentOccupancy", style = MaterialTheme.typography.bodySmall)
                    Text("Remaining slots: $remainingSlots", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    ReviewSummaryButton(
                        summary = listing!!.reviewSummary,
                        onClick = { showReviewsSheet = true }
                    )
                    Spacer(Modifier.height(12.dp))
                    if (role == "landlord") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.i_eyeopen),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${listing!!.uniqueViewCount} unique views",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Landlord
                item {
                    if (role == "tenant" || role == "administrator") {
                        val landlordName = listOfNotNull(
                            landlordProfile?.firstName?.takeIf { it.isNotBlank() },
                            landlordProfile?.lastName?.takeIf { it.isNotBlank() }
                        ).joinToString(" ").ifBlank { "Landlord" }
                        val landlordId = listing!!.landlordId
                        val profileUrl = landlordProfile?.profileImageUrl?.trim().orEmpty()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (landlordId.isNotBlank()) {
                                        Modifier.clickable { nav.navigate("landlord_profile/$landlordId") }
                                    } else {
                                        Modifier
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (profileUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = profileUrl,
                                        contentDescription = "Landlord profile photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.i_user),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        landlordName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "View landlord profile",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Description
                item {
                    Text("Description", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            listing!!.description,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Amenities
                item {
                    if (listing!!.amenities.isNotEmpty()) {
                        Text("Amenities", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listing!!.amenities.forEach { amenity ->
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        amenity,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Land Deed (tenant view)
                item {
                    if (role == "tenant" || role == "administrator") {
                        Text("Land Deed", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        if (listing!!.landDeedUrl.isNotBlank()) {
                            AsyncImage(
                                model = listing!!.landDeedUrl,
                                contentDescription = "Land deed",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clickable { showLandDeedViewer = true }
                            )
                        } else {
                            Text(
                                "Land deed not provided.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Admin review (contract)
                item {
                    if (role == "administrator") {
                        val template = listing!!.contractTemplate
                        val templateTitle = template?.get("title") as? String ?: "Not provided"
                        val templateTerms = template?.get("terms") as? String ?: "Not provided"
                        val templateRent = (template?.get("monthlyRent") as? Number)?.toInt()
                        val templateDeposit = (template?.get("deposit") as? Number)?.toInt()

                        Text("Contract Review", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Title: $templateTitle", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text("Terms:", style = MaterialTheme.typography.bodySmall)
                        Text(templateTerms, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Template Rent: ${templateRent?.let { "PHP $it" } ?: "Not provided"} | Template Deposit: ${templateDeposit?.let { "PHP $it" } ?: "Not provided"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Contact Button
                item {
                    if (role == "tenant") {
                        val isInteractable = listing!!.status == "active"
                        Button(
                            onClick = {
                                nav.navigate("initiate_chat/${listingId}/${listing!!.landlordId}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = isInteractable
                        ) {
                            Text(if (isInteractable) "Contact Landlord" else "Listing Unavailable")
                        }
                    } else if (role == "administrator") {
                        val status = listing!!.status.ifBlank { "under_review" }
                        when (status) {
                            "active", "inactive" -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            listingRepository.takeDownListing(
                                                listingId = listingId,
                                                adminUid = uid,
                                                notes = "Delisted by admin"
                                            )
                                            notificationRepository.createNotification(
                                                userId = listing!!.landlordId,
                                                type = "listing_delisted",
                                                title = "Listing Delisted",
                                                message = "Your listing \"${listing!!.title}\" was delisted by admin.",
                                                actionUrl = "landlord_listing/${listingId}",
                                                listingId = listingId,
                                                senderId = uid
                                            )
                                            viewModel.loadListing(listingId)
                                            nav.popBackStack()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                                ) {
                                    Text("Delist Listing", color = Color.White)
                                }
                            }
                            "under_review", "rejected" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                listingRepository.approveListing(
                                                    listingId = listingId,
                                                    adminUid = uid,
                                                    notes = "Approved by admin"
                                                )
                                                viewModel.loadListing(listingId)
                                                nav.popBackStack()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8D45))
                                    ) {
                                        Text("Approve", color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            rejectDialogError = ""
                                            showRejectDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                                    ) {
                                        Text("Reject", color = Color.White)
                                    }
                                }
                            }
                            else -> {
                                // No moderation action for delisted/unknown states from this screen.
                            }
                        }
                    }
                }
            }
    } else {
                    Text(
                        "Listing not found",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(32.dp)
                    )
        }
    }

    if (showReviewsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showReviewsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Reviews", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                ReviewSummaryRow(summary = listing?.reviewSummary)
                Spacer(Modifier.height(12.dp))

                // Show review submission form for tenants
                if (role == "tenant" && uid.isNotBlank() && listing != null) {
                    Button(
                        onClick = { showReviewSubmissionSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B8D45),
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (tenantReview != null) "Edit Your Review" else "Leave a Review")
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    if (tenantReview != null) {
                        androidx.compose.material3.Divider()
                        Spacer(Modifier.height(12.dp))
                    }
                }

                when {
                    isReviewsLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(16.dp)
                        )
                    }
                    reviews.isEmpty() -> {
                        Text(
                            "No reviews yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(reviews, key = { it.id }) { review ->
                                ReviewCard(review = review)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showReviewSubmissionSheet && listing != null && uid.isNotBlank()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showReviewSubmissionSheet = false },
            sheetState = sheetState
        ) {
            ReviewSubmissionSheet(
                existingReview = tenantReview,
                onSubmit = { recommended, comment ->
                    scope.launch {
                        try {
                            android.util.Log.d("ReviewSubmission", "Submitting review for listing: $listingId, reviewer: $uid")
                            val review = io.github.howshous.data.models.ListingReview(
                                listingId = listingId,
                                reviewerId = uid,
                                recommended = recommended,
                                comment = comment.trim()
                            )
                            android.util.Log.d("ReviewSubmission", "Review object created: $review")
                            val success = reviewRepository.addReview(listingId, review)
                            android.util.Log.d("ReviewSubmission", "addReview returned: $success")
                            if (success) {
                                // Refresh reviews and reload listing to update summary
                                reviews = reviewRepository.getReviewsForListing(listingId)
                                tenantReview = reviewRepository.getTenantReviewForListing(listingId, uid)
                                viewModel.loadListing(listingId)
                                showReviewSubmissionSheet = false
                            } else {
                                android.util.Log.e("ReviewSubmission", "addReview returned false")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ReviewSubmission", "Error submitting review: ${e.message}", e)
                            e.printStackTrace()
                        }
                    }
                },
                onUpdate = { recommended, comment ->
                    scope.launch {
                        try {
                            if (tenantReview == null) return@launch
                            val success = reviewRepository.updateReview(
                                listingId,
                                tenantReview!!.id,
                                recommended,
                                comment.trim()
                            )
                            if (success) {
                                // Refresh reviews and reload listing to update summary
                                reviews = reviewRepository.getReviewsForListing(listingId)
                                tenantReview = reviewRepository.getTenantReviewForListing(listingId, uid)
                                viewModel.loadListing(listingId)
                                showReviewSubmissionSheet = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        try {
                            if (tenantReview == null) return@launch
                            val success = reviewRepository.deleteReview(listingId, tenantReview!!.id)
                            if (success) {
                                // Refresh reviews and reload listing to update summary
                                reviews = reviewRepository.getReviewsForListing(listingId)
                                tenantReview = null
                                viewModel.loadListing(listingId)
                                showReviewSubmissionSheet = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onDismiss = {
                    showReviewSubmissionSheet = false
                },
                isLoading = isReviewSubmitting
            )
        }
    }

    if (showRejectDialog && role == "administrator" && listing != null) {
        AlertDialog(
            onDismissRequest = {
                showRejectDialog = false
                rejectDialogError = ""
            },
            title = { Text("Reject Listing") },
            text = {
                Column {
                    Text(
                        "Select at least one reason before confirming rejection.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    adminRejectReasonOptions.forEach { reason ->
                        val checked = rejectionReasons.contains(reason)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    rejectionReasons = if (checked) rejectionReasons - reason else rejectionReasons + reason
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { toggled ->
                                    rejectionReasons = if (toggled) rejectionReasons + reason else rejectionReasons - reason
                                }
                            )
                            Text(reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (rejectDialogError.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(rejectDialogError, color = Color(0xFFB00020), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rejectionReasons.isEmpty()) {
                            rejectDialogError = "Select at least one rejection reason before rejecting."
                            return@TextButton
                        }
                        scope.launch {
                            val reasonsText = rejectionReasons.joinToString("; ")
                            listingRepository.rejectListing(
                                listingId = listingId,
                                adminUid = uid,
                                notes = reasonsText
                            )
                            notificationRepository.createNotification(
                                userId = listing!!.landlordId,
                                type = "listing_rejected",
                                title = "Listing Rejected",
                                message = "Your listing \"${listing!!.title}\" was rejected. Reasons: $reasonsText. Please update and resubmit for review.",
                                actionUrl = "edit_listing/${listingId}",
                                listingId = listingId,
                                senderId = uid
                            )
                            showRejectDialog = false
                            rejectDialogError = ""
                            viewModel.loadListing(listingId)
                            nav.popBackStack()
                        }
                    }
                ) {
                    Text("Confirm Reject", color = Color(0xFFB00020))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        rejectDialogError = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPhotoViewer && selectedPhotoUrl.isNotBlank()) {
        var scale by remember(selectedPhotoUrl) { mutableStateOf(1f) }
        var offset by remember(selectedPhotoUrl) { mutableStateOf(Offset.Zero) }
        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale
            offset += panChange
        }

        Dialog(onDismissRequest = { showPhotoViewer = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = selectedPhotoUrl,
                    contentDescription = "Listing image full view",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(transformableState)
                )
                IconButton(
                    onClick = { showPhotoViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showLandDeedViewer && listing != null && listing!!.landDeedUrl.isNotBlank()) {
        val deedUrl = listing!!.landDeedUrl
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = newScale
            offset += panChange
        }

        Dialog(onDismissRequest = { showLandDeedViewer = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = deedUrl,
                    contentDescription = "Land deed full view",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(transformableState)
                )
                IconButton(
                    onClick = { showLandDeedViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: io.github.howshous.data.models.ListingReview) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateLabel = review.createdAt?.toDate()?.let { formatter.format(it) } ?: "Recently"
    val badgeColor = if (review.recommended) ReviewGreen else ReviewRed
    val badgeText = if (review.recommended) "Recommended" else "Not recommended"
    val comment = review.comment.trim()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            if (comment.isNotBlank()) {
                Text(
                    comment,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "No comment provided.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
