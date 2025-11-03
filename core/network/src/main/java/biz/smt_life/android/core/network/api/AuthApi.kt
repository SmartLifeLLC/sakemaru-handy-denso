package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.domain.model.AuthResult
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for authentication API.
 * Not used yet - will be connected when real backend is available.
 */
interface AuthApi {
    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResult
}

data class LoginRequest(
    val staffCode: String,
    val password: String
)
