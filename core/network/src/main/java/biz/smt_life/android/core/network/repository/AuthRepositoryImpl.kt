package biz.smt_life.android.core.network.repository

import android.content.Context
import android.provider.Settings
import biz.smt_life.android.core.domain.model.AuthResult
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.network.api.AuthService
import biz.smt_life.android.core.network.NetworkException
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Real implementation of AuthRepository using Retrofit.
 * Maps API responses to domain models per Swagger specification.
 */
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun login(staffCode: String, password: String): Result<AuthResult> {
        return try {
            val deviceId = "ANDROID-${Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )}"

            val response = authService.login(
                code = staffCode,
                password = password,
                deviceId = deviceId
            )

            if (response.isSuccess && response.result?.data != null) {
                val loginData = response.result.data
                val authResult = AuthResult(
                    token = loginData.token,
                    pickerId = loginData.picker.id,
                    pickerCode = loginData.picker.code,
                    pickerName = loginData.picker.name,
                    defaultWarehouseId = loginData.picker.defaultWarehouseId
                )
                Result.success(authResult)
            } else {
                val errorMessage = response.result?.errorMessage ?: "Login failed"
                Result.failure(NetworkException.Unauthorized(errorMessage))
            }
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(NetworkException.Unauthorized("Invalid credentials"))
                422 -> {
                    // Parse validation errors if needed
                    Result.failure(NetworkException.ValidationError("Validation failed"))
                }
                else -> Result.failure(NetworkException.ServerError("Server error: ${e.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(NetworkException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            authService.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if logout fails, we consider it successful locally
            // The token will be cleared from local storage
            Result.success(Unit)
        }
    }

    override suspend fun validateSession(): Result<AuthResult> {
        return try {
            val response = authService.me()

            if (response.isSuccess && response.result?.data != null) {
                val picker = response.result.data
                // For session validation, we don't have the token in the response
                // so we return a minimal AuthResult (token will be retrieved from storage)
                val authResult = AuthResult(
                    token = "", // Token is already stored locally
                    pickerId = picker.id,
                    pickerCode = picker.code,
                    pickerName = picker.name,
                    defaultWarehouseId = picker.defaultWarehouseId
                )
                Result.success(authResult)
            } else {
                Result.failure(NetworkException.Unauthorized("Session invalid"))
            }
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(NetworkException.Unauthorized("Session expired"))
                else -> Result.failure(NetworkException.ServerError("Server error: ${e.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(NetworkException.NetworkError("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
}
