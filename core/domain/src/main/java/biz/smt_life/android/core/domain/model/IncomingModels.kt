package biz.smt_life.android.core.domain.model

/**
 * Domain models for Incoming (入庫) feature.
 * UI-friendly representation mapped from API responses.
 */

// ============================================================
// Warehouse (Extended for Incoming)
// ============================================================

/**
 * Warehouse with additional fields for incoming.
 */
data class IncomingWarehouse(
    val id: Int,
    val code: String,
    val name: String,
    val kanaName: String? = null,
    val outOfStockOption: String? = null
)

// ============================================================
// Location
// ============================================================

/**
 * Location (棚位置) for incoming.
 */
data class Location(
    val id: Int,
    val code1: String? = null,
    val code2: String? = null,
    val code3: String? = null,
    val name: String? = null,
    val displayName: String? = null
) {
    /**
     * Get the full display name, falling back to concatenated codes.
     */
    val fullDisplayName: String
        get() = displayName ?: listOfNotNull(code1, code2, code3).joinToString("-")
}

data class IncomingSnapshot(
    val inspectionDate: String,
    val generatedAt: String? = null,
    val warehouse: IncomingWarehouse? = null,
    val matchingWarehouseIds: List<Int> = emptyList(),
    val products: List<IncomingProduct> = emptyList(),
    val locations: List<Location> = emptyList()
)

// ============================================================
// Incoming Product (Product with schedules)
// ============================================================

/**
 * Product with incoming schedules.
 */
data class IncomingProduct(
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val searchCode: String? = null,
    val searchCodes: List<String> = emptyList(),
    val itemQuantityCodes: List<IncomingItemQuantityCode> = emptyList(),
    val janCodes: List<String> = emptyList(),
    val volume: String? = null,
    val volumeUnit: String? = null,
    val capacityCase: Int? = null,
    val temperatureType: String? = null,
    val images: List<String> = emptyList(),
    val defaultLocation: Location? = null,
    val totalExpectedQuantity: Int = 0,
    val totalReceivedQuantity: Int = 0,
    val totalRemainingQuantity: Int = 0,
    val warehouses: List<IncomingWarehouseSummary> = emptyList(),
    val schedules: List<IncomingSchedule> = emptyList()
) {
    /**
     * Get the primary JAN code for display.
     */
    val primaryJanCode: String?
        get() = janCodes.firstOrNull()

    /**
     * Get the full volume string (e.g., "720ml").
     */
    val fullVolume: String?
        get() = if (volume != null && volumeUnit != null) {
            "$volume$volumeUnit"
        } else {
            volume
        }

    /**
     * Check if there are remaining quantities to receive.
     */
    val hasRemainingQuantity: Boolean
        get() = totalRemainingQuantity > 0
}

/**
 * Warehouse summary within a product.
 */
data class IncomingWarehouseSummary(
    val warehouseId: Int,
    val warehouseCode: String,
    val warehouseName: String,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val remainingQuantity: Int = 0
)

data class IncomingItemQuantityCode(
    val productCode: String? = null,
    val ownCode: String? = null,
    val quantityCode: String? = null,
    val quantity: Int? = null
)

// ============================================================
// Incoming Schedule
// ============================================================

/**
 * Status of incoming schedule.
 */
enum class IncomingScheduleStatus {
    PENDING,      // 未入庫
    PARTIAL,      // 一部入庫済み
    CONFIRMED,    // 入庫完了（確定済み）
    TRANSMITTED,  // 連携済み
    CANCELLED;    // キャンセル

    companion object {
        fun fromString(value: String?): IncomingScheduleStatus = when (value?.uppercase()) {
            "PENDING" -> PENDING
            "PARTIAL" -> PARTIAL
            "CONFIRMED" -> CONFIRMED
            "TRANSMITTED" -> TRANSMITTED
            "CANCELLED" -> CANCELLED
            else -> PENDING
        }
    }

    /**
     * Check if work can be started on this schedule.
     */
    val canStartWork: Boolean
        get() = this == PENDING || this == PARTIAL

    /**
     * Check if this schedule can be edited from history.
     */
    val canEditFromHistory: Boolean
        get() = this == CONFIRMED
}

/**
 * Individual schedule for incoming.
 */
data class IncomingSchedule(
    val id: Int,
    val warehouseId: Int,
    val warehouseName: String? = null,
    val isUnplanned: Boolean = false,
    val slipNumber: String? = null,
    val orderSource: String? = null,
    val orderSourceLabel: String? = null,
    val inspectionPolicy: String? = null,
    val isEosSent: Boolean = false,
    val orderDate: String? = null,
    val contractorId: Int? = null,
    val contractorName: String? = null,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val remainingQuantity: Int = 0,
    val expectedPieceQuantity: Int? = null,
    val receivedPieceQuantity: Int? = null,
    val remainingPieceQuantity: Int? = null,
    val capacityCase: Int? = null,
    val quantityType: IncomingQuantityType = IncomingQuantityType.PIECE,
    val expectedArrivalDate: String? = null,
    val expirationDate: String? = null,
    val status: IncomingScheduleStatus = IncomingScheduleStatus.PENDING,
    val location: Location? = null
) {
    /**
     * Get progress text for display.
     */
    val progressText: String
        get() = "$receivedQuantity/$expectedQuantity"

    /**
     * Check if schedule is complete.
     */
    val isComplete: Boolean
        get() = remainingQuantity == 0 && expectedQuantity > 0
}

/**
 * Quantity type for incoming.
 */
enum class IncomingQuantityType {
    PIECE,
    CASE;

    companion object {
        fun fromString(value: String?): IncomingQuantityType = when (value?.uppercase()) {
            "PIECE" -> PIECE
            "CASE" -> CASE
            else -> PIECE
        }
    }
}

// ============================================================
// Incoming Work Item
// ============================================================

/**
 * Status of work item.
 */
enum class IncomingWorkStatus {
    WORKING,      // 作業中
    COMPLETED,    // 完了
    CANCELLED;    // キャンセル

    companion object {
        fun fromString(value: String?): IncomingWorkStatus = when (value?.uppercase()) {
            "WORKING" -> WORKING
            "COMPLETED" -> COMPLETED
            "CANCELLED" -> CANCELLED
            else -> WORKING
        }
    }

    /**
     * Check if this work item can be edited.
     */
    val canEdit: Boolean
        get() = this == WORKING || this == COMPLETED
}

/**
 * Work item representing ongoing or completed incoming work.
 */
data class IncomingWorkItem(
    val id: Int,
    val incomingScheduleId: Int,
    val pickerId: Int,
    val warehouseId: Int,
    val locationId: Int? = null,
    val location: Location? = null,
    val workQuantity: Int = 0,
    val workArrivalDate: String? = null,
    val workExpirationDate: String? = null,
    val status: IncomingWorkStatus = IncomingWorkStatus.WORKING,
    val startedAt: String? = null,
    val schedule: WorkItemSchedule? = null
) {
    /**
     * Check if this work item is in progress.
     */
    val isWorking: Boolean
        get() = status == IncomingWorkStatus.WORKING

    /**
     * Check if this work item is complete.
     */
    val isCompleted: Boolean
        get() = status == IncomingWorkStatus.COMPLETED
}

/**
 * Schedule info embedded in work item.
 */
data class WorkItemSchedule(
    val id: Int,
    val itemId: Int,
    val itemCode: String? = null,
    val itemName: String? = null,
    val janCodes: List<String> = emptyList(),
    val warehouseId: Int? = null,
    val warehouseName: String? = null,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val remainingQuantity: Int = 0,
    val quantityType: IncomingQuantityType = IncomingQuantityType.PIECE,
    val expectedArrivalDate: String? = null,
    val status: IncomingScheduleStatus = IncomingScheduleStatus.PENDING
) {
    val primaryJanCode: String?
        get() = janCodes.firstOrNull()
}

// ============================================================
// Request Models
// ============================================================

/**
 * Request to start work on a schedule.
 */
data class StartWorkData(
    val incomingScheduleId: Int,
    val pickerId: Int,
    val warehouseId: Int
)

/**
 * Request to update a work item.
 */
data class UpdateWorkItemData(
    val workQuantity: Int,
    val workArrivalDate: String? = null,
    val workExpirationDate: String? = null,
    val locationId: Int? = null
)

data class IncomingInspectionBatchSyncData(
    val clientBatchUuid: String,
    val warehouseId: Int,
    val inspectionDate: String,
    val inspectedAt: String? = null,
    val pickerId: Int? = null,
    val deviceId: String? = null,
    val appVersion: String? = null,
    val details: List<IncomingInspectionDetailData>
)

data class IncomingInspectionDetailData(
    val clientLineUuid: String,
    val incomingScheduleId: Int? = null,
    val itemId: Int? = null,
    val itemCode: String? = null,
    val itemName: String? = null,
    val scannedCode: String? = null,
    val slipNumber: String? = null,
    val contractorId: Int? = null,
    val locationId: Int? = null,
    val caseQuantity: Int = 0,
    val pieceQuantity: Int = 0,
    val capacityCase: Int? = null,
    val totalPieceQuantity: Int,
    val expirationDate: String? = null,
    val inspectedAt: String? = null
)

data class IncomingInspectionBatchSyncResult(
    val status: String,
    val totalDetailCount: Int = 0,
    val successCount: Int = 0,
    val historyOnlyCount: Int = 0,
    val reviewCount: Int = 0,
    val errorCount: Int = 0,
    val details: List<IncomingInspectionDetailSyncResult> = emptyList()
)

data class IncomingInspectionDetailSyncResult(
    val clientLineUuid: String,
    val incomingScheduleId: Int? = null,
    val itemId: Int? = null,
    val itemCode: String? = null,
    val itemName: String? = null,
    val inspectionPolicy: String? = null,
    val resultStatus: String,
    val reviewReason: String? = null,
    val inspectedTotalPieceQuantity: Int = 0,
    val appliedPieceQuantity: Int = 0,
    val shortagePieceQuantity: Int = 0
)
