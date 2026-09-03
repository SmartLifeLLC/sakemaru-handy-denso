package biz.smt_life.android.sakemaru_handy_denso.warehousetransfer

import biz.smt_life.android.core.network.model.JanCodeEntry
import biz.smt_life.android.core.network.model.WarehouseTransferStockItem
import biz.smt_life.android.core.network.model.WarehouseTransferWarehouse
import java.util.UUID
import kotlinx.serialization.Serializable

enum class WarehouseTransferTab { MENU, SCAN, HISTORY }

/**
 * 倉庫移動（HANDY）の画面状態
 *
 * - 移動元倉庫 = ログインpickerの自店倉庫
 * - 移動先倉庫 = 送信時に選択
 * - 未送信履歴は送信成功時のみ削除する
 */
data class WarehouseTransferState(
    val loading: Boolean = false,
    val syncing: Boolean = false,
    val submitting: Boolean = false,
    val selectedTab: WarehouseTransferTab = WarehouseTransferTab.MENU,
    val fromWarehouseId: Int = 0,
    val keyword: String = "",
    val janDictionary: Map<String, List<JanCodeEntry>> = emptyMap(),
    val allItems: List<WarehouseTransferStockItem> = emptyList(),
    val items: List<WarehouseTransferStockItem> = emptyList(),
    val selectedItem: WarehouseTransferStockItem? = null,
    val scanQuantityType: String? = null,
    val scannedCode: String? = null,
    val scanPackageQuantity: Int? = null,
    val accumulatedBase: LocalWarehouseTransferInput? = null,
    val caseQuantity: String = "",
    val pieceQuantity: String = "",
    val dirtyInputs: Map<String, LocalWarehouseTransferInput> = emptyMap(),
    val syncedAt: Long? = null,
    /** 送信時の移動先選択ダイアログ */
    val destinationDialogVisible: Boolean = false,
    val destinationLoading: Boolean = false,
    val warehouses: List<WarehouseTransferWarehouse> = emptyList(),
    val selectedToWarehouseId: Int? = null,
    /** 直前の送信結果（送信成功後、履歴は自動削除されるため画面表示のみ） */
    val lastSubmitResult: WarehouseTransferSubmitResult? = null,
    val message: String? = null,
    val error: String? = null
) {
    val capacityCase: Int = scanPackageQuantity ?: selectedItem?.capacityCase?.coerceAtLeast(1) ?: 1
    val currentInputPieces: Int = run {
        val incCase = caseQuantity.toIntOrNull() ?: 0
        val incPiece = pieceQuantity.toIntOrNull() ?: 0
        (incCase * capacityCase) + incPiece
    }
    val totalPieces: Int = (accumulatedBase?.totalPieces ?: 0) + currentInputPieces
    val previousTotalPieces: Int = selectedItem?.let { dirtyInputs[it.localKey]?.totalPieces } ?: 0
    val hasLocalData: Boolean = allItems.isNotEmpty()
    val dirtyCount: Int = dirtyInputs.size
    val dirtyTotalPieces: Int = dirtyInputs.values.sumOf { it.totalPieces }
    val selectedToWarehouse: WarehouseTransferWarehouse? = warehouses.firstOrNull { it.id == selectedToWarehouseId }
}

@Serializable
data class LocalWarehouseTransferInput(
    val localKey: String,
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val barcode: String? = null,
    val realStockId: Int? = null,
    val locationNo: String? = null,
    val stockAllocationCode: String = "1",
    val caseQuantity: Int,
    val pieceQuantity: Int,
    val packageQuantity: Int? = null,
    val totalPieces: Int,
    val availableQuantityAtSync: Int? = null,
    val searchCode: String? = null,
    val requestUuid: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class WarehouseTransferSubmitResult(
    val candidateId: Int? = null,
    val candidateNo: String? = null,
    val toWarehouseName: String? = null,
    val itemCount: Int = 0,
    val totalQuantity: Int = 0,
    val missingCount: Int = 0,
    val sentAt: Long = System.currentTimeMillis()
)

/**
 * ローカルキャッシュ（移動元倉庫単位）
 *
 * 送信失敗時も未送信入力を保持し、再起動後に復元できる。
 */
@Serializable
data class WarehouseTransferLocalCache(
    val fromWarehouseId: Int,
    val items: List<WarehouseTransferStockItem> = emptyList(),
    val janDictionary: Map<String, List<JanCodeEntry>> = emptyMap(),
    val dirtyInputs: List<LocalWarehouseTransferInput> = emptyList(),
    val syncedAt: Long? = null,
    val lastToWarehouseId: Int? = null,
    /** 送信中に採番した upload_uuid（通信断後の再送で同じUUIDを使う） */
    val pendingUploadUuid: String? = null,
    val lastSubmitResult: WarehouseTransferSubmitResult? = null
)
