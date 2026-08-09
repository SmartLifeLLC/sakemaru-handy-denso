package biz.smt_life.android.feature.inbound.locationsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val repository: IncomingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(LocationSearchState())
    val state: StateFlow<LocationSearchState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var scanAutoSearchJob: Job? = null

    init {
        val defaultWarehouseId = tokenManager.getDefaultWarehouseId().takeIf { it > 0 }
        _state.update { it.copy(warehouseId = defaultWarehouseId) }
        loadWarehouseName(defaultWarehouseId)
    }

    fun onQueryChange(query: String) {
        val current = _state.value.query
        val added = query.length - current.length

        // スキャナー検知: 一度に4文字以上増えた → バーコード一括入力
        if (added >= 4) {
            val newValue = if (current.isNotEmpty() && query.startsWith(current)) {
                query.substring(current.length)
            } else {
                query
            }
            _state.update { it.copy(query = newValue) }
            scanAutoSearchJob?.cancel()
            scanAutoSearchJob = viewModelScope.launch {
                delay(200)
                SoundUtils.playBeep()
                searchJob?.cancel()
                search(newValue, clearQueryAfterSearch = true)
            }
            return
        }

        _state.update { it.copy(query = query) }
    }

    fun onScan(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        search(value, clearQueryAfterSearch = true)
    }

    fun prepareForScan() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                query = "",
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun searchNow() {
        searchJob?.cancel()
        search(_state.value.query)
    }

    fun moveSelectionUp() {
        _state.update {
            it.copy(selectedIndex = (it.selectedIndex - 1).coerceAtLeast(0))
        }
    }

    fun moveSelectionDown() {
        _state.update {
            val maxIndex = (it.results.size - 1).coerceAtLeast(0)
            it.copy(selectedIndex = (it.selectedIndex + 1).coerceAtMost(maxIndex))
        }
    }

    fun selectResult(index: Int) {
        _state.update { it.copy(selectedIndex = index.coerceIn(0, (it.results.size - 1).coerceAtLeast(0))) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun search(
        rawQuery: String,
        clearQueryAfterSearch: Boolean = false
    ) {
        val warehouseId = _state.value.warehouseId
        val query = rawQuery.trim()

        if (warehouseId == null) {
            _state.update { it.copy(errorMessage = "倉庫が選択されていません") }
            return
        }
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    hasSearched = true,
                    results = emptyList(),
                    selectedIndex = 0,
                    errorMessage = null
                )
            }

            repository.searchItemLocations(warehouseId, query, 10)
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            results = results,
                            selectedIndex = 0,
                            query = if (clearQueryAfterSearch) "" else it.query
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            results = emptyList(),
                            selectedIndex = 0,
                            errorMessage = mapErrorMessage(error),
                            query = if (clearQueryAfterSearch) "" else it.query
                        )
                    }
                }
        }
    }

    private fun loadWarehouseName(warehouseId: Int?) {
        if (warehouseId == null) return

        viewModelScope.launch {
            repository.getWarehouses()
                .onSuccess { warehouses ->
                    _state.update { state ->
                        state.copy(warehouse = warehouses.firstOrNull { it.id == warehouseId })
                    }
                }
        }
    }

    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.Forbidden -> "アクセス権限がありません。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラー。しばらくしてから再度お試しください。"
            is NetworkException.ValidationError -> error.message ?: "入力エラーです。"
            else -> error.message ?: "エラーが発生しました。"
        }
    }
}
