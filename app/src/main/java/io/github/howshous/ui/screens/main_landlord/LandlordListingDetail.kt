package io.github.howshous.ui.screens.main_landlord

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.firestore.FieldValue
import io.github.howshous.R
import io.github.howshous.data.firestore.ChatRepository
import io.github.howshous.data.firestore.ContractRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.data.firestore.NotificationRepository
import io.github.howshous.data.firestore.TenancyRepository
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.util.defaultContractTitle
import io.github.howshous.ui.util.defaultContractTerms
import io.github.howshous.ui.viewmodels.ListingViewModel
import kotlinx.coroutines.launch

@Composable
fun LandlordListingDetail(nav: NavController, listingId: String = "") {
    val viewModel: ListingViewModel = viewModel()
    val listing by viewModel.listing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listingRepository = remember { ListingRepository() }
    val contractRepository = remember { ContractRepository() }
    val chatRepository = remember { ChatRepository() }
    val tenancyRepository = remember { TenancyRepository() }
    val notificationRepository = remember { NotificationRepository() }
    val scope = rememberCoroutineScope()

    var contractTitle by remember { mutableStateOf("") }
    var contractTerms by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var tenancies by remember { mutableStateOf<List<io.github.howshous.data.models.Tenancy>>(emptyList()) }
    var isTenanciesLoading by remember { mutableStateOf(false) }
    var tenancyToTerminate by remember { mutableStateOf<io.github.howshous.data.models.Tenancy?>(null) }
    var isTerminating by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) {
        viewModel.observeListing(listingId)
    }

    DisposableEffect(listingId) {
        if (listingId.isBlank()) return@DisposableEffect onDispose { }
        isTenanciesLoading = true
        val registration = tenancyRepository.listenTenanciesForListing(listingId) { updated ->
            tenancies = updated
            isTenanciesLoading = false
        }
        onDispose {
            registration?.remove()
        }
    }

    LaunchedEffect(listing) {
        val template = listing?.contractTemplate
        contractTitle = template?.get("title") as? String ?: defaultContractTitle()
        contractTerms = template?.get("terms") as? String ?: defaultContractTerms()
    }

    fun isActiveTenancy(status: String): Boolean {
        return status == "signed" || status == "confirmed" || status == "needs_resign" || status == "active"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header with back button
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

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Listing ID: $listingId", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (listing != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(listing!!.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(listing!!.location, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Price: PHP ${listing!!.price}/month", style = MaterialTheme.typography.bodySmall)
                        Text("Deposit: PHP ${listing!!.deposit}", style = MaterialTheme.typography.bodySmall)
                        val capacityValue = listing!!.capacity.coerceAtLeast(1)
                        val activeCount = tenancies.count { isActiveTenancy(it.status) }
                        Text("Capacity: $capacityValue tenants", style = MaterialTheme.typography.bodySmall)
                        Text("Occupied: $activeCount / $capacityValue", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { nav.navigate("edit_listing/$listingId") }, modifier = Modifier.weight(1f)) {
                                Text("Edit")
                            }
                            Button(
                                onClick = { /* Delete listing */ },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Delete")
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Text("Tenants", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))

                        if (isTenanciesLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.height(8.dp))
                        } else if (tenancies.isEmpty()) {
                            Text("No tenants yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(Modifier.height(12.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                tenancies.forEach { tenancy ->
                                    val displayName = tenancy.tenantName.ifBlank {
                                        if (tenancy.tenantId.length > 6) {
                                            "Tenant ${tenancy.tenantId.take(6)}"
                                        } else {
                                            "Tenant ${tenancy.tenantId}"
                                        }
                                    }
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    tenancy.status.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when (tenancy.status) {
                                                        "signed", "confirmed" -> Color(0xFF1BA37C)
                                                        "needs_resign" -> Color(0xFFE27D23)
                                                        "ended" -> Color.Gray
                                                        else -> Color.Gray
                                                    }
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                "Tenant ID: ${tenancy.tenantId}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            if (isActiveTenancy(tenancy.status)) {
                                                Button(
                                                    onClick = { tenancyToTerminate = tenancy },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("End Tenancy", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }

                        Text("Contract Template", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(12.dp))

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
                                .height(160.dp),
                            maxLines = 8
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (isSaving || listing == null) return@Button
                                isSaving = true
                                saveMessage = ""
                                val updates = mutableMapOf<String, Any>()
                                if (contractTitle.isNotBlank() || contractTerms.isNotBlank()) {
                                    updates["contractTemplate"] = mapOf(
                                        "title" to contractTitle.trim(),
                                        "terms" to contractTerms.trim(),
                                        "monthlyRent" to listing!!.price,
                                        "deposit" to listing!!.deposit
                                    )
                                } else {
                                    updates["contractTemplate"] = FieldValue.delete()
                                }
                                val currentStatus = listing!!.status
                                val previousStatus = if (currentStatus == "active" || currentStatus == "inactive") {
                                    currentStatus
                                } else {
                                    listing!!.previousStatus
                                }
                                updates["status"] = "under_review"
                                updates["previousStatus"] = previousStatus
                                updates["reviewedBy"] = ""
                                updates["reviewNotes"] = ""
                                updates["reviewedAt"] = FieldValue.delete()
                                scope.launch {
                                    try {
                                        listingRepository.updateListing(
                                            listingId,
                                            updates
                                        )

                                        val tenancies = tenancyRepository.getTenanciesForListing(listingId)
                                        if (tenancies.isNotEmpty()) {
                                            for (tenancy in tenancies) {
                                                val chatId = chatRepository.getOrCreateChatForListing(
                                                    listingId = listingId,
                                                    tenantId = tenancy.tenantId,
                                                    landlordId = tenancy.landlordId
                                                )
                                                if (chatId.isBlank()) continue

                                                val contractId = contractRepository.createContractFromListingTemplate(
                                                    listingId = listingId,
                                                    chatId = chatId,
                                                    landlordId = tenancy.landlordId,
                                                    tenantId = tenancy.tenantId,
                                                    fallbackTitle = "Rental Agreement",
                                                    fallbackTerms = contractTerms.trim(),
                                                    fallbackMonthlyRent = listing!!.price,
                                                    fallbackDeposit = listing!!.deposit
                                                )

                                                if (contractId.isNotBlank()) {
                                                    chatRepository.sendMessage(
                                                        chatId,
                                                        tenancy.landlordId,
                                                        "Contract updated for this listing. Please review and sign the new contract."
                                                    )
                                                    notificationRepository.createNotification(
                                                        userId = tenancy.tenantId,
                                                        type = "contract_update",
                                                        title = "Contract Updated",
                                                        message = "Your contract for ${listing!!.title} was updated. Please review and sign the new version.",
                                                        actionUrl = "chat/$chatId",
                                                        listingId = listingId,
                                                        senderId = tenancy.landlordId
                                                    )
                                                    tenancyRepository.updateTenancyStatus(
                                                        listingId = listingId,
                                                        tenantId = tenancy.tenantId,
                                                        status = "needs_resign"
                                                    )
                                                }
                                            }
                                        }

                                        saveMessage = if (tenancies.isNotEmpty()) {
                                            "Contract template saved. Tenants have been notified to re-sign."
                                        } else {
                                            "Contract template saved."
                                        }
                                    } catch (e: Exception) {
                                        saveMessage = "Failed to save contract template."
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text("Save Contract Template")
                            }
                        }

                        if (saveMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(saveMessage, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                Text("Listing not found", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    tenancyToTerminate?.let { tenancy ->
        AlertDialog(
            onDismissRequest = { if (!isTerminating) tenancyToTerminate = null },
            title = { Text("End tenancy?") },
            text = {
                Text("This will terminate the current contract and mark the tenancy as ended.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isTerminating) return@Button
                        isTerminating = true
                        scope.launch {
                            val contractOk = contractRepository.terminateContract(tenancy.contractId, tenancy.landlordId)
                            if (contractOk) {
                                tenancyRepository.updateTenancyStatus(
                                    listingId = tenancy.listingId,
                                    tenantId = tenancy.tenantId,
                                    status = "ended"
                                )
                            }

                            if (contractOk) {
                                val chatId = chatRepository.getOrCreateChatForListing(
                                    listingId = tenancy.listingId,
                                    tenantId = tenancy.tenantId,
                                    landlordId = tenancy.landlordId
                                )
                                if (chatId.isNotBlank()) {
                                    chatRepository.sendMessage(
                                        chatId,
                                        tenancy.landlordId,
                                        "Your contract has been terminated for this listing."
                                    )
                                }

                                val listingTitle = listing?.title ?: "your listing"
                                notificationRepository.createNotification(
                                    userId = tenancy.tenantId,
                                    type = "contract_terminated",
                                    title = "Contract Terminated",
                                    message = "Your contract for $listingTitle was terminated by the landlord.",
                                    actionUrl = "listing/${tenancy.listingId}",
                                    listingId = tenancy.listingId,
                                    senderId = tenancy.landlordId
                                )
                            }

                            tenancies = tenancyRepository.getTenanciesForListing(listingId)
                            if (!contractOk) {
                                saveMessage = "Failed to terminate contract. Please try again."
                            }
                            isTerminating = false
                            tenancyToTerminate = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isTerminating
                ) {
                    if (isTerminating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Terminate", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isTerminating) tenancyToTerminate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
