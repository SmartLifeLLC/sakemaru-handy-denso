package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Request to register an item during picking
 */
@Serializable
data class RegisterItemRequest(
    val courseId: String,
    val itemId: String,
    val qtyCase: Int,
    val qtyEach: Int,
    val idempotencyKey: String
)

/**
 * Request to confirm entire course
 */
@Serializable
data class ConfirmCourseRequest(
    val courseId: String,
    val idempotencyKey: String
)
