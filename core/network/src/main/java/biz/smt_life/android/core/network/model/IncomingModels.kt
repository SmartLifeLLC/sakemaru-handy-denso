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
    @SerialName("slip_number") val slipNumber: String? = null,
    @SerialName("order_date") val orderDate: String? = null,
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("expected_piece_quantity") val expectedPieceQuantity: Int? = null,
    @SerialName("received_piece_quantity") val receivedPieceQuantity: Int? = null,
    @SerialName("remaining_piece_quantity") val remainingPieceQuantity: Int? = null,
    @SerialName("quantity_type") val quantityType: String? = null, // "PIECE" or "CASE"
    @SerialName("expected_arrival_date") val expectedArrivalDate: String? = null,
    @SerialName("expiration_date") val expirationDate: String? = null,
    val status: String? = null, // "PENDING", "PARTIAL", "CONFIRMED", "TRANSMITTED", "CANCELLED"
    val location: LocationResponse? = null
)

@Serializable
data class IncomingSnapshotResponse(
    val version: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("inspection_date") val inspectionDate: String = "",
    val warehouse: WarehouseResponse? = null,
    val rules: IncomingSnapshotRulesResponse? = null,
    val schedules: List<IncomingSnapshotScheduleResponse> = emptyList(),
    @SerialName("confirmed_eos_index") val confirmedEosIndex: List<IncomingConfirmedEosIndexResponse> = emptyList(),
    val items: List<IncomingSnapshotItemResponse> = emptyList(),
    val locations: List<LocationResponse> = emptyList()
)

@Serializable
data class IncomingSnapshotRulesResponse(
    @SerialName("eos_inspection_policy") val eosInspectionPolicy: String? = null,
    @SerialName("eos_confirmed_index_days") val eosConfirmedIndexDays: Int? = null,
    @SerialName("unplanned_order_source") val unplannedOrderSource: String? = null,
    @SerialName("quantity_input") val quantityInput: String? = null,
    @SerialName("matching_warehouse_ids") val matchingWarehouseIds: List<Int> = emptyList()
)

@Serializable
data class IncomingSnapshotScheduleResponse(
    val id: Int = 0,
    @SerialName("warehouse_id") val warehouseId: Int = 0,
    val warehouse: WarehouseResponse? = null,
    @SerialName("slip_number") val slipNumber: String? = null,
    @SerialName("order_source") val orderSource: String? = null,
    @SerialName("order_source_label") val orderSourceLabel: String? = null,
    @SerialName("inspection_policy") val inspectionPolicy: String? = null,
    @SerialName("is_eos_sent") val isEosSent: Boolean = false,
    val status: String? = null,
    @SerialName("order_date") val orderDate: String? = null,
    @SerialName("expected_arrival_date") val expectedArrivalDate: String? = null,
    @SerialName("actual_arrival_date") val actualArrivalDate: String? = null,
    val contractor: IncomingContractorResponse? = null,
    val item: IncomingSnapshotItemResponse? = null,
    val location: LocationResponse? = null,
    val quantity: IncomingSnapshotQuantityResponse? = null
)

@Serializable
data class IncomingSnapshotQuantityResponse(
    @SerialName("quantity_type") val quantityType: String? = null,
    @SerialName("expected_quantity") val expectedQuantity: Int = 0,
    @SerialName("received_quantity") val receivedQuantity: Int = 0,
    @SerialName("remaining_quantity") val remainingQuantity: Int = 0,
    @SerialName("expected_piece_quantity") val expectedPieceQuantity: Int? = null,
    @SerialName("received_piece_quantity") val receivedPieceQuantity: Int? = null,
    @SerialName("remaining_piece_quantity") val remainingPieceQuantity: Int? = null,
    @SerialName("capacity_case") val capacityCase: Int? = null
)

@Serializable
data class IncomingSnapshotItemResponse(
    val id: Int = 0,
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
    @SerialName("supplier_id") val supplierId: Int? = null,
    @SerialName("search_codes") val searchCodes: List<ItemSearchCodeResponse> = emptyList(),
    @SerialName("item_quantity_codes") val itemQuantityCodes: List<ItemQuantityCodeResponse> = emptyList(),
    @SerialName("default_location") val defaultLocation: LocationResponse? = null,
    val contractors: List<IncomingContractorResponse> = emptyList()
)

@Serializable
data class IncomingContractorResponse(
    val id: Int? = null,
    val code: String? = null,
    val name: String? = null
)

@Serializable
data class IncomingConfirmedEosIndexResponse(
    val id: Int = 0,
    @SerialName("warehouse_id") val warehouseId: Int = 0,
    @SerialName("warehouse_code") val warehouseCode: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("slip_number") val slipNumber: String? = null,
    @SerialName("item_id") val itemId: Int? = null,
    @SerialName("item_code") val itemCode: String? = null,
    @SerialName("contractor_id") val contractorId: Int? = null,
    @SerialName("contractor_code") val contractorCode: String? = null,
    @SerialName("actual_arrival_date") val actualArrivalDate: String? = null,
    @SerialName("expected_arrival_date") val expectedArrivalDate: String? = null,
    @SerialName("received_piece_quantity") val receivedPieceQuantity: Int? = null
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

@Serializable
data class IncomingInspectionBatchSyncRequest(
    @SerialName("client_batch_uuid") val clientBatchUuid: String,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("inspection_date") val inspectionDate: String? = null,
    @SerialName("inspected_at") val inspectedAt: String? = null,
    @SerialName("picker_id") val pickerId: Int? = null,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("app_version") val appVersion: String? = null,
    val details: List<IncomingInspectionDetailRequest>
)

@Serializable
data class IncomingInspectionDetailRequest(
    @SerialName("client_line_uuid") val clientLineUuid: String,
    @SerialName("incoming_schedule_id") val incomingScheduleId: Int? = null,
    @SerialName("item_id") val itemId: Int? = null,
    @SerialName("item_code") val itemCode: String? = null,
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("scanned_code") val scannedCode: String? = null,
    @SerialName("slip_number") val slipNumber: String? = null,
    @SerialName("contractor_id") val contractorId: Int? = null,
    @SerialName("location_id") val locationId: Int? = null,
    @SerialName("case_quantity") val caseQuantity: Int = 0,
    @SerialName("piece_quantity") val pieceQuantity: Int = 0,
    @SerialName("capacity_case") val capacityCase: Int? = null,
    @SerialName("total_piece_quantity") val totalPieceQuantity: Int,
    @SerialName("expiration_date") val expirationDate: String? = null,
    @SerialName("inspected_at") val inspectedAt: String? = null
)

@Serializable
data class IncomingInspectionBatchSyncResponse(
    val batch: IncomingInspectionBatchResponse,
    val details: List<IncomingInspectionDetailResponse> = emptyList()
)

@Serializable
data class IncomingInspectionBatchResponse(
    val id: Int? = null,
    @SerialName("client_batch_uuid") val clientBatchUuid: String? = null,
    val status: String = "",
    @SerialName("total_detail_count") val totalDetailCount: Int = 0,
    @SerialName("success_count") val successCount: Int = 0,
    @SerialName("history_only_count") val historyOnlyCount: Int = 0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("error_count") val errorCount: Int = 0
)

@Serializable
data class IncomingInspectionDetailResponse(
    val id: Int? = null,
    @SerialName("client_line_uuid") val clientLineUuid: String = "",
    @SerialName("incoming_schedule_id") val incomingScheduleId: Int? = null,
    @SerialName("linked_confirmed_schedule_id") val linkedConfirmedScheduleId: Int? = null,
    @SerialName("created_schedule_id") val createdScheduleId: Int? = null,
    @SerialName("item_id") val itemId: Int? = null,
    @SerialName("item_code") val itemCode: String? = null,
    @SerialName("item_name") val itemName: String? = null,
    @SerialName("inspection_policy") val inspectionPolicy: String? = null,
    @SerialName("result_status") val resultStatus: String = "",
    @SerialName("review_reason") val reviewReason: String? = null,
    @SerialName("inspected_total_piece_quantity") val inspectedTotalPieceQuantity: Int = 0,
    @SerialName("applied_piece_quantity") val appliedPieceQuantity: Int = 0,
    @SerialName("shortage_piece_quantity") val shortagePieceQuantity: Int = 0
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
