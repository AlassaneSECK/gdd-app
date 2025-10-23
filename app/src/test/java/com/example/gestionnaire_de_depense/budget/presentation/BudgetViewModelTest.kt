package com.example.gestionnaire_de_depense.budget.presentation

import com.example.gestionnaire_de_depense.budget.domain.BudgetAuthenticationRequiredException
import com.example.gestionnaire_de_depense.budget.domain.BudgetCreationResult
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntriesPage
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntry
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntryType
import com.example.gestionnaire_de_depense.budget.domain.BudgetNotFoundException
import com.example.gestionnaire_de_depense.budget.domain.BudgetRepository
import com.example.gestionnaire_de_depense.budget.domain.BudgetSummary
import com.example.gestionnaire_de_depense.util.MainDispatcherRule
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun initShouldLoadSummaryAndEntries() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeBudgetRepository()
        val viewModel = BudgetViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isInitialized)
        assertEquals(repository.summary, state.summary)
        assertEquals(repository.entries.size, state.entries.size)
    }

    @Test
    fun submitEntryShouldInsertEntryAndRefreshSummary() = runTest(dispatcherRule.dispatcher) {
        val repository = FakeBudgetRepository()
        val viewModel = BudgetViewModel(repository)
        advanceUntilIdle()

        viewModel.onEntryTypeChanged(BudgetEntryType.INCOME)
        viewModel.onEntryAmountChanged("250.00")
        viewModel.onEntryDescriptionChanged("Prime exceptionnelle")

        viewModel.submitEntry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("",
            state.entryAmountInput
        )
        assertEquals("Prime exceptionnelle", state.entries.first().description)
        assertEquals(repository.summary.availableAmount, state.summary?.availableAmount)
    }

    @Test
    fun initWithoutSessionShouldStayIdle() = runTest(dispatcherRule.dispatcher) {
        val repository = NoSessionBudgetRepository()
        val viewModel = BudgetViewModel(repository)
        var effectEmitted = false
        val job = launch {
            viewModel.effects.collect { effectEmitted = true }
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(effectEmitted.not())
        assertTrue(state.isInitialized.not())
        assertTrue(state.unauthorized.not())
        job.cancel()
    }

    @Test
    fun initWithMissingBudgetShouldExposeMessageWithoutLogout() = runTest(dispatcherRule.dispatcher) {
        val repository = MissingBudgetRepository()
        val viewModel = BudgetViewModel(repository)
        var effectEmitted = false
        val job = launch {
            viewModel.effects.collect { effectEmitted = true }
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(effectEmitted.not())
        assertTrue(state.isInitialized)
        assertEquals("Aucun budget trouvé. Commencez par créer un mouvement.", state.errorMessage)
        job.cancel()
    }

    private class FakeBudgetRepository : BudgetRepository {
        var summary = BudgetSummary(
            userId = 1L,
            availableAmount = BigDecimal("1000.00")
        )
        val entries = mutableListOf(
            BudgetEntry(
                id = 1,
                type = BudgetEntryType.INCOME,
                amount = BigDecimal("500.00"),
                occurredAt = Instant.parse("2024-10-01T08:00:00Z"),
                description = "Salaire"
            ),
            BudgetEntry(
                id = 2,
                type = BudgetEntryType.EXPENSE,
                amount = BigDecimal("45.90"),
                occurredAt = Instant.parse("2024-10-02T18:30:00Z"),
                description = "Restaurant"
            )
        )
        private var nextId = 3L

        override suspend fun fetchSummary(): BudgetSummary = summary

        override suspend fun fetchEntries(page: Int, size: Int): BudgetEntriesPage {
            val content = if (page == 0) entries.toList() else emptyList()
            return BudgetEntriesPage(
                content = content,
                pageNumber = page,
                pageSize = size,
                totalElements = content.size.toLong(),
                totalPages = 1,
                isLast = true
            )
        }

        override suspend fun createEntry(
            type: BudgetEntryType,
            amount: BigDecimal,
            occurredAt: Instant?,
            description: String?
        ): BudgetCreationResult {
            val entry = BudgetEntry(
                id = nextId++,
                type = type,
                amount = amount,
                occurredAt = occurredAt ?: Instant.parse("2024-12-31T00:00:00Z"),
                description = description
            )
            entries.add(0, entry)
            summary = summary.copy(
                availableAmount = if (type == BudgetEntryType.INCOME) {
                    summary.availableAmount.add(amount)
                } else {
                    summary.availableAmount.subtract(amount)
                }
            )
            return BudgetCreationResult(
                entry = entry,
                summary = summary
            )
        }
    }

    private class NoSessionBudgetRepository : BudgetRepository {
        override suspend fun fetchSummary(): BudgetSummary {
            throw BudgetAuthenticationRequiredException()
        }

        override suspend fun fetchEntries(page: Int, size: Int): BudgetEntriesPage {
            throw BudgetAuthenticationRequiredException()
        }

        override suspend fun createEntry(
            type: BudgetEntryType,
            amount: BigDecimal,
            occurredAt: Instant?,
            description: String?
        ): BudgetCreationResult {
            throw BudgetAuthenticationRequiredException()
        }
    }

    private class MissingBudgetRepository : BudgetRepository {
        override suspend fun fetchSummary(): BudgetSummary {
            throw BudgetNotFoundException()
        }

        override suspend fun fetchEntries(page: Int, size: Int): BudgetEntriesPage {
            throw BudgetNotFoundException()
        }

        override suspend fun createEntry(
            type: BudgetEntryType,
            amount: BigDecimal,
            occurredAt: Instant?,
            description: String?
        ): BudgetCreationResult {
            throw BudgetNotFoundException()
        }
    }
}
