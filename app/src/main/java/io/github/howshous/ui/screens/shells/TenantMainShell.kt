package io.github.howshous.ui.screens.shells

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.howshous.R
import io.github.howshous.ui.components.BottomNavBar
import io.github.howshous.ui.components.BottomNavItem
import io.github.howshous.ui.components.TopBar
import io.github.howshous.ui.screens.main_tenant.*

@Composable
fun TenantMainShell(rootNav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val innerNav = rememberNavController()
    var selectedRoute by remember { mutableStateOf("tenant_home") }

    val bottomNavItems = listOf(
        BottomNavItem("Home", R.drawable.i_home_0, R.drawable.i_home_1, "tenant_home"),
        BottomNavItem("Search", R.drawable.i_search_0, R.drawable.i_search_1, "tenant_search"),
        BottomNavItem("Contact", R.drawable.i_message_0, R.drawable.i_message_1, "tenant_contact"),
        BottomNavItem("Alerts", R.drawable.i_bell_0, R.drawable.i_bell_1, "tenant_notifications"),
        BottomNavItem("Account", R.drawable.i_account_0, R.drawable.i_account_1, "tenant_account")
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Bar
        TopBar(
            role = "tenant",
            onSettingsClick = {
                rootNav.navigate("settings")
            }
        )

        // Body - Inner NavHost
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            NavHost(
                navController = innerNav,
                startDestination = "tenant_home"
            ) {
                composable("tenant_home") {
                    selectedRoute = "tenant_home"
                    TenantHome(rootNav)
                }
                composable("tenant_search") {
                    selectedRoute = "tenant_search"
                    TenantSearch(rootNav)
                }
                composable("tenant_contact") {
                    selectedRoute = "tenant_contact"
                    TenantChatList(rootNav)
                }
                composable("tenant_notifications") {
                    selectedRoute = "tenant_notifications"
                    TenantNotifications(rootNav)
                }
                composable("tenant_account") {
                    selectedRoute = "tenant_account"
                    TenantAccount(rootNav)
                }
            }
        }

        // Bottom Nav Bar
        BottomNavBar(
            items = bottomNavItems,
            selectedRoute = selectedRoute,
            onItemClick = { route ->
                selectedRoute = route
                innerNav.navigate(route) {
                    popUpTo("tenant_home") { inclusive = false }
                    launchSingleTop = true
                }
            },
            role = "tenant"
        )
    }
}
