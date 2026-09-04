package biz.smt_life.android.sakemaru_handy_denso.inventory

import biz.smt_life.android.core.network.model.InventoryCountItemResponse
import biz.smt_life.android.core.network.model.InventoryCountResponse
import biz.smt_life.android.core.network.model.JanCodeEntry
import java.util.UUID
import kotlinx.serialization.Serializable

enum class InventoryTab { MENU, SCAN, HISTORY, SETTINGS }

data class InventoryCountState(
    val loading: Boolean = false,
    val syncing: Boolean = false,
    val submitting: Boolean = false,
    val counts: List<InventoryCountResponse> = emptyList(),
    val selectedCount: InventoryCountResponse? = null,
    val selectedTab: InventoryTab = InventoryTab.MENU,
    val keyword: String = "",
    val nameKeyword: String = "",
    val janDictionary: Map<String, List<JanCodeEntry>> = emptyMap(),
    val allItems: List<InventoryCountItemResponse> = emptyList(),
    val items: List<InventoryCountItemResponse> = emptyList(),
    val selectedItem: InventoryCountItemResponse? = null,
    val scanQuantityType: String? = null,
    val scannedCode: String? = null,
    val scanPackageQuantity: Int? = null,
    val accumulatedBase: LocalInventoryInput? = null,
    val countRound: Int = 1,
    val caseQuantity: String = "",
    val pieceQuantity: String = "",
    val dirtyInputs: Map<Int, LocalInventoryInput> = emptyMap(),
    val sentHistory: List<LocalInventoryInput> = emptyList(),
    val syncedAt: Long? = null,
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
    val previousTotalPieces: Int = run {
        val itemId = selectedItem?.id ?: return@run 0
        val dirtyTotal = dirtyInputs[itemId]?.totalPieces ?: 0
        val sentTotal = sentHistory.filter { it.itemId == itemId }.sumOf { it.totalPieces }
        dirtyTotal + sentTotal
    }
    val hasInstruction: Boolean = selectedCount != null || counts.isNotEmpty()
    val hasLocalData: Boolean = allItems.isNotEmpty()
    val dirtyCount: Int = dirtyInputs.size
}

@Serializable
data class LocalInventoryInput(
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val locationNo: String? = null,
    val countRound: Int,
    val caseQuantity: Int,
    val pieceQuantity: Int,
    val totalPieces: Int,
    val searchCode: String? = null,
    val packageQuantity: Int? = null,
    val requestUuid: String = UUID.randomUUID().toString(),
    val sent: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class InventoryLocalCache(
    val countId: Int,
    val countNo: String,
    val items: List<InventoryCountItemResponse> = emptyList(),
    val janDictionary: Map<String, List<JanCodeEntry>> = emptyMap(),
    val dirtyInputs: List<LocalInventoryInput> = emptyList(),
    val sentHistory: List<LocalInventoryInput> = emptyList(),
    val syncedAt: Long? = null,
    /** 退避送信の upload_uuid。成功するまで保持し、再送時に同じ値を使う（サーバー側で重複排除） */
    val pendingRescueUploadUuid: String? = null
)
