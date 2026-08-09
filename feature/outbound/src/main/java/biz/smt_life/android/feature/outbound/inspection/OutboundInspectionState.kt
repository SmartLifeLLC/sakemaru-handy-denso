package biz.smt_life.android.feature.outbound.inspection

import biz.smt_life.android.core.domain.model.OutboundInspectionCourse
import biz.smt_life.android.core.domain.model.OutboundInspectionFloor
import biz.smt_life.android.core.domain.model.OutboundInspectionItem
import biz.smt_life.android.core.domain.model.OutboundInspectionPeriod
import biz.smt_life.android.core.domain.model.OutboundInspectionSnapshot

data class OutboundInspectionState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val workingDate: String = "",
    val selectedPeriod: OutboundInspectionPeriod? = null,
    val snapshot: OutboundInspectionSnapshot? = null,
    val selectedCourseId: Int? = null,
    val selectedFloorKey: String? = null,
    val scanResult: OutboundInspectionScanResult = OutboundInspectionScanResult.Waiting,
    val scannedInspectionItemIds: Set<String> = emptySet()
) {
    val courses: List<OutboundInspectionCourse>
        get() = snapshot?.courses.orEmpty()

    val selectedCourse: OutboundInspectionCourse?
        get() = courses.firstOrNull { it.deliveryCourseId == selectedCourseId }

    val floors: List<OutboundInspectionFloor>
        get() = selectedCourse?.floors.orEmpty().sortedBy { it.floorSort }

    val selectedFloor: OutboundInspectionFloor?
        get() = floors.firstOrNull { it.floorKey == selectedFloorKey }
}

sealed interface OutboundInspectionScanResult {
    data object Waiting : OutboundInspectionScanResult
    data class Success(
        val item: OutboundInspectionItem,
        val rawCode: String
    ) : OutboundInspectionScanResult
    data class AlreadyScanned(
        val item: OutboundInspectionItem,
        val rawCode: String
    ) : OutboundInspectionScanResult
    data class NotFound(
        val rawCode: String
    ) : OutboundInspectionScanResult
    data class DuplicateCode(
        val rawCode: String,
        val items: List<OutboundInspectionItem>
    ) : OutboundInspectionScanResult
}
