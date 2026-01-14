package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.PricePointGreen
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.ListingViewModel

@Composable
fun ListingDetailScreen(nav: NavController, listingId: String = "") {
    val viewModel: ListingViewModel = viewModel()
    val listing by viewModel.listing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Listing Details", style = MaterialTheme.typography.titleMedium)
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
                        AsyncImage(
                            model = listing!!.photos[0],
                            contentDescription = listing!!.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
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
                    Spacer(Modifier.height(16.dp))
                }

                // Description
                item {
                    Text("Description", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(listing!!.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                }

                // Amenities
                item {
                    if (listing!!.amenities.isNotEmpty()) {
                        Text("Amenities", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        listing!!.amenities.forEach { amenity ->
                            Text("• $amenity", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Contact Button
                item {
                    Button(
                        onClick = {
                            nav.navigate("initiate_chat/${listingId}/${listing!!.landlordId}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Contact Landlord")
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
}
