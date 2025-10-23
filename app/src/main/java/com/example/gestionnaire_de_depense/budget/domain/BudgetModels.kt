package com.example.gestionnaire_de_depense.budget.domain

import java.math.BigDecimal
import java.time.Instant

enum class BudgetEntryType {
    INCOME,
    EXPENSE
}

data class BudgetSummary(
    val userId: Long,
    val availableAmount: BigDecimal
)

data class BudgetEntry(
    val id: Long,
    val type: BudgetEntryType,
    val amount: BigDecimal,
    val occurredAt: Instant?,
    val description: String?
)

data class BudgetEntriesPage(
    val content: List<BudgetEntry>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val isLast: Boolean
)

data class BudgetCreationResult(
    val entry: BudgetEntry,
    val summary: BudgetSummary
)
