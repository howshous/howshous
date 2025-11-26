package io.github.howshous.ui.screens.signup.landlord

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.theme.LandlordBlue
import io.github.howshous.ui.viewmodels.SignupViewModel
import kotlinx.coroutines.launch

@Composable
fun LandlordSignupComplete(nav: NavController, signupVM: SignupViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LandlordBlue)
                .padding(32.dp)
                .padding(padding)
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {

                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.i_back),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Image(
                    painter = painterResource(R.drawable.logo_white),
                    contentDescription = null,
                    modifier = Modifier.size(150.dp)
                )

            Spacer(Modifier.height(8.dp))

            Text("Landlord", color = Color.White, style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.spr_landlord_all_done),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "All Set!",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Verification may take some time but\nyou can immediately login.",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        scope.launch {
                            isSubmitting = true
                            val result = signupVM.finishLandlordSignup(
                                context = context
                            )
                            isSubmitting = false
                            result.onSuccess {
                                signupVM.clearAll()
                                nav.navigate("login") {
                                    popUpTo("login_choice") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }.onFailure { error ->
                                snackbarHostState.showSnackbar(
                                    message = error.localizedMessage ?: "Failed to finish signup."
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = LandlordBlue
                    ),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = LandlordBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("Login")
                    }
                }
            }
        }
    }
}