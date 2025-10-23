package com.example.gestionnaire_de_depense.budget.data

import com.example.gestionnaire_de_depense.auth.domain.AuthRepository
import com.example.gestionnaire_de_depense.auth.domain.AuthSession
import com.example.gestionnaire_de_depense.budget.domain.BudgetAuthenticationRequiredException
import com.example.gestionnaire_de_depense.budget.domain.BudgetCreationResult
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntriesPage
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntry
import com.example.gestionnaire_de_depense.budget.domain.BudgetEntryType
import com.example.gestionnaire_de_depense.budget.domain.BudgetException
import com.example.gestionnaire_de_depense.budget.domain.BudgetNetworkException
import com.example.gestionnaire_de_depense.budget.domain.BudgetNotFoundException
import com.example.gestionnaire_de_depense.budget.domain.BudgetRepository
import com.example.gestionnaire_de_depense.budget.domain.BudgetSummary
import com.example.gestionnaire_de_depense.budget.domain.BudgetUnauthorizedException
import com.example.gestionnaire_de_depense.budget.domain.BudgetUnexpectedResponseException
import com.example.gestionnaire_de_depense.budget.domain.BudgetValidationException
import com.example.gestionnaire_de_depense.budget.network.BudgetApiService
import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntriesPageResponse
import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntryCreateRequest
import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntryCreateResponse
import com.example.gestionnaire_de_depense.budget.network.model.BudgetEntryDto
import com.example.gestionnaire_de_depense.budget.network.model.BudgetSummaryResponse
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException

class BudgetRepositoryImpl(
    private val api: BudgetApiService,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BudgetRepository {

    override suspend fun fetchSummary(): BudgetSummary = execute { _, bearer ->
        api.fetchBudgetSummary(bearer).toDomain()
    }

    override suspend fun fetchEntries(page: Int, size: Int): BudgetEntriesPage =
        execute { _, bearer ->
            api.fetchEntries(page, size, bearer).toDomain()
        }

    override suspend fun createEntry(
        type: BudgetEntryType,
        amount: BigDecimal,
        occurredAt: Instant?,
        description: String?
    ): BudgetCreationResult = execute { _, bearer ->
        val request = BudgetEntryCreateRequest(
            type = type.name,
            amount = amount.toPayloadString(),
            occurredAt = occurredAt?.toString(),
            description = description?.takeIf { it.isNotBlank() }
        )
        api.createEntry(request, bearer).toDomain()
    }

    private suspend fun <T> execute(block: suspend (AuthSession, String) -> T): T {
        return withContext(ioDispatcher) {
            val session = authRepository.currentSession() ?: throw BudgetAuthenticationRequiredException()
            val bearer = "Bearer ${session.token}"
            try {
                block(session, bearer)
            } catch (error: Throwable) {
                throw mapError(error)
            }
        }
    }

    private fun mapError(throwable: Throwable): BudgetException {
        return when (throwable) {
            is HttpException -> when (throwable.code()) {
                400 -> BudgetValidationException(throwable.message())
                401 -> BudgetUnauthorizedException(throwable)
                404 -> BudgetNotFoundException()
                else -> BudgetNetworkException(throwable)
            }

            is IOException -> BudgetNetworkException(throwable)
            is BudgetException -> throwable
            else -> BudgetUnexpectedResponseException(cause = throwable)
        }
    }

    private fun BudgetSummaryResponse.toDomain(): BudgetSummary {
        val amount = availableAmount.toBigDecimalOrNull()
            ?: throw BudgetUnexpectedResponseException(
                "Montant budget invalide: ${availableAmount.describe()}"
            )
        val userIdentifier = userId
            ?: throw BudgetUnexpectedResponseException("Identifiant utilisateur manquant dans le résumé budget.")
        return BudgetSummary(
            userId = userIdentifier,
            availableAmount = amount
        )
    }

    private fun BudgetEntriesPageResponse.toDomain(): BudgetEntriesPage {
        return BudgetEntriesPage(
            content = content.map { it.toDomain() },
            pageNumber = number,
            pageSize = size,
            totalElements = totalElements,
            totalPages = totalPages,
            isLast = last
        )
    }

    private fun BudgetEntryDto.toDomain(): BudgetEntry {
        val amountValue = amount.toBigDecimalOrNull()
            ?: throw BudgetUnexpectedResponseException(
                "Montant d'entrée invalide: ${amount.describe()}"
            )
        val typeValue = runCatching { BudgetEntryType.valueOf(type.uppercase()) }.getOrElse {
            throw BudgetUnexpectedResponseException("Type d'entrée inconnu: $type", it)
        }
        val occurredAtInstant = occurredAtIso?.let {
            runCatching { Instant.parse(it) }.getOrElse { error ->
                throw BudgetUnexpectedResponseException("Horodatage invalide: $occurredAtIso", error)
            }
        }
        return BudgetEntry(
            id = id,
            type = typeValue,
            amount = amountValue,
            occurredAt = occurredAtInstant,
            description = description
        )
    }

    private fun BudgetEntryCreateResponse.toDomain(): BudgetCreationResult {
        return BudgetCreationResult(
            entry = entry.toDomain(),
            summary = budget.toDomain()
        )
    }

    private fun BigDecimal.toPayloadString(): String = this.stripTrailingZeros().toPlainString()

    private fun JsonPrimitive.toBigDecimalOrNull(): BigDecimal? {
        val raw = contentOrNull ?: toString()
        return raw.toBigDecimalOrNull()
    }

    private fun JsonPrimitive.describe(): String = contentOrNull ?: toString()

    private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()
}
