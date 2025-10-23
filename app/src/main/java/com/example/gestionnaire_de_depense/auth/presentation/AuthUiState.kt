package com.example.gestionnaire_de_depense.auth.presentation

import com.example.gestionnaire_de_depense.ui.auth.AuthMode
import java.time.Instant
import java.util.regex.Pattern

// Conteneur unique pour l'état de l'écran d'authentification afin de garder la logique UI prévisible.
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val mode: AuthMode = AuthMode.Login,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val sessionEmail: String? = null,
    val sessionExpiresAt: Instant? = null
) {
    val isEmailValid: Boolean
        get() = isValidEmail(email)

    val isPasswordValid: Boolean
        get() = password.length >= MIN_PASSWORD_LENGTH

    val isConfirmPasswordValid: Boolean
        get() = mode == AuthMode.Login || (confirmPassword.isNotBlank() && confirmPassword == password)

    val canSubmit: Boolean
        get() = !isLoading && isEmailValid && isPasswordValid && isConfirmPasswordValid

    companion object {
        internal const val MIN_PASSWORD_LENGTH = 8
        internal const val MAX_EMAIL_LENGTH = 255

        private val emailPattern: Pattern = Pattern.compile(
            "^[A-Za-z0-9+._%\\-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )

        // Centralise la validation e-mail pour qu'elle soit partagée entre la UI et le ViewModel.
        fun isValidEmail(email: String): Boolean {
            return email.length <= MAX_EMAIL_LENGTH && emailPattern.matcher(email).matches()
        }
    }
}
