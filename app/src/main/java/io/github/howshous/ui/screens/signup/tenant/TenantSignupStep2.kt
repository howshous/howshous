package io.github.howshous.ui.screens.signup.tenant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.TenantGreen
import io.github.howshous.ui.theme.TenantGreenDark
import io.github.howshous.ui.util.saveBitmapToCache
import io.github.howshous.ui.viewmodels.SignupViewModel

@Composable
fun TenantSignupStep2(nav: NavController, signupVM: SignupViewModel) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TenantGreen)
            .padding(32.dp)
    ) {
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                val uri = saveBitmapToCache(context, bitmap)
                selectedImageUri = uri
            }
        }

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            // Back
            Row(modifier = Modifier.fillMaxWidth()) {
                DebouncedIconButton(onClick = { nav.popBackStack() }) {
                    Icon(
                        painter = painterResource(R.drawable.i_back),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Logo
            Image(
                painter = painterResource(R.drawable.logo_white),
                contentDescription = "HowsHous logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Tenant",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(16.dp))

            // Majestic Tenant ID
            Image(
                painter = painterResource(R.drawable.spr_tenant_id),
                contentDescription = "An illustration representing an ID",
                modifier = Modifier.size(180.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Upload a photo of your valid ID.",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // OPEN CAMERA button
            Button(
                onClick = { cameraLauncher.launch() },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TenantGreenDark,
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.i_camera),
                        contentDescription = "A camera",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Open Camera")
                }
            }

            Spacer(Modifier.height(16.dp))

            // OR divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                HorizontalDivider(
                    color = Color.White,
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "  OR  ",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(
                    color = Color.White,
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Upload Photo
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TenantGreenDark,
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.i_gallery),
                        contentDescription = "A gallery",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Photo")
                }
            }

            Spacer(Modifier.height(40.dp))

            OutlinedButton(
                onClick = {
                    if (selectedImageUri != null) {
                        signupVM.setTenantImage(step = 2, uri = selectedImageUri!!)
                        nav.navigate("tenant_su_complete")
                    }
                },
                enabled = selectedImageUri != null,
                border = BorderStroke(2.dp, if (selectedImageUri != null) Color.White else Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Transparent,
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
            ) {
                Text("Next")
            }
        }
    }
}
