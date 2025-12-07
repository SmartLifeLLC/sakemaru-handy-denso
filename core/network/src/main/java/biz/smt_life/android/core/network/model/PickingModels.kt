package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models for Picking Tasks API responses per handy_swagger_20251109.json.
 * These mirror the server response structure exactly.
 */

@Serializable
data class PickingTaskResponse(
    val course: CourseInfo,
    @SerialName("picking_area") val pickingArea: PickingAreaInfo,
    val wave: WaveInfo,
    @SerialName("picking_list") val pickingList: List<PickingItem>
)

@Serializable
data class CourseInfo(
    val code: String,
    val name: String
)

@Serializable
data class PickingAreaInfo(
    val code: String,
    val name: String
)

@Serializable
data class WaveInfo(
    @SerialName("wms_picking_task_id") val wmsPickingTaskId: Int,
    @SerialName("wms_wave_id") val wmsWaveId: Int
)

@Serializable
data class PickingItem(
    @SerialName("wms_picking_item_result_id") val wmsPickingItemResultId: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_name") val itemName: String,
    @SerialName("jan_code") val janCode: String? = null,
    @SerialName("volume") val volume: String? = null, // e.g., "720ml"
    @SerialName("capacity_case") val capacityCase: Int? = null, // Units per case (入数)
    val packaging: String? = null,
    @SerialName("temperature_type") val temperatureType: String? = null,
    val images: List<String> = emptyList(), // Image URLs
    @SerialName("planned_qty_type") val plannedQtyType: String, // "CASE" or "PIECE"
    @SerialName("planned_qty") val plannedQty: String,
    @SerialName("picked_qty") val pickedQty: String,
    val status: String = "PENDING", // "PENDING", "PICKING", "COMPLETED", "SHORTAGE"
    @SerialName("walking_order") val walkingOrder: Int = 0,
    @SerialName("slip_number") val slipNumber: Int
)

/**
 * Request/Response models for Picking Tasks mutations per handy_swagger_20251124.json.
 */

/**
 * Request body for POST /api/picking/tasks/{id}/start
 * Empty body (server only needs the path param and headers).
 */
@Serializable
data class StartTaskRequest(
    // Empty request body - server uses path param only
    val dummy: String? = null // Workaround for empty body serialization
)

/**
 * Response for POST /api/picking/tasks/{id}/start
 */
@Serializable
data class StartTaskResponse(
    @SerialName("id") val wmsPickingTaskId: Int,
    val status: String, // e.g., "PICKING"
    @SerialName("started_at") val startedAt: String? = null
)

/**
 * Request body for POST /api/picking/tasks/{wms_picking_item_result_id}/update
 */
@Serializable
data class UpdatePickingRequest(
    @SerialName("picked_qty") val pickedQty: String, // Decimal as string (e.g., "2.50")
    @SerialName("picked_qty_type") val pickedQtyType: String // "CASE" or "PIECE"
)

/**
 * Response for POST /api/picking/tasks/{wms_picking_item_result_id}/update
 * Returns the updated status for the item.
 * Note: Item details must be preserved from local state; API returns only minimal fields.
 */
@Serializable
data class UpdatePickingResponse(
    @SerialName("id") val wmsPickingItemResultId: Int,
    @SerialName("picked_qty") val pickedQty: String,
    @SerialName("shortage_qty") val shortageQty: String? = null,
    val status: String // "PENDING", "PICKING", "COMPLETED", "SHORTAGE"
)

/**
 * Response for POST /api/picking/tasks/{id}/complete
 */
@Serializable
data class CompleteTaskResponse(
    @SerialName("wms_picking_task_id") val wmsPickingTaskId: Int,
    val status: String, // "COMPLETED" or "SHORTAGE"
    @SerialName("completed_at") val completedAt: String? = null
)

/**
 * Response for POST /api/picking/tasks/{wms_picking_item_result_id}/cancel
 * Cancels a picked item, reverting its status to PENDING.
 */
@Serializable
data class CancelPickingResponse(
    @SerialName("id") val wmsPickingItemResultId: Int,
    @SerialName("picked_qty") val pickedQty: Int,
    @SerialName("shortage_qty") val shortageQty: Int,
    val status: String // "PENDING" after cancellation
)
