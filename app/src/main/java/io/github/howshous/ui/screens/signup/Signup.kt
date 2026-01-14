package io.github.howshous.ui.screens.signup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.LandlordBlue
import io.github.howshous.ui.theme.TenantGreen
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.theme.lighterGray
import io.github.howshous.ui.theme.slightlyGray
import io.github.howshous.ui.viewmodels.SignupViewModel
import io.github.howshous.utils.ValidationUtils

@Composable
fun Signup(nav: NavController, signupVM: SignupViewModel) {

    val first by signupVM.firstName.collectAsState()
    val last by signupVM.lastName.collectAsState()
    val contactValue by signupVM.contact.collectAsState()
    val passwordValue by signupVM.password.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val firstValid = first.trim().isNotEmpty()
    val lastValid = last.trim().isNotEmpty()
    val contactTrimmed = contactValue.trim()
    val contactValid = contactTrimmed.isNotEmpty() && (
        ValidationUtils.isValidEmail(contactTrimmed) || ValidationUtils.isValidPhone(contactTrimmed)
    )
    val passwordValid = ValidationUtils.isValidPassword(passwordValue)
    val canContinue = firstValid && lastValid && contactValid && passwordValid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TenantGreen,
                        LandlordBlue
                    )
                )
            )
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {

            // Back → always login_choice
            Row(modifier = Modifier.fillMaxWidth()) {
                DebouncedIconButton(onClick = {
                    nav.navigate("login_choice") { launchSingleTop = true }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.i_back),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_white),
                contentDescription = "HowsHaus logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(Modifier.height(24.dp))

            // FIRST NAME
            OutlinedTextField(
                value = first,
                onValueChange = { signupVM.setFirstName(it) },
                placeholder = { Text("First Name") },
                singleLine = true,
                shape = InputShape,
                colors = inputColors(),
                isError = showErrors && !firstValid,
                supportingText = {
                    if (showErrors && !firstValid) {
                        Text("First name is required.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // LAST NAME
            OutlinedTextField(
                value = last,
                onValueChange = { signupVM.setLastName(it) },
                placeholder = { Text("Last Name") },
                singleLine = true,
                shape = InputShape,
                colors = inputColors(),
                isError = showErrors && !lastValid,
                supportingText = {
                    if (showErrors && !lastValid) {
                        Text("Last name is required.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // EMAIL / PHONE
            OutlinedTextField(
                value = contactValue,
                onValueChange = { signupVM.setContact(it) },
                placeholder = { Text("Email/Phone Number") },
                singleLine = true,
                shape = InputShape,
                colors = inputColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = showErrors && !contactValid,
                supportingText = {
                    if (showErrors && !contactValid) {
                        Text("Enter a valid email or 10-digit phone number.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // PASSWORD
            OutlinedTextField(
                value = passwordValue,
                onValueChange = { signupVM.setPassword(it) },
                placeholder = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(
                                id = if (passwordVisible) R.drawable.i_eyeopen else R.drawable.i_eyeclosed
                            ),
                            contentDescription = "Toggle password visibility",
                            tint = slightlyGray
                        )
                    }
                },
                shape = InputShape,
                colors = inputColors(),
                isError = showErrors && !passwordValid,
                supportingText = {
                    if (showErrors && !passwordValid) {
                        Text("Password must be at least 6 characters.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!canContinue) {
                        showErrors = true
                        return@Button
                    }
                    nav.navigate("signup_crossroads")
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = LandlordBlue
                )
            ) {
                Text("Continue")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { nav.navigate("login") },
                border = BorderStroke(2.dp, lighterGray),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = lighterGray
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
            ) {
                Text("I already have an account.")
            }
        }
    }
}
