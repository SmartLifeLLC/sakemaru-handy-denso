package biz.smt_life.android.core.domain.model

/**
 * Domain models for Picking Tasks.
 * UI-friendly representation mapped from API responses.
 */

/**
 * UI counters for picking tasks.
 * Centralized calculation based on server-provided status values.
 */
data class PickingTaskUiCounters(
    val totalCount: Int,        // Total items in picking_list
    val registeredCount: Int,   // Items that are not PENDING (PICKING, COMPLETED, SHORTAGE)
    val pendingCount: Int       // Items with status == PENDING
)

/**
 * Represents a picking task grouped by delivery course and picking area.
 */
data class PickingTask(
    val taskId: Int,
    val waveId: Int,
    val courseName: String,
    val courseCode: String,
    val pickingAreaName: String,
    val pickingAreaCode: String,
    val items: List<PickingTaskItem>
) {
    // Status-based counts (server-controlled, computed from items)
    val totalItems: Int
        get() = items.size

    val pendingCount: Int
        get() = items.count { it.status == ItemStatus.PENDING }

    val pickingCount: Int
        get() = items.count { it.status == ItemStatus.PICKING }

    val completedOrShortageCount: Int
        get() = items.count { it.status == ItemStatus.COMPLETED || it.status == ItemStatus.SHORTAGE }

    // Registered count = items that are not PENDING (i.e., PICKING, COMPLETED, SHORTAGE)
    val registeredCount: Int
        get() = items.count { it.status != ItemStatus.PENDING }

    // Completed items for legacy UI (items with picked_qty > 0 or status COMPLETED/SHORTAGE)
    val completedItems: Int
        get() = items.count { it.isCompleted }

    // Progress text for UI display
    val progressText: String
        get() = "$registeredCount/$totalItems"

    // Navigation mode determination (per spec 2.5.1)
    val hasUnregisteredItems: Boolean
        get() = pendingCount > 0

    val hasPickingItems: Boolean
        get() = pickingCount > 0

    val isFullyProcessed: Boolean
        get() = pendingCount == 0 && pickingCount == 0 && completedOrShortageCount == totalItems

    // Legacy properties (still used for UI display)
    val isCompleted: Boolean
        get() = completedItems == totalItems && totalItems > 0

    val isInProgress: Boolean
        get() = completedItems > 0 && completedItems < totalItems

    /**
     * Convert to UI counters for consistent display.
     * Use this helper in all screens that show "登録件数/全体件数".
     */
    fun toUiCounters(): PickingTaskUiCounters = PickingTaskUiCounters(
        totalCount = totalItems,
        registeredCount = registeredCount,
        pendingCount = pendingCount
    )
}

/**
 * Individual item in a picking task.
 */
data class PickingTaskItem(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val janCode: String?,
    val volume: String?, // e.g., "720ml" (容量)
    val capacityCase: Int?, // Units per case (入数)
    val packaging: String?,
    val temperatureType: String?,
    val images: List<String>, // Image URLs for 画像 button
    val plannedQtyType: QuantityType,
    val plannedQty: Double,
    val pickedQty: Double,
    val status: ItemStatus, // Server-controlled status
    val walkingOrder: Int,
    val slipNumber: Int
) {
    val isCompleted: Boolean
        get() = status == ItemStatus.COMPLETED || status == ItemStatus.SHORTAGE
}

/**
 * Status of picking item as determined by server.
 * Client must never modify status locally; always use server response.
 */
enum class ItemStatus {
    PENDING,      // Not yet registered
    PICKING,      // Registered, in history, can still be edited
    COMPLETED,    // Fully picked
    SHORTAGE;     // Shortage detected

    companion object {
        fun fromString(value: String): ItemStatus = when (value.uppercase()) {
            "PENDING" -> PENDING
            "PICKING" -> PICKING
            "COMPLETED" -> COMPLETED
            "SHORTAGE" -> SHORTAGE
            else -> PENDING // Default fallback
        }
    }
}

/**
 * Quantity type for picking items.
 */
enum class QuantityType {
    CASE,
    PIECE;

    companion object {
        fun fromString(value: String): QuantityType = when (value.uppercase()) {
            "CASE" -> CASE
            "PIECE" -> PIECE
            else -> PIECE // Default fallback
        }
    }
}
