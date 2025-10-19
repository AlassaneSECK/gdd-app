package com.example.gestionnaire_de_depense.auth.presentation

import com.example.gestionnaire_de_depense.auth.domain.AuthRepository
import com.example.gestionnaire_de_depense.auth.domain.AuthSession
import com.example.gestionnaire_de_depense.auth.domain.EmailAlreadyUsedException
import com.example.gestionnaire_de_depense.auth.domain.InvalidCredentialsException
import com.example.gestionnaire_de_depense.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.time.Instant

class AuthViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun submitLoginSuccessShouldSetAuthenticated() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAuthenticated)
        assertEquals(null, state.errorMessage)
        assertEquals("user@example.com", state.sessionEmail)
        assertNotNull(state.sessionExpiresAt)
    }

    @Test
    fun submitWithInvalidEmailShouldExposeError() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("password123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Adresse e-mail invalide.", state.errorMessage)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun registerPasswordMismatchShouldExposeError() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.toggleMode() // Passer en mode inscription
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password124")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Les mots de passe ne correspondent pas.", state.errorMessage)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun loginUnauthorizedShouldSurfaceInvalidCredentials() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeAuthRepository(nextError = InvalidCredentialsException())
        val viewModel = AuthViewModel(repository)

        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Identifiants invalides. Vérifiez vos informations ou utilisez l'option mot de passe oublié.",
            state.errorMessage
        )
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun registerConflictShouldSurfaceSpecificMessage() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeAuthRepository(nextError = EmailAlreadyUsedException())
        val viewModel = AuthViewModel(repository)

        viewModel.toggleMode()
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")

        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Adresse déjà utilisée. Essayez de vous connecter ou utilisez l'option mot de passe oublié.",
            state.errorMessage
        )
        assertFalse(state.isAuthenticated)
    }

    private class FakeAuthRepository(
        private var nextError: Throwable? = null
    ) : AuthRepository {

        private val _session = MutableStateFlow<AuthSession?>(null)
        private val _authState = MutableStateFlow(false)
        override val session: Flow<AuthSession?> = _session
        override val authState: Flow<Boolean> = _authState

        override suspend fun login(email: String, password: String) {
            nextError?.let { error ->
                nextError = null
                throw error
            }
            _session.value = buildSession(email)
            _authState.value = true
        }

        override suspend fun register(email: String, password: String) {
            nextError?.let { error ->
                nextError = null
                throw error
            }
            _session.value = buildSession(email)
            _authState.value = true
        }

        override suspend fun clearToken() {
            _session.value = null
            _authState.value = false
        }

        override suspend fun currentSession(): AuthSession? = _session.value

        override suspend fun currentToken(): String? = _session.value?.token

        private fun buildSession(email: String): AuthSession {
            val issuedAt = Instant.now()
            return AuthSession(
                token = "token-$email",
                subject = email,
                issuedAt = issuedAt,
                expiresAt = issuedAt.plusSeconds(3600)
            )
        }
    }
}
