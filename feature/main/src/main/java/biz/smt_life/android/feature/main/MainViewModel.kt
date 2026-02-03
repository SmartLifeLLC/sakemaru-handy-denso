package biz.smt_life.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.ui.HostPreferences
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Main screen per Spec 2.3.0.
 * Handles picker info display and logout functionality.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val hostPreferences: HostPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        loadData()
    }

    fun retry() {
        _uiState.value = MainUiState.Loading
        loadData()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Call logout API
                authRepository.logout()
            } catch (e: Exception) {
                // Even if logout API fails, clear local auth
            } finally {
                // Always clear local auth data
                tokenManager.clearAuth()
                // Emit logout event
                _logoutEvent.emit(Unit)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Get picker info from TokenManager
                val pickerCode = tokenManager.getPickerCode()
                val pickerName = tokenManager.getPickerName()
                val warehouseId = tokenManager.getDefaultWarehouseId()
                val authToken = tokenManager.getToken() ?: ""

                // Get host URL from HostPreferences
                val hostUrl = hostPreferences.baseUrl.first()

                // TODO: Replace with actual repository calls when available
                // For now, use warehouse ID as the warehouse name placeholder
                val warehouse = if (warehouseId > 0) {
                    Warehouse(warehouseId.toString(), "倉庫 #$warehouseId")
                } else {
                    Warehouse("001", "東京倉庫")
                }

                val pendingCounts = PendingCounts(
                    inbound = 0,
                    outbound = 0,
                    inventory = 0
                )
                val currentDate = getCurrentDate()
                val appVersion = "Ver.1.0" // TODO: Get from BuildConfig

                _uiState.value = MainUiState.Ready(
                    pickerCode = pickerCode,
                    pickerName = pickerName,
                    warehouse = warehouse,
                    pendingCounts = pendingCounts,
                    currentDate = currentDate,
                    hostUrl = hostUrl,
                    appVersion = appVersion,
                    authKey = authToken,
                    warehouseId = warehouseId.toString()
                )
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(
                    message = e.message ?: "不明なエラーが発生しました"
                )
            }
        }
    }

    private fun getCurrentDate(): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"))
                    .format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPAN))
            } else {
                SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Tokyo")
                }.format(Date())
            }
        } catch (e: Exception) {
            SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date())
        }
    }
}

