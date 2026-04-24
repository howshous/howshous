package io.github.howshous.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import io.github.howshous.data.models.Listing
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.VacancyBlue
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.util.buildCropIntent
import io.github.howshous.ui.util.getCroppedUri
import io.github.howshous.ui.util.saveBitmapToCache
import io.github.howshous.ui.util.uploadCompressedImage
import io.github.howshous.ui.util.defaultContractTitle
import io.github.howshous.ui.util.defaultContractTerms
import io.github.howshous.ui.util.defaultContractTermsFilled
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(nav: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("1") }
    var genderPolicy by remember { mutableStateOf("") }
    var genderPolicyExpanded by remember { mutableStateOf(false) }
    var contractTitle by remember { mutableStateOf(defaultContractTitle()) }
    var contractTerms by remember { mutableStateOf(defaultContractTerms()) }
    var selectedAmenities by remember { mutableStateOf(setOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var selectedPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingCropUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isCropping by remember { mutableStateOf(false) }
    var landDeedUri by remember { mutableStateOf<Uri?>(null) }
    var step by remember { mutableStateOf(1) }

    val landlordAccent = VacancyBlue
    val landlordAccentOn = Color.White
    
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

    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listingRepository = remember { ListingRepository() }
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
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DebouncedIconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Create Listing", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("Step $step of 2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (step >= 1) landlordAccent else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (step >= 2) landlordAccent else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (step == 1) {
                    // Basic Information Section
                    Text(
                        text = "Property Information",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Property Title") },
                                shape = RoundedCornerShape(8.dp),
                                colors = inputColors(accentColor = landlordAccent),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Description") },
                                shape = RoundedCornerShape(8.dp),
                                colors = inputColors(accentColor = landlordAccent),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4,
                                minLines = 3
                            )

                            ExposedDropdownMenuBox(
                                expanded = locationExpanded,
                                onExpandedChange = { locationExpanded = !locationExpanded }
                            ) {
                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Location") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = inputColors(accentColor = landlordAccent),
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
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Pricing & Capacity Section
                    Text(
                        text = "Pricing & Capacity",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = price,
                                    onValueChange = { price = it },
                                    label = { Text("Monthly Rent") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = inputColors(accentColor = landlordAccent),
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = deposit,
                                    onValueChange = { deposit = it },
                                    label = { Text("Deposit") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = inputColors(accentColor = landlordAccent),
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            OutlinedTextField(
                                value = capacity,
                                onValueChange = { capacity = it.filter { c -> c.isDigit() } },
                                label = { Text("Capacity (tenants)") },
                                shape = RoundedCornerShape(8.dp),
                                colors = inputColors(accentColor = landlordAccent),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = genderPolicyExpanded,
                                onExpandedChange = { genderPolicyExpanded = !genderPolicyExpanded }
                            ) {
                                OutlinedTextField(
                                    value = when (genderPolicy) {
                                        "female" -> "Females only"
                                        "male" -> "Males only"
                                        "any" -> "Any gender"
                                        else -> ""
                                    },
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Gender policy") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderPolicyExpanded) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = inputColors(accentColor = landlordAccent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = genderPolicyExpanded,
                                    onDismissRequest = { genderPolicyExpanded = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Females only") },
                                        onClick = {
                                            genderPolicy = "female"
                                            genderPolicyExpanded = false
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Males only") },
                                        onClick = {
                                            genderPolicy = "male"
                                            genderPolicyExpanded = false
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Any gender") },
                                        onClick = {
                                            genderPolicy = "any"
                                            genderPolicyExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Amenities Section
                    Text(
                        text = "Amenities",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
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
                                        label = { Text(amenity, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = landlordAccent,
                                            selectedLabelColor = landlordAccentOn,
                                            selectedTrailingIconColor = landlordAccentOn,
                                            selectedLeadingIconColor = landlordAccentOn
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Listing Photos Section
                    Text(
                        text = "Listing Photos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Add clear photos of the property from different angles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = landlordAccent.copy(alpha = 0.1f),
                                        contentColor = landlordAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, landlordAccent)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Take Photo")
                                }

                                Button(
                                    onClick = { photoPicker.launch("image/*") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = landlordAccent.copy(alpha = 0.1f),
                                        contentColor = landlordAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, landlordAccent)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Browse")
                                }
                            }

                            if (selectedPhotoUris.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${selectedPhotoUris.size} photo${if (selectedPhotoUris.size != 1) "s" else ""} selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = landlordAccent
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(selectedPhotoUris) { uri ->
                                        Box(modifier = Modifier.size(100.dp)) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = "Listing photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                                    .background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                            )
                                            IconButton(
                                                onClick = {
                                                    selectedPhotoUris = selectedPhotoUris.filter { it != uri }
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .background(
                                                        color = Color.Black.copy(alpha = 0.6f),
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove photo",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Land Deed Section
                    Text(
                        text = "Ownership Proof",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Upload a clear photo of your land deed or proof of ownership",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { landDeedCameraLauncher.launch(null) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = landlordAccent.copy(alpha = 0.1f),
                                        contentColor = landlordAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, landlordAccent)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Take")
                                }

                                Button(
                                    onClick = { landDeedGalleryLauncher.launch("image/*") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = landlordAccent.copy(alpha = 0.1f),
                                        contentColor = landlordAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, landlordAccent)
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Browse")
                                }
                            }

                            if (landDeedUri != null) {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = landDeedUri,
                                        contentDescription = "Land deed photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                    )
                                    IconButton(
                                        onClick = { landDeedUri = null },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(
                                                color = Color.Black.copy(alpha = 0.6f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove deed",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Next Button
                    Button(
                        onClick = {
                            val priceValue = price.toIntOrNull()
                            val capacityValue = capacity.toIntOrNull()
                            if (title.isBlank() || location.isBlank() || priceValue == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please complete listing details first.")
                                }
                                return@Button
                            }
                            if (capacityValue == null || capacityValue <= 0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid capacity.")
                                }
                                return@Button
                            }
                            if (genderPolicy.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please choose a gender policy.")
                                }
                                return@Button
                            }
                            if (landDeedUri == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please upload a land deed image.")
                                }
                                return@Button
                            }
                            val depositValue = deposit.toIntOrNull() ?: 0
                            contractTerms = defaultContractTermsFilled(priceValue, depositValue)
                            step = 2
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = title.isNotEmpty() && location.isNotEmpty() && price.isNotEmpty() && capacity.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = landlordAccent,
                            contentColor = landlordAccentOn
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue to Contract Review", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    // Contract Template Section
                    Text(
                        text = "Contract Template",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        "Review and customize your rental contract template",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = contractTitle,
                                onValueChange = { contractTitle = it },
                                label = { Text("Contract Title") },
                                shape = RoundedCornerShape(8.dp),
                                colors = inputColors(accentColor = landlordAccent),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = contractTerms,
                                onValueChange = { contractTerms = it },
                                label = { Text("Contract Terms") },
                                shape = RoundedCornerShape(8.dp),
                                colors = inputColors(accentColor = landlordAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp),
                                maxLines = 10
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Back", style = MaterialTheme.typography.labelLarge)
                        }

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
                                            val deedUri = landDeedUri
                                            if (deedUri == null) {
                                                isSubmitting = false
                                                snackbarHostState.showSnackbar("Please upload a land deed image.")
                                                return@launch
                                            }

                                            val newListingId = UUID.randomUUID().toString()
                                            try {
                                                val deedUrl = uploadCompressedImage(
                                                    context,
                                                    deedUri,
                                                    "listing_uploads/$newListingId/land_deed.jpg"
                                                )
                                                val photoUrls = if (selectedPhotoUris.isNotEmpty()) {
                                                    selectedPhotoUris.mapIndexedNotNull { index, uri ->
                                                        val photoUrl = uploadCompressedImage(
                                                            context,
                                                            uri,
                                                            "listing_uploads/$newListingId/photo_$index.jpg"
                                                        )
                                                        photoUrl.takeIf { it.isNotBlank() }
                                                    }
                                                } else {
                                                    emptyList()
                                                }
                                                val listing = Listing(
                                                    id = newListingId,
                                                    landlordId = landlordId,
                                                    title = title.trim(),
                                                    description = description.trim(),
                                                    location = location.trim(),
                                                    price = priceValue,
                                                    deposit = depositValue,
                                                    capacity = capacityValue,
                                                    currentOccupancy = 0,
                                                    hasAvailableSlots = true,
                                                    genderPolicy = genderPolicy,
                                                    status = "under_review",
                                                    amenities = selectedAmenities.toList(),
                                                    photos = photoUrls,
                                                    landDeedUrl = deedUrl,
                                                    contractTemplate = template
                                                )
                                                val newId = listingRepository.createListing(listing)
                                                isSubmitting = false
                                                if (newId.isNotEmpty()) {
                                                    nav.previousBackStackEntry
                                                        ?.savedStateHandle
                                                        ?.set("listingCreated", true)
                                                    nav.popBackStack()
                                                } else {
                                                    snackbarHostState.showSnackbar("Failed to create listing. Please try again.")
                                                }
                                            } catch (e: Exception) {
                                                isSubmitting = false
                                                snackbarHostState.showSnackbar("Upload failed. Please try again.")
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = landlordAccent,
                                contentColor = landlordAccentOn
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = landlordAccentOn
                                )
                            } else {
                                Text("Create Listing", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
