package io.github.howshous.ui.screens.shells

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.howshous.R
import io.github.howshous.ui.components.BottomNavBar
import io.github.howshous.ui.components.BottomNavItem
import io.github.howshous.ui.components.TopBar
import io.github.howshous.ui.screens.main_admin.AdminAccount
import io.github.howshous.ui.screens.main_admin.AdminHome
import io.github.howshous.ui.screens.main_admin.AdminListings
import io.github.howshous.ui.screens.main_admin.AdminReviewQueue
import io.github.howshous.ui.screens.main_admin.AdminUsers

@Composable
fun AdminMainShell(rootNav: NavHostController) {
    val innerNav = rememberNavController()
    var selectedRoute by remember { mutableStateOf("admin_home") }

    val bottomNavItems = listOf(
        BottomNavItem("Home", R.drawable.i_home_0, R.drawable.i_home_1, "admin_home"),
        BottomNavItem("Review", R.drawable.i_list_0, R.drawable.i_list_1, "admin_review"),
        BottomNavItem("Listings", R.drawable.i_search_0, R.drawable.i_search_1, "admin_listings"),
        BottomNavItem("Users", R.drawable.i_bell_0, R.drawable.i_bell_1, "admin_users"),
        BottomNavItem("Account", R.drawable.i_account_0, R.drawable.i_account_1, "admin_account")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            role = "administrator",
            onSettingsClick = { rootNav.navigate("settings") }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            NavHost(
                navController = innerNav,
                startDestination = "admin_home"
            ) {
                composable("admin_home") {
                    selectedRoute = "admin_home"
                    AdminHome()
                }
                composable("admin_review") {
                    selectedRoute = "admin_review"
                    AdminReviewQueue(rootNav)
                }
                composable("admin_listings") {
                    selectedRoute = "admin_listings"
                    AdminListings(rootNav)
                }
                composable("admin_users") {
                    selectedRoute = "admin_users"
                    AdminUsers()
                }
                composable("admin_account") {
                    selectedRoute = "admin_account"
                    AdminAccount()
                }
            }
        }

        BottomNavBar(
            items = bottomNavItems,
            selectedRoute = selectedRoute,
            onItemClick = { route ->
                selectedRoute = route
                innerNav.navigate(route) {
                    popUpTo("admin_home") { inclusive = false }
                    launchSingleTop = true
                }
            },
            role = "administrator"
        )
    }
}
