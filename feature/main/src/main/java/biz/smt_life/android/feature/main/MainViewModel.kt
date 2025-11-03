package biz.smt_life.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    // TODO: Inject repositories when available
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun retry() {
        _uiState.value = MainUiState.Loading
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // TODO: Replace with actual repository calls
                val warehouse = Warehouse("001", "東京倉庫")
                val pendingCounts = PendingCounts(
                    inbound = 5,
                    outbound = 12,
                    inventory = 3
                )
                val currentDate = getCurrentDate()
                val hostUrl = "https://handy.click" // TODO: Get from settings
                val appVersion = "Ver.1.1.1" // TODO: Get from BuildConfig

                _uiState.value = MainUiState.Ready(
                    warehouse = warehouse,
                    pendingCounts = pendingCounts,
                    currentDate = currentDate,
                    hostUrl = hostUrl,
                    appVersion = appVersion
                )
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd EEE", Locale.JAPAN)
        return dateFormat.format(Date())
    }
}
