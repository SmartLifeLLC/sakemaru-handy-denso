package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryCountListData(
    @SerialName("inventory_counts") val inventoryCounts: List<InventoryCountResponse> = emptyList()
)

@Serializable
data class InventoryCountResponse(
    val id: Int,
    @SerialName("count_no") val countNo: String,
    @SerialName("warehouse_id") val warehouseId: Int,
    @SerialName("warehouse_code") val warehouseCode: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("count_date") val countDate: String? = null,
    val status: String,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("current_round") val currentRound: Int = 1,
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("counted_items") val countedItems: Int = 0,
    @SerialName("final_counted_items") val finalCountedItems: Int = 0
)

@Serializable
data class InventoryCountScanData(
    val items: List<InventoryCountItemResponse> = emptyList()
)

@Serializable
data class InventoryCountSubmitData(
    val item: InventoryCountItemResponse
)

@Serializable
data class InventoryItemsData(
    val items: List<InventoryCountItemResponse> = emptyList(),
    val meta: InventoryPageMeta? = null
)

@Serializable
data class InventoryPageMeta(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 500,
    val total: Int = 0,
    @SerialName("last_page") val lastPage: Int = 1
)

@Serializable
data class InventoryBulkCountData(
    @SerialName("updated_count") val updatedCount: Int = 0,
    @SerialName("missing_item_ids") val missingItemIds: List<Int> = emptyList(),
    val items: List<InventoryCountItemResponse> = emptyList()
)

@Serializable
data class InventoryCountItemResponse(
    val id: Int,
    @SerialName("paper_barcode") val paperBarcode: String? = null,
    @SerialName("search_text") val searchText: String? = null,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    val barcode: String? = null,
    val volume: String? = null,
    @SerialName("volume_unit") val volumeUnit: String? = null,
    @SerialName("volume_unit_label") val volumeUnitLabel: String? = null,
    @SerialName("capacity_case") val capacityCase: Int = 1,
    val location: InventoryLocationResponse? = null,
    @SerialName("system_quantity") val systemQuantity: Int = 0,
    @SerialName("system_case_quantity") val systemCaseQuantity: Int = 0,
    @SerialName("system_piece_quantity") val systemPieceQuantity: Int = 0,
    @SerialName("system_total_piece_quantity") val systemTotalPieceQuantity: Int = 0,
    @SerialName("first_count_quantity") val firstCountQuantity: Double? = null,
    @SerialName("second_count_quantity") val secondCountQuantity: Double? = null,
    @SerialName("final_count_quantity") val finalCountQuantity: Double? = null,
    @SerialName("current_count_quantity") val currentCountQuantity: Double? = null,
    @SerialName("difference_quantity") val differenceQuantity: Double? = null,
    @SerialName("input_count") val inputCount: Int = 0,
    @SerialName("search_codes") val searchCodes: List<SearchCode> = emptyList()
)

@Serializable
data class InventoryLocationResponse(
    val id: Int? = null,
    @SerialName("floor_name") val floorName: String? = null,
    @SerialName("location_no") val locationNo: String? = null,
    val code1: String? = null,
    val code2: String? = null,
    val code3: String? = null
)

@Serializable
data class InventoryCountRequest(
    @SerialName("case_quantity") val caseQuantity: Int,
    @SerialName("piece_quantity") val pieceQuantity: Int,
    @SerialName("count_round") val countRound: Int,
    @SerialName("device_id") val deviceId: String? = "DENSO",
    val quantity: Int? = null,
    @SerialName("search_code") val searchCode: String? = null,
    @SerialName("request_uuid") val requestUuid: String
)

@Serializable
data class InventoryBulkCountRequest(
    @SerialName("count_round") val countRound: Int,
    @SerialName("device_id") val deviceId: String? = "DENSO",
    val items: List<InventoryBulkCountItemRequest>
)

@Serializable
data class JanCodeEntry(
    @SerialName("i") val compactItemId: Int? = null,
    @SerialName("item_id") val legacyItemId: Int? = null,
    @SerialName("ct") val codeType: String? = null,
    @SerialName("t") val compactQuantityType: String? = null,
    @SerialName("quantity_type") val legacyQuantityType: String? = null,
    @SerialName("q") val compactPackageQuantity: Int? = null,
    @SerialName("package_quantity") val legacyPackageQuantity: Int? = null
) {
    val itemId: Int get() = compactItemId ?: legacyItemId ?: 0
    val quantityType: String get() = decodeInventoryQuantityType(compactQuantityType ?: legacyQuantityType)
    val packageQuantity: Int get() = (compactPackageQuantity ?: legacyPackageQuantity ?: 1).coerceAtLeast(1)
}

@Serializable
data class JanCodeDictionaryData(
    @SerialName("jan_codes") val janCodes: Map<String, List<JanCodeEntry>> = emptyMap()
)

@Serializable
data class SearchCode(
    @SerialName("c") val compactCode: String? = null,
    @SerialName("code") val legacyCode: String? = null,
    @SerialName("ct") val codeType: String? = null,
    @SerialName("t") val compactQuantityType: String? = null,
    @SerialName("quantity_type") val legacyQuantityType: String? = null,
    @SerialName("q") val compactPackageQuantity: Int? = null,
    @SerialName("package_quantity") val legacyPackageQuantity: Int? = null
) {
    val code: String get() = compactCode ?: legacyCode.orEmpty()
    val quantityType: String get() = decodeInventoryQuantityType(compactQuantityType ?: legacyQuantityType)
    val packageQuantity: Int get() = (compactPackageQuantity ?: legacyPackageQuantity ?: 1).coerceAtLeast(1)
}

@Serializable
data class InventoryBulkCountItemRequest(
    @SerialName("item_id") val itemId: Int,
    @SerialName("case_quantity") val caseQuantity: Int,
    @SerialName("piece_quantity") val pieceQuantity: Int,
    val quantity: Int? = null,
    @SerialName("search_code") val searchCode: String? = null,
    @SerialName("request_uuid") val requestUuid: String
)

@Serializable
data class InventoryRescueRequest(
    /** 送信単位の冪等キー。成功するまで同じ値で再送する */
    @SerialName("upload_uuid") val uploadUuid: String? = null,
    @SerialName("original_count_id") val originalCountId: Int,
    @SerialName("original_count_no") val originalCountNo: String,
    @SerialName("count_round") val countRound: Int,
    @SerialName("device_id") val deviceId: String? = "DENSO",
    val items: List<InventoryRescueItemRequest>
)

@Serializable
data class InventoryRescueItemRequest(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("location_no") val locationNo: String? = null,
    @SerialName("case_quantity") val caseQuantity: Int,
    @SerialName("piece_quantity") val pieceQuantity: Int,
    @SerialName("total_pieces") val totalPieces: Int,
    @SerialName("search_code") val searchCode: String? = null,
    @SerialName("package_quantity") val packageQuantity: Int? = null,
    @SerialName("request_uuid") val requestUuid: String,
    @SerialName("input_at") val inputAt: Long
)

@Serializable
data class InventoryRescueData(
    @SerialName("rescue_id") val rescueId: Int,
    @SerialName("received_count") val receivedCount: Int,
    val duplicated: Boolean = false
)

private fun decodeInventoryQuantityType(value: String?): String =
    when (value) {
        "0" -> "PIECE"
        "1" -> "CASE"
        "2" -> "CARTON"
        else -> value ?: "PIECE"
    }
