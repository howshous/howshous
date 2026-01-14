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
import io.github.howshous.data.firestore.UserRepository
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.viewmodels.AccountViewModel
import io.github.howshous.ui.data.readUidFlow
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(nav: NavController) {
    val context = LocalContext.current
    val uid by readUidFlow(context).collectAsState(initial = "")
    val viewModel: AccountViewModel = viewModel()
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load existing data
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            viewModel.loadUserProfile(uid)
        }
    }

    // Populate fields from loaded profile
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            firstName = profile.firstName
            lastName = profile.lastName
            phone = profile.phone ?: ""
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Edit Profile", style = MaterialTheme.typography.titleMedium)
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    shape = InputShape,
                    colors = inputColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (uid.isNotEmpty()) {
                            isSaving = true
                            scope.launch {
                                try {
                                    val userRepository = UserRepository()
                                    userRepository.updateUserProfile(uid, mapOf(
                                        "firstName" to firstName,
                                        "lastName" to lastName,
                                        "phone" to phone
                                    ))
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = firstName.isNotEmpty() && lastName.isNotEmpty() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
