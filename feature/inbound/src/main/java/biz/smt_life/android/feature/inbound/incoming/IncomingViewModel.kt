package biz.smt_life.android.feature.inbound.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.UpdateWorkItemData
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the native Incoming (入庫) feature.
 * Manages state for all incoming screens: warehouse selection, product list,
 * schedule list, input, and history.
 */
@HiltViewModel
class IncomingViewModel @Inject constructor(
    private val repository: IncomingRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(IncomingState())
    val state: StateFlow<IncomingState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var locationSearchJob: Job? = null

    init {
        initializeSessionData()
    }

    /**
     * Initialize picker info from session.
     */
    private fun initializeSessionData() {
        val pickerId = tokenManager.getPickerId()
        val pickerName = tokenManager.getPickerName()

        _state.update {
            it.copy(
                pickerId = pickerId,
                pickerName = pickerName
            )
        }
    }

    // ============================================================
    // Warehouse Selection
    // ============================================================

    /**
     * Load warehouses for selection.
     */
    fun loadWarehouses() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingWarehouses = true, errorMessage = null) }

            repository.getWarehouses()
                .onSuccess { warehouses ->
                    _state.update {
                        it.copy(
                            isLoadingWarehouses = false,
                            warehouses = warehouses
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingWarehouses = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    /**
     * Select a warehouse and load products.
     */
    fun selectWarehouse(warehouse: IncomingWarehouse) {
        _state.update {
            it.copy(
                selectedWarehouse = warehouse,
                products = emptyList(),
                searchQuery = ""
            )
        }
        loadProducts()
    }

    // ============================================================
    // Product List
    // ============================================================

    /**
     * Load products for the selected warehouse.
     */
    fun loadProducts() {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val pickerId = _state.value.pickerId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingProducts = true, errorMessage = null) }

            // Load products and working schedule IDs in parallel
            val productsResult = repository.getSchedules(warehouseId, null)
            val workingIdsResult = repository.getWorkingScheduleIds(warehouseId, pickerId)

            productsResult
                .onSuccess { products ->
                    val workingIds = workingIdsResult.getOrDefault(emptySet())
                    _state.update {
                        it.copy(
                            isLoadingProducts = false,
                            products = products,
                            workingScheduleIds = workingIds,
                            selectedProductIndex = 0
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingProducts = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    /**
     * Update search query and search products.
     */
    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchProducts(query)
    }

    /**
     * Search products with debounce.
     */
    private fun searchProducts(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms

            val warehouseId = _state.value.selectedWarehouse?.id ?: return@launch
            val pickerId = _state.value.pickerId ?: return@launch

            _state.update { it.copy(isSearching = true) }

            repository.getSchedules(warehouseId, query.ifBlank { null })
                .onSuccess { products ->
                    val workingIdsResult = repository.getWorkingScheduleIds(warehouseId, pickerId)
                    val workingIds = workingIdsResult.getOrDefault(emptySet())
                    _state.update {
                        it.copy(
                            isSearching = false,
                            products = products,
                            workingScheduleIds = workingIds,
                            selectedProductIndex = 0
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    /**
     * Handle barcode scan on product list.
     */
    fun onProductBarcodeScan(barcode: String) {
        _state.update { it.copy(searchQuery = barcode) }
        searchProducts(barcode)
    }

    /**
     * Move selection up in product list.
     */
    fun moveProductSelectionUp() {
        _state.update {
            val newIndex = (it.selectedProductIndex - 1).coerceAtLeast(0)
            it.copy(selectedProductIndex = newIndex)
        }
    }

    /**
     * Move selection down in product list.
     */
    fun moveProductSelectionDown() {
        _state.update {
            val maxIndex = (it.products.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedProductIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedProductIndex = newIndex)
        }
    }

    /**
     * Select current product and navigate to schedule list.
     */
    fun selectCurrentProduct(): IncomingProduct? {
        val products = _state.value.products
        val index = _state.value.selectedProductIndex
        if (index >= 0 && index < products.size) {
            val product = products[index]
            _state.update {
                it.copy(
                    selectedProduct = product,
                    selectedScheduleIndex = 0
                )
            }
            return product
        }
        return null
    }

    /**
     * Select a specific product.
     */
    fun selectProduct(product: IncomingProduct) {
        _state.update {
            it.copy(
                selectedProduct = product,
                selectedScheduleIndex = 0
            )
        }
    }

    // ============================================================
    // Schedule List
    // ============================================================

    /**
     * Move selection up in schedule list.
     */
    fun moveScheduleSelectionUp() {
        _state.update {
            val newIndex = (it.selectedScheduleIndex - 1).coerceAtLeast(0)
            it.copy(selectedScheduleIndex = newIndex)
        }
    }

    /**
     * Move selection down in schedule list.
     */
    fun moveScheduleSelectionDown() {
        _state.update {
            val schedules = it.selectedProduct?.schedules ?: emptyList()
            val maxIndex = (schedules.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedScheduleIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedScheduleIndex = newIndex)
        }
    }

    /**
     * Select current schedule and prepare for input.
     */
    fun selectCurrentSchedule(): IncomingSchedule? {
        val schedules = _state.value.selectedProduct?.schedules ?: return null
        val index = _state.value.selectedScheduleIndex
        if (index >= 0 && index < schedules.size) {
            val schedule = schedules[index]
            prepareInputForSchedule(schedule, isFromHistory = false)
            return schedule
        }
        return null
    }

    /**
     * Select a specific schedule.
     */
    fun selectSchedule(schedule: IncomingSchedule) {
        prepareInputForSchedule(schedule, isFromHistory = false)
    }

    /**
     * Prepare input screen for a schedule.
     */
    private fun prepareInputForSchedule(schedule: IncomingSchedule, isFromHistory: Boolean, workItem: IncomingWorkItem? = null) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        _state.update {
            it.copy(
                selectedSchedule = schedule,
                currentWorkItem = workItem,
                isFromHistory = isFromHistory,
                inputQuantity = workItem?.workQuantity?.toString()
                    ?: schedule.remainingQuantity.toString(),
                inputExpirationDate = workItem?.workExpirationDate
                    ?: schedule.expirationDate
                    ?: "",
                inputLocationSearch = workItem?.location?.displayName
                    ?: schedule.location?.displayName
                    ?: "",
                inputLocationId = workItem?.locationId ?: schedule.location?.id,
                inputLocation = workItem?.location ?: schedule.location,
                locationSuggestions = emptyList()
            )
        }
    }

    // ============================================================
    // Input Screen
    // ============================================================

    /**
     * Update quantity input.
     */
    fun onQuantityChange(value: String) {
        // Only allow digits
        val filtered = value.filter { it.isDigit() }
        _state.update { it.copy(inputQuantity = filtered) }
    }

    /**
     * Update expiration date input.
     */
    fun onExpirationDateChange(value: String) {
        // Format as YYYY-MM-DD
        val digits = value.filter { it.isDigit() }.take(8)
        val formatted = buildString {
            digits.forEachIndexed { index, char ->
                if (index == 4 || index == 6) append('-')
                append(char)
            }
        }
        _state.update { it.copy(inputExpirationDate = formatted) }
    }

    /**
     * Update location search and search for locations.
     */
    fun onLocationSearchChange(value: String) {
        _state.update { it.copy(inputLocationSearch = value) }
        searchLocations(value)
    }

    /**
     * Search locations with debounce.
     */
    private fun searchLocations(query: String) {
        locationSearchJob?.cancel()

        if (query.length < 1) {
            _state.update { it.copy(locationSuggestions = emptyList()) }
            return
        }

        locationSearchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms

            val warehouseId = _state.value.selectedWarehouse?.id ?: return@launch

            _state.update { it.copy(isLoadingLocations = true) }

            repository.searchLocations(warehouseId, query, 20)
                .onSuccess { locations ->
                    _state.update {
                        it.copy(
                            isLoadingLocations = false,
                            locationSuggestions = locations
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingLocations = false) }
                }
        }
    }

    /**
     * Handle location barcode scan.
     */
    fun onLocationBarcodeScan(barcode: String) {
        onLocationSearchChange(barcode)
    }

    /**
     * Select a location from suggestions.
     */
    fun selectLocation(location: Location) {
        _state.update {
            it.copy(
                inputLocationId = location.id,
                inputLocation = location,
                inputLocationSearch = location.displayName ?: location.fullDisplayName,
                locationSuggestions = emptyList()
            )
        }
    }

    /**
     * Set quantity to expected (remaining) quantity.
     */
    fun setQuantityToExpected() {
        val schedule = _state.value.selectedSchedule ?: return
        _state.update { it.copy(inputQuantity = schedule.remainingQuantity.toString()) }
    }

    /**
     * Check if form can be submitted.
     */
    fun canSubmit(): Boolean {
        val state = _state.value
        val quantity = state.inputQuantity.toIntOrNull() ?: 0
        val maxQuantity = state.selectedSchedule?.remainingQuantity ?: 0

        return quantity > 0 && quantity <= maxQuantity
    }

    /**
     * Submit incoming entry (Register).
     * Flow: startWork -> updateWorkItem -> completeWorkItem
     */
    fun submitEntry(onSuccess: () -> Unit) {
        val state = _state.value
        val schedule = state.selectedSchedule ?: return
        val warehouseId = state.selectedWarehouse?.id ?: return
        val pickerId = state.pickerId ?: return

        val quantity = state.inputQuantity.toIntOrNull() ?: return
        val expirationDate = state.inputExpirationDate.ifBlank { null }
        val locationId = state.inputLocationId

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                // Step 1: Start work (or get existing work item)
                val workItem = if (state.currentWorkItem != null) {
                    // Already have a work item (editing from history)
                    state.currentWorkItem
                } else {
                    // Start new work
                    val startResult = repository.startWork(
                        StartWorkData(
                            incomingScheduleId = schedule.id,
                            pickerId = pickerId,
                            warehouseId = warehouseId
                        )
                    )

                    if (startResult.isFailure) {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = mapErrorMessage(startResult.exceptionOrNull()!!)
                            )
                        }
                        return@launch
                    }

                    startResult.getOrThrow()
                }

                // Step 2: Update work item
                val updateResult = repository.updateWorkItem(
                    id = workItem.id,
                    data = UpdateWorkItemData(
                        workQuantity = quantity,
                        workArrivalDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        workExpirationDate = expirationDate,
                        locationId = locationId
                    )
                )

                if (updateResult.isFailure) {
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapErrorMessage(updateResult.exceptionOrNull()!!)
                        )
                    }
                    return@launch
                }

                // Step 3: Complete work item (only for new entries, not edits)
                if (!state.isFromHistory) {
                    val completeResult = repository.completeWorkItem(workItem.id)

                    if (completeResult.isFailure) {
                        _state.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = mapErrorMessage(completeResult.exceptionOrNull()!!)
                            )
                        }
                        return@launch
                    }
                }

                // Success - show message but keep schedule until navigation
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "入庫を確定しました",
                        currentWorkItem = null
                    )
                }

                // Reload products to get updated quantities
                loadProducts()

                // Wait for success message to be shown before navigating
                delay(1500)

                // Clear schedule after navigation
                _state.update { it.copy(selectedSchedule = null) }

                onSuccess()

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = mapErrorMessage(e)
                    )
                }
            }
        }
    }

    // ============================================================
    // History Screen
    // ============================================================

    /**
     * Load history for today.
     */
    fun loadHistory() {
        val warehouseId = _state.value.selectedWarehouse?.id ?: return
        val pickerId = _state.value.pickerId

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            _state.update { it.copy(isLoadingHistory = true, errorMessage = null) }

            repository.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = "all",
                fromDate = today,
                limit = 100
            )
                .onSuccess { items ->
                    _state.update {
                        it.copy(
                            isLoadingHistory = false,
                            historyItems = items,
                            selectedHistoryIndex = 0
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = mapErrorMessage(error)
                        )
                    }
                }
        }
    }

    /**
     * Move selection up in history list.
     */
    fun moveHistorySelectionUp() {
        _state.update {
            val newIndex = (it.selectedHistoryIndex - 1).coerceAtLeast(0)
            it.copy(selectedHistoryIndex = newIndex)
        }
    }

    /**
     * Move selection down in history list.
     */
    fun moveHistorySelectionDown() {
        _state.update {
            val maxIndex = (it.historyItems.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedHistoryIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedHistoryIndex = newIndex)
        }
    }

    /**
     * Select history item for editing.
     */
    fun selectHistoryItem(workItem: IncomingWorkItem): Boolean {
        // Check if editable
        if (!workItem.status.canEdit) {
            _state.update { it.copy(errorMessage = "この項目は編集できません") }
            return false
        }

        // Check if schedule is editable
        val scheduleStatus = workItem.schedule?.status
        if (scheduleStatus != null && !scheduleStatus.canEditFromHistory && !scheduleStatus.canStartWork) {
            _state.update { it.copy(errorMessage = "このスケジュールは編集できません") }
            return false
        }

        // Create a schedule from work item for input screen
        val schedule = IncomingSchedule(
            id = workItem.incomingScheduleId,
            warehouseId = workItem.warehouseId,
            warehouseName = workItem.schedule?.warehouseName,
            expectedQuantity = workItem.schedule?.expectedQuantity ?: 0,
            receivedQuantity = workItem.schedule?.receivedQuantity ?: 0,
            remainingQuantity = workItem.schedule?.remainingQuantity ?: 0,
            expectedArrivalDate = workItem.schedule?.expectedArrivalDate,
            expirationDate = workItem.workExpirationDate,
            status = workItem.schedule?.status ?: biz.smt_life.android.core.domain.model.IncomingScheduleStatus.PENDING,
            location = workItem.location
        )

        prepareInputForSchedule(schedule, isFromHistory = true, workItem = workItem)
        return true
    }

    // ============================================================
    // General Operations
    // ============================================================

    /**
     * Clear error message.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Clear success message.
     */
    fun clearSuccess() {
        _state.update { it.copy(successMessage = null) }
    }

    /**
     * Clear messages.
     */
    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /**
     * Reset state for going back to warehouse selection.
     */
    fun resetToWarehouseSelection() {
        _state.update {
            IncomingState(
                pickerId = it.pickerId,
                pickerName = it.pickerName,
                warehouses = it.warehouses
            )
        }
    }

    /**
     * Reset state for going back to product list.
     */
    fun resetToProductList() {
        _state.update {
            it.copy(
                selectedProduct = null,
                selectedSchedule = null,
                currentWorkItem = null,
                isFromHistory = false,
                inputQuantity = "",
                inputExpirationDate = "",
                inputLocationSearch = "",
                inputLocationId = null,
                inputLocation = null,
                locationSuggestions = emptyList()
            )
        }
    }

    /**
     * Map exception to user-friendly Japanese error message.
     */
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
