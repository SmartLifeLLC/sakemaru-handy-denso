package biz.smt_life.android.core.network.api

import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingProductResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.UpdateWorkItemRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for Incoming (入庫) API endpoints.
 * Based on incoming-api-android-prompt.md specification.
 *
 * Security: Requires X-API-Key and Authorization Bearer token (added by interceptors)
 */
interface IncomingApi {

    // ============================================================
    // Warehouse Endpoints
    // ============================================================

    /**
     * GET /api/master/warehouses
     * Fetch list of warehouses for warehouse selection screen.
     */
    @GET("/api/master/warehouses")
    suspend fun getWarehouses(): ApiEnvelope<List<WarehouseResponse>>

    // ============================================================
    // Schedule Endpoints
    // ============================================================

    /**
     * GET /api/incoming/schedules
     * Fetch incoming schedules (products with pending incoming).
     *
     * @param warehouseId Required - Filter by warehouse
     * @param search Optional - Search by JAN code, item code, or item name
     */
    @GET("/api/incoming/schedules")
    suspend fun getSchedules(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null
    ): ApiEnvelope<List<IncomingProductResponse>>

    /**
     * GET /api/incoming/schedules/{id}
     * Fetch details of a specific incoming schedule.
     *
     * @param id Schedule ID
     */
    @GET("/api/incoming/schedules/{id}")
    suspend fun getScheduleDetail(
        @Path("id") id: Int
    ): ApiEnvelope<IncomingProductResponse>

    // ============================================================
    // Work Item Endpoints
    // ============================================================

    /**
     * GET /api/incoming/work-items
     * Fetch work items (history of incoming work).
     *
     * @param warehouseId Required - Filter by warehouse
     * @param pickerId Optional - Filter by picker
     * @param status Optional - Filter by status: "WORKING", "COMPLETED", "CANCELLED", "all" (default: "WORKING")
     * @param fromDate Optional - Start date (YYYY-MM-DD)
     * @param toDate Optional - End date (YYYY-MM-DD)
     * @param limit Optional - Number of records to fetch (default: 100)
     */
    @GET("/api/incoming/work-items")
    suspend fun getWorkItems(
        @Query("warehouse_id") warehouseId: Int,
        @Query("picker_id") pickerId: Int? = null,
        @Query("status") status: String? = null,
        @Query("from_date") fromDate: String? = null,
        @Query("to_date") toDate: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<IncomingWorkItemResponse>>

    /**
     * POST /api/incoming/work-items
     * Start a new work item for an incoming schedule.
     *
     * @param request Contains incoming_schedule_id, picker_id, warehouse_id
     * @return Created work item. If already working on this schedule, returns existing work item
     *         with code "ALREADY_WORKING"
     */
    @POST("/api/incoming/work-items")
    suspend fun startWork(
        @Body request: StartWorkRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    /**
     * PUT /api/incoming/work-items/{id}
     * Update an existing work item (quantity, dates, location).
     *
     * @param id Work item ID
     * @param request Update data: work_quantity, work_arrival_date, work_expiration_date, location_id
     */
    @PUT("/api/incoming/work-items/{id}")
    suspend fun updateWorkItem(
        @Path("id") id: Int,
        @Body request: UpdateWorkItemRequest
    ): ApiEnvelope<IncomingWorkItemResponse>

    /**
     * POST /api/incoming/work-items/{id}/complete
     * Complete a work item (finalize incoming).
     *
     * @param id Work item ID
     */
    @POST("/api/incoming/work-items/{id}/complete")
    suspend fun completeWorkItem(
        @Path("id") id: Int
    ): ApiEnvelope<Unit?>

    /**
     * DELETE /api/incoming/work-items/{id}
     * Cancel a work item.
     *
     * @param id Work item ID
     */
    @DELETE("/api/incoming/work-items/{id}")
    suspend fun cancelWorkItem(
        @Path("id") id: Int
    ): ApiEnvelope<Unit?>

    // ============================================================
    // Location Endpoints
    // ============================================================

    /**
     * GET /api/incoming/locations
     * Search for locations (for location autocomplete).
     *
     * @param warehouseId Required - Filter by warehouse
     * @param search Optional - Search keyword (code1, code2, code3, name)
     * @param limit Optional - Number of records (default: 50)
     */
    @GET("/api/incoming/locations")
    suspend fun searchLocations(
        @Query("warehouse_id") warehouseId: Int,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int? = null
    ): ApiEnvelope<List<LocationResponse>>
}
