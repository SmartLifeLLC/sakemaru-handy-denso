package biz.smt_life.android.core.network.fake

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
import biz.smt_life.android.core.domain.repository.OutboundInspectionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeOutboundInspectionRepository @Inject constructor() : OutboundInspectionRepository {

    override suspend fun getSnapshot(
        warehouseId: Int,
        period: OutboundInspectionPeriod,
        workingDate: String
    ): Result<OutboundInspectionSnapshot> {
        return Result.success(buildSnapshot(period, workingDate))
    }

    private fun buildSnapshot(
        period: OutboundInspectionPeriod,
        workingDate: String
    ): OutboundInspectionSnapshot {
        val compactDate = workingDate.replace("-", "")
        val courses = listOf(
            course(
                courseId = 100,
                courseCode = "C910017",
                courseName = "服部 卓哉",
                floors = listOf(
                    floor(
                        key = "1F",
                        sort = 1,
                        items = listOf(
                            item(
                                inspectionItemId = "91-2421-100-1F-10001-10",
                                itemId = 10001,
                                itemCode = "ITEM001",
                                itemName = "テスト清酒 720ml",
                                packaging = "720ml",
                                capacityCase = 12,
                                locationCode = "A-01-01",
                                floorId = 1,
                                floorName = "1F",
                                ordered = quantity(1, "CASE", 12),
                                planned = quantity(10, "PIECE", 12),
                                scanCodes = listOf(
                                    scanCode("04901234567890", "JAN", "PIECE", 1),
                                    scanCode("4901234567890", "OTHER", "PIECE", 2)
                                ),
                                resultIds = listOf(900001, 900002)
                            ),
                            item(
                                inspectionItemId = "91-2421-100-1F-10004-11",
                                itemId = 10004,
                                itemCode = "ITEM004",
                                itemName = "重複コード商品D",
                                packaging = "1800ml",
                                capacityCase = 6,
                                locationCode = "A-02-01",
                                floorId = 1,
                                floorName = "1F",
                                ordered = quantity(2, "PIECE", 6),
                                planned = quantity(2, "PIECE", 6),
                                scanCodes = listOf(scanCode("4999999999999", "JAN", "PIECE", 1)),
                                resultIds = listOf(900004)
                            ),
                            item(
                                inspectionItemId = "91-2421-100-1F-10005-12",
                                itemId = 10005,
                                itemCode = "ITEM005",
                                itemName = "重複コード商品E",
                                packaging = "1800ml",
                                capacityCase = 6,
                                locationCode = "A-02-02",
                                floorId = 1,
                                floorName = "1F",
                                ordered = quantity(1, "PIECE", 6),
                                planned = quantity(1, "PIECE", 6),
                                scanCodes = listOf(scanCode("4999999999999", "OTHER", "PIECE", 2)),
                                resultIds = listOf(900005)
                            )
                        )
                    ),
                    floor(
                        key = "2F",
                        sort = 2,
                        items = listOf(
                            item(
                                inspectionItemId = "91-2421-100-2F-10002-20",
                                itemId = 10002,
                                itemCode = "ITEM002",
                                itemName = "テスト焼酎 900ml",
                                packaging = "900ml",
                                capacityCase = 12,
                                locationCode = "B-01-01",
                                floorId = 2,
                                floorName = "2F",
                                ordered = quantity(1, "CASE", 12),
                                planned = quantity(12, "PIECE", 12),
                                scanCodes = listOf(scanCode("490-2222-333333", "JAN", "PIECE", 1)),
                                resultIds = listOf(900003)
                            )
                        )
                    ),
                    floor(
                        key = "YX",
                        sort = 999,
                        items = listOf(
                            item(
                                inspectionItemId = "91-2421-100-YX-10003-30",
                                itemId = 10003,
                                itemCode = "ITEM003",
                                itemName = "YXテストワイン",
                                packaging = "750ml",
                                capacityCase = 12,
                                locationCode = "YX-01-01",
                                floorId = 1,
                                floorName = "YX",
                                ordered = quantity(2, "CASE", 12),
                                planned = quantity(2, "CASE", 12),
                                scanCodes = listOf(scanCode("４９０３３３３４４４４４４", "SDP", "CASE", 1)),
                                resultIds = listOf(900006)
                            )
                        )
                    )
                )
            ),
            course(
                courseId = 101,
                courseCode = "C910146",
                courseName = "上中 孝浩",
                floors = listOf(
                    floor(
                        key = "1F",
                        sort = 1,
                        items = listOf(
                            item(
                                inspectionItemId = "91-2421-101-1F-10006-40",
                                itemId = 10006,
                                itemCode = "ITEM006",
                                itemName = "別コース商品",
                                packaging = "500ml",
                                capacityCase = 24,
                                locationCode = "A-03-01",
                                floorId = 1,
                                floorName = "1F",
                                ordered = quantity(24, "PIECE", 24),
                                planned = quantity(24, "PIECE", 24),
                                scanCodes = listOf(scanCode("4906666777777", "JAN", "PIECE", 1)),
                                resultIds = listOf(900007)
                            )
                        )
                    )
                )
            )
        )

        return OutboundInspectionSnapshot(
            warehouse = OutboundInspectionWarehouse(91, "091", "華むすびの蔵センター"),
            businessDate = workingDate,
            period = period,
            periodLabel = period.label,
            generatedAt = "${workingDate}T${if (period == OutboundInspectionPeriod.Morning) "10" else "14"}:20:30+09:00",
            source = OutboundInspectionSource(
                waveGroupId = if (period == OutboundInspectionPeriod.Morning) 2421 else 2422,
                groupNo = "WG-${compactDate}-${if (period == OutboundInspectionPeriod.Morning) "MORNING" else "AFTERNOON"}",
                shippingDate = workingDate,
                createdAt = "${workingDate}T${if (period == OutboundInspectionPeriod.Morning) "09" else "13"}:30:00+09:00",
                listType = "secondary_v2",
                waveIds = listOf(10001, 10002)
            ),
            courses = courses
        )
    }

    private fun course(
        courseId: Int,
        courseCode: String,
        courseName: String,
        floors: List<OutboundInspectionFloor>
    ): OutboundInspectionCourse {
        return OutboundInspectionCourse(
            deliveryCourseId = courseId,
            deliveryCourseCode = courseCode,
            deliveryCourseName = courseName,
            floors = floors,
            summary = OutboundInspectionSummary(
                floorCount = floors.size,
                itemCount = floors.sumOf { it.items.size },
                scanCodeCount = floors.sumOf { floor -> floor.items.sumOf { it.scanCodes.size } },
                totalPieces = floors.sumOf { floor -> floor.items.sumOf { it.plannedQuantity.totalPieceQty } }
            )
        )
    }

    private fun floor(
        key: String,
        sort: Int,
        items: List<OutboundInspectionItem>
    ): OutboundInspectionFloor {
        return OutboundInspectionFloor(
            floorKey = key,
            floorLabel = key,
            floorSort = sort,
            items = items,
            summary = OutboundInspectionSummary(
                itemCount = items.size,
                scanCodeCount = items.sumOf { it.scanCodes.size },
                totalCaseQty = items.sumOf { it.plannedQuantity.caseQty },
                totalPieceQty = items.sumOf { it.plannedQuantity.pieceQty },
                totalPieces = items.sumOf { it.plannedQuantity.totalPieceQty }
            )
        )
    }

    private fun item(
        inspectionItemId: String,
        itemId: Int,
        itemCode: String,
        itemName: String,
        packaging: String,
        capacityCase: Int,
        locationCode: String,
        floorId: Int,
        floorName: String,
        ordered: OutboundInspectionQuantity,
        planned: OutboundInspectionQuantity,
        scanCodes: List<OutboundInspectionScanCode>,
        resultIds: List<Int>
    ): OutboundInspectionItem {
        return OutboundInspectionItem(
            inspectionItemId = inspectionItemId,
            itemId = itemId,
            itemCode = itemCode,
            itemName = itemName,
            packaging = packaging,
            capacityCase = capacityCase,
            capacityCarton = 1,
            location = OutboundInspectionLocation(
                locationId = resultIds.firstOrNull(),
                locationCode = locationCode,
                floorId = floorId,
                floorName = floorName
            ),
            orderedQuantity = ordered,
            plannedQuantity = planned,
            scanCodes = scanCodes,
            source = OutboundInspectionItemSource(
                wmsPickingItemResultIds = resultIds,
                sourceType = "EARNING"
            )
        )
    }

    private fun quantity(quantity: Int, type: String, capacityCase: Int): OutboundInspectionQuantity {
        val safeCapacity = capacityCase.coerceAtLeast(1)
        val totalPieces = if (type == "CASE") quantity * safeCapacity else quantity
        return OutboundInspectionQuantity(
            quantity = quantity,
            quantityType = type,
            caseQty = if (type == "CASE") quantity else totalPieces / safeCapacity,
            pieceQty = if (type == "CASE") 0 else totalPieces % safeCapacity,
            totalPieceQty = totalPieces
        )
    }

    private fun scanCode(
        code: String,
        codeType: String,
        quantityType: String,
        priority: Int
    ): OutboundInspectionScanCode {
        return OutboundInspectionScanCode(
            code = code,
            codeType = codeType,
            quantityType = quantityType,
            priority = priority
        )
    }
}
