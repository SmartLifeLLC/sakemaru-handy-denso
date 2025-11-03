package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model for authentication result.
 * Mapped from API response per Swagger specification.
 */
@Serializable
data class AuthResult(
    val token: String,
    val pickerId: Int,
    val pickerCode: String,
    val pickerName: String,
    val defaultWarehouseId: Int
)
