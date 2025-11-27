package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.NearWhite
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatDetailScreen(nav: NavController, chatId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showContractDialog by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listingRepository = remember { ListingRepository() }
    var listing by remember { mutableStateOf<io.github.howshous.data.models.Listing?>(null) }

    LaunchedEffect(chatId) {
        if (chatId.isNotEmpty()) {
            viewModel.loadMessagesForChat(chatId)
        }
    }

    LaunchedEffect(currentChat) {
        currentChat?.listingId?.let { listingId ->
            if (listingId.isNotEmpty()) {
                listing = listingRepository.getListing(listingId)
            }
        }
    }

    val isLandlord = currentChat?.landlordId == uid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header with back button
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
            Text("Conversation", style = MaterialTheme.typography.titleMedium)
        }

        // Messages
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                Text(
                    "No messages yet. Start the conversation!",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    // Show contracts
                    items(contracts.reversed()) { contract ->
                        ContractMessageCard(
                            contract = contract,
                            isLandlord = isLandlord,
                            onViewContract = { showContractDialog = contract.id },
                            onSignContract = {
                                scope.launch {
                                    viewModel.signContract(contract.id)
                                }
                            }
                        )
                    }
                    // Show messages
                    items(messages.reversed()) { message ->
                        val isUserMessage = message.senderId == uid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 250.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUserMessage) Color(0xFF1BA37C) else Color(0xFFFFFFFF)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        message.text,
                                        color = if (isUserMessage) Color.White else Color.Black,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        formatTime(message.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUserMessage) Color.White.copy(alpha = 0.7f) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contract button for landlords
        if (isLandlord && listing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val contractId = viewModel.sendContract(
                                chatId = chatId,
                                listingId = listing!!.id,
                                landlordId = uid,
                                tenantId = currentChat?.tenantId ?: "",
                                monthlyRent = listing!!.price,
                                deposit = listing!!.deposit
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("📄 Send Contract")
                }
            }
        }

        // Message input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank() && uid.isNotEmpty()) {
                        viewModel.sendMessage(chatId, uid, messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(0xFF1BA37C))
            }
        }
    }

    // Contract dialog
    showContractDialog?.let { contractId ->
        val contract = contracts.find { it.id == contractId }
        contract?.let {
            ContractDialog(
                contract = it,
                isLandlord = isLandlord,
                onDismiss = { showContractDialog = null },
                onSign = {
                    scope.launch {
                        viewModel.signContract(it.id)
                        showContractDialog = null
                    }
                }
            )
        }
    }
}

@Composable
fun ContractMessageCard(
    contract: io.github.howshous.data.models.Contract,
    isLandlord: Boolean,
    onViewContract: () -> Unit,
    onSignContract: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onViewContract() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📄", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        contract.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF1976D2)
                    )
                }
                Text(
                    contract.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (contract.status) {
                        "signed" -> Color(0xFF4CAF50)
                        "pending" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Rent: ₱${contract.monthlyRent}/month", style = MaterialTheme.typography.bodySmall)
            Text("Deposit: ₱${contract.deposit}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            if (!isLandlord && contract.status == "pending") {
                Button(
                    onClick = onSignContract,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Agree to Contract")
                }
            } else {
                TextButton(
                    onClick = onViewContract,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Details")
                }
            }
        }
    }
}

@Composable
fun ContractDialog(
    contract: io.github.howshous.data.models.Contract,
    isLandlord: Boolean,
    onDismiss: () -> Unit,
    onSign: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contract.title) },
        text = {
            Column {
                Text("Monthly Rent: ₱${contract.monthlyRent}", style = MaterialTheme.typography.bodyMedium)
                Text("Deposit: ₱${contract.deposit}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Text("Terms and Conditions:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(contract.terms, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (!isLandlord && contract.status == "pending") {
                Button(onClick = onSign) {
                    Text("Agree to Contract")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (!isLandlord && contract.status == "pending") {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun formatTime(timestamp: com.google.firebase.Timestamp?): String {
    return if (timestamp != null) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp.seconds * 1000))
    } else {
        ""
    }
}
