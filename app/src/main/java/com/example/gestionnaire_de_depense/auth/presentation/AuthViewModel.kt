package com.example.gestionnaire_de_depense.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gestionnaire_de_depense.auth.domain.AuthException
import com.example.gestionnaire_de_depense.auth.domain.AuthNetworkException
import com.example.gestionnaire_de_depense.auth.domain.AuthRepository
import com.example.gestionnaire_de_depense.auth.domain.EmailAlreadyUsedException
import com.example.gestionnaire_de_depense.auth.domain.InvalidAuthRequestException
import com.example.gestionnaire_de_depense.auth.domain.InvalidAuthTokenException
import com.example.gestionnaire_de_depense.auth.domain.InvalidCredentialsException
import com.example.gestionnaire_de_depense.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Gère l'orchestration entre validation locale, dépôt réseau et état UI de l'écran d'authentification.
class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    // StateFlow privé pour que seule la ViewModel puisse modifier l'état de l'écran.
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    fun onEmailChanged(value: String) {
        _uiState.update {
            it.copy(email = value, errorMessage = null)
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(password = value, errorMessage = null)
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, errorMessage = null)
        }
    }

    fun toggleMode() {
        _uiState.update { current ->
            val newMode = if (current.mode == AuthMode.Login) AuthMode.Register else AuthMode.Login
            current.copy(
                mode = newMode,
                password = "",
                confirmPassword = "",
                errorMessage = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submit() {
        // On valide d'abord côté client pour éviter des allers-retours API inutiles.
        val snapshot = _uiState.value
        val validationError = validateInput(snapshot)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            // On expose un loader pessimiste pendant l'appel réseau.
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val email = snapshot.email.trim()
                val password = snapshot.password
                when (snapshot.mode) {
                    AuthMode.Login -> repository.login(email, password)
                    AuthMode.Register -> repository.register(email, password)
                }
                _uiState.update {
                    it.copy(
                        password = "",
                        confirmPassword = "",
                        errorMessage = null
                    )
                }
            } catch (authError: AuthException) {
                _uiState.update { it.copy(errorMessage = mapError(authError)) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(errorMessage = mapError(AuthNetworkException(throwable))) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    suspend fun logout() {
        repository.clearToken()
    }

    private fun observeAuthState() {
        // On écoute le DataStore pour propager automatiquement l'état d'authentification.
        viewModelScope.launch {
            repository.session.collect { session ->
                _uiState.update { current ->
                    current.copy(
                        isAuthenticated = session != null,
                        sessionEmail = session?.subject,
                        sessionExpiresAt = session?.expiresAt
                    )
                }
            }
        }
    }

    private fun validateInput(state: AuthUiState): String? {
        val email = state.email.trim()
        if (email.length > AuthUiState.MAX_EMAIL_LENGTH) {
            return "Adresse e-mail trop longue."
        }
        if (!AuthUiState.isValidEmail(email)) {
            return "Adresse e-mail invalide."
        }
        if (state.password.length < AuthUiState.MIN_PASSWORD_LENGTH) {
            return "Le mot de passe doit contenir au moins 8 caractères."
        }
        if (state.mode == AuthMode.Register && state.password != state.confirmPassword) {
            return "Les mots de passe ne correspondent pas."
        }
        return null
    }

    private fun mapError(error: AuthException): String {
        return when (error) {
            is InvalidCredentialsException -> "Identifiants invalides. Vérifiez vos informations ou utilisez l'option mot de passe oublié."
            is InvalidAuthRequestException -> "Adresse e-mail ou mot de passe invalide."
            is EmailAlreadyUsedException -> "Adresse déjà utilisée. Essayez de vous connecter ou utilisez l'option mot de passe oublié."
            is InvalidAuthTokenException -> "Votre session n'a pas pu être validée. Veuillez vous reconnecter."
            is AuthNetworkException -> "Service indisponible, réessayez plus tard."
            else -> "Une erreur est survenue. Réessayez."
        }
    }

    companion object {
        // Fabrique simple pour injecter le dépôt sans dépendre d'un framework DI.
        fun provideFactory(repository: AuthRepository) = viewModelFactory {
            initializer {
                AuthViewModel(repository)
            }
        }
    }
}
