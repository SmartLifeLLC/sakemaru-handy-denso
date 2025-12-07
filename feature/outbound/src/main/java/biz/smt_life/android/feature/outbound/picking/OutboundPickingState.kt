package biz.smt_life.android.feature.outbound.picking

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem

/**
 * UI State for Outbound Picking (2.5.2 - Data Input Screen).
 * Represents the state for picking items in a selected task.
 *
 * Important:
 * - `originalTask` holds the full task with all items (for computing counters)
 * - `pendingItems` is a filtered list of PENDING items only (for navigation)
 * - Counters (registeredCount, totalCount) are always computed from `originalTask`
 */
data class OutboundPickingState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val originalTask: PickingTask? = null,      // Full task from server
    val pendingItems: List<PickingTaskItem> = emptyList(), // Filtered PENDING items
    val currentIndex: Int = 0,                  // Index within pendingItems
    val pickedQtyInput: String = "",
    val isUpdating: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val isCompleting: Boolean = false,
    val showImageDialog: Boolean = false,
    val warehouseId: Int = 0                    // Warehouse ID for refreshing task
) {
    /**
     * Backward compatibility: task property returns filtered task for existing code.
     * @Deprecated Use originalTask and pendingItems instead.
     */
    @Deprecated("Use originalTask and pendingItems instead")
    val task: PickingTask?
        get() = originalTask?.copy(items = pendingItems)
    /**
     * Current item being picked (from pendingItems list).
     */
    val currentItem: PickingTaskItem?
        get() = pendingItems.getOrNull(currentIndex)

    /**
     * Whether this is the last PENDING item.
     */
    val isLastItem: Boolean
        get() = currentIndex >= (pendingItems.size - 1)

    /**
     * Whether user can navigate to previous PENDING item.
     */
    val canMovePrev: Boolean
        get() = currentIndex > 0

    /**
     * Whether user can navigate to next PENDING item.
     */
    val canMoveNext: Boolean
        get() = currentIndex < (pendingItems.size - 1)

    /**
     * Counters for header display (based on originalTask, not filtered items).
     */
    val totalCount: Int
        get() = originalTask?.totalItems ?: 0

    val registeredCount: Int
        get() = originalTask?.registeredCount ?: 0

    val pendingCount: Int
        get() = originalTask?.pendingCount ?: 0

    /**
     * Helper to get quantity type label (CASE / PIECE) in Japanese.
     */
    val quantityTypeLabel: String
        get() = when (currentItem?.plannedQtyType?.name) {
            "CASE" -> "ケース"
            "PIECE" -> "バラ"
            else -> ""
        }

    /**
     * Helper to format quantity for display (e.g., "2.0 ケース").
     */
    fun formatQuantity(qty: Double, type: String): String {
        val typeLabel = when (type) {
            "CASE" -> "ケース"
            "PIECE" -> "バラ"
            else -> ""
        }
        return "${String.format("%.1f", qty)} $typeLabel"
    }

    /**
     * Whether the register button should be enabled.
     */
    val canRegister: Boolean
        get() = !isUpdating && pickedQtyInput.isNotBlank() && currentItem != null

    /**
     * Whether the image button should be enabled (images available).
     */
    val hasImages: Boolean
        get() = currentItem?.images?.isNotEmpty() == true
}
