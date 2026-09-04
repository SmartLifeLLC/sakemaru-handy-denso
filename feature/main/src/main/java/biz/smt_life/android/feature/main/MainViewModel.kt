package biz.smt_life.android.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.ui.HostPreferences
import biz.smt_life.android.core.ui.MasterDataPreferences
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
    private val incomingRepository: IncomingRepository,
    private val tokenManager: TokenManager,
    private val hostPreferences: HostPreferences,
    private val masterDataPreferences: MasterDataPreferences
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

    fun showWarehouseDialog() {
        val current = _uiState.value
        if (current is MainUiState.Ready) {
            _uiState.value = current.copy(showWarehouseDialog = true)
        }
    }

    fun dismissWarehouseDialog() {
        val current = _uiState.value
        if (current is MainUiState.Ready) {
            _uiState.value = current.copy(showWarehouseDialog = false)
        }
    }

    fun selectWarehouse(warehouse: IncomingWarehouse) {
        tokenManager.setDefaultWarehouseId(warehouse.id)
        val current = _uiState.value
        if (current is MainUiState.Ready) {
            _uiState.value = current.copy(
                warehouse = Warehouse(warehouse.id.toString(), warehouse.name),
                warehouseId = warehouse.id.toString(),
                showWarehouseDialog = false
            )
        }
    }

    fun refreshMasterData() {
        viewModelScope.launch {
            refreshMasterDataInternal(showResult = true)
        }
    }

    fun clearMasterUpdateMessage() {
        val current = _uiState.value
        if (current is MainUiState.Ready) {
            _uiState.value = current.copy(masterUpdateMessage = null)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val pickerCode = tokenManager.getPickerCode()
                val pickerName = tokenManager.getPickerName()
                val warehouseId = tokenManager.getDefaultWarehouseId()
                val authToken = tokenManager.getToken() ?: ""

                val hostUrl = hostPreferences.baseUrl.first()

                val warehouses = incomingRepository.getWarehouses().getOrDefault(emptyList())
                val masterLastUpdatedAtMillis = masterDataPreferences.getLastUpdatedAtMillisOnce()

                val matched = warehouses.firstOrNull { it.id == warehouseId }
                val warehouse = if (matched != null) {
                    Warehouse(matched.id.toString(), matched.name)
                } else if (warehouseId > 0) {
                    Warehouse(warehouseId.toString(), "倉庫 #$warehouseId")
                } else {
                    Warehouse("0", "未設定")
                }

                val pendingCounts = PendingCounts(
                    inbound = 0,
                    outbound = 0,
                    inventory = 0
                )
                val currentDate = getCurrentDate()
                val appVersion = "Ver.1.7.0"

                _uiState.value = MainUiState.Ready(
                    pickerCode = pickerCode,
                    pickerName = pickerName,
                    warehouse = warehouse,
                    pendingCounts = pendingCounts,
                    currentDate = currentDate,
                    hostUrl = hostUrl,
                    appVersion = appVersion,
                    authKey = authToken,
                    warehouseId = warehouseId.toString(),
                    warehouses = warehouses,
                    masterLastUpdatedAt = formatMasterLastUpdatedAt(masterLastUpdatedAtMillis)
                )

            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(
                    message = e.message ?: "不明なエラーが発生しました"
                )
            }
        }
    }

    private suspend fun refreshMasterDataInternal(showResult: Boolean) {
        val current = _uiState.value
        if (current is MainUiState.Ready && current.isMasterUpdating) {
            return
        }

        if (current is MainUiState.Ready) {
            _uiState.value = current.copy(
                isMasterUpdating = true,
                showWarehouseDialog = false,
                masterUpdateMessage = null
            )
        }

        try {
            val warehouseId = tokenManager.getDefaultWarehouseId()
            val warehouses = incomingRepository.getWarehouses().getOrThrow()
            val matched = warehouses.firstOrNull { it.id == warehouseId }
            if (matched != null) {
                incomingRepository.refreshIncomingItemMaster(matched.id).getOrThrow()
            }

            val updatedAtMillis = System.currentTimeMillis()
            masterDataPreferences.setLastUpdatedAtMillis(updatedAtMillis)

            val pickerCode = tokenManager.getPickerCode()
            val pickerName = tokenManager.getPickerName()
            val authToken = tokenManager.getToken() ?: ""
            val hostUrl = hostPreferences.baseUrl.first()
            val warehouse = if (matched != null) {
                Warehouse(matched.id.toString(), matched.name)
            } else if (warehouseId > 0) {
                Warehouse(warehouseId.toString(), "倉庫 #$warehouseId")
            } else {
                Warehouse("0", "未設定")
            }

            _uiState.value = MainUiState.Ready(
                pickerCode = pickerCode,
                pickerName = pickerName,
                warehouse = warehouse,
                pendingCounts = PendingCounts(
                    inbound = 0,
                    outbound = 0,
                    inventory = 0
                ),
                currentDate = getCurrentDate(),
                hostUrl = hostUrl,
                appVersion = "Ver.1.7.0",
                authKey = authToken,
                warehouseId = warehouseId.toString(),
                warehouses = warehouses,
                isMasterUpdating = false,
                masterLastUpdatedAt = formatMasterLastUpdatedAt(updatedAtMillis),
                masterUpdateMessage = if (showResult) "商品マスタを更新しました。" else null
            )
        } catch (e: Exception) {
            val latest = _uiState.value
            if (latest is MainUiState.Ready) {
                _uiState.value = latest.copy(
                    isMasterUpdating = false,
                    masterUpdateMessage = if (showResult) {
                        "商品マスタの取得に失敗しました: ${formatMasterRefreshError(e)}"
                    } else {
                        null
                    }
                )
            } else {
                _uiState.value = MainUiState.Error(
                    message = e.message ?: "マスタ更新に失敗しました"
                )
            }
        }
    }

    private fun formatMasterRefreshError(error: Throwable): String {
        return error.message?.takeIf { it.isNotBlank() } ?: "エラーが発生しました。"
    }

    private fun formatMasterLastUpdatedAt(updatedAtMillis: Long?): String? {
        if (updatedAtMillis == null) {
            return null
        }
        return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).apply {
            timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        }.format(Date(updatedAtMillis))
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
