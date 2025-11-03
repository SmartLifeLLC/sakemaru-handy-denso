package biz.smt_life.android.feature.inbound

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.InboundEntryRequest
import biz.smt_life.android.core.domain.repository.InboundRepository
import biz.smt_life.android.core.network.IdempotencyKeyGenerator
import biz.smt_life.android.core.network.NetworkException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboundViewModel @Inject constructor(
    private val repository: InboundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InboundState())
    val state: StateFlow<InboundState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun onSearchQueryChange(value: String) {
        _state.update { it.copy(searchQuery = value, errorMessage = null) }
    }

    fun searchItems() {
        val query = _state.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, errorMessage = null) }
            repository.searchItems(query)
                .onSuccess { items ->
                    _state.update { it.copy(isSearching = false, searchResults = items) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSearching = false, errorMessage = error.message ?: "Search failed")
                    }
                }
        }
    }

    fun onBarcodeScan(barcode: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, barcodeInput = barcode, errorMessage = null) }
            repository.getItemByBarcode(barcode)
                .onSuccess { item ->
                    if (item != null) {
                        _state.update {
                            it.copy(
                                isSearching = false,
                                selectedItem = item,
                                searchResults = emptyList()
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isSearching = false,
                                errorMessage = "Item not found for barcode: $barcode"
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSearching = false, errorMessage = error.message ?: "Scan failed")
                    }
                }
        }
    }

    fun selectItem(item: biz.smt_life.android.core.domain.model.ItemDto) {
        _state.update { it.copy(selectedItem = item, searchResults = emptyList()) }
    }

    fun clearSelection() {
        _state.update {
            it.copy(
                selectedItem = null,
                qtyCase = "0",
                qtyEach = "0",
                expirationDate = TextFieldValue(""),
                labelCount = "0",
                fieldErrors = emptyMap()
            )
        }
    }

    fun onQtyCaseChange(value: String) {
        _state.update { it.copy(qtyCase = value, fieldErrors = it.fieldErrors - "qty") }
    }

    fun onQtyEachChange(value: String) {
        _state.update { it.copy(qtyEach = value, fieldErrors = it.fieldErrors - "qty") }
    }

    fun onExpirationDateChange(value: TextFieldValue) {
        // 이전 숫자와 새 숫자 비교
        val currentDigits = _state.value.expirationDate.text.filter { it.isDigit() }
        val newDigits = value.text.filter { it.isDigit() }

        // 최대 8자리까지만 허용
        val limited = newDigits.take(8)

        // yyyy.mm.dd 형식으로 포매팅
        val formatted = buildString {
            limited.forEachIndexed { index, char ->
                if (index == 4 || index == 6) {
                    append('.')
                }
                append(char)
            }
        }

        // 숫자가 변경된 경우에만 커서를 끝으로 이동
        val newValue = if (currentDigits != limited) {
            // 숫자 추가/삭제 발생 -> 커서를 끝으로
            TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length)
            )
        } else {
            // 커서만 이동 -> 현재 커서 위치 유지
            value.copy(text = formatted)
        }

        _state.update { it.copy(expirationDate = newValue) }
    }

    fun onLabelCountChange(value: String) {
        _state.update { it.copy(labelCount = value, fieldErrors = it.fieldErrors - "labelCount") }
    }

    fun addEntry() {
        val currentState = _state.value
        val item = currentState.selectedItem ?: return

        val request = InboundEntryRequest(
            itemId = item.id,
            qtyCase = currentState.qtyCase.toIntOrNull() ?: 0,
            qtyEach = currentState.qtyEach.toIntOrNull() ?: 0,
            expDate = currentState.expirationDate.text.ifBlank { null },
            labelCount = currentState.labelCount.toIntOrNull() ?: 0,
            idempotencyKey = IdempotencyKeyGenerator.generate()
        )

        viewModelScope.launch {
            _state.update { it.copy(isAdding = true, errorMessage = null, fieldErrors = emptyMap()) }
            repository.addEntry(request)
                .onSuccess { entry ->
                    _state.update {
                        it.copy(
                            isAdding = false,
                            successMessage = "Entry added successfully",
                            selectedItem = null,
                            qtyCase = "0",
                            qtyEach = "0",
                            expirationDate = TextFieldValue(""),
                            labelCount = "0"
                        )
                    }
                    loadHistory()
                }
                .onFailure { error ->
                    val (message, fieldErrors) = when (error) {
                        is NetworkException.Validation -> "Validation failed" to error.errors
                        else -> (error.message ?: "Failed to add entry") to emptyMap()
                    }
                    _state.update { it.copy(isAdding = false, errorMessage = message, fieldErrors = fieldErrors) }
                }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true) }
            repository.getHistory()
                .onSuccess { entries ->
                    _state.update { it.copy(isLoadingHistory = false, history = entries) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = error.message ?: "Failed to load history"
                        )
                    }
                }
        }
    }

    fun toggleHistorySheet() {
        _state.update { it.copy(showHistory = !it.showHistory) }
        if (_state.value.showHistory) {
            loadHistory()
        }
    }

    fun toggleEntrySelection(entryId: String) {
        _state.update {
            val newSelection = if (entryId in it.selectedEntries) {
                it.selectedEntries - entryId
            } else {
                it.selectedEntries + entryId
            }
            it.copy(selectedEntries = newSelection)
        }
    }

    fun confirmSelected() {
        val ids = _state.value.selectedEntries.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true, errorMessage = null) }
            repository.confirmEntries(ids, IdempotencyKeyGenerator.generate())
                .onSuccess {
                    _state.update {
                        it.copy(
                            isConfirming = false,
                            selectedEntries = emptySet(),
                            successMessage = "${ids.size} entries confirmed"
                        )
                    }
                    loadHistory()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isConfirming = false, errorMessage = error.message ?: "Failed to confirm")
                    }
                }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
