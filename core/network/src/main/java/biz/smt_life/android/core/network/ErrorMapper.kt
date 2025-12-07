package biz.smt_life.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Generic error envelope for parsing error responses.
 * Matches the ApiEnvelope structure used by the API.
 */
@Serializable
private data class ApiErrorEnvelope(
    @SerialName("is_success") val isSuccess: Boolean = false,
    val code: String? = null,
    val result: ErrorResult? = null
) {
    @Serializable
    data class ErrorResult(
        @SerialName("error_message") val errorMessage: String? = null,
        val errors: Map<String, List<String>>? = null,
        val message: String? = null,
        @SerialName("debug_message") val debugMessage: String? = null
    )
}

object ErrorMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun mapException(throwable: Throwable): NetworkException = when (throwable) {
        is HttpException -> mapHttpException(throwable)
        is IOException -> NetworkException.NetworkError("ネットワーク接続に失敗しました")
        is NetworkException -> throwable
        else -> NetworkException.Unknown(throwable.message ?: "Unknown error")
    }

    private fun mapHttpException(exception: HttpException): NetworkException {
        val errorBody = exception.response()?.errorBody()?.string()

        // Try to parse as ApiEnvelope structure
        val errorEnvelope = errorBody?.let {
            try {
                json.decodeFromString<ApiErrorEnvelope>(it)
            } catch (e: Exception) {
                null
            }
        }

        // Extract comprehensive error message from API response
        val errorMessage = extractErrorMessage(errorEnvelope)

        return when (exception.code()) {
            400 -> NetworkException.ValidationError(errorMessage ?: "リクエストが無効です")
            401 -> NetworkException.Unauthorized(errorMessage ?: "認証エラー")
            403 -> NetworkException.Forbidden(errorMessage ?: "アクセスが拒否されました")
            404 -> NetworkException.NotFound(errorMessage ?: "データが見つかりません")
            409 -> NetworkException.Conflict(errorMessage ?: "データが競合しています")
            422 -> NetworkException.ValidationError(errorMessage ?: "入力エラーです")
            in 500..599 -> NetworkException.ServerError(errorMessage ?: "サーバーエラーが発生しました")
            else -> NetworkException.Unknown(errorMessage ?: "エラーが発生しました")
        }
    }

    /**
     * Extracts and combines error messages from API error envelope.
     * Prioritizes error_message and combines with detailed errors if available.
     */
    private fun extractErrorMessage(envelope: ApiErrorEnvelope?): String? {
        if (envelope == null) return null

        val result = envelope.result ?: return null
        val primaryMessage = result.errorMessage ?: result.message
        val detailedErrors = result.errors
            ?.values
            ?.flatten()
            ?.joinToString(separator = "\n")

        return when {
            !primaryMessage.isNullOrBlank() && !detailedErrors.isNullOrBlank() ->
                "$primaryMessage\n$detailedErrors"
            !primaryMessage.isNullOrBlank() ->
                primaryMessage
            !detailedErrors.isNullOrBlank() ->
                detailedErrors
            else ->
                null
        }
    }
}
