package io.github.howshous.ui.screens.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.theme.InputShape
import io.github.howshous.ui.theme.inputColors
import io.github.howshous.ui.theme.PrimaryTeal
import io.github.howshous.ui.theme.slightlyGray
import io.github.howshous.ui.theme.lighterGray
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.howshous.ui.viewmodels.LoginViewModel
import io.github.howshous.ui.components.DebouncedIconButton

@Composable
fun Login(nav: NavController) {
    val loginVM: LoginViewModel = viewModel()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryTeal)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // BACK BUTTON → login_choice
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

        // LOGO
        Image(
            painter = painterResource(id = R.drawable.logo_white),
            contentDescription = "HowsHous logo",
            modifier = Modifier.size(150.dp)
        )

        Spacer(Modifier.height(32.dp))

        // EMAIL FIELD
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email/Phone") },
            singleLine = true,
            shape = InputShape,
            colors = inputColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // PASSWORD FIELD
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            singleLine = true,
            shape = InputShape,
            colors = inputColors(),
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        painter = painterResource(
                            id = if (passwordVisible)
                                R.drawable.i_eyeopen
                            else
                                R.drawable.i_eyeclosed
                        ),
                        contentDescription = "Toggle password visibility",
                        tint = slightlyGray
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                loginVM.login(
                email = email,
                password = password,
                context = context,
                nav = nav
            ) },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = PrimaryTeal
            )
        ) {
            Text("Login")
        }

        Spacer(Modifier.height(24.dp))


        OutlinedButton(
            onClick = { nav.navigate("signup") },
            border = BorderStroke(2.dp, lighterGray),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = lighterGray
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp)
        ) {
            Text("I don't have an account.")
        }
    }
}
