package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem

/**
 * UI State for Picking History screen (2.5.3 - 出庫処理＞履歴).
 *
 * Display modes:
 * - Editable mode: at least one PICKING item exists, show delete & confirm buttons
 * - Read-only mode: all items COMPLETED/SHORTAGE, hide buttons, list is read-only
 */
data class PickingHistoryState(
    val task: PickingTask? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null,
    val showConfirmDialog: Boolean = false,
    val itemToDelete: PickingTaskItem? = null
) {
    /**
     * Items to show in history (status == PICKING).
     */
    val historyItems: List<PickingTaskItem>
        get() = task?.items?.filter {
            it.status == biz.smt_life.android.core.domain.model.ItemStatus.PICKING
        } ?: emptyList()

    /**
     * Editable mode: at least one PICKING item exists.
     * In this mode, user can delete individual items and confirm all.
     */
    val isEditableMode: Boolean
        get() = task != null && task.hasPickingItems

    /**
     * Read-only mode: all items are COMPLETED or SHORTAGE.
     * In this mode, hide delete/confirm buttons.
     */
    val isReadOnlyMode: Boolean
        get() = task != null && task.isFullyProcessed

    /**
     * Whether the confirm-all button should be enabled.
     */
    val canConfirmAll: Boolean
        get() = isEditableMode && historyItems.isNotEmpty() && !isConfirming && !isDeleting
}
