package biz.smt_life.android.feature.outbound.inspection

import biz.smt_life.android.core.domain.model.OutboundInspectionItem
import biz.smt_life.android.core.domain.model.OutboundInspectionItemSource
import biz.smt_life.android.core.domain.model.OutboundInspectionLocation
import biz.smt_life.android.core.domain.model.OutboundInspectionQuantity
import biz.smt_life.android.core.domain.model.OutboundInspectionScanCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboundInspectionScanMatcherTest {

    @Test
    fun normalizedKeys_matchesFullWidthHyphenAndLeadingZero() {
        val keys = OutboundInspectionScanMatcher.normalizedKeys("０４９０-１２３４ ５６７８９０")

        assertTrue("04901234567890" in keys)
        assertTrue("4901234567890" in keys)
    }

    @Test
    fun match_returnsSuccessWhenCodeBelongsToSingleItem() {
        val item = item("item-1", 1, listOf("04901234567890"))

        val result = OutboundInspectionScanMatcher.match(
            rawCode = "4901234567890",
            items = listOf(item),
            scannedInspectionItemIds = emptySet()
        )

        assertTrue(result is OutboundInspectionScanResult.Success)
        assertEquals("item-1", (result as OutboundInspectionScanResult.Success).item.inspectionItemId)
    }

    @Test
    fun match_returnsAlreadyScannedWithoutChangingItem() {
        val item = item("item-1", 1, listOf("4901234567890"))

        val result = OutboundInspectionScanMatcher.match(
            rawCode = "4901234567890",
            items = listOf(item),
            scannedInspectionItemIds = setOf("item-1")
        )

        assertTrue(result is OutboundInspectionScanResult.AlreadyScanned)
        assertEquals("item-1", (result as OutboundInspectionScanResult.AlreadyScanned).item.inspectionItemId)
    }

    @Test
    fun match_returnsDuplicateWhenSameCodeBelongsToMultipleItems() {
        val items = listOf(
            item("item-1", 1, listOf("4999999999999")),
            item("item-2", 2, listOf("4999999999999"))
        )

        val result = OutboundInspectionScanMatcher.match(
            rawCode = "4999999999999",
            items = items,
            scannedInspectionItemIds = emptySet()
        )

        assertTrue(result is OutboundInspectionScanResult.DuplicateCode)
        assertEquals(2, (result as OutboundInspectionScanResult.DuplicateCode).items.size)
    }

    @Test
    fun match_returnsNotFoundWhenCodeIsOutsideSelectedScope() {
        val item = item("item-1", 1, listOf("4901234567890"))

        val result = OutboundInspectionScanMatcher.match(
            rawCode = "1111111111111",
            items = listOf(item),
            scannedInspectionItemIds = emptySet()
        )

        assertTrue(result is OutboundInspectionScanResult.NotFound)
    }

    private fun item(
        inspectionItemId: String,
        itemId: Int,
        scanCodes: List<String>
    ): OutboundInspectionItem {
        val quantity = OutboundInspectionQuantity(
            quantity = 1,
            quantityType = "PIECE",
            caseQty = 0,
            pieceQty = 1,
            totalPieceQty = 1
        )
        return OutboundInspectionItem(
            inspectionItemId = inspectionItemId,
            itemId = itemId,
            itemCode = "ITEM$itemId",
            itemName = "商品$itemId",
            packaging = null,
            capacityCase = 1,
            capacityCarton = null,
            location = OutboundInspectionLocation(
                locationId = null,
                locationCode = "A-01-01",
                floorId = 1,
                floorName = "1F"
            ),
            orderedQuantity = quantity,
            plannedQuantity = quantity,
            scanCodes = scanCodes.mapIndexed { index, code ->
                OutboundInspectionScanCode(
                    code = code,
                    codeType = "JAN",
                    quantityType = "PIECE",
                    priority = index + 1
                )
            },
            source = OutboundInspectionItemSource(
                wmsPickingItemResultIds = listOf(itemId),
                sourceType = "EARNING"
            )
        )
    }
}
