package biz.smt_life.android.feature.inbound.incoming

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.IncomingInspectionBatchSyncData
import biz.smt_life.android.core.domain.model.IncomingInspectionDetailData
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWarehouseSummary
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
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
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
                syncedProducts = emptyList(),
                itemMasterProducts = emptyList(),
                syncedLocations = emptyList(),
                products = emptyList(),
                searchQuery = "",
                workingScheduleIds = emptySet(),
                hasSyncedIncomingData = false,
                lastSyncedAt = null,
                inspectionDate = null,
                clientBatchUuid = UUID.randomUUID().toString(),
                pendingInspectionDetails = emptyList(),
                syncResultDetails = emptyList(),
                syncResultMessage = null,
                showItemMasterRefreshPrompt = false,
                pendingItemMasterRefreshCode = null
            )
        }
        ensureItemMasterForWarehouse(warehouse.id)
    }

    /**
     * Select the default work warehouse from the login session.
     * Incoming no longer opens a dedicated warehouse list from the main menu.
     */
    fun ensureDefaultWarehouseSelected() {
        val currentState = _state.value
        if (currentState.isLoadingWarehouses) return

        val defaultWarehouseId = tokenManager.getDefaultWarehouseId()
        if (currentState.selectedWarehouse?.id == defaultWarehouseId) return

        if (defaultWarehouseId <= 0) {
            _state.update { it.copy(errorMessage = "作業倉庫が未設定です。メイン画面で倉庫を選択してください。") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoadingWarehouses = true, errorMessage = null) }

            repository.getWarehouses()
                .onSuccess { warehouses ->
                    val selectedWarehouse = warehouses.firstOrNull { it.id == defaultWarehouseId }
                    _state.update {
                        val shouldResetWorkData = it.selectedWarehouse?.id != selectedWarehouse?.id
                        it.copy(
                            isLoadingWarehouses = false,
                            warehouses = warehouses,
                            selectedWarehouse = selectedWarehouse,
                            syncedProducts = if (shouldResetWorkData) emptyList() else it.syncedProducts,
                            itemMasterProducts = if (shouldResetWorkData) emptyList() else it.itemMasterProducts,
                            syncedLocations = if (shouldResetWorkData) emptyList() else it.syncedLocations,
                            products = if (shouldResetWorkData) emptyList() else it.products,
                            searchQuery = if (shouldResetWorkData) "" else it.searchQuery,
                            workingScheduleIds = if (shouldResetWorkData) emptySet() else it.workingScheduleIds,
                            hasSyncedIncomingData = if (shouldResetWorkData) false else it.hasSyncedIncomingData,
                            lastSyncedAt = if (shouldResetWorkData) null else it.lastSyncedAt,
                            inspectionDate = if (shouldResetWorkData) null else it.inspectionDate,
                            clientBatchUuid = if (shouldResetWorkData) UUID.randomUUID().toString() else it.clientBatchUuid,
                            pendingInspectionDetails = if (shouldResetWorkData) emptyList() else it.pendingInspectionDetails,
                            syncResultDetails = if (shouldResetWorkData) emptyList() else it.syncResultDetails,
                            syncResultMessage = if (shouldResetWorkData) null else it.syncResultMessage,
                            showItemMasterRefreshPrompt = if (shouldResetWorkData) false else it.showItemMasterRefreshPrompt,
                            pendingItemMasterRefreshCode = if (shouldResetWorkData) null else it.pendingItemMasterRefreshCode,
                            errorMessage = if (selectedWarehouse == null) {
                                "選択中の倉庫が入庫処理で利用できません。メイン画面で倉庫を選択し直してください。"
                            } else {
                                null
                            }
                        )
                    }
                    selectedWarehouse?.let { ensureItemMasterForWarehouse(it.id) }
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

    // ============================================================
    // Product List
    // ============================================================

    /**
     * Load products for the selected warehouse.
     */
    fun loadProducts() {
        syncIncomingData()
    }

    /**
     * Download the current incoming work data for the selected warehouse.
     */
    fun syncIncomingData() {
        syncIncomingDataForDate()
    }

    private fun syncIncomingDataForDate(
        inspectionDateOverride: String? = null,
        clearPendingDetails: Boolean = true,
        successMessageOverride: String? = null
    ) {
        val warehouseId = _state.value.selectedWarehouse?.id
        if (warehouseId == null) {
            _state.update { it.copy(errorMessage = "作業倉庫が選択されていません。") }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSyncingIncomingData = true,
                    isLoadingProducts = true,
                    errorMessage = null
                )
            }

            val inspectionDate = inspectionDateOverride ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val masterResult = repository.ensureIncomingItemMaster(warehouseId)
            val cachedItemMaster = masterResult.getOrNull()
            val itemMasterProducts = cachedItemMaster?.products ?: _state.value.itemMasterProducts
            val snapshotResult = repository.getIncomingSnapshot(warehouseId, inspectionDate)

            snapshotResult
                .onSuccess { snapshot ->
                    _state.update {
                        it.copy(
                            isSyncingIncomingData = false,
                            isSyncingItemMaster = false,
                            isLoadingProducts = false,
                            syncedProducts = snapshot.products,
                            itemMasterProducts = itemMasterProducts,
                            itemMasterSyncedDate = cachedItemMaster?.masterDate
                                ?.takeIf { date -> date.isNotBlank() }
                                ?: it.itemMasterSyncedDate,
                            syncedLocations = snapshot.locations,
                            products = filterProducts(
                                scheduleProducts = snapshot.products,
                                itemMasterProducts = itemMasterProducts,
                                query = it.searchQuery,
                                pendingDetails = if (clearPendingDetails) emptyList() else it.pendingInspectionDetails
                            ),
                            workingScheduleIds = emptySet(),
                            hasSyncedIncomingData = true,
                            inspectionDate = snapshot.inspectionDate,
                            clientBatchUuid = if (clearPendingDetails) UUID.randomUUID().toString() else it.clientBatchUuid,
                            pendingInspectionDetails = if (clearPendingDetails) emptyList() else it.pendingInspectionDetails,
                            syncResultDetails = if (clearPendingDetails) emptyList() else it.syncResultDetails,
                            syncResultMessage = if (clearPendingDetails) null else it.syncResultMessage,
                            lastSyncedAt = LocalDateTime.now().format(SYNCED_AT_FORMATTER),
                            selectedProductIndex = 0,
                            successMessage = successMessageOverride ?: "入庫データを同期しました"
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSyncingIncomingData = false,
                            isSyncingItemMaster = false,
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
        val numericQuery = query.filter { it.isDigit() }
        _state.update { it.copy(searchQuery = numericQuery) }
        searchProducts(numericQuery)
    }

    fun submitProductSearchAndClear() {
        val query = _state.value.searchQuery.filter { it.isDigit() }
        searchProducts(query, clearQueryAfterSearch = true, promptIfMissing = true)
    }

    fun searchCurrentProductQuery() {
        val query = _state.value.searchQuery.filter { it.isDigit() }
        searchProducts(query, debounce = false, promptIfMissing = query.isNotBlank())
    }

    /**
     * Search products with debounce.
     */
    private fun searchProducts(
        query: String,
        clearQueryAfterSearch: Boolean = false,
        promptIfMissing: Boolean = false,
        debounce: Boolean = !clearQueryAfterSearch
    ) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) {
                delay(300) // Debounce 300ms
            }

            _state.update {
                if (!it.hasSyncedIncomingData) {
                    it.copy(
                        isSearching = false,
                        products = emptyList(),
                        searchQuery = if (clearQueryAfterSearch) "" else it.searchQuery
                    )
                } else {
                    val filtered = filterProducts(it.syncedProducts, it.itemMasterProducts, query, it.pendingInspectionDetails)
                    it.copy(
                        isSearching = false,
                        products = filtered,
                        selectedProductIndex = 0,
                        searchQuery = if (clearQueryAfterSearch) "" else it.searchQuery,
                        showItemMasterRefreshPrompt = promptIfMissing && query.isNotBlank() && filtered.isEmpty(),
                        pendingItemMasterRefreshCode = if (promptIfMissing && query.isNotBlank() && filtered.isEmpty()) {
                            query
                        } else {
                            it.pendingItemMasterRefreshCode
                        }
                    )
                }
            }
        }
    }

    /**
     * Handle barcode scan on product list.
     */
    fun onProductBarcodeScan(barcode: String) {
        val numericBarcode = barcode.filter { it.isDigit() }
        searchProducts(numericBarcode, clearQueryAfterSearch = true, promptIfMissing = true)
    }

    fun promptItemMasterRefreshIfSearchMissing() {
        val state = _state.value
        if (!state.hasSyncedIncomingData) {
            _state.update { it.copy(errorMessage = "入荷予定の取得が必要です。F1 入予取得を実施してください。") }
            return
        }

        val query = state.searchQuery
        if (query.isBlank()) return

        val filtered = filterProducts(state.syncedProducts, state.itemMasterProducts, query, state.pendingInspectionDetails)
        _state.update {
            it.copy(
                products = filtered,
                selectedProductIndex = 0,
                showItemMasterRefreshPrompt = filtered.isEmpty(),
                pendingItemMasterRefreshCode = if (filtered.isEmpty()) query else null
            )
        }
    }

    fun dismissItemMasterRefreshPrompt() {
        _state.update {
            it.copy(
                showItemMasterRefreshPrompt = false,
                pendingItemMasterRefreshCode = null
            )
        }
    }

    fun refreshItemMasterForMissingProduct() {
        val warehouseId = _state.value.selectedWarehouse?.id
        if (warehouseId == null) {
            _state.update { it.copy(errorMessage = "作業倉庫が選択されていません。") }
            return
        }
        if (!_state.value.hasSyncedIncomingData) {
            _state.update { it.copy(errorMessage = "入荷予定の取得が必要です。F1 入予取得を実施してください。") }
            return
        }

        val query = _state.value.pendingItemMasterRefreshCode
            ?: _state.value.searchQuery
        if (query.isBlank()) {
            dismissItemMasterRefreshPrompt()
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSyncingItemMaster = true,
                    showItemMasterRefreshPrompt = false,
                    errorMessage = null
                )
            }

            repository.refreshIncomingItemMaster(warehouseId)
                .onSuccess { itemMaster ->
                    _state.update {
                        val filtered = filterProducts(it.syncedProducts, itemMaster.products, query, it.pendingInspectionDetails)
                        it.copy(
                            isSyncingItemMaster = false,
                            itemMasterProducts = itemMaster.products,
                            itemMasterSyncedDate = itemMaster.masterDate,
                            products = filtered,
                            selectedProductIndex = 0,
                            searchQuery = query,
                            pendingItemMasterRefreshCode = null,
                            successMessage = if (filtered.isEmpty()) {
                                "最新マスタを取得しましたが、商品が見つかりません。"
                            } else {
                                "商品マスタを更新しました。"
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSyncingItemMaster = false,
                            pendingItemMasterRefreshCode = null,
                            errorMessage = "商品マスタの取得に失敗しました: ${mapErrorMessage(error)}"
                        )
                    }
                }
        }
    }

    private fun ensureItemMasterForWarehouse(warehouseId: Int) {
        viewModelScope.launch {
            repository.ensureIncomingItemMaster(warehouseId)
                .onSuccess { itemMaster ->
                    _state.update {
                        if (it.selectedWarehouse?.id != warehouseId) {
                            it.copy(isSyncingItemMaster = false)
                        } else {
                            it.copy(
                                itemMasterProducts = itemMaster.products,
                                itemMasterSyncedDate = itemMaster.masterDate
                                    .takeIf { date -> date.isNotBlank() }
                                    ?: it.itemMasterSyncedDate,
                                products = if (it.hasSyncedIncomingData) {
                                    filterProducts(it.syncedProducts, itemMaster.products, it.searchQuery, it.pendingInspectionDetails)
                                } else {
                                    emptyList()
                                }
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isSyncingItemMaster = false
                        )
                    }
                }
        }
    }

    fun prepareProductBarcodeScan() {
        searchJob?.cancel()
        _state.update {
            it.copy(
                searchQuery = "",
                isSearching = false
            )
        }
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
            val schedules = it.selectedProduct
                ?.let { product -> visibleSchedules(product, it.pendingInspectionDetails) }
                ?: emptyList()
            val maxIndex = (schedules.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedScheduleIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedScheduleIndex = newIndex)
        }
    }

    /**
     * Select current schedule and prepare for input.
     */
    fun selectCurrentSchedule(): IncomingSchedule? {
        val state = _state.value
        val product = state.selectedProduct ?: return null
        val schedules = visibleSchedules(product, state.pendingInspectionDetails)
        if (schedules.isEmpty()) {
            _state.update { it.copy(errorMessage = "未入力の入荷予定がありません。履歴から削除すると再表示されます。") }
            return null
        }

        val index = state.selectedScheduleIndex.coerceIn(0, schedules.lastIndex)
        if (index >= 0 && index < schedules.size) {
            val schedule = schedules[index]
            if (!schedule.canOpenForIncomingInput()) {
                _state.update { it.copy(errorMessage = schedule.unavailableMessage()) }
                return null
            }
            prepareInputForSchedule(schedule, isFromHistory = false)
            return schedule
        }
        return null
    }

    /**
     * Select a specific schedule.
     */
    fun selectSchedule(schedule: IncomingSchedule): Boolean {
        val state = _state.value
        val product = state.selectedProduct
        if (product != null && state.pendingInspectionDetails.any { it.matchesSchedule(schedule, product) }) {
            _state.update { it.copy(errorMessage = "入力済みの入荷予定です。履歴から削除すると再表示されます。") }
            return false
        }

        if (!schedule.canOpenForIncomingInput()) {
            _state.update { it.copy(errorMessage = schedule.unavailableMessage()) }
            return false
        }
        prepareInputForSchedule(schedule, isFromHistory = false)
        return true
    }

    /**
     * Prepare input screen for a schedule.
     */
    private fun prepareInputForSchedule(schedule: IncomingSchedule, isFromHistory: Boolean, workItem: IncomingWorkItem? = null) {
        val selectedProduct = _state.value.selectedProduct
        val masterProduct = selectedProduct?.let { product ->
            _state.value.itemMasterProducts.firstOrNull { it.itemId == product.itemId }
        }
        val existingPendingDetail = selectedProduct?.let { product ->
            _state.value.pendingInspectionDetails.firstOrNull { it.matchesSchedule(schedule, product) }
        }
        val capacityCase = schedule.capacityCase ?: _state.value.selectedProduct?.capacityCase
        val inputTotal = workItem?.workQuantity
            ?: existingPendingDetail?.totalPieceQuantity
            ?: if (schedule.isUnplanned) 0 else schedule.remainingPieceQuantity ?: schedule.remainingQuantity
        val (caseQuantity, pieceQuantity) = if (existingPendingDetail != null) {
            existingPendingDetail.caseQuantity to existingPendingDetail.pieceQuantity
        } else {
            splitCasePiece(inputTotal, capacityCase)
        }
        val defaultLocationId = workItem?.locationId
            ?: existingPendingDetail?.locationId
            ?: schedule.location?.id
            ?: selectedProduct?.defaultLocation?.id
            ?: masterProduct?.defaultLocation?.id
        val defaultLocation = workItem?.location
            ?: defaultLocationId?.let { locationId -> _state.value.syncedLocations.firstOrNull { it.id == locationId } }
            ?: schedule.location
            ?: selectedProduct?.defaultLocation
            ?: masterProduct?.defaultLocation
        val defaultExpirationDate = workItem?.workExpirationDate
            ?: existingPendingDetail?.expirationDate
            ?: schedule.expirationDate
            ?: schedule.defaultExpirationDate
            ?: selectedProduct?.defaultExpirationDate
            ?: masterProduct?.defaultExpirationDate
            ?: ""

        _state.update {
            it.copy(
                selectedSchedule = schedule,
                currentWorkItem = workItem,
                isFromHistory = isFromHistory,
                inputQuantity = inputTotal.toString(),
                inputCaseQuantity = caseQuantity.toString(),
                inputPieceQuantity = pieceQuantity.toString(),
                inputExpirationDate = defaultExpirationDate,
                inputLocationSearch = defaultLocation?.displayName
                    ?: defaultLocation?.fullDisplayName
                    ?: "",
                inputLocationId = defaultLocationId,
                inputLocation = defaultLocation,
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

    fun onCaseQuantityChange(value: String) {
        _state.update { it.copy(inputCaseQuantity = value.filter { char -> char.isDigit() }) }
    }

    fun onPieceQuantityChange(value: String) {
        _state.update { it.copy(inputPieceQuantity = value.filter { char -> char.isDigit() }) }
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

            _state.update { it.copy(isLoadingLocations = true) }

            if (_state.value.hasSyncedIncomingData) {
                _state.update {
                    it.copy(
                        isLoadingLocations = false,
                        locationSuggestions = findSyncedLocations(query)
                    )
                }
                return@launch
            }

            val warehouseId = _state.value.selectedWarehouse?.id ?: return@launch

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

    fun submitLocationSearchAndClear() {
        val query = _state.value.inputLocationSearch.trim()
        locationSearchJob?.cancel()

        if (query.isBlank()) {
            _state.update {
                it.copy(
                    inputLocationSearch = "",
                    locationSuggestions = emptyList(),
                    isLoadingLocations = false
                )
            }
            return
        }

        if (_state.value.hasSyncedIncomingData) {
            _state.update {
                it.copy(
                    inputLocationSearch = "",
                    locationSuggestions = findSyncedLocations(query),
                    isLoadingLocations = false
                )
            }
            return
        }

        val warehouseId = _state.value.selectedWarehouse?.id
        if (warehouseId == null) {
            _state.update { it.copy(inputLocationSearch = "", locationSuggestions = emptyList()) }
            return
        }

        locationSearchJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    inputLocationSearch = "",
                    isLoadingLocations = true
                )
            }

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

    private fun findSyncedLocations(query: String): List<Location> {
        val normalizedQuery = query.normalizeSearchKey()
        return _state.value.syncedLocations
            .filter { location ->
                listOfNotNull(
                    location.code1,
                    location.code2,
                    location.code3,
                    location.name,
                    location.displayName,
                    location.fullDisplayName
                ).any { it.normalizeSearchKey().contains(normalizedQuery) }
            }
            .take(20)
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
        val capacityCase = schedule.capacityCase ?: _state.value.selectedProduct?.capacityCase
        val total = schedule.remainingPieceQuantity ?: schedule.remainingQuantity
        val (caseQuantity, pieceQuantity) = splitCasePiece(total, capacityCase)
        _state.update {
            it.copy(
                inputQuantity = total.toString(),
                inputCaseQuantity = caseQuantity.toString(),
                inputPieceQuantity = pieceQuantity.toString()
            )
        }
    }

    /**
     * Check if form can be submitted.
     */
    fun canSubmit(): Boolean {
        val state = _state.value
        if (state.selectedSchedule?.isEosConfirmationBlocked() == true) {
            return false
        }

        if (state.hasSyncedIncomingData) {
            return calculateInputTotalPieceQuantity(state) > 0
        }

        val quantity = state.inputQuantity.toIntOrNull() ?: 0
        val maxQuantity = state.selectedSchedule?.remainingQuantity ?: 0

        return quantity > 0 && quantity <= maxQuantity
    }

    fun quantityWarningMessage(): String? {
        val state = _state.value
        val schedule = state.selectedSchedule ?: return null
        if (!state.hasSyncedIncomingData || schedule.isUnplanned) return null

        val total = calculateInputTotalPieceQuantity(state)
        val remaining = schedule.remainingPieceQuantity ?: schedule.remainingQuantity
        return if (total > remaining && remaining > 0) {
            "予定数を超えています。送信時にWMSで超過入荷として判定されます。"
        } else {
            null
        }
    }

    /**
     * Submit incoming entry (Register).
     * Flow: startWork -> updateWorkItem -> completeWorkItem
     */
    fun submitEntry(onSuccess: (Boolean) -> Unit) {
        val state = _state.value
        if (state.selectedSchedule?.isEosConfirmationBlocked() == true) {
            _state.update { it.copy(errorMessage = "EOS発注は入荷確定処理できません。") }
            return
        }

        if (state.hasSyncedIncomingData) {
            submitInspectionDetail(onSuccess)
            return
        }

        val schedule = state.selectedSchedule ?: return
        val warehouseId = state.selectedWarehouse?.id ?: return
        val pickerId = state.pickerId ?: return

        val quantity = state.inputQuantity.toIntOrNull() ?: return
        val expirationDate = state.inputExpirationDate.ifBlank { null }
        val locationId = state.inputLocationId
        val nextScheduleIndex = nextRemainingScheduleIndexForSameProduct(
            product = state.selectedProduct,
            pendingDetails = state.pendingInspectionDetails,
            completedSchedule = schedule
        )

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
                _state.update {
                    it.copy(
                        selectedSchedule = null,
                        selectedScheduleIndex = nextScheduleIndex ?: it.selectedScheduleIndex
                    )
                }

                onSuccess(nextScheduleIndex != null)

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

    private fun submitInspectionDetail(onSuccess: (Boolean) -> Unit) {
        val state = _state.value
        val product = state.selectedProduct ?: return
        val schedule = state.selectedSchedule ?: return
        if (schedule.isEosConfirmationBlocked()) {
            _state.update { it.copy(errorMessage = "EOS発注は入荷確定処理できません。") }
            return
        }
        val capacityCase = schedule.capacityCase ?: product.capacityCase
        val caseQuantity = state.inputCaseQuantity.toIntOrNull() ?: 0
        val pieceQuantity = state.inputPieceQuantity.toIntOrNull() ?: 0
        val totalPieceQuantity = calculateInputTotalPieceQuantity(state)

        if (totalPieceQuantity <= 0) {
            _state.update { it.copy(errorMessage = "1以上の数量を入力してください。") }
            return
        }

        val detail = IncomingInspectionDetailData(
            clientLineUuid = state.pendingInspectionDetails.firstOrNull {
                it.matchesSchedule(schedule, product)
            }?.clientLineUuid ?: UUID.randomUUID().toString(),
            incomingScheduleId = schedule.id.takeUnless { schedule.isUnplanned },
            itemId = product.itemId,
            itemCode = product.itemCode,
            itemName = product.itemName,
            scannedCode = product.primaryJanCode ?: product.searchCodes.firstOrNull(),
            slipNumber = schedule.slipNumber,
            contractorId = schedule.contractorId,
            locationId = state.inputLocationId ?: schedule.location?.id ?: product.defaultLocation?.id,
            caseQuantity = caseQuantity,
            pieceQuantity = pieceQuantity,
            capacityCase = capacityCase,
            totalPieceQuantity = totalPieceQuantity,
            expirationDate = state.inputExpirationDate.ifBlank { null },
            inspectedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )

        val updatedDetails = state.pendingInspectionDetails
            .filterNot { pendingDetail -> pendingDetail.matchesSchedule(schedule, product) } + detail
        val nextScheduleIndex = nextRemainingScheduleIndexForSameProduct(
            product = product,
            pendingDetails = updatedDetails,
            completedSchedule = schedule
        )

        _state.update {
            val refreshedProducts = filterProducts(it.syncedProducts, it.itemMasterProducts, it.searchQuery, updatedDetails)
            it.copy(
                products = refreshedProducts,
                selectedProduct = refreshedProducts.firstOrNull { refreshedProduct -> refreshedProduct.itemId == product.itemId },
                selectedProductIndex = it.selectedProductIndex.coerceAtMost((refreshedProducts.size - 1).coerceAtLeast(0)),
                pendingInspectionDetails = updatedDetails,
                isSubmitting = false,
                successMessage = if (schedule.isUnplanned) {
                    "予定なし入荷として検品データに追加しました"
                } else {
                    "検品データに追加しました"
                },
                selectedSchedule = null,
                selectedScheduleIndex = nextScheduleIndex ?: 0
            )
        }
        onSuccess(nextScheduleIndex != null)
    }

    fun syncInspectionBatch() {
        val state = _state.value
        val warehouseId = state.selectedWarehouse?.id
        val inspectionDate = state.inspectionDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        if (state.isSyncingInspectionBatch) {
            return
        }

        if (warehouseId == null) {
            _state.update { it.copy(errorMessage = "作業倉庫が選択されていません。") }
            return
        }

        if (state.pendingInspectionDetails.isEmpty()) {
            _state.update { it.copy(errorMessage = "未送信の検品データがありません。") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSyncingInspectionBatch = true,
                    errorMessage = null,
                    syncResultMessage = null
                )
            }

            val result = repository.syncIncomingInspectionBatch(
                IncomingInspectionBatchSyncData(
                    clientBatchUuid = state.clientBatchUuid,
                    warehouseId = warehouseId,
                    inspectionDate = inspectionDate,
                    inspectedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    pickerId = state.pickerId?.takeIf { it > 0 },
                    deviceId = getDeviceSerial(),
                    appVersion = APP_VERSION,
                    details = state.pendingInspectionDetails
                )
            )

            result
                .onSuccess { syncResult ->
                    val message = "送信完了 成功:${syncResult.successCount} 履歴のみ:${syncResult.historyOnlyCount} 要確認:${syncResult.reviewCount} エラー:${syncResult.errorCount}"
                    _state.update {
                        it.copy(
                            isSyncingInspectionBatch = false,
                            pendingInspectionDetails = emptyList(),
                            clientBatchUuid = UUID.randomUUID().toString(),
                            syncResultDetails = syncResult.details,
                            syncResultMessage = message,
                            successMessage = message
                        )
                    }
                    syncIncomingDataForDate(
                        inspectionDateOverride = inspectionDate,
                        clearPendingDetails = false,
                        successMessageOverride = "$message / 入荷予定を再同期しました"
                    )
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSyncingInspectionBatch = false,
                            errorMessage = mapErrorMessage(error)
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
            val maxIndex = (it.pendingInspectionDetails.size - 1).coerceAtLeast(0)
            val newIndex = (it.selectedHistoryIndex + 1).coerceAtMost(maxIndex)
            it.copy(selectedHistoryIndex = newIndex)
        }
    }

    fun selectPendingInspectionDetail(index: Int) {
        _state.update {
            val maxIndex = (it.pendingInspectionDetails.size - 1).coerceAtLeast(0)
            it.copy(selectedHistoryIndex = index.coerceIn(0, maxIndex))
        }
    }

    fun removeSelectedPendingInspectionDetail(): Boolean {
        val current = _state.value
        val details = current.pendingInspectionDetails
        if (details.isEmpty()) {
            _state.update { it.copy(errorMessage = "削除できる未送信データがありません。") }
            return false
        }

        val index = current.selectedHistoryIndex.coerceIn(0, details.lastIndex)
        val updatedDetails = details.toMutableList().also { list -> list.removeAt(index) }
        _state.update {
            val refreshedProducts = filterProducts(it.syncedProducts, it.itemMasterProducts, it.searchQuery, updatedDetails)
            it.copy(
                products = refreshedProducts,
                selectedProduct = it.selectedProduct?.let { selectedProduct ->
                    refreshedProducts.firstOrNull { product -> product.itemId == selectedProduct.itemId }
                },
                pendingInspectionDetails = updatedDetails,
                selectedProductIndex = it.selectedProductIndex.coerceAtMost((refreshedProducts.size - 1).coerceAtLeast(0)),
                selectedHistoryIndex = index.coerceAtMost((updatedDetails.size - 1).coerceAtLeast(0)),
                successMessage = "未送信データを削除しました。"
            )
        }
        return true
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
                inputCaseQuantity = "",
                inputPieceQuantity = "",
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

    private fun filterProducts(
        scheduleProducts: List<IncomingProduct>,
        itemMasterProducts: List<IncomingProduct>,
        query: String,
        pendingDetails: List<IncomingInspectionDetailData> = emptyList()
    ): List<IncomingProduct> {
        val visibleScheduleProducts = scheduleProducts
            .mapNotNull { product -> product.withVisibleSchedules(pendingDetails) }
        val normalizedQuery = query.normalizeSearchKey()
        if (normalizedQuery.isBlank()) return visibleScheduleProducts

        val scheduleItemIds = scheduleProducts.map { it.itemId }.toSet()
        val matchedSchedules = visibleScheduleProducts.filter { it.matches(normalizedQuery) }
        val matchedMaster = itemMasterProducts
            .asSequence()
            .filter { it.itemId !in scheduleItemIds }
            .mapNotNull { product -> product.withVisibleSchedules(pendingDetails) }
            .filter { it.matches(normalizedQuery) }
            .take(50)
            .toList()

        return matchedSchedules + matchedMaster
    }

    private fun IncomingProduct.matches(normalizedQuery: String): Boolean {
        return itemCode.normalizeSearchKey().contains(normalizedQuery) ||
            searchCode?.normalizeSearchKey()?.contains(normalizedQuery) == true ||
            janCodes.any { it.normalizeSearchKey().contains(normalizedQuery) } ||
            searchCodes.any { it.normalizeSearchKey().contains(normalizedQuery) } ||
            itemQuantityCodes.any { quantityCode ->
                listOfNotNull(
                    quantityCode.productCode,
                    quantityCode.ownCode,
                    quantityCode.quantityCode
                ).any { it.normalizeSearchKey().contains(normalizedQuery) }
            }
    }

    private fun String.normalizeSearchKey(): String {
        return trim().lowercase()
    }

    private fun IncomingSchedule.isEosConfirmationBlocked(): Boolean {
        return inspectionPolicy == "EOS_HISTORY_ONLY" ||
            inspectionPolicy == "EOS_ALREADY_CONFIRMED" ||
            isEosSent ||
            orderSource.equals("EOS", ignoreCase = true) ||
            orderSourceLabel?.contains("EOS", ignoreCase = true) == true
    }

    private fun IncomingSchedule.canOpenForIncomingInput(): Boolean {
        return (isUnplanned || status.canStartWork) && !isEosConfirmationBlocked()
    }

    private fun IncomingSchedule.unavailableMessage(): String {
        return if (isEosConfirmationBlocked()) {
            "EOS発注は入荷確定処理できません。"
        } else {
            "この入荷予定は選択できません。"
        }
    }

    private fun calculateInputTotalPieceQuantity(state: IncomingState): Int {
        val capacityCase = state.selectedSchedule?.capacityCase
            ?: state.selectedProduct?.capacityCase
            ?: 1
        val caseQuantity = state.inputCaseQuantity.toIntOrNull() ?: 0
        val pieceQuantity = state.inputPieceQuantity.toIntOrNull() ?: 0
        return caseQuantity * capacityCase + pieceQuantity
    }

    private fun splitCasePiece(totalPieceQuantity: Int, capacityCase: Int?): Pair<Int, Int> {
        val capacity = capacityCase?.takeIf { it > 1 } ?: return 0 to totalPieceQuantity
        return totalPieceQuantity / capacity to totalPieceQuantity % capacity
    }

    fun visibleSchedulesForSelectedProduct(): List<IncomingSchedule> {
        val state = _state.value
        val product = state.selectedProduct ?: return emptyList()
        return visibleSchedules(product, state.pendingInspectionDetails)
    }

    private fun visibleSchedules(
        product: IncomingProduct,
        pendingDetails: List<IncomingInspectionDetailData>
    ): List<IncomingSchedule> {
        return product.schedules.filter { schedule ->
            pendingDetails.none { pendingDetail -> pendingDetail.matchesSchedule(schedule, product) }
        }
    }

    private fun IncomingProduct.withVisibleSchedules(
        pendingDetails: List<IncomingInspectionDetailData>
    ): IncomingProduct? {
        val schedules = visibleSchedules(this, pendingDetails)
        if (schedules.isEmpty()) return null

        return copy(
            totalExpectedQuantity = schedules.sumOf { it.expectedPieceQuantity ?: it.expectedQuantity },
            totalReceivedQuantity = schedules.sumOf { it.receivedPieceQuantity ?: it.receivedQuantity },
            totalRemainingQuantity = schedules.sumOf { it.remainingPieceQuantity ?: it.remainingQuantity },
            warehouses = schedules
                .groupBy { it.warehouseId }
                .map { (warehouseId, warehouseSchedules) ->
                    IncomingWarehouseSummary(
                        warehouseId = warehouseId,
                        warehouseCode = "",
                        warehouseName = warehouseSchedules.firstOrNull()?.warehouseName.orEmpty(),
                        expectedQuantity = warehouseSchedules.sumOf { it.expectedPieceQuantity ?: it.expectedQuantity },
                        receivedQuantity = warehouseSchedules.sumOf { it.receivedPieceQuantity ?: it.receivedQuantity },
                        remainingQuantity = warehouseSchedules.sumOf { it.remainingPieceQuantity ?: it.remainingQuantity }
                    )
                },
            schedules = schedules
        )
    }

    private fun nextRemainingScheduleIndexForSameProduct(
        product: IncomingProduct?,
        pendingDetails: List<IncomingInspectionDetailData>,
        completedSchedule: IncomingSchedule
    ): Int? {
        if (product == null) return null

        val nextIndex = visibleSchedules(product, pendingDetails).indexOfFirst { schedule ->
            val remainingQuantity = schedule.remainingPieceQuantity ?: schedule.remainingQuantity
            !schedule.isUnplanned &&
                schedule.id != completedSchedule.id &&
                remainingQuantity > 0 &&
                schedule.canOpenForIncomingInput() &&
                pendingDetails.none { pendingDetail -> pendingDetail.matchesSchedule(schedule, product) }
        }

        return nextIndex.takeIf { it >= 0 }
    }

    private fun IncomingInspectionDetailData.matchesSchedule(
        schedule: IncomingSchedule,
        product: IncomingProduct
    ): Boolean {
        return if (schedule.isUnplanned) {
            incomingScheduleId == null && itemId == product.itemId
        } else {
            incomingScheduleId == schedule.id
        }
    }

    @Suppress("DEPRECATION")
    private fun getDeviceSerial(): String {
        return Build.SERIAL
            ?.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            ?: Build.MODEL
            ?: "unknown"
    }

    private companion object {
        const val APP_VERSION = "1.7.0"
        val SYNCED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
