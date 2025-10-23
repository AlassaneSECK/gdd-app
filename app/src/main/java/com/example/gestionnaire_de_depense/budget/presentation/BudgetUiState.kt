package com.example.gestionnaire_de_depense.budget.presentation

import com.example.gestionnaire_de_depense.budget.domain.BudgetEntry
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntryType
import com.example.gestionnaire_de_depense.budget.domain.BudgetSummary

data class BudgetUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val summary: BudgetSummary? = null,
    val entries: List<BudgetEntry> = emptyList(),
    val hasMoreEntries: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextPageIndex: Int = 0,
    val errorMessage: String? = null,
    val showCreateEntryDialog: Boolean = false,
    val entryType: BudgetEntryType = BudgetEntryType.EXPENSE,
    val entryAmountInput: String = "",
    val entryDescriptionInput: String = "",
    val entryDateInput: String = "",
    val isCreateEntryInFlight: Boolean = false,
    val isInitialized: Boolean = false,
    val unauthorized: Boolean = false
)

sealed interface BudgetEffect {
    object SessionExpired : BudgetEffect
}
