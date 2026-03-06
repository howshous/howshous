package io.github.howshous.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FieldValue
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.models.Listing
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.util.buildCropIntent
import io.github.howshous.ui.util.getCroppedUri
import io.github.howshous.ui.util.saveBitmapToCache
import io.github.howshous.ui.util.uploadCompressedImage
import io.github.howshous.ui.util.defaultContractTitle
import io.github.howshous.ui.util.defaultContractTerms
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(nav: NavController, listingId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val listingRepository = remember { ListingRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("1") }
    var contractTitle by remember { mutableStateOf(defaultContractTitle()) }
    var contractTerms by remember { mutableStateOf(defaultContractTerms()) }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var selectedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var existingPhotoUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCropUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isCropping by remember { mutableStateOf(false) }
    var landDeedUri by remember { mutableStateOf<Uri?>(null) }
    var existingLandDeedUrl by remember { mutableStateOf("") }
    var currentReviewStatus by remember { mutableStateOf("") }

    val baguioLocations = listOf(
        "Baguio City Center",
        "Session Road",
        "Burnham Park",
        "Camp John Hay",
        "Mines View Park",
        "Wright Park",
        "Legarda Road",
        "Leonard Wood Road",
        "Naguilian Road",
        "Marcos Highway",
        "Loakan Road",
        "Asin Road",
        "Irisan",
        "Aurora Hill",
        "Quezon Hill",
        "Pinsao",
        "Bakakeng",
        "Trancoville",
        "Lower Magsaysay",
        "Upper Magsaysay",
        "Pacdal",
        "Lualhati",
        "Happy Hallow",
        "Loakan Proper",
        "Dagsian",
        "Crystal Cave",
        "Lucban",
        "Gibraltar",
        "Teodoro Alonzo",
        "Rizal Monument"
    )

    val availableAmenities = listOf(
        "Free Parking", "WiFi", "Air Conditioning", "Pets Allowed",
        "Kitchen Access", "Laundry", "Security", "CCTV", "Furnished",
        "Near Public Transport", "Gym Access", "Swimming Pool"
    )

    LaunchedEffect(listingId) {
        if (listingId.isBlank()) return@LaunchedEffect
        val listing = listingRepository.getListing(listingId)
        if (listing != null) {
            currentReviewStatus = listing.reviewStatus
            title = listing.title
            description = listing.description
            location = listing.location
            price = listing.price.toString()
            deposit = listing.deposit.toString()
            capacity = listing.capacity.toString()
            selectedAmenities = listing.amenities.toSet()
            existingPhotoUrls = listing.photos
            existingLandDeedUrl = listing.landDeedUrl
            val template = listing.contractTemplate
            contractTitle = template?.get("title") as? String ?: defaultContractTitle()
            contractTerms = template?.get("terms") as? String ?: defaultContractTerms()
        }
    }

    lateinit var cropLauncher: ActivityResultLauncher<Intent>
    fun launchNextCrop() {
        if (isCropping || pendingCropUris.isEmpty()) return
        val next = pendingCropUris.first()
        pendingCropUris = pendingCropUris.drop(1)
        isCropping = true
        cropLauncher.launch(
            buildCropIntent(
                context = context,
                sourceUri = next,
                freeStyle = true
            )
        )
    }
    cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cropped = getCroppedUri(result.data)
        if (cropped != null) {
            selectedPhotoUris = (selectedPhotoUris + cropped).distinct()
        }
        isCropping = false
        if (pendingCropUris.isNotEmpty()) {
            launchNextCrop()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingCropUris = pendingCropUris + uris
            launchNextCrop()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToCache(context, bitmap)
            if (uri != null) {
                pendingCropUris = pendingCropUris + uri
                launchNextCrop()
            }
        }
    }

    val landDeedCropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cropped = getCroppedUri(result.data)
        if (cropped != null) {
            landDeedUri = cropped
        }
    }

    val landDeedCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToCache(context, bitmap)
            if (uri != null) {
                landDeedCropLauncher.launch(
                    buildCropIntent(
                        context = context,
                        sourceUri = uri,
                        freeStyle = true
                    )
                )
            }
        }
    }

    val landDeedGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            landDeedCropLauncher.launch(
                buildCropIntent(
                    context = context,
                    sourceUri = uri,
                    freeStyle = true
                )
            )
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DebouncedIconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text("Edit Listing", style = MaterialTheme.typography.titleMedium)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Property Title") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = !locationExpanded }
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Location (Baguio City)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                        shape = InputShape,
                        colors = inputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        baguioLocations.forEach { loc ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    location = loc
                                    locationExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Monthly Rent") },
                        shape = InputShape,
                        colors = inputColors(),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = deposit,
                        onValueChange = { deposit = it },
                        label = { Text("Deposit") },
                        shape = InputShape,
                        colors = inputColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { c -> c.isDigit() } },
                    label = { Text("Capacity (tenants)") },
                    shape = InputShape,
                    colors = inputColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Amenities",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableAmenities) { amenity ->
                        FilterChip(
                            selected = selectedAmenities.contains(amenity),
                            onClick = {
                                selectedAmenities = if (selectedAmenities.contains(amenity)) {
                                    selectedAmenities - amenity
                                } else {
                                    selectedAmenities + amenity
                                }
                            },
                            label = { Text(amenity, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Contract Template",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = contractTitle,
                    onValueChange = { contractTitle = it },
                    label = { Text("Contract Title") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = contractTerms,
                    onValueChange = { contractTerms = it },
                    label = { Text("Contract Terms") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    maxLines = 6
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Listing Photos",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Take Photo")
                    }

                    Button(
                        onClick = { photoPicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (selectedPhotoUris.isEmpty()) "Choose Photos" else "Add Photos")
                    }
                }

                val hasAnyPhotos = existingPhotoUrls.isNotEmpty() || selectedPhotoUris.isNotEmpty()
                if (hasAnyPhotos) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(existingPhotoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Listing photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(120.dp)
                            )
                        }
                        items(selectedPhotoUris) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Listing photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(120.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Land Deed (Proof of Ownership)",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { landDeedCameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Take Photo")
                    }

                    Button(
                        onClick = { landDeedGalleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (landDeedUri == null) "Choose Photo" else "Replace Photo")
                    }
                }

                val deedPreviewUrl = landDeedUri ?: existingLandDeedUrl.takeIf { it.isNotBlank() }
                if (deedPreviewUrl != null) {
                    Spacer(Modifier.height(12.dp))
                    AsyncImage(
                        model = deedPreviewUrl,
                        contentDescription = "Land deed photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        val landlordId = uid
                        val priceValue = price.toIntOrNull()
                        val depositValue = deposit.toIntOrNull() ?: 0
                        val capacityValue = capacity.toIntOrNull() ?: 0

                        when {
                            landlordId.isBlank() -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unable to determine landlord account.")
                                }
                            }

                            priceValue == null -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid monthly rent.")
                                }
                            }

                            capacityValue <= 0 -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid capacity.")
                                }
                            }

                            else -> {
                                scope.launch {
                                    isSubmitting = true
                                    val template = if (contractTitle.isNotBlank() || contractTerms.isNotBlank()) {
                                        mapOf(
                                            "title" to contractTitle.trim(),
                                            "terms" to contractTerms.trim(),
                                            "monthlyRent" to priceValue,
                                            "deposit" to depositValue
                                        )
                                    } else {
                                        null
                                    }
                                    val updates = mutableMapOf<String, Any>(
                                        "title" to title.trim(),
                                        "description" to description.trim(),
                                        "location" to location.trim(),
                                        "price" to priceValue,
                                        "deposit" to depositValue,
                                        "capacity" to capacityValue,
                                        "amenities" to selectedAmenities.toList(),
                                        "status" to "active",
                                        "reviewStatus" to "under_review",
                                        "reviewedBy" to "",
                                        "reviewNotes" to "",
                                        "reviewedAt" to FieldValue.delete()
                                    )
                                    if (template != null) {
                                        updates["contractTemplate"] = template
                                    }

                                    if (selectedPhotoUris.isNotEmpty()) {
                                        try {
                                            val photoUrls = selectedPhotoUris.mapIndexedNotNull { index, uri ->
                                                val photoUrl = uploadCompressedImage(
                                                    context,
                                                    uri,
                                                    "listing_uploads/$listingId/photo_$index.jpg"
                                                )
                                                photoUrl.takeIf { it.isNotBlank() }
                                            }
                                            if (photoUrls.isNotEmpty()) {
                                                updates["photos"] = photoUrls
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Photo upload failed. Keeping existing photos.")
                                        }
                                    } else if (existingPhotoUrls.isNotEmpty()) {
                                        updates["photos"] = existingPhotoUrls
                                    }

                                    if (landDeedUri != null) {
                                        try {
                                            val deedUrl = uploadCompressedImage(
                                                context,
                                                landDeedUri!!,
                                                "listing_uploads/$listingId/land_deed.jpg"
                                            )
                                            if (deedUrl.isNotBlank()) {
                                                updates["landDeedUrl"] = deedUrl
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Land deed upload failed. Keeping existing land deed.")
                                        }
                                    } else if (existingLandDeedUrl.isNotBlank()) {
                                        updates["landDeedUrl"] = existingLandDeedUrl
                                    }

                                    listingRepository.updateListing(listingId, updates)
                                    isSubmitting = false
                                    if (currentReviewStatus == "rejected") {
                                        snackbarHostState.showSnackbar("Listing resubmitted for admin review.")
                                    }
                                    nav.popBackStack()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = title.isNotEmpty() && location.isNotEmpty() && price.isNotEmpty() && capacity.isNotEmpty() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
