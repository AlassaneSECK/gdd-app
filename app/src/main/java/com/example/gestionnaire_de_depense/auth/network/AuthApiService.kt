package com.example.gestionnaire_de_depense.auth.network

import com.example.gestionnaire_de_depense.auth.network.model.AuthRequest
import com.example.gestionnaire_de_depense.auth.network.model.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

// Déclare les points d'entrée Retrofit de l'API d'authentification.
interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse
}
