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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.data.readUidFlow
import io.github.howshous.ui.theme.InfoSurface
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.VacancyBlue
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.util.MarkdownText
import io.github.howshous.ui.viewmodels.LandlordAnalyticsAIViewModel
import io.github.howshous.ui.viewmodels.MessageAuthor
import kotlinx.coroutines.launch

@Composable
fun LandlordAnalyticsAIChatScreen(
    nav: NavController,
    viewModel: LandlordAnalyticsAIViewModel = viewModel()
) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val messages by viewModel.messages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    var currentPrompt by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uid) {
        if (uid.isNotBlank()) viewModel.initializeChat(uid)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DebouncedIconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Insights", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { viewModel.clearChatHistory() }) {
                    Text("Clear chat")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isUser = message.author == MessageAuthor.TENANT
                    Surface(
                        color = if (isUser) VacancyBlue else InfoSurface,
                        contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (isUser) "You" else "Insight", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            if (isUser) Text(message.text) else MarkdownText(message.text)
                        }
                    }
                }
                item {
                    if (isThinking) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLight)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = currentPrompt,
                    onValueChange = { currentPrompt = it },
                    placeholder = { Text("Ask about your views, saves, and booking/conversion rate...") },
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (currentPrompt.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Type a question first.") }
                        } else {
                            viewModel.sendMessage(currentPrompt)
                            currentPrompt = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isThinking,
                    colors = ButtonDefaults.buttonColors(containerColor = VacancyBlue, contentColor = Color.White)
                ) {
                    Text(if (isThinking) "Thinking..." else "Get insight")
                }
            }
        }
    }
}

