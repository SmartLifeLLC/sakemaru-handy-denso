package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.PickingTaskResponse
import biz.smt_life.android.core.network.model.StartTaskRequest
import biz.smt_life.android.core.network.model.StartTaskResponse
import biz.smt_life.android.core.network.model.UpdatePickingRequest
import biz.smt_life.android.core.network.model.UpdatePickingResponse
import biz.smt_life.android.core.network.model.CompleteTaskResponse
import biz.smt_life.android.core.network.model.CancelPickingResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Picking Tasks API per handy_swagger_20251124.json.
 *
 * Security: Requires X-API-Key and Authorization Bearer token
 */
interface PickingApi {
    /**
     * GET /api/picking/tasks
     * Fetch picking tasks for warehouse with optional picker filter.
     *
     * Query parameters:
     * - warehouse_id (required): Filter tasks by warehouse
     * - picker_id (optional): Filter tasks by specific picker (for "My Area" tab)
     * - picking_area_id (optional): Filter tasks by specific area
     */
    @GET("/api/picking/tasks")
    suspend fun getPickingTasks(
        @Query("warehouse_id") warehouseId: Int,
        @Query("picker_id") pickerId: Int? = null,
        @Query("picking_area_id") pickingAreaId: Int? = null
    ): ApiEnvelope<List<PickingTaskResponse>>

    /**
     * POST /api/picking/tasks/{id}/start
     * Start a picking task. Changes status to PICKING and sets started_at timestamp.
     *
     * @param id Picking Task ID (wave.wms_picking_task_id)
     * @param idempotencyKey Unique key to prevent duplicate requests
     */
    @POST("/api/picking/tasks/{id}/start")
    suspend fun startPickingTask(
        @Path("id") id: Int,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: StartTaskRequest = StartTaskRequest()
    ): ApiEnvelope<StartTaskResponse>

    /**
     * POST /api/picking/tasks/{wms_picking_item_result_id}/update
     * Update picked quantity for a specific item in the picking task.
     *
     * @param resultId Picking Item Result ID
     * @param idempotencyKey Unique key to prevent duplicate requests
     * @param request Update request with picked_qty and picked_qty_type
     */
    @POST("/api/picking/tasks/{wms_picking_item_result_id}/update")
    suspend fun updatePickingResult(
        @Path("wms_picking_item_result_id") resultId: Int,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: UpdatePickingRequest
    ): ApiEnvelope<UpdatePickingResponse>

    /**
     * POST /api/picking/tasks/{id}/complete
     * Complete a picking task. Status will be COMPLETED if all items are picked,
     * SHORTAGE if any shortages exist.
     *
     * @param id Picking Task ID (wave.wms_picking_task_id)
     * @param idempotencyKey Unique key to prevent duplicate requests
     */
    @POST("/api/picking/tasks/{id}/complete")
    suspend fun completePickingTask(
        @Path("id") id: Int,
        @Header("Idempotency-Key") idempotencyKey: String
    ): ApiEnvelope<CompleteTaskResponse>

    /**
     * POST /api/picking/tasks/{wms_picking_item_result_id}/cancel
     * Cancel a picked item, reverting its status to PENDING.
     * This removes the item from history (PICKING) and makes it available for re-registration.
     *
     * @param resultId Picking Item Result ID
     * @param idempotencyKey Unique key to prevent duplicate requests
     */
    @POST("/api/picking/tasks/{wms_picking_item_result_id}/cancel")
    suspend fun cancelPickingItem(
        @Path("wms_picking_item_result_id") resultId: Int,
        @Header("Idempotency-Key") idempotencyKey: String
    ): ApiEnvelope<CancelPickingResponse>
}
