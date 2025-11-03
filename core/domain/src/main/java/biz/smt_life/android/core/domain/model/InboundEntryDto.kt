package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InboundEntryDto(
    val id: String,
    val itemId: String,
    val itemName: String,
    val qtyCase: Int,
    val qtyEach: Int,
    val expDate: String?,
    val labelCount: Int,
    val status: String, // "pending", "confirmed"
    val createdAt: String
)

@Serializable
data class InboundEntryRequest(
    val itemId: String,
    val qtyCase: Int,
    val qtyEach: Int,
    val expDate: String?,
    val labelCount: Int,
    val idempotencyKey: String
)

@Serializable
data class InboundConfirmRequest(
    val ids: List<String>,
    val idempotencyKey: String
)

@Serializable
data class InboundConfirmResponse(
    val updated: List<InboundEntryDto>
)
