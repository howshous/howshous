package io.github.howshous.ui.screens.main_landlord

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import coil.compose.AsyncImage
import io.github.howshous.data.firestore.IssueRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.OverdueRed
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.util.saveBitmapToCache
import io.github.howshous.ui.util.uploadCompressedImage
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(nav: NavController, issueId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val issueRepository = remember { IssueRepository() }
    val listingRepository = remember { ListingRepository() }
    
    var issue by remember { mutableStateOf<io.github.howshous.data.models.Issue?>(null) }
    var listing by remember { mutableStateOf<io.github.howshous.data.models.Listing?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var resolutionPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(issueId) {
        issue = issueRepository.getIssue(issueId)
        issue?.let {
            listing = listingRepository.getListing(it.listingId)
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToCache(context, it)
            resolutionPhotoUri = uri
        }
    }
    
    // Permission launcher for camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos")
            }
        }
    }
    
    fun launchCamera() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(permission)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Details") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (issue == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceLight)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (issue!!.status == "pending") Color(0xFFFFF9C4) else PricePointGreen
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Status: ${issue!!.status.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (issue!!.status == "pending") OverdueRed else Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Type: ${issue!!.issueType}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Reported: ${issue!!.reportedAt?.toDate()?.toString() ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Listing Info
                listing?.let {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Property", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(it.title, style = MaterialTheme.typography.bodyMedium)
                            Text(it.location, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                // Description
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(issue!!.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                // Resolution Photo (if resolved)
                if (issue!!.status == "resolved" && issue!!.resolutionPhotoUrl.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resolution Evidence", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            AsyncImage(
                                model = issue!!.resolutionPhotoUrl,
                                contentDescription = "Resolution photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                // Resolution Photo Preview (if taken but not saved)
                resolutionPhotoUri?.let { uri ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resolution Photo", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            AsyncImage(
                                model = uri,
                                contentDescription = "Resolution photo preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                // Action Buttons
                if (issue!!.status == "pending") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { launchCamera() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        ) {
                            Text(if (resolutionPhotoUri == null) "Take Resolution Photo" else "Retake Photo")
                        }
                        
                        Button(
                            onClick = {
                                if (resolutionPhotoUri == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please take a photo first")
                                    }
                                    return@Button
                                }
                                
                                isLoading = true
                                scope.launch {
                                    try {
                                        val photoUrl = uploadCompressedImage(
                                            context,
                                            resolutionPhotoUri!!,
                                            "issues/${issueId}/resolution.jpg"
                                        )
                                        
                                        issueRepository.resolveIssue(issueId, photoUrl)
                                        
                                        snackbarHostState.showSnackbar("Issue marked as resolved!")
                                        nav.popBackStack()
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading && resolutionPhotoUri != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PricePointGreen
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Mark as Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}

