package biz.smt_life.android.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds X-API-Key header to all requests per Swagger securitySchemes.
 */
class ApiKeyInterceptor @Inject constructor(
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", apiKey)
            .build()
        return chain.proceed(request)
    }
}
