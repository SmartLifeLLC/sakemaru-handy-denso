package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.InventoryCountListData
import biz.smt_life.android.core.network.model.InventoryCountRequest
import biz.smt_life.android.core.network.model.InventoryCountScanData
import biz.smt_life.android.core.network.model.InventoryCountSubmitData
import biz.smt_life.android.core.network.model.InventoryItemsData
import biz.smt_life.android.core.network.model.InventoryBulkCountRequest
import biz.smt_life.android.core.network.model.InventoryBulkCountData
import biz.smt_life.android.core.network.model.JanCodeDictionaryData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryCountApi {
    @GET("/api/wms/inventory-counts")
    suspend fun getInventoryCounts(
        @Query("warehouse_id") warehouseId: Int
    ): ApiEnvelope<InventoryCountListData>

    @GET("/api/wms/inventory-counts/active")
    suspend fun getActiveInventoryCount(
        @Query("warehouse_id") warehouseId: Int
    ): ApiEnvelope<InventoryCountListData>

    @POST("/api/wms/inventory-counts/{id}/scan")
    suspend fun scanItem(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): ApiEnvelope<InventoryCountScanData>

    @GET("/api/wms/inventory-counts/{id}/jan-codes")
    suspend fun getJanCodes(
        @Path("id") id: Int
    ): ApiEnvelope<JanCodeDictionaryData>

    @GET("/api/wms/inventory-counts/{id}/items")
    suspend fun getItems(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 500,
        @Query("compact") compact: Int = 1
    ): ApiEnvelope<InventoryItemsData>

    @POST("/api/wms/inventory-counts/{id}/counts/bulk")
    suspend fun submitBulkCounts(
        @Path("id") id: Int,
        @Body request: InventoryBulkCountRequest
    ): ApiEnvelope<InventoryBulkCountData>

    @POST("/api/wms/inventory-count-items/{itemId}/count")
    suspend fun submitCount(
        @Path("itemId") itemId: Int,
        @Body request: InventoryCountRequest
    ): ApiEnvelope<InventoryCountSubmitData>
}
