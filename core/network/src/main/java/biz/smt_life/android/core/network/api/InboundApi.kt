package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.domain.model.InboundConfirmRequest
import biz.smt_life.android.core.domain.model.InboundConfirmResponse
import biz.smt_life.android.core.domain.model.InboundEntryDto
import biz.smt_life.android.core.domain.model.InboundEntryRequest
import biz.smt_life.android.core.domain.model.ItemDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for inbound API.
 * Not used yet - will be connected when real backend is available.
 */
interface InboundApi {
    @GET("/items")
    suspend fun searchItems(@Query("query") query: String): List<ItemDto>

    @GET("/items/barcode/{code}")
    suspend fun getItemByBarcode(@Path("code") code: String): ItemDto

    @POST("/inbound/entries")
    suspend fun addEntry(@Body request: InboundEntryRequest): InboundEntryDto

    @GET("/inbound/history")
    suspend fun getHistory(): List<InboundEntryDto>

    @POST("/inbound/confirm")
    suspend fun confirmEntries(@Body request: InboundConfirmRequest): InboundConfirmResponse
}
