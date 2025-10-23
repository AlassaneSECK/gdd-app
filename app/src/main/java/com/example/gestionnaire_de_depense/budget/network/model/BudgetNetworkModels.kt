package com.example.gestionnaire_de_depense.budget.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class BudgetSummaryResponse(
    val userId: Long? = null,
    val availableAmount: JsonPrimitive
)

@Serializable
data class BudgetEntryDto(
    val id: Long,
    val type: String,
    val amount: JsonPrimitive,
    @SerialName("occurredAt")
    val occurredAtIso: String? = null,
    val description: String? = null
)

@Serializable
data class BudgetEntriesPageResponse(
    val content: List<BudgetEntryDto>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val last: Boolean
)

@Serializable
data class BudgetEntryCreateRequest(
    val type: String,
    val amount: String,
    val occurredAt: String? = null,
    val description: String? = null
)

@Serializable
data class BudgetEntryCreateResponse(
    val entry: BudgetEntryDto,
    val budget: BudgetSummaryResponse
)
