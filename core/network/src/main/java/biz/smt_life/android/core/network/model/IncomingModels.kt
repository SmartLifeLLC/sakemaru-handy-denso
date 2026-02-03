package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models for Incoming API responses.
 * Based on incoming-api-android-prompt.md specification.
 */

// ============================================================
// Warehouse Models
// ============================================================

@Serializable
data class WarehouseResponse(
    val id: Int,
    val code: String,
    val name: String,
    @SerialName("kana_name") val kanaName: String? = null,
    @SerialName("out_of_stock_option") val outOfStockOption: String? = null
)

// ============================================================
// Location Models
// ============================================================

@Serializable
data class LocationResponse(
    val id: Int,
    val code1: String? = null,
    val code2: String? = null,
    val code3: String? = null,
    val name: String? = null,
    @SerialName("display_name") val displayName: String? = null
)

// ============================================================
// Incoming Schedule Models
// ============================================================

@Serializable
data class IncomingProductResponse(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("search_code") val searchCode: String? = null,
    @SerialName("jan_codes") val janCodes: List<String> = emptyList(),
    val volume: String? = null,
    @SerialName("volume_unit") val volumeUnit: String? = null,
    @SerialName("capacity_case") val capacityCase: Int? = null,
    @SerialName("temperature_type") val temperatureType: String? = null,
    val images: List<String> = emptyList(),
    @SerialName("default_location") val defaultLocation: LocationResponse? = null,
    @SerialName("total_expected_quantity") val totalExpectedQuantity: Int = 0,
    @SerialName("total_received_quantity") val totalReceivedQuantity: Int = 0,
    @SerialName("total_remaining_quantity") val totalRemainingQuantity: Int = 0,
    val warehouses: List<IncomingWarehouseSummaryResponse> = emptyList(),
    val schedules: List<IncomingScheduleResponse> = emptyList()
)

@Serializable
data class IncomingWarehouseSummaryResponse(
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_code") val warehouseCode: String,
    @SerialName("warehouse_name") val warehouseName: String,
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0
)

@Serializable
data class IncomingScheduleResponse(
    val id: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("quantity_type") val quantityType: String? = null, // "PIECE" or "CASE"
    @SerialName("expected_arrival_date") val expectedArrivalDate: String? = null,
    @SerialName("expiration_date") val expirationDate: String? = null,
    val status: String? = null, // "PENDING", "PARTIAL", "CONFIRMED", "TRANSMITTED", "CANCELLED"
    val location: LocationResponse? = null
)

// ============================================================
// Incoming Work Item Models
// ============================================================

@Serializable
data class IncomingWorkItemResponse(
    val id: Int,
    @SerialName("incoming_schedule_id") val incomingScheduleId: Int,
    @SerialName("picker_id") val pickerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("location_id") val locationId: Int? = null,
    val location: LocationResponse? = null,
    @SerialName("work_quantity") val workQuantity: Int = 0,
    @SerialName("work_arrival_date") val workArrivalDate: String? = null,
    @SerialName("work_expiration_date") val workExpirationDate: String? = null,
    val status: String? = null, // "WORKING", "COMPLETED", "CANCELLED"
    @SerialName("started_at") val startedAt: String? = null,
    val schedule: WorkItemScheduleResponse? = null
)

@Serializable
data class WorkItemScheduleResponse(
    val id: Int,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String? = null,
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("jan_codes") val janCodes: List<String> = emptyList(),
    @SerialName("warehouse_id") val warehouseId: Int? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("quantity_type") val quantityType: String? = null,
    @SerialName("expected_arrival_date") val expectedArrivalDate: String? = null,
    val status: String? = null
)

// ============================================================
// Request Models
// ============================================================

@Serializable
data class StartWorkRequest(
    @SerialName("incoming_schedule_id") val incomingScheduleId: Int,
    @SerialName("picker_id") val pickerId: Int,
    @SerialName("warehouse_id") val warehouseId: Int
)

@Serializable
data class UpdateWorkItemRequest(
    @SerialName("work_quantity") val workQuantity: Int,
    @SerialName("work_arrival_date") val workArrivalDate: String? = null,
    @SerialName("work_expiration_date") val workExpirationDate: String? = null,
    @SerialName("location_id") val locationId: Int? = null
)
