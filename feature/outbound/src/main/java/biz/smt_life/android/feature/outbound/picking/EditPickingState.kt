package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

/**
 * UI State for Edit Picking screen (出庫検品 編集).
 * Supports case/piece (ケース/バラ) editing for a single history item.
 */
data class EditPickingState(
    val item: PickingTaskItem? = null,
    val taskId: Int = 0,
    val inputCases: String = "0",
    val inputPieces: String = "0",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
) {
    /**
     * 受注数 ケース (read-only display).
     */
    val orderCases: Int
        get() {
            val item = item ?: return 0
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
            val item = item ?: return 0
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
            val cf = item?.capacityCase ?: 1
            return ((cases * cf) + pieces).toDouble()
        }

    val canSave: Boolean
        get() = !isSaving && item != null &&
                (inputCases.isNotBlank() || inputPieces.isNotBlank())
}
