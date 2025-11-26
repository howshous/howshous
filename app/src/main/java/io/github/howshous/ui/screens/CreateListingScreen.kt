package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.data.models.Listing
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.inputColors
import kotlinx.coroutines.launch

@Composable
fun CreateListingScreen(nav: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listingRepository = remember { ListingRepository() }

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
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Text("Create Listing", style = MaterialTheme.typography.titleMedium)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
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

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        val landlordId = uid
                        val priceValue = price.toIntOrNull()
                        val depositValue = deposit.toIntOrNull() ?: 0

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

                            else -> {
                                scope.launch {
                                    isSubmitting = true
                                    val listing = Listing(
                                        landlordId = landlordId,
                                        title = title.trim(),
                                        description = description.trim(),
                                        location = location.trim(),
                                        price = priceValue,
                                        deposit = depositValue,
                                        status = "active"
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
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = title.isNotEmpty() && location.isNotEmpty() && price.isNotEmpty() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Listing")
                    }
                }
            }
        }
    }
}
