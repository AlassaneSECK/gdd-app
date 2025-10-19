package com.example.gestionnaire_de_depense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.gestionnaire_de_depense.auth.presentation.AuthUiState
import com.example.gestionnaire_de_depense.auth.presentation.AuthViewModel
import com.example.gestionnaire_de_depense.di.AppContainer
import com.example.gestionnaire_de_depense.ui.auth.AuthContent
import com.example.gestionnaire_de_depense.ui.auth.AuthMode
import com.example.gestionnaire_de_depense.ui.auth.AuthScreen
import com.example.gestionnaire_de_depense.ui.home.HomeScreen
import com.example.gestionnaire_de_depense.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val appContainer: AppContainer by lazy {
        AppContainer(applicationContext)
    }

    private val authViewModel: AuthViewModel by lazy {
        ViewModelProvider(this, appContainer.authViewModelFactory)[AuthViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                // NavController Compose : il pilote les changements d'écran (auth -> home).
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Destinations.AUTH
                ) {
                    composable(Destinations.AUTH) {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthenticated = {
                                // Une fois le token stocké, on remonte sur l'écran d'accueil et on retire l'écran d'auth de la pile.
                                navController.navigate(Destinations.HOME) {
                                    popUpTo(Destinations.AUTH) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Destinations.HOME) {
                        val state by authViewModel.uiState.collectAsStateWithLifecycle()
                        val coroutineScope = rememberCoroutineScope()
                        LaunchedEffect(state.isAuthenticated) {
                            if (!state.isAuthenticated) {
                                navController.navigate(Destinations.AUTH) {
                                    popUpTo(Destinations.HOME) { inclusive = true }
                                }
                            }
                        }
                        HomeScreen(
                            sessionEmail = state.sessionEmail,
                            sessionExpiresAt = state.sessionExpiresAt,
                            onLogout = {
                                coroutineScope.launch {
                                    authViewModel.logout()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Centralise les routes Navigation Compose pour éviter les chaînes magiques.
private object Destinations {
    const val AUTH = "auth"
    const val HOME = "home"
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    AppTheme {
        AuthContent(
            state = AuthUiState(
                mode = AuthMode.Login
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onModeToggle = {},
            onErrorDismissed = {}
        )
    }
}
