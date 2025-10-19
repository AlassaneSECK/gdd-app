package com.example.gestionnaire_de_depense.auth.data

import com.example.gestionnaire_de_depense.auth.network.AuthApiService
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val LOGIN_PATH = "/api/auth/login"
private const val REGISTER_PATH = "/api/auth/register"

// URLs configurables pour faciliter les tests (MockWebServer) ou un changement d'environnement.
data class AuthEndpoints(
    val login: HttpUrl,
    val register: HttpUrl
)

private val DefaultEndpoints = AuthEndpoints(
    login = "http://alass-code.com:8080/api/auth/login".toHttpUrl(),
    register = "http://alass-code.com:8080/api/auth/register".toHttpUrl()
)

object AuthNetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // Construit le client Retrofit pré-configuré pour l'API d'authentification.
    internal fun createAuthApi(endpoints: AuthEndpoints = DefaultEndpoints): AuthApiService {
        val contentType = "application/json".toMediaType()
        val baseUrl = endpoints.register.newBuilder()
            .encodedPath("/")
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(provideOkHttpClient(endpoints))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AuthApiService::class.java)
    }

    private fun provideOkHttpClient(endpoints: AuthEndpoints): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Ré-écrit l'URL selon la route demandée afin de mixer HTTP / HTTPS si besoin.
                val request = chain.request()
                val rewritten = rewriteRequestUrl(request, endpoints)
                chain.proceed(rewritten)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun rewriteRequestUrl(request: Request, endpoints: AuthEndpoints): Request {
        val targetUrl = when (request.url.encodedPath) {
            LOGIN_PATH -> endpoints.login
            REGISTER_PATH -> endpoints.register
            else -> request.url
        }
        return if (targetUrl == request.url) {
            request
        } else {
            request.newBuilder().url(targetUrl).build()
        }
    }
}
