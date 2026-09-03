package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.JanCodeDictionaryData
import biz.smt_life.android.core.network.model.WarehouseTransferStockItemsData
import biz.smt_life.android.core.network.model.WarehouseTransferSubmitData
import biz.smt_life.android.core.network.model.WarehouseTransferSubmitRequest
import biz.smt_life.android.core.network.model.WarehouseTransferWarehousesData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 倉庫移動候補 API（HANDY）
 */
interface WarehouseTransferApi {
    @GET("/api/wms/warehouse-transfer/stock-items")
    suspend fun getStockItems(
        @Query("warehouse_id") warehouseId: Int,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 500,
        @Query("compact") compact: Int = 1,
        @Query("include_zero") includeZero: Int = 0
    ): ApiEnvelope<WarehouseTransferStockItemsData>

    @GET("/api/wms/warehouse-transfer/jan-codes")
    suspend fun getJanCodes(
        @Query("warehouse_id") warehouseId: Int,
        @Query("include_zero") includeZero: Int = 0
    ): ApiEnvelope<JanCodeDictionaryData>

    @GET("/api/wms/warehouse-transfer/warehouses")
    suspend fun getWarehouses(
        @Query("exclude_warehouse_id") excludeWarehouseId: Int? = null
    ): ApiEnvelope<WarehouseTransferWarehousesData>

    @POST("/api/wms/warehouse-transfer-candidates")
    suspend fun submitCandidates(
        @Body request: WarehouseTransferSubmitRequest
    ): ApiEnvelope<WarehouseTransferSubmitData>
}
