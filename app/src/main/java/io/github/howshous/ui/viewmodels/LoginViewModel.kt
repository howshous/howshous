package io.github.howshous.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.github.howshous.data.auth.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    fun login(
        email: String,
        password: String,
        context: Context,
        nav: NavController
    ) {
        viewModelScope.launch {
            val repo = AuthRepository(context)

            val result = repo.signInWithEmail(email, password)

            result.onSuccess { uid ->
                nav.navigate("dashboard_router") {
                    popUpTo("login_choice") { inclusive = true }
                    launchSingleTop = true
                }
            }

            result.onFailure {
                it.printStackTrace()
                // optional TODO: show UI error
            }
        }
    }
}
