package biz.smt_life.android.core.network.model

import biz.smt_life.android.core.domain.model.OutboundInspectionCourse
import biz.smt_life.android.core.domain.model.OutboundInspectionFloor
import biz.smt_life.android.core.domain.model.OutboundInspectionItem
import biz.smt_life.android.core.domain.model.OutboundInspectionItemSource
import biz.smt_life.android.core.domain.model.OutboundInspectionLocation
import biz.smt_life.android.core.domain.model.OutboundInspectionPeriod
import biz.smt_life.android.core.domain.model.OutboundInspectionQuantity
import biz.smt_life.android.core.domain.model.OutboundInspectionScanCode
import biz.smt_life.android.core.domain.model.OutboundInspectionSnapshot
import biz.smt_life.android.core.domain.model.OutboundInspectionSource
import biz.smt_life.android.core.domain.model.OutboundInspectionSummary
import biz.smt_life.android.core.domain.model.OutboundInspectionWarehouse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutboundInspectionSnapshotResponse(
    val warehouse: OutboundInspectionWarehouseResponse,
    @SerialName("business_date") val businessDate: String,
    val period: String,
    @SerialName("period_label") val periodLabel: String,
    @SerialName("generated_at") val generatedAt: String,
    val source: OutboundInspectionSourceResponse,
    val courses: List<OutboundInspectionCourseResponse> = emptyList(),
    val summary: OutboundInspectionSummaryResponse = OutboundInspectionSummaryResponse()
) {
    fun toDomain(): OutboundInspectionSnapshot = OutboundInspectionSnapshot(
        warehouse = warehouse.toDomain(),
        businessDate = businessDate,
        period = OutboundInspectionPeriod.fromApiValue(period),
        periodLabel = periodLabel,
        generatedAt = generatedAt,
        source = source.toDomain(),
        courses = courses.map { it.toDomain() }
    )
}

@Serializable
data class OutboundInspectionWarehouseResponse(
    val id: Int,
    val code: String = "",
    val name: String = ""
) {
    fun toDomain(): OutboundInspectionWarehouse = OutboundInspectionWarehouse(id, code, name)
}

@Serializable
data class OutboundInspectionSourceResponse(
    @SerialName("wave_group_id") val waveGroupId: Int,
    @SerialName("group_no") val groupNo: String = "",
    @SerialName("shipping_date") val shippingDate: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("list_type") val listType: String = "secondary_v2",
    @SerialName("wave_ids") val waveIds: List<Int> = emptyList()
) {
    fun toDomain(): OutboundInspectionSource = OutboundInspectionSource(
        waveGroupId = waveGroupId,
        groupNo = groupNo,
        shippingDate = shippingDate,
        createdAt = createdAt,
        listType = listType,
        waveIds = waveIds
    )
}

@Serializable
data class OutboundInspectionCourseResponse(
    @SerialName("delivery_course_id") val deliveryCourseId: Int,
    @SerialName("delivery_course_code") val deliveryCourseCode: String = "",
    @SerialName("delivery_course_name") val deliveryCourseName: String = "",
    val floors: List<OutboundInspectionFloorResponse> = emptyList(),
    val summary: OutboundInspectionSummaryResponse = OutboundInspectionSummaryResponse()
) {
    fun toDomain(): OutboundInspectionCourse = OutboundInspectionCourse(
        deliveryCourseId = deliveryCourseId,
        deliveryCourseCode = deliveryCourseCode,
        deliveryCourseName = deliveryCourseName,
        floors = floors.map { it.toDomain() },
        summary = summary.toDomain()
    )
}

@Serializable
data class OutboundInspectionFloorResponse(
    @SerialName("floor_key") val floorKey: String,
    @SerialName("floor_label") val floorLabel: String = floorKey,
    @SerialName("floor_sort") val floorSort: Int = 0,
    val items: List<OutboundInspectionItemResponse> = emptyList(),
    val summary: OutboundInspectionSummaryResponse = OutboundInspectionSummaryResponse()
) {
    fun toDomain(): OutboundInspectionFloor = OutboundInspectionFloor(
        floorKey = floorKey,
        floorLabel = floorLabel,
        floorSort = floorSort,
        items = items.map { it.toDomain() },
        summary = summary.toDomain()
    )
}

@Serializable
data class OutboundInspectionItemResponse(
    @SerialName("inspection_item_id") val inspectionItemId: String,
    @SerialName("item_id") val itemId: Int,
    @SerialName("item_code") val itemCode: String = "",
    @SerialName("item_name") val itemName: String = "",
    val packaging: String? = null,
    @SerialName("capacity_case") val capacityCase: Int = 1,
    @SerialName("capacity_carton") val capacityCarton: Int? = null,
    val location: OutboundInspectionLocationResponse = OutboundInspectionLocationResponse(),
    @SerialName("ordered_quantity") val orderedQuantity: OutboundInspectionQuantityResponse,
    @SerialName("planned_quantity") val plannedQuantity: OutboundInspectionQuantityResponse,
    @SerialName("scan_codes") val scanCodes: List<OutboundInspectionScanCodeResponse> = emptyList(),
    val source: OutboundInspectionItemSourceResponse = OutboundInspectionItemSourceResponse()
) {
    fun toDomain(): OutboundInspectionItem = OutboundInspectionItem(
        inspectionItemId = inspectionItemId,
        itemId = itemId,
        itemCode = itemCode,
        itemName = itemName,
        packaging = packaging,
        capacityCase = capacityCase,
        capacityCarton = capacityCarton,
        location = location.toDomain(),
        orderedQuantity = orderedQuantity.toDomain(),
        plannedQuantity = plannedQuantity.toDomain(),
        scanCodes = scanCodes.map { it.toDomain() },
        source = source.toDomain()
    )
}

@Serializable
data class OutboundInspectionLocationResponse(
    @SerialName("location_id") val locationId: Int? = null,
    @SerialName("location_code") val locationCode: String = "",
    @SerialName("floor_id") val floorId: Int? = null,
    @SerialName("floor_name") val floorName: String? = null
) {
    fun toDomain(): OutboundInspectionLocation = OutboundInspectionLocation(
        locationId = locationId,
        locationCode = locationCode,
        floorId = floorId,
        floorName = floorName
    )
}

@Serializable
data class OutboundInspectionQuantityResponse(
    val quantity: Int = 0,
    @SerialName("quantity_type") val quantityType: String = "PIECE",
    @SerialName("case_qty") val caseQty: Int = 0,
    @SerialName("piece_qty") val pieceQty: Int = 0,
    @SerialName("total_piece_qty") val totalPieceQty: Int = 0
) {
    fun toDomain(): OutboundInspectionQuantity = OutboundInspectionQuantity(
        quantity = quantity,
        quantityType = quantityType,
        caseQty = caseQty,
        pieceQty = pieceQty,
        totalPieceQty = totalPieceQty
    )
}

@Serializable
data class OutboundInspectionScanCodeResponse(
    val code: String,
    @SerialName("code_type") val codeType: String = "JAN",
    @SerialName("quantity_type") val quantityType: String? = null,
    val priority: Int? = null
) {
    fun toDomain(): OutboundInspectionScanCode = OutboundInspectionScanCode(
        code = code,
        codeType = codeType,
        quantityType = quantityType,
        priority = priority
    )
}

@Serializable
data class OutboundInspectionItemSourceResponse(
    @SerialName("wms_picking_item_result_ids") val wmsPickingItemResultIds: List<Int> = emptyList(),
    @SerialName("source_type") val sourceType: String = "EARNING"
) {
    fun toDomain(): OutboundInspectionItemSource = OutboundInspectionItemSource(
        wmsPickingItemResultIds = wmsPickingItemResultIds,
        sourceType = sourceType
    )
}

@Serializable
data class OutboundInspectionSummaryResponse(
    @SerialName("course_count") val courseCount: Int = 0,
    @SerialName("floor_count") val floorCount: Int = 0,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("scan_code_count") val scanCodeCount: Int = 0,
    @SerialName("total_case_qty") val totalCaseQty: Int = 0,
    @SerialName("total_piece_qty") val totalPieceQty: Int = 0,
    @SerialName("total_pieces") val totalPieces: Int = 0
) {
    fun toDomain(): OutboundInspectionSummary = OutboundInspectionSummary(
        courseCount = courseCount,
        floorCount = floorCount,
        itemCount = itemCount,
        scanCodeCount = scanCodeCount,
        totalCaseQty = totalCaseQty,
        totalPieceQty = totalPieceQty,
        totalPieces = totalPieces
    )
}
