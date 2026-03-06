import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import io.github.howshous.ui.screens.shells.LandlordMainShell
import io.github.howshous.ui.screens.shells.TenantMainShell
import io.github.howshous.ui.screens.shells.AdminMainShell

@Composable
fun DashboardRouter(
    nav: NavHostController,
    role: String
) {
    when (role) {
        "tenant" -> TenantMainShell(nav)
        "landlord" -> LandlordMainShell(nav)
        "administrator" -> AdminMainShell(nav)
    }
}
