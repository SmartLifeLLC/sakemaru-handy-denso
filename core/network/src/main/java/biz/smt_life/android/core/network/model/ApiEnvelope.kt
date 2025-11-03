package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard API response envelope matching Swagger specification.
 * All API responses follow this structure.
 */
@Serializable
data class ApiEnvelope<T>(
    @SerialName("is_success") val isSuccess: Boolean,
    val code: String? = null,
    val result: ResultBlock<T>? = null
) {
    @Serializable
    data class ResultBlock<T>(
        val data: T? = null,
        val message: String? = null,
        @SerialName("error_message") val errorMessage: String? = null,
        val errors: Map<String, List<String>>? = null,
        @SerialName("debug_message") val debugMessage: String? = null
    )
}
