package com.example.gestionnaire_de_depense.budget.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gestionnaire_de_depense.budget.domain.BudgetAuthenticationRequiredException
import com.example.gestionnaire_de_depense.budget.domain.BudgetCreationResult
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntriesPage
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntryType
import com.example.gestionnaire_de_depense.budget.domain.BudgetException
import com.example.gestionnaire_de_depense.budget.domain.BudgetNetworkException
import com.example.gestionnaire_de_depense.budget.domain.BudgetNotFoundException
import com.example.gestionnaire_de_depense.budget.domain.BudgetRepository
import com.example.gestionnaire_de_depense.budget.domain.BudgetSummary
import com.example.gestionnaire_de_depense.budget.domain.BudgetUnauthorizedException
import com.example.gestionnaire_de_depense.budget.domain.BudgetUnexpectedResponseException
import com.example.gestionnaire_de_depense.budget.domain.BudgetValidationException
import java.math.BigDecimal
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState(isLoading = true))
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BudgetEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effects: SharedFlow<BudgetEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            refreshInternal(initial = true)
        }
    }

    fun refresh() {
        val snapshot = _uiState.value
        if (snapshot.isRefreshing || snapshot.isLoading) return
        viewModelScope.launch {
            refreshInternal(initial = !snapshot.isInitialized)
        }
    }

    fun onCreateEntryClick() {
        _uiState.update { it.copy(showCreateEntryDialog = true) }
    }

    fun onDismissCreateEntry() {
        _uiState.update {
            it.copy(
                showCreateEntryDialog = false,
                entryAmountInput = "",
                entryDescriptionInput = "",
                entryDateInput = ""
            )
        }
    }

    fun onEntryTypeChanged(type: BudgetEntryType) {
        _uiState.update { it.copy(entryType = type) }
    }

    fun onEntryAmountChanged(value: String) {
        _uiState.update { it.copy(entryAmountInput = value) }
    }

    fun onEntryDescriptionChanged(value: String) {
        _uiState.update { it.copy(entryDescriptionInput = value) }
    }

    fun onEntryDateChanged(value: String) {
        _uiState.update { it.copy(entryDateInput = value) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadMore() {
        val snapshot = _uiState.value
        if (snapshot.isLoadingMore || snapshot.isLoading || !snapshot.hasMoreEntries) return
        viewModelScope.launch {
            requestNextPage(snapshot.nextPageIndex)
        }
    }

    fun submitEntry() {
        val snapshot = _uiState.value
        val amount = snapshot.entryAmountInput.parseDecimalOrNull()
        if (amount == null) {
            emitMessage("Montant de l'entrée invalide.")
            return
        }
        val occurredAt = snapshot.entryDateInput.takeIf { it.isNotBlank() }?.let {
            try {
                Instant.parse(it)
            } catch (error: DateTimeParseException) {
                emitMessage("Format de date invalide. Utilisez ISO 8601.")
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCreateEntryInFlight = true) }
            try {
                val result = repository.createEntry(
                    type = snapshot.entryType,
                    amount = amount,
                    occurredAt = occurredAt,
                    description = snapshot.entryDescriptionInput.takeIf { it.isNotBlank() }
                )
                onEntryCreated(result)
            } catch (error: Throwable) {
                handleActionError(error) {
                    _uiState.update { it.copy(isCreateEntryInFlight = false) }
                }
            }
        }
    }

    private suspend fun refreshInternal(initial: Boolean) {
        if (initial) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        } else {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        }
        try {
            val summary = repository.fetchSummary()
            val entries = repository.fetchEntries(page = 0, size = PAGE_SIZE)
            applyInitialLoad(summary, entries)
        } catch (error: Throwable) {
            handleLoadError(error)
        }
    }

    private suspend fun requestNextPage(page: Int) {
        _uiState.update { it.copy(isLoadingMore = true) }
        try {
            val pageResult = repository.fetchEntries(page = page, size = PAGE_SIZE)
            _uiState.update {
                it.copy(
                    entries = it.entries + pageResult.content,
                    hasMoreEntries = !pageResult.isLast,
                    nextPageIndex = pageResult.pageNumber + 1,
                    isLoadingMore = false
                )
            }
        } catch (error: Throwable) {
            handleActionError(error) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun applyInitialLoad(summary: BudgetSummary, entries: BudgetEntriesPage) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                summary = summary,
                entries = entries.content,
                hasMoreEntries = !entries.isLast,
                nextPageIndex = entries.pageNumber + 1,
                isInitialized = true,
                unauthorized = false,
                isLoadingMore = false
            )
        }
    }

    private fun onEntryCreated(result: BudgetCreationResult) {
        _uiState.update {
            it.copy(
                summary = result.summary,
                entries = listOf(result.entry) + it.entries,
                showCreateEntryDialog = false,
                entryAmountInput = "",
                entryDescriptionInput = "",
                entryDateInput = "",
                entryType = BudgetEntryType.EXPENSE,
                isCreateEntryInFlight = false
            )
        }
    }

    private fun handleLoadError(throwable: Throwable) {
        val error = throwable.asBudgetException()
        when (error) {
            is BudgetAuthenticationRequiredException -> {
                _uiState.value = BudgetUiState()
            }
            is BudgetUnauthorizedException -> {
                viewModelScope.launch { _effects.emit(BudgetEffect.SessionExpired) }
                _uiState.update {
                    BudgetUiState(
                        errorMessage = "Votre session a expiré. Veuillez vous reconnecter.",
                        unauthorized = true
                    )
                }
            }

            is BudgetNotFoundException -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isInitialized = true,
                        entries = emptyList(),
                        hasMoreEntries = false,
                        isLoadingMore = false,
                        errorMessage = "Aucun budget trouvé. Commencez par créer un mouvement."
                    )
                }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.toMessage()
                    )
                }
            }
        }
    }

    private fun handleActionError(throwable: Throwable, finally: () -> Unit) {
        val error = throwable.asBudgetException()
        finally()
        when (error) {
            is BudgetAuthenticationRequiredException -> {
                _uiState.value = BudgetUiState()
            }
            is BudgetUnauthorizedException -> {
                viewModelScope.launch { _effects.emit(BudgetEffect.SessionExpired) }
                _uiState.update {
                    it.copy(
                        unauthorized = true,
                        errorMessage = "Votre session a expiré. Veuillez vous reconnecter."
                    )
                }
            }

            is BudgetNotFoundException -> {
                emitMessage("Aucun budget trouvé pour l'instant.")
            }

            else -> emitMessage(error.toMessage())
        }
    }

    private fun emitMessage(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun Throwable.asBudgetException(): BudgetException {
        return this as? BudgetException ?: BudgetUnexpectedResponseException(cause = this)
    }

    private fun BudgetException.toMessage(): String {
        return when (this) {
            is BudgetValidationException -> message ?: "Requête invalide."
            is BudgetNetworkException -> "Service indisponible, réessayez plus tard."
            is BudgetUnexpectedResponseException -> "Réponse inattendue du serveur."
            else -> "Une erreur est survenue. Réessayez."
        }
    }

    private fun String.parseDecimalOrNull(): BigDecimal? {
        val normalized = replace(',', '.').trim()
        if (normalized.isEmpty()) return null
        return runCatching { BigDecimal(normalized) }.getOrNull()
    }

    companion object {
        private const val PAGE_SIZE = 20

        fun provideFactory(repository: BudgetRepository) = viewModelFactory {
            initializer {
                BudgetViewModel(repository)
            }
        }
    }
}
