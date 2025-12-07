package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for Picking Tasks.
 * Provides access to picking task data and operations from the backend.
 *
 * SINGLE SOURCE OF TRUTH:
 * This repository maintains a StateFlow of all tasks in memory.
 * All ViewModels should observe this flow to ensure consistent state across screens.
 */
interface PickingTaskRepository {
    /**
     * Observable flow of all picking tasks.
     * This is the single source of truth for task data.
     * All ViewModels should observe this flow to stay synchronized.
     */
    val tasksFlow: StateFlow<List<PickingTask>>

    /**
     * Observable flow for a specific task by ID.
     * Returns null if task not found in current state.
     *
     * @param taskId The picking task ID to observe
     * @return Flow emitting the task or null
     */
    fun taskFlow(taskId: Int): Flow<PickingTask?>

    /**
     * Fetch picking tasks for "My Area" tab (filtered by picker).
     * Updates the tasksFlow on success.
     *
     * @param warehouseId Required warehouse ID
     * @param pickerId Picker ID for filtering
     * @return Result containing list of picking tasks or error
     */
    suspend fun getMyAreaTasks(warehouseId: Int, pickerId: Int): Result<List<PickingTask>>

    /**
     * Fetch all picking tasks for "All Courses" tab (no picker filter).
     * Updates the tasksFlow on success.
     *
     * @param warehouseId Required warehouse ID
     * @return Result containing list of picking tasks or error
     */
    suspend fun getAllTasks(warehouseId: Int): Result<List<PickingTask>>

    /**
     * Start a picking task. Changes status to PICKING and sets started_at timestamp.
     *
     * @param taskId Picking Task ID (wave.wms_picking_task_id)
     * @return Result containing unit on success or error
     */
    suspend fun startTask(taskId: Int): Result<Unit>

    /**
     * Update picked quantity for a specific item in the picking task.
     * After successful update, refreshes the task in tasksFlow.
     *
     * @param resultId Picking Item Result ID
     * @param pickedQty Picked quantity
     * @param pickedQtyType Quantity type ("CASE" or "PIECE")
     * @return Result containing updated item or error
     */
    suspend fun updatePickingItem(
        resultId: Int,
        pickedQty: Double,
        pickedQtyType: String
    ): Result<PickingTaskItem>

    /**
     * Complete a picking task. Status will be COMPLETED if all items are picked,
     * SHORTAGE if any shortages exist.
     *
     * @param taskId Picking Task ID (wave.wms_picking_task_id)
     * @return Result containing unit on success or error
     */
    suspend fun completeTask(taskId: Int): Result<Unit>

    /**
     * Cancel a picked item, reverting its status to PENDING.
     * After successful cancellation, refreshes the task to update tasksFlow.
     *
     * @param resultId Picking Item Result ID
     * @param taskId Picking Task ID (for refreshing after cancel)
     * @param warehouseId Warehouse ID (for refreshing after cancel)
     * @return Result containing unit on success or error
     */
    suspend fun cancelPickingItem(
        resultId: Int,
        taskId: Int,
        warehouseId: Int
    ): Result<Unit>

    /**
     * Refresh a single picking task from the server.
     * Used after mutations to get updated status and counts.
     * Updates the task in tasksFlow on success.
     *
     * @param taskId Picking Task ID (wave.wms_picking_task_id)
     * @param warehouseId Warehouse ID for filtering
     * @return Result containing the updated task or error
     */
    suspend fun refreshTask(taskId: Int, warehouseId: Int): Result<PickingTask>
}
