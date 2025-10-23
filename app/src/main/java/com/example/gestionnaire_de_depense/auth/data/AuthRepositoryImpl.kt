package com.example.gestionnaire_de_depense.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gestionnaire_de_depense.auth.domain.AuthNetworkException
import com.example.gestionnaire_de_depense.auth.domain.AuthRepository
import com.example.gestionnaire_de_depense.auth.domain.AuthSession
import com.example.gestionnaire_de_depense.auth.domain.EmailAlreadyUsedException
import com.example.gestionnaire_de_depense.auth.domain.InvalidAuthRequestException
import com.example.gestionnaire_de_depense.auth.domain.InvalidAuthTokenException
import com.example.gestionnaire_de_depense.auth.domain.InvalidCredentialsException
import com.example.gestionnaire_de_depense.auth.network.AuthApiService
import com.example.gestionnaire_de_depense.auth.network.model.AuthRequest
import com.example.gestionnaire_de_depense.auth.network.model.AuthResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import retrofit2.HttpException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.util.Base64

// Implémentation concrète du dépôt : délègue l'appel réseau et gère la persistance du token dans DataStore.
class AuthRepositoryImpl(
    private val api: AuthApiService,
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    private val tokenKey = stringPreferencesKey("auth_token")
    private val subjectKey = stringPreferencesKey("auth_subject")
    private val issuedAtKey = longPreferencesKey("auth_issued_at")
    private val expiresAtKey = longPreferencesKey("auth_expires_at")
    private val jwtJson: Json = Json { ignoreUnknownKeys = true }

    override val session: Flow<AuthSession?> = combine(
        dataStore.data,
        expirationTicker()
    ) { preferences, _ ->
        preferences.toSession()
    }
        .map { session ->
            session?.takeUnless { it.isExpired(Instant.now(), EXPIRATION_MARGIN) }
        }
        .distinctUntilChanged()

    // Stream booléen qui indique si une session valide est connue en local ; consommé par la ViewModel.
    override val authState: Flow<Boolean> = session
        .map { it != null }
        .distinctUntilChanged()

    override suspend fun login(email: String, password: String) {
        performAuthCall { api.login(AuthRequest(email = email, password = password)) }
    }

    override suspend fun register(email: String, password: String) {
        performAuthCall { api.register(AuthRequest(email = email, password = password)) }
    }

    override suspend fun clearToken() {
        withContext(ioDispatcher) { clearPersistedSession() }
    }

    override suspend fun currentSession(): AuthSession? = withContext(ioDispatcher) {
        val preferences = dataStore.data.first()
        val session = preferences.toSession() ?: return@withContext null

        if (session.isExpired(Instant.now(), EXPIRATION_MARGIN)) {
            clearPersistedSession()
            return@withContext null
        }
        session
    }

    override suspend fun currentToken(): String? = currentSession()?.token

    private suspend fun performAuthCall(block: suspend () -> AuthResponse) {
        // Encapsule la logique commune login/register : appel Retrofit + sauvegarde du token.
        return withContext(ioDispatcher) {
            try {
                val response = block()
                val session = parseSession(response.token)
                if (session.isExpired(Instant.now(), EXPIRATION_MARGIN)) {
                    throw InvalidAuthTokenException()
                }
                persistSession(session)
            } catch (error: Throwable) {
                throw mapToDomainError(error)
            }
        }
    }

    private fun mapToDomainError(throwable: Throwable): Throwable {
        return when (throwable) {
            is HttpException -> when (throwable.code()) {
                400 -> InvalidAuthRequestException()
                401 -> InvalidCredentialsException()
                409 -> EmailAlreadyUsedException()
                else -> AuthNetworkException(throwable)
            }

            is IOException, is SerializationException -> AuthNetworkException(throwable)
            else -> throwable
        }
    }

    private suspend fun persistSession(session: AuthSession) {
        dataStore.edit { preferences ->
            preferences[tokenKey] = session.token
            preferences[subjectKey] = session.subject
            preferences[issuedAtKey] = session.issuedAt.toEpochMilli()
            preferences[expiresAtKey] = session.expiresAt.toEpochMilli()
        }
    }

    private suspend fun clearPersistedSession() {
        dataStore.edit { preferences ->
            preferences.remove(tokenKey)
            preferences.remove(subjectKey)
            preferences.remove(issuedAtKey)
            preferences.remove(expiresAtKey)
        }
    }

    private fun Preferences.toSession(): AuthSession? {
        val token = this[tokenKey] ?: return null
        val subject = this[subjectKey] ?: return null
        val issuedAtMillis = this[issuedAtKey] ?: return null
        val expiresAtMillis = this[expiresAtKey] ?: return null

        return try {
            val issuedAt = Instant.ofEpochMilli(issuedAtMillis)
            val expiresAt = Instant.ofEpochMilli(expiresAtMillis)
            AuthSession(
                token = token,
                subject = subject,
                issuedAt = issuedAt,
                expiresAt = expiresAt
            )
        } catch (error: DateTimeException) {
            null
        }
    }

    private fun parseSession(token: String): AuthSession {
        val segments = token.split(".")
        if (segments.size != 3) {
            throw InvalidAuthTokenException()
        }
        val payloadJson = decodeBase64Url(segments[1])
        return try {
            val payload = jwtJson.parseToJsonElement(payloadJson).jsonObject
            val subject = payload["sub"]?.jsonPrimitive?.contentOrNull ?: throw InvalidAuthTokenException()
            val issuedAtSeconds = payload["iat"]?.jsonPrimitive?.longOrNull ?: throw InvalidAuthTokenException()
            val expiresAtSeconds = payload["exp"]?.jsonPrimitive?.longOrNull ?: throw InvalidAuthTokenException()
            val issuedAt = Instant.ofEpochSecond(issuedAtSeconds)
            val expiresAt = Instant.ofEpochSecond(expiresAtSeconds)

            AuthSession(
                token = token,
                subject = subject,
                issuedAt = issuedAt,
                expiresAt = expiresAt
            )
        } catch (error: SerializationException) {
            throw InvalidAuthTokenException(error)
        } catch (error: IllegalArgumentException) {
            throw InvalidAuthTokenException(error)
        } catch (error: DateTimeException) {
            throw InvalidAuthTokenException(error)
        }
    }

    private fun decodeBase64Url(segment: String): String {
        return try {
            val padding = (4 - segment.length % 4) % 4
            val normalized = buildString(segment.length + padding) {
                append(segment)
                repeat(padding) { append('=') }
            }
            val decoded = Base64.getUrlDecoder().decode(normalized)
            String(decoded, StandardCharsets.UTF_8)
        } catch (error: IllegalArgumentException) {
            throw InvalidAuthTokenException(error)
        }
    }

    private fun expirationTicker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(EXPIRATION_TICK_MILLIS)
        }
    }

    companion object {
        private val EXPIRATION_MARGIN: Duration = Duration.ofMinutes(2)
        private const val EXPIRATION_TICK_MILLIS = 30_000L
    }
}
