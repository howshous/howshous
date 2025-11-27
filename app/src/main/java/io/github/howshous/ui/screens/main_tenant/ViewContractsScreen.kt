package io.github.howshous.ui.screens.main_tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.data.firestore.ContractRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.models.Contract
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.ContractsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ViewContractsScreen(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ContractsViewModel = viewModel()
    val contracts by viewModel.signedContracts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedContract by remember { mutableStateOf<Contract?>(null) }
    val listingRepository = remember { ListingRepository() }
    var listingTitles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            viewModel.loadSignedContracts(uid)
        }
    }

    LaunchedEffect(contracts) {
        if (contracts.isNotEmpty()) {
            scope.launch {
                val map = mutableMapOf<String, String>()
                contracts.forEach { contract ->
                    if (contract.listingId.isNotEmpty() && !map.containsKey(contract.listingId)) {
                        val listing = listingRepository.getListing(contract.listingId)
                        listing?.let { map[contract.listingId] = it.title }
                    }
                }
                listingTitles = map
            }
        }
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
                .background(Color(0xFFF3EDF7))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("My Contracts", style = MaterialTheme.typography.titleLarge)
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(32.dp))
        } else if (contracts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No signed contracts yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contracts) { contract ->
                    val listingTitle = listingTitles[contract.listingId] ?: "Unknown Listing"
                    ContractCard(
                        contract = contract,
                        listingTitle = listingTitle,
                        onClick = { selectedContract = contract }
                    )
                }
            }
        }
    }

    // Contract detail dialog
    selectedContract?.let { contract ->
        ContractDetailDialog(
            contract = contract,
            listingTitle = listingTitles[contract.listingId] ?: "Unknown Listing",
            onDismiss = { selectedContract = null }
        )
    }
}

@Composable
fun ContractCard(
    contract: Contract,
    listingTitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                listingTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                contract.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Rent: ₱${contract.monthlyRent}/month",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Deposit: ₱${contract.deposit}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            contract.signedAt?.let {
                Text(
                    "Signed: ${formatDate(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ContractDetailDialog(
    contract: Contract,
    listingTitle: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listingTitle) },
        text = {
            Column {
                Text(contract.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(16.dp))
                Text("Monthly Rent: ₱${contract.monthlyRent}", style = MaterialTheme.typography.bodyMedium)
                Text("Deposit: ₱${contract.deposit}", style = MaterialTheme.typography.bodyMedium)
                contract.signedAt?.let {
                    Text("Signed: ${formatDate(it)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                Text("Terms and Conditions:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(contract.terms, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        .format(Date(timestamp.seconds * 1000))
}

