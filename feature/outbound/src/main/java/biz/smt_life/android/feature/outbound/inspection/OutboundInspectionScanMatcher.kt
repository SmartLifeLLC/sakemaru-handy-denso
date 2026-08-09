package biz.smt_life.android.feature.outbound.inspection

import biz.smt_life.android.core.domain.model.OutboundInspectionItem
import java.text.Normalizer
import java.util.Locale

object OutboundInspectionScanMatcher {
    fun match(
        rawCode: String,
        items: List<OutboundInspectionItem>,
        scannedInspectionItemIds: Set<String>
    ): OutboundInspectionScanResult {
        val scanKeys = normalizedKeys(rawCode)
        if (scanKeys.isEmpty()) {
            return OutboundInspectionScanResult.NotFound(rawCode)
        }

        val matchedItems = items
            .filter { item -> item.scanCodes.any { code -> normalizedKeys(code.code).any { it in scanKeys } } }
            .distinctBy { it.inspectionItemId }

        return when {
            matchedItems.isEmpty() -> OutboundInspectionScanResult.NotFound(rawCode)
            matchedItems.size > 1 -> OutboundInspectionScanResult.DuplicateCode(rawCode, matchedItems)
            matchedItems.first().inspectionItemId in scannedInspectionItemIds -> {
                OutboundInspectionScanResult.AlreadyScanned(matchedItems.first(), rawCode)
            }
            else -> OutboundInspectionScanResult.Success(matchedItems.first(), rawCode)
        }
    }

    fun normalizedKeys(value: String): Set<String> {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(Regex("[\\s　]"), "")
            .replace(Regex("[-‐‑‒–—―ー−]"), "")
            .uppercase(Locale.ROOT)
            .trim()

        if (normalized.isEmpty()) {
            return emptySet()
        }

        val withoutLeadingZeros = normalized.trimStart('0')
        return buildSet {
            add(normalized)
            if (withoutLeadingZeros.isNotEmpty()) {
                add(withoutLeadingZeros)
            }
        }
    }
}
