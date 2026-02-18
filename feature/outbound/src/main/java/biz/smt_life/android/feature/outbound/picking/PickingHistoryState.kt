package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem

/**
 * UI State for Picking History screen (P22 - 出庫処理＞履歴).
 *
 * Display modes:
 * - Editable mode: at least one PICKING item exists → F2:戻る / F3:削除 / F4:確定
 * - Read-only mode: all items COMPLETED/SHORTAGE → F2:戻る only + message + list
 */
data class PickingHistoryState(
    val task: PickingTask? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null,
    val showConfirmDialog: Boolean = false,
    val itemToDelete: PickingTaskItem? = null,
    val selectedItem: PickingTaskItem? = null
) {
    /**
     * Items to show in history list: all non-PENDING items.
     * Per spec: PICKING + COMPLETED + SHORTAGE are displayed.
     */
    val historyItems: List<PickingTaskItem>
        get() = task?.items?.filter {
            it.status != ItemStatus.PENDING
        } ?: emptyList()

    /**
     * Count of PICKING items (for confirm dialog).
     */
    val pickingItemCount: Int
        get() = task?.items?.count { it.status == ItemStatus.PICKING } ?: 0

    /**
     * Editable mode: at least one PICKING item exists.
     */
    val isEditableMode: Boolean
        get() = task != null && task.hasPickingItems

    /**
     * Read-only mode: all items are COMPLETED or SHORTAGE.
     */
    val isReadOnlyMode: Boolean
        get() = task != null && task.isFullyProcessed

    /**
     * Whether the confirm-all button should be enabled.
     */
    val canConfirmAll: Boolean
        get() = isEditableMode && pickingItemCount > 0 && !isConfirming && !isDeleting

    /**
     * Whether an item is currently selected (for F3 delete).
     */
    val hasSelection: Boolean
        get() = selectedItem != null && selectedItem.status == ItemStatus.PICKING
}
