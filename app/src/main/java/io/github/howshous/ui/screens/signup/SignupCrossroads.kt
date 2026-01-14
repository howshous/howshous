package io.github.howshous.ui.screens.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.R
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.LandlordBlue
import io.github.howshous.ui.theme.LandlordBlueAlt
import io.github.howshous.ui.theme.TenantGreen
import io.github.howshous.ui.theme.TenantGreenAlt
import io.github.howshous.ui.viewmodels.SignupViewModel

@Composable
fun SignupCrossroads(nav: NavController, signupVM: SignupViewModel) {

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
            .padding(32.dp)
    ) {

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

            Image(
                painter = painterResource(id = R.drawable.logo_white),
                contentDescription = "HowsHaus logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(Modifier.height(32.dp))

            // TENANT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 8.dp)
                    .clickable {
                        signupVM.setRole("tenant")
                        nav.navigate("tenant_su_1")
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TenantGreenAlt
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sus_tenant_icon),
                        contentDescription = "Tenant icon",
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("Tenant", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Search for houses to rent, receive payment notices, access to emergency tools",
                            color = Color.White
                        )
                    }
                }
            }

            // LANDLORD CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 8.dp)
                    .clickable {
                        signupVM.setRole("landlord")
                        nav.navigate("landlord_su_1")
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LandlordBlueAlt
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sus_landlord_icon),
                        contentDescription = "Landlord icon",
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("Landlord", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "List and manage housing for rent, keep track of tenants' dues and concerns",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
