package biz.smt_life.android.feature.main

import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(
        val pickerCode: String?,
        val pickerName: String?,
        val warehouse: Warehouse,
        val pendingCounts: PendingCounts,
        val currentDate: String,
        val hostUrl: String,
        val appVersion: String,
        val authKey: String,
        val warehouseId: String
    ) : MainUiState

    data class Error(
        val message: String
    ) : MainUiState
}
