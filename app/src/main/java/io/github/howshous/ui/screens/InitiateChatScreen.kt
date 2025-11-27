package io.github.howshous.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.data.firestore.ChatRepository
import io.github.howshous.data.models.Chat
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.viewmodels.ChatViewModel
import io.github.howshous.ui.data.readUidFlow
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@Composable
fun InitiateChatScreen(nav: NavController, listingId: String = "", landlordId: String = "") {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

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
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Start Conversation", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Send a message to the landlord", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    errorMessage = ""
                },
                label = { Text("Your Message") },
                shape = InputShape,
                colors = inputColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 5
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (uid.isEmpty()) {
                        errorMessage = "User not authenticated"
                        return@Button
                    }
                    if (message.isEmpty()) {
                        errorMessage = "Message cannot be empty"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        try {
                            val chatRepository = ChatRepository()
                            
                            // Try to find existing chat or create new one
                            val chats = chatRepository.getChatsForUser(uid)
                            val existingChat = chats.find { 
                                it.listingId == listingId && 
                                it.tenantId == uid && 
                                it.landlordId == landlordId
                            }

                            val chatId = if (existingChat != null) {
                                // Send message to existing chat
                                chatRepository.sendMessage(existingChat.id, uid, message)
                                existingChat.id
                            } else {
                                // Create new chat
                                val newChat = Chat(
                                    id = "", // Firestore will generate
                                    listingId = listingId,
                                    tenantId = uid,
                                    landlordId = landlordId,
                                    lastMessage = message,
                                    lastTimestamp = Timestamp.now()
                                )
                                val createdChatId = chatRepository.createChat(newChat)
                                // Send initial message
                                if (createdChatId.isNotEmpty()) {
                                    chatRepository.sendMessage(createdChatId, uid, message)
                                }
                                createdChatId
                            }

                            if (chatId.isNotEmpty()) {
                                nav.navigate("chat/$chatId") {
                                    popUpTo("initiate_chat/$listingId/$landlordId") { inclusive = true }
                                }
                            } else {
                                errorMessage = "Failed to create conversation"
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to start conversation: ${e.message}"
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = message.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Message")
                }
            }
        }
    }
}
