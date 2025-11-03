package biz.smt_life.android.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
data class ApiErrorResponse(
    val message: String? = null,
    val errors: Map<String, String>? = null
)

object ErrorMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun mapException(throwable: Throwable): NetworkException = when (throwable) {
        is HttpException -> mapHttpException(throwable)
        is IOException -> NetworkException.NetworkError("Network connection failed")
        is NetworkException -> throwable
        else -> NetworkException.Unknown(throwable.message ?: "Unknown error")
    }

    private fun mapHttpException(exception: HttpException): NetworkException {
        val errorBody = exception.response()?.errorBody()?.string()
        val errorResponse = errorBody?.let {
            try {
                json.decodeFromString<ApiErrorResponse>(it)
            } catch (e: Exception) {
                null
            }
        }

        return when (exception.code()) {
            400 -> errorResponse?.errors?.let { NetworkException.Validation(it) }
                ?: NetworkException.Validation(mapOf("general" to (errorResponse?.message ?: "Bad request")))
            401 -> NetworkException.Unauthorized(errorResponse?.message ?: "Unauthorized")
            403 -> NetworkException.Forbidden(errorResponse?.message ?: "Forbidden")
            404 -> NetworkException.NotFound(errorResponse?.message ?: "Not found")
            409 -> NetworkException.Conflict(errorResponse?.message ?: "Conflict")
            422 -> errorResponse?.errors?.let { NetworkException.Validation(it) }
                ?: NetworkException.Validation(mapOf("general" to (errorResponse?.message ?: "Validation failed")))
            in 500..599 -> NetworkException.ServerError(errorResponse?.message ?: "Server error")
            else -> NetworkException.Unknown(errorResponse?.message ?: "Unknown error")
        }
    }
}
