package biz.smt_life.android.core.domain.model

enum class OutboundInspectionPeriod(
    val apiValue: String,
    val label: String
) {
    Morning("morning", "午前"),
    Afternoon("afternoon", "午後");

    companion object {
        fun fromApiValue(value: String): OutboundInspectionPeriod =
            entries.firstOrNull { it.apiValue == value } ?: Morning
    }
}

data class OutboundInspectionSnapshot(
    val warehouse: OutboundInspectionWarehouse,
    val businessDate: String,
    val period: OutboundInspectionPeriod,
    val periodLabel: String,
    val generatedAt: String,
    val source: OutboundInspectionSource,
    val courses: List<OutboundInspectionCourse>
)

data class OutboundInspectionWarehouse(
    val id: Int,
    val code: String,
    val name: String
)

data class OutboundInspectionSource(
    val waveGroupId: Int,
    val groupNo: String,
    val shippingDate: String,
    val createdAt: String,
    val listType: String,
    val waveIds: List<Int>
)

data class OutboundInspectionCourse(
    val deliveryCourseId: Int,
    val deliveryCourseCode: String,
    val deliveryCourseName: String,
    val floors: List<OutboundInspectionFloor>,
    val summary: OutboundInspectionSummary = OutboundInspectionSummary()
) {
    val displayName: String
        get() = if (deliveryCourseCode.isBlank()) {
            deliveryCourseName
        } else {
            "[$deliveryCourseCode] $deliveryCourseName"
        }
}

data class OutboundInspectionFloor(
    val floorKey: String,
    val floorLabel: String,
    val floorSort: Int,
    val items: List<OutboundInspectionItem>,
    val summary: OutboundInspectionSummary = OutboundInspectionSummary()
)

data class OutboundInspectionItem(
    val inspectionItemId: String,
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val packaging: String?,
    val capacityCase: Int,
    val capacityCarton: Int?,
    val location: OutboundInspectionLocation,
    val orderedQuantity: OutboundInspectionQuantity,
    val plannedQuantity: OutboundInspectionQuantity,
    val scanCodes: List<OutboundInspectionScanCode>,
    val source: OutboundInspectionItemSource
)

data class OutboundInspectionLocation(
    val locationId: Int?,
    val locationCode: String,
    val floorId: Int?,
    val floorName: String?
)

data class OutboundInspectionQuantity(
    val quantity: Int,
    val quantityType: String,
    val caseQty: Int,
    val pieceQty: Int,
    val totalPieceQty: Int
) {
    val displayText: String
        get() = "ケース:${caseQty} バラ:${pieceQty} 総バラ:${totalPieceQty}"
}

data class OutboundInspectionScanCode(
    val code: String,
    val codeType: String,
    val quantityType: String?,
    val priority: Int?
)

data class OutboundInspectionItemSource(
    val wmsPickingItemResultIds: List<Int>,
    val sourceType: String
)

data class OutboundInspectionSummary(
    val courseCount: Int = 0,
    val floorCount: Int = 0,
    val itemCount: Int = 0,
    val scanCodeCount: Int = 0,
    val totalCaseQty: Int = 0,
    val totalPieceQty: Int = 0,
    val totalPieces: Int = 0
)
