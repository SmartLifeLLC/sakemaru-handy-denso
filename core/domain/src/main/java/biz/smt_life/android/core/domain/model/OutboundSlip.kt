package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OutboundSlip(
    val id: String,
    val slipNumber: String,
    val customerName: String,
    val outboundDate: String,
    val done: Int,
    val total: Int,
    val status: String, // "pending", "in_progress", "completed"
    val items: List<OutboundItem>
)

@Serializable
data class OutboundItem(
    val id: String,
    val itemId: String,
    val itemCode: String,
    val itemName: String,
    val packSize: Int,
    val capacity: String?,
    val jan: String?,
    val area: String?,
    val location: String?,
    val orderQtyCase: Int,
    val orderQtyEach: Int,
    val outboundQtyCase: Int,
    val outboundQtyEach: Int,
    val course: String?,
    val status: String // "pending", "completed"
)

@Serializable
data class PickingCourse(
    val id: String,
    val courseName: String,
    val done: Int,
    val total: Int,
    val isMyAssignment: Boolean
)

@Serializable
data class OutboundEntry(
    val id: String,
    val slipId: String,
    val itemId: String,
    val itemName: String,
    val qtyCase: Int,
    val qtyEach: Int,
    val course: String?,
    val status: String,
    val createdAt: String
)

@Serializable
data class OutboundAddRequest(
    val slipId: String,
    val itemId: String,
    val qtyCase: Int,
    val qtyEach: Int,
    val course: String?,
    val idempotencyKey: String
)

@Serializable
data class OutboundConfirmRequest(
    val ids: List<String>,
    val idempotencyKey: String
)

@Serializable
data class OutboundConfirmResponse(
    val updated: List<OutboundEntry>
)
