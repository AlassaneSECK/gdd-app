package com.example.gestionnaire_de_depense.auth.domain

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<Boolean>
    val session: Flow<AuthSession?>

    suspend fun login(email: String, password: String)

    suspend fun register(email: String, password: String)

    suspend fun clearToken()

    suspend fun currentSession(): AuthSession?

    suspend fun currentToken(): String?
}
