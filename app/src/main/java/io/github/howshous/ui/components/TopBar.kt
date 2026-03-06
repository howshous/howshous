package io.github.howshous.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.howshous.R
import io.github.howshous.ui.theme.AdminPower
import io.github.howshous.ui.theme.LandlordBlue
import io.github.howshous.ui.theme.TenantGreen

@Composable
fun TopBar(
    role: String,
    onSettingsClick: () -> Unit = {}
) {
    val backgroundColor = when (role) {
        "tenant" -> TenantGreen
        "landlord" -> LandlordBlue
        "administrator" -> AdminPower
        else -> TenantGreen
    }
    
    val roleDisplayName = when (role) {
        "tenant" -> "Tenant"
        "landlord" -> "Landlord"
        "administrator" -> "Administrator"
        else -> "User"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Logo + Role title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.logo_white),
                    contentDescription = "HowsHous Logo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "HowsHous $roleDisplayName",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Right: Settings icon (clickable)
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.i_settings),
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
