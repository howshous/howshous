package io.github.howshous.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.theme.PrimaryTeal
import io.github.howshous.ui.theme.PrimaryTealLight
import io.github.howshous.ui.theme.NearWhite
import io.github.howshous.ui.util.SampleAccountSeeder
import kotlinx.coroutines.launch

@Composable
fun LoginChoice(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSeeding by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PrimaryTealLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .background(PrimaryTealLight)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(PrimaryTeal),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_white),
                    contentDescription = "HowsHous logo",
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(Modifier.height(48.dp))

            // Login button (50% width)
            Button(
                onClick = { nav.navigate("login") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = NearWhite
                )
            ) {
                Text("Login")
            }

            Spacer(Modifier.height(24.dp))

            // Sign up button (outlined, 50%)
            OutlinedButton(
                onClick = { nav.navigate("signup") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryTeal
                ),
                border = BorderStroke(3.dp, PrimaryTeal)
            ) {
                Text("Sign Up")
            }

            Spacer(Modifier.height(20.dp))

            OutlinedButton(
                onClick = {
                    if (isSeeding) return@OutlinedButton
                    isSeeding = true
                    scope.launch {
                        val result = SampleAccountSeeder.generateIfMissing(context)
                        snackbarHostState.showSnackbar(result.message)
                        isSeeding = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp),
                enabled = !isSeeding
            ) {
                if (isSeeding) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Text("Generate Sample Data")
                }
            }
        }
    }
}
