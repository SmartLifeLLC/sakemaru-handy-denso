package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.LoginData
import biz.smt_life.android.core.network.model.Picker
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service for authentication endpoints matching Swagger specification.
 */
interface AuthService {
    /**
     * Login with staff code and password (form-urlencoded).
     * Requires X-API-Key header (added by interceptor).
     * Does NOT require Bearer token.
     */
    @FormUrlEncoded
    @POST("/api/auth/login")
    suspend fun login(
        @Field("code") code: String,
        @Field("password") password: String,
        @Field("device_id") deviceId: String
    ): ApiEnvelope<LoginData>

    /**
     * Logout - revokes current token.
     * Requires X-API-Key + Authorization Bearer (added by interceptors).
     */
    @POST("/api/auth/logout")
    suspend fun logout(): ApiEnvelope<Unit?>

    /**
     * Get current picker information - validates session.
     * Requires X-API-Key + Authorization Bearer (added by interceptors).
     */
    @GET("/api/me")
    suspend fun me(): ApiEnvelope<Picker>
}
