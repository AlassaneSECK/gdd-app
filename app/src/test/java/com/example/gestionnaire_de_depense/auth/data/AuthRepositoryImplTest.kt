package com.example.gestionnaire_de_depense.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gestionnaire_de_depense.auth.domain.EmailAlreadyUsedException
import com.example.gestionnaire_de_depense.auth.domain.InvalidAuthTokenException
import com.example.gestionnaire_de_depense.auth.domain.InvalidCredentialsException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.Base64
import java.time.temporal.ChronoUnit
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AuthRepositoryImpl

    private lateinit var dataStoreDir: File
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        dataStoreDir = createTempDirectory().toFile()
        testScope = TestScope(testDispatcher)

        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = {
                File(dataStoreDir, "auth.preferences_pb")
            }
        )

        val endpoints = AuthEndpoints(
            login = mockWebServer.url("/api/auth/login"),
            register = mockWebServer.url("/api/auth/register")
        )

        val api = AuthNetworkModule.createAuthApi(endpoints)

        repository = AuthRepositoryImpl(
            api = api,
            dataStore = dataStore,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        dataStoreDir.deleteRecursively()
    }

    @Test
    fun loginShouldStoreSessionAndEmitAuthenticated() = runTest(testDispatcher) {
        val issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = issuedAt.plusSeconds(3600)
        val token = createJwt(issuedAt = issuedAt, expiresAt = expiresAt)
        mockWebServer.enqueue(successResponse(token = token))

        repository.login("user@example.com", "password123")
        advanceUntilIdle()

        val storedToken = repository.currentToken()
        val session = repository.session.first { it != null }!!
        val isAuthenticated = repository.authState.first { it }
        val request = mockWebServer.takeRequest()

        assertEquals(token, storedToken)
        assertEquals("user@example.com", session.subject)
        assertEquals(expiresAt, session.expiresAt)
        assertTrue(isAuthenticated)
        assertEquals("/api/auth/login", request.path)
    }

    @Test
    fun registerShouldStoreSessionAndEmitAuthenticated() = runTest(testDispatcher) {
        val issuedAt = Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = issuedAt.plusSeconds(5400)
        val token = createJwt(subject = "new@example.com", issuedAt = issuedAt, expiresAt = expiresAt)
        mockWebServer.enqueue(successResponse(code = 201, token = token))

        repository.register("new@example.com", "password123")
        advanceUntilIdle()

        val storedToken = repository.currentToken()
        val session = repository.session.first { it != null }!!
        val isAuthenticated = repository.authState.first { it }
        val request = mockWebServer.takeRequest()

        assertEquals(token, storedToken)
        assertEquals("new@example.com", session.subject)
        assertEquals(expiresAt, session.expiresAt)
        assertTrue(isAuthenticated)
        assertEquals("/api/auth/register", request.path)
    }

    @Test
    fun loginUnauthorizedShouldThrowInvalidCredentialsException() = runTest(testDispatcher) {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        try {
            repository.login("user@example.com", "password123")
            fail("Expected InvalidCredentialsException")
        } catch (error: Throwable) {
            assertTrue(error is InvalidCredentialsException)
        }
    }

    @Test
    fun registerConflictShouldThrowEmailAlreadyUsedException() = runTest(testDispatcher) {
        mockWebServer.enqueue(MockResponse().setResponseCode(409))

        try {
            repository.register("user@example.com", "password123")
            fail("Expected EmailAlreadyUsedException")
        } catch (error: Throwable) {
            assertTrue(error is EmailAlreadyUsedException)
        }
    }

    @Test
    fun clearTokenShouldEmitUnauthenticated() = runTest(testDispatcher) {
        val token = createJwt()
        mockWebServer.enqueue(successResponse(token = token))
        repository.login("user@example.com", "password123")
        advanceUntilIdle()

        repository.clearToken()
        advanceUntilIdle()

        val session = repository.currentSession()
        val storedToken = repository.currentToken()
        val isAuthenticated = repository.authState.first()

        assertNull(session)
        assertNull(storedToken)
        assertFalse(isAuthenticated)
    }

    @Test
    fun loginWithExpiredTokenShouldThrowInvalidAuthTokenException() = runTest(testDispatcher) {
        val now = Instant.now()
        val expiredToken = createJwt(
            issuedAt = now.minusSeconds(7200),
            expiresAt = now.minusSeconds(600)
        )
        mockWebServer.enqueue(successResponse(token = expiredToken))

        try {
            repository.login("user@example.com", "password123")
            fail("Expected InvalidAuthTokenException")
        } catch (error: Throwable) {
            assertTrue(error is InvalidAuthTokenException)
        }
    }

    @Test
    fun currentSessionShouldClearExpiredPreferences() = runTest(testDispatcher) {
        val now = Instant.now()
        val expiredIssuedAt = now.minusSeconds(7200)
        val expiredExpiresAt = now.minusSeconds(600)

        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("auth_token")] = "stale-token"
            preferences[stringPreferencesKey("auth_subject")] = "user@example.com"
            preferences[longPreferencesKey("auth_issued_at")] = expiredIssuedAt.toEpochMilli()
            preferences[longPreferencesKey("auth_expires_at")] = expiredExpiresAt.toEpochMilli()
        }

        val session = repository.currentSession()

        assertNull(session)

        val persisted = dataStore.data.first()
        assertNull(persisted[stringPreferencesKey("auth_token")])
    }

    private fun successResponse(code: Int = 200, token: String = createJwt()): MockResponse {
        return MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody("""{"token":"$token"}""")
    }

    companion object {
        private fun createJwt(
            subject: String = "user@example.com",
            issuedAt: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            expiresAt: Instant = issuedAt.plusSeconds(3600)
        ): String {
            val header = """{"alg":"HS256","typ":"JWT"}"""
            val payload =
                """{"sub":"$subject","iat":${issuedAt.epochSecond},"exp":${expiresAt.epochSecond}}"""

            val encoder = Base64.getUrlEncoder().withoutPadding()
            val headerEncoded = encoder.encodeToString(header.toByteArray(Charsets.UTF_8))
            val payloadEncoded = encoder.encodeToString(payload.toByteArray(Charsets.UTF_8))

            return listOf(headerEncoded, payloadEncoded, "signature").joinToString(".")
        }
    }
}
