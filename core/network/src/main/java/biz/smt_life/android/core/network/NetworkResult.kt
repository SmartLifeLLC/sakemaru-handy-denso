package biz.smt_life.android.core.network

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: NetworkException) : NetworkResult<Nothing>()
}

sealed class NetworkException(message: String) : Exception(message) {
    data class Unauthorized(val msg: String = "Unauthorized") : NetworkException(msg)
    data class Forbidden(val msg: String = "Forbidden") : NetworkException(msg)
    data class NotFound(val msg: String = "Not found") : NetworkException(msg)
    data class Conflict(val msg: String = "Conflict") : NetworkException(msg)
    data class Validation(val errors: Map<String, String>) : NetworkException("Validation failed")
    data class ValidationError(val msg: String = "Validation error") : NetworkException(msg)
    data class ServerError(val msg: String = "Server error") : NetworkException(msg)
    data class NetworkError(val msg: String = "Network error") : NetworkException(msg)
    data class Unknown(val msg: String = "Unknown error") : NetworkException(msg)
}

fun <T> NetworkResult<T>.toResult(): Result<T> = when (this) {
    is NetworkResult.Success -> Result.success(data)
    is NetworkResult.Error -> Result.failure(exception)
}
