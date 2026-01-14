package io.github.howshous.ui.screens.main_tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.howshous.data.firestore.ChatRepository
import io.github.howshous.data.firestore.ContractRepository
import io.github.howshous.data.firestore.ListingRepository
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.viewmodels.ContractsViewModel
import kotlinx.coroutines.launch

@Composable
fun ReportIssueScreen(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: ContractsViewModel = viewModel()
    val contractRepository = remember { ContractRepository() }
    val chatRepository = remember { ChatRepository() }
    val listingRepository = remember { ListingRepository() }
    val scope = rememberCoroutineScope()
    
    var selectedIssueType by remember { mutableStateOf("") }
    var issueDescription by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var hasContract by remember { mutableStateOf(false) }
    var currentContract by remember { mutableStateOf<io.github.howshous.data.models.Contract?>(null) }
    var snackbarHostState = remember { SnackbarHostState() }

    val issueTypes = listOf(
        "Power Outage",
        "No Water",
        "Safety Hazard",
        "Plumbing Issue",
        "Electrical Problem",
        "Heating/Cooling Issue",
        "Security Concern",
        "Other"
    )

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            hasContract = contractRepository.hasSignedContract(uid)
            if (hasContract) {
                // Get the most recent signed contract
                val contracts = contractRepository.getSignedContractsForTenant(uid)
                if (contracts.isNotEmpty()) {
                    currentContract = contracts.first()
                }
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
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Report Issue", style = MaterialTheme.typography.titleLarge)
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!hasContract) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "No Active Contract",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "You need to have a signed contract to report issues to your landlord.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    Text(
                        "Select Issue Type",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    issueTypes.forEach { issueType ->
                        FilterChip(
                            selected = selectedIssueType == issueType,
                            onClick = { selectedIssueType = issueType },
                            label = { Text(issueType) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Describe the Issue",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        placeholder = { Text("Please describe the issue in detail...") },
                        shape = InputShape,
                        colors = inputColors(),
                        maxLines = 10
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (selectedIssueType.isEmpty()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please select an issue type")
                                }
                                return@Button
                            }
                            if (issueDescription.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please describe the issue")
                                }
                                return@Button
                            }
                            if (currentContract == null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unable to find your contract")
                                }
                                return@Button
                            }

                            isLoading = true
                            scope.launch {
                                try {
                                    val contract = currentContract!!
                                    // Find or create chat with landlord
                                    val chats = chatRepository.getChatsForUser(uid)
                                    val chat = chats.find { 
                                        it.landlordId == contract.landlordId && 
                                        it.tenantId == uid 
                                    } ?: run {
                                        // Create new chat if doesn't exist
                                        val newChat = io.github.howshous.data.models.Chat(
                                            id = "",
                                            listingId = contract.listingId,
                                            tenantId = uid,
                                            landlordId = contract.landlordId,
                                            lastMessage = "Issue reported: $selectedIssueType",
                                            lastTimestamp = com.google.firebase.Timestamp.now()
                                        )
                                        val chatId = chatRepository.createChat(newChat)
                                        newChat.copy(id = chatId)
                                    }

                                    // Send issue report as message
                                    val message = "🚨 ISSUE REPORT\n\nType: $selectedIssueType\n\nDescription: $issueDescription"
                                    chatRepository.sendMessage(chat.id, uid, message)

                                    // Create issue record
                                    val issueRepository = io.github.howshous.data.firestore.IssueRepository()
                                    val issue = io.github.howshous.data.models.Issue(
                                        id = "",
                                        listingId = contract.listingId,
                                        tenantId = uid,
                                        landlordId = contract.landlordId,
                                        contractId = contract.id,
                                        chatId = chat.id,
                                        issueType = selectedIssueType,
                                        description = issueDescription,
                                        status = "pending",
                                        reportedAt = com.google.firebase.Timestamp.now()
                                    )
                                    issueRepository.createIssue(issue)

                                    snackbarHostState.showSnackbar("Issue reported successfully!")
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed to report issue: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading && selectedIssueType.isNotEmpty() && issueDescription.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit Report")
                        }
                    }
                }
            }
        }
    }
}

