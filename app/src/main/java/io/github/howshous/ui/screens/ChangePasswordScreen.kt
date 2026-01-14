package io.github.howshous.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.SurfaceLight
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors
import kotlinx.coroutines.launch
import io.github.howshous.R

@Composable
fun ChangePasswordScreen(nav: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

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
            Text("Change Password", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Current Password
            OutlinedTextField(
                value = currentPassword,
                onValueChange = {
                    currentPassword = it
                    errorMessage = ""
                },
                label = { Text("Current Password") },
                shape = InputShape,
                colors = inputColors(),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                        Icon(
                            painter = painterResource(
                                id = if (showCurrentPassword)
                                    R.drawable.i_eyeopen
                                else
                                    R.drawable.i_eyeclosed
                            ),
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            // New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    errorMessage = ""
                },
                label = { Text("New Password") },
                shape = InputShape,
                colors = inputColors(),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showNewPassword = !showNewPassword }) {
                        Icon(
                            painter = painterResource(
                                id = if (showNewPassword)
                                    R.drawable.i_eyeopen
                                else
                                    R.drawable.i_eyeclosed
                            ),
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            // Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = ""
                },
                label = { Text("Confirm Password") },
                shape = InputShape,
                colors = inputColors(),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            painter = painterResource(
                                id = if (showConfirmPassword)
                                    R.drawable.i_eyeopen
                                else
                                    R.drawable.i_eyeclosed
                            ),
                            contentDescription = "Toggle password visibility"
                        )
                    }
                }
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> errorMessage = "Enter current password"
                        newPassword.isEmpty() -> errorMessage = "Enter new password"
                        confirmPassword.isEmpty() -> errorMessage = "Confirm new password"
                        newPassword != confirmPassword -> errorMessage = "Passwords do not match"
                        newPassword.length < 6 -> errorMessage = "Password must be at least 6 characters"
                        else -> {
                            isLoading = true
                            scope.launch {
                                try {
                                    val user = auth.currentUser
                                    if (user != null && user.email != null) {
                                        // Re-authenticate with current password
                                        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                                        user.reauthenticate(credential).addOnSuccessListener {
                                            // Now update password
                                            user.updatePassword(newPassword).addOnSuccessListener {
                                                isLoading = false
                                                nav.popBackStack()
                                            }.addOnFailureListener { e ->
                                                errorMessage = "Failed to change password: ${e.message}"
                                                isLoading = false
                                            }
                                        }.addOnFailureListener { e ->
                                            errorMessage = "Current password is incorrect"
                                            isLoading = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Error: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Change Password")
                }
            }
        }
    }
}
