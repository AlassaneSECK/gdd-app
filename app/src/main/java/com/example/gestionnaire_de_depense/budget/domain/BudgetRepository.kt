package com.example.gestionnaire_de_depense.budget.domain

import java.math.BigDecimal
import java.time.Instant

interface BudgetRepository {
    suspend fun fetchSummary(): BudgetSummary

    suspend fun fetchEntries(page: Int, size: Int): BudgetEntriesPage

    suspend fun createEntry(
        type: BudgetEntryType,
        amount: BigDecimal,
        occurredAt: Instant?,
        description: String?
    ): BudgetCreationResult
}
