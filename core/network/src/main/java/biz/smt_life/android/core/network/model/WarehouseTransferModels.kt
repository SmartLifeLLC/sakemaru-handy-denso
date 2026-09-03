package biz.smt_life.android.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 倉庫移動候補（HANDY → WMS）API モデル
 *
 * - GET  /api/wms/warehouse-transfer/stock-items
 * - GET  /api/wms/warehouse-transfer/jan-codes
 * - GET  /api/wms/warehouse-transfer/warehouses
 * - POST /api/wms/warehouse-transfer-candidates
 */
@Serializable
data class WarehouseTransferStockItemsData(
    val items: List<WarehouseTransferStockItem> = emptyList(),
    val meta: InventoryPageMeta? = null
)

@Serializable
data class WarehouseTransferStockItem(
    val id: Int,
    @SerialName("real_stock_id") val realStockId: Int? = null,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    val barcode: String? = null,
    val volume: String? = null,
    @SerialName("volume_unit") val volumeUnit: String? = null,
    @SerialName("volume_unit_label") val volumeUnitLabel: String? = null,
    @SerialName("capacity_case") val capacityCase: Int = 1,
    @SerialName("capacity_carton") val capacityCarton: Int? = null,
    val location: InventoryLocationResponse? = null,
    @SerialName("stock_allocation_code") val stockAllocationCode: String = "1",
    @SerialName("stock_allocation_name") val stockAllocationName: String? = null,
    @SerialName("current_quantity") val currentQuantity: Double = 0.0,
    @SerialName("available_quantity") val availableQuantity: Double = 0.0,
    @SerialName("case_quantity") val caseQuantity: Int = 0,
    @SerialName("piece_quantity") val pieceQuantity: Int = 0,
    @SerialName("search_codes") val searchCodes: List<SearchCode> = emptyList()
) {
    /** 未送信入力のローカルキー（商品ID + 在庫区分CD） */
    val localKey: String get() = "${itemId}:${stockAllocationCode}"
    val availableTotalPieces: Int get() = availableQuantity.toInt()
}

@Serializable
data class WarehouseTransferWarehousesData(
    val warehouses: List<WarehouseTransferWarehouse> = emptyList()
)

@Serializable
data class WarehouseTransferWarehouse(
    val id: Int,
    val code: String,
    val name: String,
    @SerialName("kana_name") val kanaName: String? = null
)

@Serializable
data class WarehouseTransferSubmitRequest(
    @SerialName("upload_uuid") val uploadUuid: String,
    @SerialName("device_id") val deviceId: String? = "DENSO",
    @SerialName("from_warehouse_id") val fromWarehouseId: Int,
    @SerialName("to_warehouse_id") val toWarehouseId: Int,
    @SerialName("process_date") val processDate: String,
    @SerialName("delivered_date") val deliveredDate: String,
    val items: List<WarehouseTransferSubmitItem>
)

@Serializable
data class WarehouseTransferSubmitItem(
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String,
    @SerialName("real_stock_id") val realStockId: Int? = null,
    @SerialName("stock_allocation_code") val stockAllocationCode: String = "1",
    @SerialName("case_quantity") val caseQuantity: Int = 0,
    @SerialName("piece_quantity") val pieceQuantity: Int = 0,
    @SerialName("package_quantity") val packageQuantity: Int? = null,
    val quantity: Int,
    @SerialName("search_code") val searchCode: String? = null,
    @SerialName("request_uuid") val requestUuid: String
)

@Serializable
data class WarehouseTransferSubmitData(
    val candidate: WarehouseTransferCandidateSummary? = null,
    @SerialName("accepted_count") val acceptedCount: Int = 0,
    @SerialName("missing_item_ids") val missingItemIds: List<Int> = emptyList(),
    val duplicated: Boolean = false
)

@Serializable
data class WarehouseTransferCandidateSummary(
    val id: Int,
    @SerialName("candidate_no") val candidateNo: String,
    val status: String? = null,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("from_warehouse_id") val fromWarehouseId: Int? = null,
    @SerialName("from_warehouse_code") val fromWarehouseCode: String? = null,
    @SerialName("from_warehouse_name") val fromWarehouseName: String? = null,
    @SerialName("to_warehouse_id") val toWarehouseId: Int? = null,
    @SerialName("to_warehouse_code") val toWarehouseCode: String? = null,
    @SerialName("to_warehouse_name") val toWarehouseName: String? = null,
    @SerialName("process_date") val processDate: String? = null,
    @SerialName("delivered_date") val deliveredDate: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("total_quantity") val totalQuantity: Double = 0.0
)
