package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

/**
 * UI State for Outbound Picking (P21 - Data Input Screen).
 * Supports case/piece (ケース/バラ) separated input.
 */
data class OutboundPickingState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val originalTask: PickingTask? = null,
    val pendingItems: List<PickingTaskItem> = emptyList(),
    val currentIndex: Int = 0,
    val inputCases: String = "0",     // 出荷数 ケース
    val inputPieces: String = "0",    // 出荷数 バラ
    val isUpdating: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val isCompleting: Boolean = false,
    val showImageDialog: Boolean = false,
    val warehouseId: Int = 0
) {
    val currentItem: PickingTaskItem?
        get() = pendingItems.getOrNull(currentIndex)

    val isLastItem: Boolean
        get() = currentIndex >= (pendingItems.size - 1)

    val canMovePrev: Boolean
        get() = currentIndex > 0

    val canMoveNext: Boolean
        get() = currentIndex < (pendingItems.size - 1)

    val totalCount: Int
        get() = originalTask?.totalItems ?: 0

    val registeredCount: Int
        get() = originalTask?.registeredCount ?: 0

    val pendingCount: Int
        get() = originalTask?.pendingCount ?: 0

    /**
     * 受注数 ケース (read-only display).
     */
    val orderCases: Int
        get() {
            val item = currentItem ?: return 0
            return when (item.plannedQtyType) {
                QuantityType.CASE -> item.plannedQty.toInt()
                QuantityType.PIECE -> {
                    val cf = item.capacityCase
                    if (cf != null && cf > 0) (item.plannedQty / cf).toInt() else 0
                }
            }
        }

    /**
     * 受注数 バラ (read-only display).
     */
    val orderPieces: Int
        get() {
            val item = currentItem ?: return 0
            return when (item.plannedQtyType) {
                QuantityType.CASE -> 0
                QuantityType.PIECE -> {
                    val cf = item.capacityCase
                    if (cf != null && cf > 0) (item.plannedQty.toInt() % cf)
                    else item.plannedQty.toInt()
                }
            }
        }

    /**
     * Total picked quantity in pieces for API submission.
     */
    val totalPickedQty: Double
        get() {
            val cases = inputCases.toIntOrNull() ?: 0
            val pieces = inputPieces.toIntOrNull() ?: 0
            val cf = currentItem?.capacityCase ?: 1
            return ((cases * cf) + pieces).toDouble()
        }

    val canRegister: Boolean
        get() = !isUpdating && currentItem != null &&
                (inputCases.isNotBlank() || inputPieces.isNotBlank())

    val hasImages: Boolean
        get() = currentItem?.images?.isNotEmpty() == true

    // Legacy compatibility
    val quantityTypeLabel: String
        get() = when (currentItem?.plannedQtyType?.name) {
            "CASE" -> "ケース"
            "PIECE" -> "バラ"
            else -> ""
        }

    // Keep for backward compat with deprecated task property
    @Deprecated("Use originalTask and pendingItems instead")
    val task: PickingTask?
        get() = originalTask?.copy(items = pendingItems)

    // Keep for backward compat
    val pickedQtyInput: String
        get() = totalPickedQty.toString()
}
