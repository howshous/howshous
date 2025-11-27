package io.github.howshous.ui.screens.main_tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.viewmodels.MessageAuthor
import io.github.howshous.ui.viewmodels.TenantAIHelperViewModel
import io.github.howshous.ui.util.MarkdownText
import kotlinx.coroutines.launch

@Composable
fun TenantAIHelperScreen(nav: NavController, tenantAIHelperViewModel: TenantAIHelperViewModel = viewModel()) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val messages by tenantAIHelperViewModel.messages.collectAsState()
    val isThinking by tenantAIHelperViewModel.isThinking.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentPrompt by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Initialize chat history when user ID is available
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            tenantAIHelperViewModel.initializeChat(uid)
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("AI Boarding House Guide", style = MaterialTheme.typography.titleMedium)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isTenant = message.author == MessageAuthor.TENANT
                    Surface(
                        color = if (isTenant) MaterialTheme.colorScheme.primary else Color.White,
                        contentColor = if (isTenant) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isTenant) "You" else "AI Assistant",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            if (isTenant) {
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                MarkdownText(text = message.text)
                            }
                        }
                    }
                }

                item {
                    if (isThinking) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = currentPrompt,
                    onValueChange = { currentPrompt = it },
                    placeholder = { Text("Describe your budget, location, or amenities...") },
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (currentPrompt.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Tell me what you're looking for so I can help.")
                            }
                        } else {
                            tenantAIHelperViewModel.sendMessage(currentPrompt)
                            currentPrompt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isThinking
                ) {
                    Text(if (isThinking) "Thinking..." else "Ask the AI")
                }
            }
        }
    }
}

