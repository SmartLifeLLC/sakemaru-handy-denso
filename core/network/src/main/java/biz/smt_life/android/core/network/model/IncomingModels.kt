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

// ============================================================
// Item Location Search Models
// ============================================================

@Serializable
data class ItemLocationSearchResponse(
    val item: LocationSearchItemResponse,
    val warehouse: LocationSearchWarehouseResponse,
    val stock: LocationSearchStockResponse,
    val locations: LocationSearchLocationsResponse
)

@Serializable
data class LocationSearchItemResponse(
    val id: Int,
    val code: String = "",
    val name: String = "",
    val kana: String? = null,
    val volume: String? = null,
    @SerialName("volume_unit") val volumeUnit: String? = null,
    @SerialName("capacity_case") val capacityCase: Int? = null,
    @SerialName("capacity_carton") val capacityCarton: Int? = null,
    val packaging: String? = null,
    @SerialName("temperature_type") val temperatureType: String? = null,
    @SerialName("uses_expiration_date") val usesExpirationDate: Boolean = false,
    val images: List<String> = emptyList(),
    @SerialName("search_codes") val searchCodes: List<ItemSearchCodeResponse> = emptyList(),
    @SerialName("jan_codes") val janCodes: List<String> = emptyList(),
    @SerialName("item_quantity_codes") val itemQuantityCodes: List<ItemQuantityCodeResponse> = emptyList()
)

@Serializable
data class ItemSearchCodeResponse(
    val code: String = "",
    @SerialName("code_type") val codeType: String? = null,
    @SerialName("quantity_type") val quantityType: String? = null,
    val priority: Int? = null
)

@Serializable
data class ItemQuantityCodeResponse(
    @SerialName("product_code") val productCode: String? = null,
    @SerialName("own_code") val ownCode: String? = null,
    @SerialName("quantity_code") val quantityCode: String? = null,
    val quantity: Int? = null,
    @SerialName("can_order") val canOrder: Boolean = false
)

@Serializable
data class LocationSearchWarehouseResponse(
    val id: Int,
    val code: String = "",
    val name: String = "",
    @SerialName("kana_name") val kanaName: String? = null
)

@Serializable
data class LocationSearchStockResponse(
    val status: String? = null,
    @SerialName("has_stock") val hasStock: Boolean = false,
    @SerialName("lot_count") val lotCount: Int = 0,
    @SerialName("location_count") val locationCount: Int = 0,
    @SerialName("current_quantity") val currentQuantity: Int = 0,
    @SerialName("reserved_quantity") val reservedQuantity: Int = 0,
    @SerialName("available_quantity") val availableQuantity: Int = 0,
    @SerialName("earliest_expiration_date") val earliestExpirationDate: String? = null,
    @SerialName("latest_expiration_date") val latestExpirationDate: String? = null
)

@Serializable
data class LocationSearchLocationsResponse(
    val suggested: ItemLocationResponse? = null,
    @SerialName("default") val defaultLocation: ItemLocationResponse? = null,
    val stock: List<StockLocationResponse> = emptyList()
)

@Serializable
data class ItemLocationResponse(
    val id: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("floor_id") val floorId: Int? = null,
    val code: String = "",
    @SerialName("display_name") val displayName: String = "",
    val name: String? = null,
    val source: String? = null,
    @SerialName("is_no_location") val isNoLocation: Boolean = false
)

@Serializable
data class StockLocationResponse(
    val id: Int,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("floor_id") val floorId: Int? = null,
    val code: String = "",
    @SerialName("display_name") val displayName: String = "",
    val name: String? = null,
    val source: String? = null,
    @SerialName("is_no_location") val isNoLocation: Boolean = false,
    @SerialName("lot_count") val lotCount: Int = 0,
    @SerialName("current_quantity") val currentQuantity: Int = 0,
    @SerialName("reserved_quantity") val reservedQuantity: Int = 0,
    @SerialName("available_quantity") val availableQuantity: Int = 0
)
