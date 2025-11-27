package io.github.howshous.ui.components.navigation

import DashboardRouter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.howshous.ui.data.readRoleFlow
import io.github.howshous.ui.screens.Splash
import io.github.howshous.ui.screens.LoginChoice
import io.github.howshous.ui.screens.login.Login
import io.github.howshous.ui.screens.signup.SignupCrossroads
import io.github.howshous.ui.screens.signup.Signup
import io.github.howshous.ui.screens.signup.landlord.LandlordSignupComplete
import io.github.howshous.ui.screens.signup.landlord.LandlordSignupStep1
import io.github.howshous.ui.screens.signup.landlord.LandlordSignupStep2
import io.github.howshous.ui.screens.signup.landlord.LandlordSignupStep3
import io.github.howshous.ui.screens.signup.tenant.TenantSignupComplete
import io.github.howshous.ui.screens.signup.tenant.TenantSignupStep1
import io.github.howshous.ui.screens.signup.tenant.TenantSignupStep2
import io.github.howshous.ui.screens.SettingsScreen
import io.github.howshous.ui.screens.ChatDetailScreen
import io.github.howshous.ui.screens.EditProfileScreen
import io.github.howshous.ui.screens.ChangePasswordScreen
import io.github.howshous.ui.screens.CreateListingScreen
import io.github.howshous.ui.screens.InitiateChatScreen
import io.github.howshous.ui.screens.ListingDetailScreen
import io.github.howshous.ui.screens.main_tenant.TenantAIHelperScreen
import io.github.howshous.ui.screens.main_tenant.ViewContractsScreen
import io.github.howshous.ui.screens.main_tenant.EmergencyScreen
import io.github.howshous.ui.screens.main_tenant.ReportIssueScreen
import io.github.howshous.ui.viewmodels.SignupViewModel

@Composable
fun HowsHousApp(nav: NavHostController = rememberNavController()) {

    val signupVM: SignupViewModel = viewModel()

    NavHost(
        navController = nav,
        startDestination = "splash"
    ) {
        composable("splash") { Splash(nav) }

        // Login / Signup
        composable("login_choice") { LoginChoice(nav) }

        // Login
        composable("login") { Login(nav) }

        // Signup

        composable("signup") { Signup(nav, signupVM) }
        composable("signup_crossroads") { SignupCrossroads(nav, signupVM) }

        // Tenant signup steps
        composable("tenant_su_1") { TenantSignupStep1(nav, signupVM) }
        composable("tenant_su_2") { TenantSignupStep2(nav, signupVM) }
        composable("tenant_su_complete") { TenantSignupComplete(nav, signupVM) }

        // Landlord signup steps
        composable("landlord_su_1") { LandlordSignupStep1(nav, signupVM) }
        composable("landlord_su_2") { LandlordSignupStep2(nav, signupVM) }
        composable("landlord_su_3") { LandlordSignupStep3(nav, signupVM) }
        composable("landlord_su_complete") { LandlordSignupComplete(nav, signupVM) }

        composable("dashboard_router") {
            val context = LocalContext.current
            val role = readRoleFlow(context).collectAsState(initial = "").value
            DashboardRouter(nav, role)
        }

        composable("settings") { SettingsScreen(nav) }

        composable("edit_profile") { EditProfileScreen(nav) }

        composable("change_password") { ChangePasswordScreen(nav) }

        composable("create_listing") { CreateListingScreen(nav) }

        composable("listing/{listingId}") { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            ListingDetailScreen(nav, listingId)
        }

        composable("tenant_ai_helper") {
            TenantAIHelperScreen(nav)
        }

        composable("initiate_chat/{listingId}/{landlordId}") { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val landlordId = backStackEntry.arguments?.getString("landlordId") ?: ""
            InitiateChatScreen(nav, listingId, landlordId)
        }

        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(nav, chatId)
        }

        composable("view_contracts") {
            ViewContractsScreen(nav)
        }

        composable("emergency") {
            EmergencyScreen(nav)
        }

        composable("report_issue") {
            ReportIssueScreen(nav)
        }

        composable("issue_detail/{issueId}") { backStackEntry ->
            val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
            io.github.howshous.ui.screens.main_landlord.IssueDetailScreen(nav, issueId)
        }
    }
}