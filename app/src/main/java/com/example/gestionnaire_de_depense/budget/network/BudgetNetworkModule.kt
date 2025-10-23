package com.example.gestionnaire_de_depense.budget.network

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

data class BudgetNetworkConfig(
    val baseUrl: HttpUrl
)

private val DefaultConfig = BudgetNetworkConfig(
    baseUrl = "http://alass-code.com:8080/".toHttpUrl()
)

object BudgetNetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createBudgetApi(config: BudgetNetworkConfig = DefaultConfig): BudgetApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(provideClient())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BudgetApiService::class.java)
    }

    private fun provideClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
