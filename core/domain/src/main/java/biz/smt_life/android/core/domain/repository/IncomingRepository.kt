package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.UpdateWorkItemData

/**
 * Repository interface for Incoming (入庫) operations.
 * Provides access to incoming schedule data and work item operations.
 */
interface IncomingRepository {

    // ============================================================
    // Warehouse Operations
    // ============================================================

    /**
     * Fetch list of warehouses for warehouse selection.
     *
     * @return Result containing list of warehouses or error
     */
    suspend fun getWarehouses(): Result<List<IncomingWarehouse>>

    // ============================================================
    // Schedule Operations
    // ============================================================

    /**
     * Fetch incoming schedules (products with pending incoming).
     * Used for the product list screen.
     *
     * @param warehouseId Required warehouse ID
     * @param search Optional search query (JAN code, item code, or item name)
     * @return Result containing list of products with schedules or error
     */
    suspend fun getSchedules(
        warehouseId: Int,
        search: String? = null
    ): Result<List<IncomingProduct>>

    /**
     * Fetch details of a specific incoming schedule.
     *
     * @param id Schedule ID
     * @return Result containing product with schedule details or error
     */
    suspend fun getScheduleDetail(id: Int): Result<IncomingProduct>

    // ============================================================
    // Work Item Operations
    // ============================================================

    /**
     * Fetch work items (history of incoming work).
     *
     * @param warehouseId Required warehouse ID
     * @param pickerId Optional picker ID filter
     * @param status Optional status filter: "WORKING", "COMPLETED", "CANCELLED", "all"
     * @param fromDate Optional start date (YYYY-MM-DD)
     * @param toDate Optional end date (YYYY-MM-DD)
     * @param limit Optional limit on number of records
     * @return Result containing list of work items or error
     */
    suspend fun getWorkItems(
        warehouseId: Int,
        pickerId: Int? = null,
        status: String? = null,
        fromDate: String? = null,
        toDate: String? = null,
        limit: Int? = null
    ): Result<List<IncomingWorkItem>>

    /**
     * Fetch work items with WORKING status for the current picker.
     * Used to track which schedules are currently being worked on.
     *
     * @param warehouseId Required warehouse ID
     * @param pickerId Picker ID
     * @return Result containing list of working schedule IDs
     */
    suspend fun getWorkingScheduleIds(
        warehouseId: Int,
        pickerId: Int
    ): Result<Set<Int>>

    /**
     * Start work on a schedule. Creates a new work item or returns existing one.
     *
     * @param data Start work request data
     * @return Result containing created/existing work item or error
     *         If already working, returns the existing work item with code "ALREADY_WORKING"
     */
    suspend fun startWork(data: StartWorkData): Result<IncomingWorkItem>

    /**
     * Update an existing work item (quantity, dates, location).
     *
     * @param id Work item ID
     * @param data Update request data
     * @return Result containing updated work item or error
     */
    suspend fun updateWorkItem(id: Int, data: UpdateWorkItemData): Result<IncomingWorkItem>

    /**
     * Complete a work item (finalize incoming).
     *
     * @param id Work item ID
     * @return Result containing unit on success or error
     */
    suspend fun completeWorkItem(id: Int): Result<Unit>

    /**
     * Cancel a work item.
     *
     * @param id Work item ID
     * @return Result containing unit on success or error
     */
    suspend fun cancelWorkItem(id: Int): Result<Unit>

    // ============================================================
    // Location Operations
    // ============================================================

    /**
     * Search for locations (for location autocomplete).
     *
     * @param warehouseId Required warehouse ID
     * @param search Optional search keyword
     * @param limit Optional limit on number of records
     * @return Result containing list of locations or error
     */
    suspend fun searchLocations(
        warehouseId: Int,
        search: String? = null,
        limit: Int? = null
    ): Result<List<Location>>
}
