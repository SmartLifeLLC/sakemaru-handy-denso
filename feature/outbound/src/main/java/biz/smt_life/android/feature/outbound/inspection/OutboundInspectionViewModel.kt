package biz.smt_life.android.feature.outbound.inspection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.OutboundInspectionCourse
import biz.smt_life.android.core.domain.model.OutboundInspectionFloor
import biz.smt_life.android.core.domain.model.OutboundInspectionPeriod
import biz.smt_life.android.core.domain.repository.OutboundInspectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class OutboundInspectionViewModel @Inject constructor(
    private val repository: OutboundInspectionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OutboundInspectionState(workingDate = today()))
    val state: StateFlow<OutboundInspectionState> = _state.asStateFlow()

    fun loadSnapshot(period: OutboundInspectionPeriod, onSuccess: () -> Unit = {}) {
        val workingDate = _state.value.workingDate
        val current = _state.value
        if (
            current.snapshot != null &&
            current.selectedPeriod == period &&
            current.snapshot.businessDate == workingDate
        ) {
            _state.update {
                it.copy(
                    selectedCourseId = null,
                    selectedFloorKey = null,
                    scanResult = OutboundInspectionScanResult.Waiting,
                    scannedInspectionItemIds = emptySet(),
                    errorMessage = null
                )
            }
            onSuccess()
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedPeriod = period,
                    snapshot = null,
                    selectedCourseId = null,
                    selectedFloorKey = null,
                    scanResult = OutboundInspectionScanResult.Waiting,
                    scannedInspectionItemIds = emptySet()
                )
            }

            repository.getSnapshot(WAREHOUSE_ID, period, workingDate)
                .onSuccess { snapshot ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            snapshot = snapshot,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "出庫検品データを取得できませんでした"
                        )
                    }
                }
        }
    }

    fun resetSnapshot() {
        _state.update {
            it.copy(
                snapshot = null,
                selectedPeriod = null,
                selectedCourseId = null,
                selectedFloorKey = null,
                scanResult = OutboundInspectionScanResult.Waiting,
                scannedInspectionItemIds = emptySet(),
                errorMessage = null
            )
        }
    }

    fun moveWorkingDate(days: Long) {
        _state.update {
            it.copy(
                workingDate = parseDate(it.workingDate).plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE),
                snapshot = null,
                selectedPeriod = null,
                selectedCourseId = null,
                selectedFloorKey = null,
                scanResult = OutboundInspectionScanResult.Waiting,
                scannedInspectionItemIds = emptySet(),
                errorMessage = null
            )
        }
    }

    fun selectCourse(course: OutboundInspectionCourse) {
        _state.update {
            it.copy(
                selectedCourseId = course.deliveryCourseId,
                selectedFloorKey = null,
                scanResult = OutboundInspectionScanResult.Waiting,
                scannedInspectionItemIds = emptySet()
            )
        }
    }

    fun selectFloor(floor: OutboundInspectionFloor) {
        _state.update {
            it.copy(
                selectedFloorKey = floor.floorKey,
                scanResult = OutboundInspectionScanResult.Waiting,
                scannedInspectionItemIds = emptySet()
            )
        }
    }

    fun handleScan(rawCode: String): OutboundInspectionScanResult {
        val current = _state.value
        val items = current.selectedFloor?.items.orEmpty()
        val result = OutboundInspectionScanMatcher.match(
            rawCode = rawCode,
            items = items,
            scannedInspectionItemIds = current.scannedInspectionItemIds
        )

        _state.update { state ->
            val scannedIds = if (result is OutboundInspectionScanResult.Success) {
                state.scannedInspectionItemIds + result.item.inspectionItemId
            } else {
                state.scannedInspectionItemIds
            }
            state.copy(
                scanResult = result,
                scannedInspectionItemIds = scannedIds
            )
        }

        return result
    }

    fun confirmScanResult() {
        _state.update { it.copy(scanResult = OutboundInspectionScanResult.Waiting) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val WAREHOUSE_ID = 91
        private val TOKYO_ZONE: ZoneId = ZoneId.of("Asia/Tokyo")

        private fun today(): String {
            return LocalDate.now(TOKYO_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        private fun parseDate(value: String): LocalDate {
            return runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }
                .getOrElse { LocalDate.now(TOKYO_ZONE) }
        }
    }
}
