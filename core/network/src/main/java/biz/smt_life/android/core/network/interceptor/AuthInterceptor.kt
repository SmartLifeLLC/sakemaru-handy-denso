package biz.smt_life.android.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds Authorization Bearer token to authenticated requests.
 * Excludes the login endpoint as it doesn't require auth.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip adding auth header for login endpoint
        if (request.url.encodedPath.endsWith("/api/auth/login")) {
            return chain.proceed(request)
        }

        val token = tokenProvider()
        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authenticatedRequest)
    }
}
