package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem

/**
 * UI State for Picking History screen (P22 - 出庫処理＞履歴).
 *
 * Display modes:
 * - Editable mode: at least one PICKING item exists → F1:送信
 * - Read-only mode: all COMPLETED/SHORTAGE → read-only list
 */
data class PickingHistoryState(
    val task: PickingTask? = null,
    val isLoading: Boolean = false,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null,
    val showConfirmDialog: Boolean = false
) {
    /**
     * Items to show in history list: all non-PENDING items.
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
        get() = isEditableMode && pickingItemCount > 0 && !isConfirming
}
