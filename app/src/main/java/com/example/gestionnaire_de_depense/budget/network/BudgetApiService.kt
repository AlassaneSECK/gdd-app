package com.example.gestionnaire_de_depense.budget.network

import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntriesPageResponse
import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntryCreateRequest
import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntryCreateResponse
import com.example.gestionnaire_de_depense.budget.network.model.BudgetSummaryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface BudgetApiService {
    @GET("api/budget")
    suspend fun fetchBudgetSummary(
        @Header("Authorization") authorization: String
    ): BudgetSummaryResponse

    @GET("api/budget/entries")
    suspend fun fetchEntries(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Header("Authorization") authorization: String
    ): BudgetEntriesPageResponse

    @POST("api/budget/entries")
    suspend fun createEntry(
        @Body request: BudgetEntryCreateRequest,
        @Header("Authorization") authorization: String
    ): BudgetEntryCreateResponse
}
