package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.OutboundInspectionSnapshotResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OutboundInspectionApi {
    @GET("/api/outbound-inspections/snapshot")
    suspend fun getSnapshot(
        @Query("warehouse_id") warehouseId: Int,
        @Query("period") period: String,
        @Query("working_date") workingDate: String
    ): ApiEnvelope<OutboundInspectionSnapshotResponse>
}
