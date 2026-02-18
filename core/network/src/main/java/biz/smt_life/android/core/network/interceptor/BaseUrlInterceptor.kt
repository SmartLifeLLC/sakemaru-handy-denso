package biz.smt_life.android.core.network.interceptor

import biz.smt_life.android.core.ui.HostPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Dynamically replaces the base URL on each request using the current HostPreferences value.
 * This allows the URL changed in Settings to take effect immediately without app restart.
 */
class BaseUrlInterceptor @Inject constructor(
    private val hostPreferences: HostPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentBaseUrl = runBlocking { hostPreferences.getBaseUrlOnce() }
        val baseHttpUrl = currentBaseUrl.toHttpUrl()

        val originalRequest = chain.request()
        val newUrl = originalRequest.url.newBuilder()
            .scheme(baseHttpUrl.scheme)
            .host(baseHttpUrl.host)
            .port(baseHttpUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
