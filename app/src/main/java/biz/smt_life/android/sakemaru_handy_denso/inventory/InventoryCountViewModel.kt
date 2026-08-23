package biz.smt_life.android.sakemaru_handy_denso.inventory

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.network.api.InventoryCountApi
import biz.smt_life.android.core.network.model.InventoryBulkCountItemRequest
import biz.smt_life.android.core.network.model.InventoryBulkCountRequest
import biz.smt_life.android.core.network.model.InventoryCountItemResponse
import biz.smt_life.android.core.network.model.InventoryCountResponse
import biz.smt_life.android.core.network.model.JanCodeEntry
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltViewModel
class InventoryCountViewModel @Inject constructor(
    private val api: InventoryCountApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
    private val json: Json
) : ViewModel() {
    private val preferences = context.getSharedPreferences("inventory_count_cache", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(InventoryCountState(loading = true))
    val state: StateFlow<InventoryCountState> = _state.asStateFlow()

    init {
        SoundUtils.init(context)
        loadInstructions()
    }

    fun loadInstructions() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = "棚卸し指示確認中...") }
            val warehouseId = tokenManager.getDefaultWarehouseId()
            runCatching { api.getInventoryCounts(warehouseId) }
                .onSuccess { response ->
                    if (!response.isSuccess) {
                        _state.update {
                            it.copy(
                                loading = false,
                                error = response.result?.errorMessage ?: "棚卸し指示の確認に失敗しました",
                                message = null
                            )
                        }
                        restoreBestCache()
                        return@onSuccess
                    }

                    val counts = response.result?.data?.inventoryCounts.orEmpty()
                    val selected = counts.firstOrNull()
                    _state.update {
                        it.copy(
                            loading = false,
                            counts = counts,
                            selectedCount = selected,
                            countRound = selected?.currentRound ?: 1,
                            error = if (counts.isEmpty()) "棚卸し指示なし" else null,
                            message = if (counts.isEmpty()) null else "棚卸し指示を取得しました"
                        )
                    }
                    selected?.let { restoreCache(it.id) }
                    if (selected != null && _state.value.allItems.isEmpty()) {
                        syncAllItems()
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "棚卸し指示の確認に失敗しました") }
                    restoreBestCache()
                }
        }
    }

    fun selectCount(id: Int) {
        _state.update { state ->
            val selected = state.counts.firstOrNull { it.id == id }
            state.copy(
                selectedCount = selected,
                countRound = selected?.currentRound ?: state.countRound,
                allItems = emptyList(),
                items = emptyList(),
                selectedItem = null,
                accumulatedBase = null,
                dirtyInputs = emptyMap(),
                sentHistory = emptyList(),
                syncedAt = null,
                message = null,
                error = null
            )
        }
        restoreCache(id)
    }

    fun resetLocalData() {
        preferences.edit { clear() }
        _state.update {
            it.copy(
                allItems = emptyList(),
                items = emptyList(),
                selectedItem = null,
                accumulatedBase = null,
                janDictionary = emptyMap(),
                dirtyInputs = emptyMap(),
                sentHistory = emptyList(),
                syncedAt = null,
                caseQuantity = "",
                pieceQuantity = "",
                keyword = "",
                scanQuantityType = null,
                scannedCode = null,
                scanPackageQuantity = null,
                message = "データを初期化しました",
                error = null
            )
        }
        syncAllItems()
    }

    fun syncAllItems() {
        val count = _state.value.selectedCount ?: return
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, error = null, message = "棚卸しデータ取得中...") }
            val all = mutableListOf<InventoryCountItemResponse>()
            var page = 1
            var lastPage = 1

            do {
                _state.update { it.copy(message = "取得中... ${all.size}件 (${page}/${lastPage}ページ)") }
                val response = runCatching { api.getItems(count.id, page, 500) }
                    .getOrElse {
                        _state.update { state -> state.copy(syncing = false, error = it.message ?: "棚卸しデータ取得に失敗しました", message = null) }
                        return@launch
                    }
                val data = response.result?.data
                all += data?.items.orEmpty()
                lastPage = data?.meta?.lastPage ?: page
                page++
            } while (page <= lastPage)

            _state.update {
                it.copy(
                    allItems = all,
                    items = emptyList(),
                    selectedItem = null,
                    dirtyInputs = remapDirtyInputs(it.dirtyInputs, all),
                    message = "商品 ${all.size} 件取得。JANコード取得中...",
                    error = null
                )
            }

            // JAN辞書取得
            val syncedAt = System.currentTimeMillis()
            runCatching { api.getJanCodes(count.id) }
                .onSuccess { response ->
                    if (response.isSuccess) {
                        val dict = response.result?.data?.janCodes.orEmpty()
                        _state.update {
                            it.copy(
                                syncing = false,
                                janDictionary = dict,
                                syncedAt = syncedAt,
                                message = "同期完了（商品 ${all.size} 件 / JAN ${dict.size} 件）"
                            )
                        }
                    } else {
                        _state.update { it.copy(syncing = false, syncedAt = syncedAt, message = "商品 ${all.size} 件取得完了") }
                    }
                }
                .onFailure {
                    _state.update { it.copy(syncing = false, syncedAt = syncedAt, message = "商品 ${all.size} 件取得完了") }
                }

            persistCache()
        }
    }

    fun selectTab(tab: InventoryTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    private var scanRequestSerial: Long = 0L

    fun setKeyword(value: String) {
        _state.update {
            it.copy(
                keyword = value,
                error = if (value.isNotBlank()) null else it.error,
                message = if (value.isNotBlank()) null else it.message
            )
        }
    }

    fun setNameKeyword(value: String) {
        _state.update { it.copy(nameKeyword = value) }
    }

    private var preScanCaseQuantity: String = ""
    private var preScanPieceQuantity: String = ""

    fun onScanStart() {
        preScanCaseQuantity = _state.value.caseQuantity
        preScanPieceQuantity = _state.value.pieceQuantity
    }

    fun scanBarcode(code: String) {
        val current = _state.value
        if (current.selectedItem != null && (preScanCaseQuantity.isNotBlank() || preScanPieceQuantity.isNotBlank())) {
            _state.update { it.copy(caseQuantity = preScanCaseQuantity, pieceQuantity = preScanPieceQuantity) }
            saveLocalInput()
        }
        _state.update { it.copy(keyword = code) }
        scan()
    }

    fun scan() {
        val requestSerial = ++scanRequestSerial
        val raw = _state.value.keyword.trim()
        val keyword = normalize(raw)
        if (keyword.isEmpty()) {
            _state.update { it.copy(error = "商品コード、JAN、バーコードを入力してください") }
            return
        }
        _state.update { it.copy(keyword = "") }

        if (raw.length == 14 && raw.all { it.isDigit() }) {
            _state.update { it.copy(error = "ITFコードエラー", message = null) }
            SoundUtils.playError()
            return
        }

        fun applyResult(results: List<InventoryCountItemResponse>, quantityType: String?, packageQuantity: Int?) {
            if (results.size != 1) {
                _state.update {
                    it.copy(
                        items = emptyList(),
                        selectedItem = null,
                        scanQuantityType = null,
                        scannedCode = raw,
                        scanPackageQuantity = null,
                        accumulatedBase = null,
                        caseQuantity = "",
                        pieceQuantity = "",
                        keyword = "",
                        error = "コード不明",
                        message = null
                    )
                }
                SoundUtils.playError()
                return
            }

            val singleItem = results.first()
            val existingInput = _state.value.dirtyInputs[singleItem.id]
            _state.update {
                it.copy(
                    items = results,
                    selectedItem = singleItem,
                    scanQuantityType = quantityType,
                    scannedCode = raw,
                    scanPackageQuantity = packageQuantity,
                    accumulatedBase = existingInput,
                    caseQuantity = "",
                    pieceQuantity = "",
                    keyword = "",
                    error = null,
                    message = null
                )
            }
            SoundUtils.playBeep()
        }

        // 1. JAN辞書から検索
        val janEntries = janEntriesFor(raw, _state.value)
        if (janEntries != null && janEntries.isNotEmpty()) {
            val itemIds = janEntries.map { it.itemId }.toSet()
            val results = _state.value.allItems.filter { it.itemId in itemIds }
            if (results.isNotEmpty()) {
                val packageQuantity = if (results.size == 1) {
                    janEntries.firstOrNull { it.itemId == results.first().itemId }?.packageQuantity
                } else {
                    null
                }
                applyResult(results, janEntries.first().quantityType, packageQuantity)
                return
            }
        }

        // 2. ローカル商品コード完全一致
        val keywordCandidates = searchCodeCandidates(raw).toSet()
        val localResults = _state.value.allItems.filter { item ->
            normalize(item.itemCode) in keywordCandidates
                || normalize(item.barcode ?: "") in keywordCandidates
                || normalize(item.paperBarcode ?: "") in keywordCandidates
        }
        if (localResults.isNotEmpty()) {
            applyResult(localResults, null, null)
            return
        }

        _state.update { it.copy(error = "コード不明", message = null) }
        SoundUtils.playError()
    }

    fun searchByName() {
        val keyword = normalize(_state.value.nameKeyword.trim())
        if (keyword.isEmpty()) {
            _state.update { it.copy(error = "商品名を入力してください") }
            return
        }
        searchLocal { item -> normalize(item.itemName).contains(keyword) }
    }

    private fun searchLocal(predicate: (InventoryCountItemResponse) -> Boolean) {
        val results = _state.value.allItems.filter(predicate).take(50)
        val singleItem = results.firstOrNull()
        val existingInput = singleItem?.let { _state.value.dirtyInputs[it.id] }
        _state.update {
            it.copy(
                items = results,
                selectedItem = singleItem,
                accumulatedBase = existingInput,
                scanQuantityType = null,
                scannedCode = null,
                scanPackageQuantity = null,
                caseQuantity = "",
                pieceQuantity = "",
                error = if (results.isEmpty()) "ローカルデータに商品が見つかりません" else null,
                message = if (results.isNotEmpty()) "${results.size}件見つかりました" else null
            )
        }
    }

    fun selectItem(item: InventoryCountItemResponse) {
        val current = _state.value
        val existingInput = current.dirtyInputs[item.id]
        val scannedPackageQuantity = scannedPackageQuantityFor(item, current)
        _state.update {
            it.copy(
                selectedItem = item,
                accumulatedBase = existingInput,
                scanPackageQuantity = existingInput?.packageQuantity ?: scannedPackageQuantity,
                caseQuantity = "",
                pieceQuantity = "",
                message = null,
                error = null
            )
        }
    }

    fun clearSelection() {
        val current = _state.value
        val item = current.selectedItem
        val restoredDirty = if (item != null) {
            val base = current.accumulatedBase
            if (base != null) {
                current.dirtyInputs + (item.id to base)
            } else {
                current.dirtyInputs - item.id
            }
        } else {
            current.dirtyInputs
        }
        _state.update {
            it.copy(
                selectedItem = null,
                items = emptyList(),
                dirtyInputs = restoredDirty,
                caseQuantity = "",
                pieceQuantity = "",
                keyword = "",
                scanQuantityType = null,
                scannedCode = null,
                scanPackageQuantity = null,
                accumulatedBase = null,
                message = null,
                error = null
            )
        }
        persistCache()
    }

    fun removeInput(itemId: Int) {
        _state.update { it.copy(dirtyInputs = it.dirtyInputs - itemId) }
        persistCache()
    }

    fun confirmInput() {
        saveLocalInput()
        val current = _state.value
        val item = current.selectedItem ?: return
        val input = current.dirtyInputs[item.id]
        SoundUtils.playSuccess()
        _state.update {
            it.copy(
                selectedItem = null,
                items = emptyList(),
                caseQuantity = "",
                pieceQuantity = "",
                keyword = "",
                scanQuantityType = null,
                scannedCode = null,
                scanPackageQuantity = null,
                accumulatedBase = null,
                message = "${item.itemName} の入力を保存しました（総バラ: ${input?.totalPieces ?: 0}）",
                error = null
            )
        }
    }

    fun setRound(round: Int) {
        if (round in 1..3) {
            _state.update { it.copy(countRound = round) }
            val current = _state.value
            if (current.selectedItem != null && (current.caseQuantity.isNotBlank() || current.pieceQuantity.isNotBlank())) {
                saveLocalInput()
            }
        }
    }

    fun setCaseQuantity(value: String) {
        val filtered = value.filter(Char::isDigit)
        if (filtered.length - _state.value.caseQuantity.length >= 4) {
            _state.update { it.copy(caseQuantity = "") }
            SoundUtils.playErrorWithVibration(context)
            return
        }
        _state.update { it.copy(caseQuantity = filtered) }
    }

    fun setPieceQuantity(value: String) {
        val filtered = value.filter(Char::isDigit)
        if (filtered.length - _state.value.pieceQuantity.length >= 4) {
            _state.update { it.copy(pieceQuantity = "") }
            SoundUtils.playErrorWithVibration(context)
            return
        }
        _state.update { it.copy(pieceQuantity = filtered) }
    }

    private fun saveLocalInput() {
        val current = _state.value
        val item = current.selectedItem ?: return
        if (current.caseQuantity.isBlank() && current.pieceQuantity.isBlank()) {
            return
        }
        val incrementCase = current.caseQuantity.toIntOrNull() ?: 0
        val incrementPiece = current.pieceQuantity.toIntOrNull() ?: 0
        val base = current.accumulatedBase
        val existing = current.dirtyInputs[item.id]
        val packageQuantity = current.scanPackageQuantity ?: existing?.packageQuantity ?: scannedPackageQuantityFor(item, current)
        val capacityCase = packageQuantity ?: item.capacityCase.coerceAtLeast(1)
        val baseTotal = base?.totalPieces ?: 0
        val incrementTotal = (incrementCase * capacityCase) + incrementPiece
        val total = baseTotal + incrementTotal
        val canMergeCasePiece = base != null && base.packageQuantity == packageQuantity
        val caseQty = if (canMergeCasePiece) base.caseQuantity + incrementCase else incrementCase
        val pieceQty = if (canMergeCasePiece) base.pieceQuantity + incrementPiece else incrementPiece
        val input = LocalInventoryInput(
            itemId = item.id,
            itemCode = item.itemCode,
            itemName = item.itemName,
            locationNo = item.location?.locationNo,
            countRound = current.countRound,
            caseQuantity = caseQty,
            pieceQuantity = pieceQty,
            totalPieces = total,
            searchCode = scannedSearchCodeFor(item, current) ?: existing?.searchCode,
            packageQuantity = packageQuantity,
            requestUuid = existing?.requestUuid ?: base?.requestUuid ?: UUID.randomUUID().toString()
        )
        _state.update { it.copy(dirtyInputs = it.dirtyInputs + (item.id to input)) }
        persistCache()
    }

    fun finishRound() {
        val current = _state.value
        if (current.submitting) return

        val count = current.selectedCount ?: return
        val inputs = current.dirtyInputs.values.filter { it.countRound == current.countRound }
        if (inputs.isEmpty()) {
            _state.update { it.copy(error = "${roundLabel(current.countRound)}の未送信入力はありません") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null, message = "${roundLabel(current.countRound)}送信中...") }
            val chunks = inputs.chunked(BULK_SUBMIT_CHUNK_SIZE)
            val updatedById = mutableMapOf<Int, InventoryCountItemResponse>()
            val missingItemIds = mutableSetOf<Int>()
            val sentItemIds = mutableSetOf<Int>()
            var failureMessage: String? = null

            for ((index, chunk) in chunks.withIndex()) {
                _state.update {
                    it.copy(message = "${roundLabel(current.countRound)}送信中... ${index + 1}/${chunks.size}")
                }

                val request = InventoryBulkCountRequest(
                    countRound = current.countRound,
                    items = chunk.map {
                        InventoryBulkCountItemRequest(
                            itemId = it.itemId,
                            caseQuantity = it.caseQuantity,
                            pieceQuantity = it.pieceQuantity,
                            quantity = it.totalPieces,
                            searchCode = it.searchCode,
                            requestUuid = it.requestUuid
                        )
                    }
                )

                val result = runCatching { api.submitBulkCounts(count.id, request) }
                if (result.isFailure) {
                    failureMessage = result.exceptionOrNull()?.message ?: "送信に失敗しました。ローカル入力は保持されています"
                    break
                }

                val response = result.getOrThrow()

                if (!response.isSuccess) {
                    failureMessage = response.result?.errorMessage ?: "送信に失敗しました。ローカル入力は保持されています"
                    break
                }

                val data = response.result?.data
                val updated = data?.items.orEmpty()
                updatedById += updated.associateBy { it.id }
                missingItemIds += data?.missingItemIds.orEmpty()
                sentItemIds += updated.map { it.id }
            }

            val sentInputs = inputs.filter { it.itemId in sentItemIds }
            val partial = sentInputs.size != inputs.size || missingItemIds.isNotEmpty() || failureMessage != null
            _state.update { state ->
                val sent = sentInputs.map { it.copy(sent = true, updatedAt = System.currentTimeMillis()) }
                state.copy(
                    submitting = false,
                    allItems = state.allItems.map { updatedById[it.id] ?: it },
                    items = state.items.map { updatedById[it.id] ?: it },
                    selectedItem = state.selectedItem?.let { updatedById[it.id] ?: it },
                    dirtyInputs = state.dirtyInputs - sentItemIds,
                    sentHistory = (sent + state.sentHistory).take(100),
                    caseQuantity = "",
                    pieceQuantity = "",
                    accumulatedBase = null,
                    message = "${roundLabel(state.countRound)}を${sentInputs.size}件送信しました",
                    error = if (partial) {
                        failureMessage ?: "一部の明細が更新されませんでした。未更新分はローカルに保持されています"
                    } else {
                        null
                    }
                )
            }
            persistCache()
        }
    }

    private fun restoreCache(countId: Int) {
        val raw = preferences.getString(cacheKey(countId), null) ?: return
        val cache = runCatching { json.decodeFromString<InventoryLocalCache>(raw) }.getOrNull() ?: return
        _state.update {
            it.copy(
                allItems = cache.items,
                janDictionary = cache.janDictionary,
                dirtyInputs = cache.dirtyInputs.associateBy { input -> input.itemId },
                sentHistory = cache.sentHistory,
                syncedAt = cache.syncedAt,
                message = it.message
            )
        }
    }

    private fun restoreBestCache() {
        val currentId = _state.value.selectedCount?.id
        if (currentId != null) {
            val raw = preferences.getString(cacheKey(currentId), null)
            if (raw != null) {
                val cache = runCatching { json.decodeFromString<InventoryLocalCache>(raw) }.getOrNull()
                if (cache != null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            allItems = cache.items,
                            janDictionary = cache.janDictionary,
                            dirtyInputs = cache.dirtyInputs.associateBy { input -> input.itemId },
                            sentHistory = cache.sentHistory,
                            syncedAt = cache.syncedAt,
                            message = null
                        )
                    }
                    return
                }
            }
        }

        val cacheKeys = preferences.all.keys.filter { it.startsWith(CACHE_PREFIX) }
        if (cacheKeys.isEmpty()) return

        val bestKey = cacheKeys.maxByOrNull { key ->
            val raw = preferences.getString(key, null) ?: return@maxByOrNull Long.MIN_VALUE
            val cache = runCatching { json.decodeFromString<InventoryLocalCache>(raw) }.getOrNull()
                ?: return@maxByOrNull Long.MIN_VALUE
            val hasDirty = if (cache.dirtyInputs.isNotEmpty()) 1_000_000_000_000L else 0L
            hasDirty + (cache.syncedAt ?: 0L)
        } ?: return

        val raw = preferences.getString(bestKey, null) ?: return
        val cache = runCatching { json.decodeFromString<InventoryLocalCache>(raw) }.getOrNull() ?: return

        val offlineCount = InventoryCountResponse(
            id = cache.countId,
            countNo = cache.countNo,
            warehouseId = 0,
            status = "IN_PROGRESS",
            currentRound = cache.dirtyInputs.firstOrNull()?.countRound ?: 1,
            totalItems = cache.items.size
        )
        _state.update {
            it.copy(
                loading = false,
                selectedCount = offlineCount,
                countRound = offlineCount.currentRound,
                allItems = cache.items,
                janDictionary = cache.janDictionary,
                dirtyInputs = cache.dirtyInputs.associateBy { input -> input.itemId },
                sentHistory = cache.sentHistory,
                syncedAt = cache.syncedAt,
                message = "オフラインキャッシュから復元しました（${cache.countNo}）"
            )
        }
    }

    private fun persistCache() {
        val state = _state.value
        val count = state.selectedCount ?: return
        val cache = InventoryLocalCache(
            countId = count.id,
            countNo = count.countNo,
            items = state.allItems,
            janDictionary = state.janDictionary,
            dirtyInputs = state.dirtyInputs.values.toList(),
            sentHistory = state.sentHistory,
            syncedAt = state.syncedAt
        )
        val encoded = json.encodeToString(cache)
        preferences.edit(commit = false) {
            putString(cacheKey(count.id), encoded)
        }
    }

    private fun janEntriesFor(code: String, state: InventoryCountState): List<JanCodeEntry>? {
        val candidates = searchCodeCandidates(code)

        return candidates.firstNotNullOfOrNull { state.janDictionary[it] }
    }

    private fun searchCodeCandidates(code: String): List<String> {
        val candidates = linkedSetOf<String>()

        fun add(value: String) {
            value.trim().takeIf { it.isNotEmpty() }?.let { candidates += it }
        }

        add(code)
        add(normalize(code))

        candidates.toList().forEach { value ->
            if (value.all { it.isDigit() }) {
                val trimmed = value.trimStart('0')
                if (trimmed.isNotEmpty()) {
                    add(trimmed)
                }

                val numeric = trimmed.ifEmpty { "0" }
                if (numeric.length <= 8) {
                    add(numeric.padStart(8, '0'))
                }
                if (numeric.length <= 13) {
                    add(numeric.padStart(13, '0'))
                }
            }
        }

        return candidates.toList()
    }

    private fun scannedSearchCodeFor(item: InventoryCountItemResponse, state: InventoryCountState): String? {
        val code = state.scannedCode?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val entries = janEntriesFor(code, state)

        return if (entries == null || entries.isEmpty() || entries.any { it.itemId == item.itemId }) code else null
    }

    private fun scannedPackageQuantityFor(item: InventoryCountItemResponse, state: InventoryCountState): Int? {
        val code = state.scannedCode?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val entries = janEntriesFor(code, state) ?: return null

        return entries.firstOrNull { it.itemId == item.itemId }?.packageQuantity
    }

    private fun normalize(value: String): String =
        value.map {
            when (it) {
                in 'Ａ'..'Ｚ' -> it - 0xFEE0
                in 'ａ'..'ｚ' -> it - 0xFEE0
                in '０'..'９' -> it - 0xFEE0
                else -> it
            }
        }.joinToString("").lowercase()

    private fun roundLabel(round: Int): String = if (round == 3) "最終" else "${round}回目"

    private fun remapDirtyInputs(
        dirtyInputs: Map<Int, LocalInventoryInput>,
        newItems: List<InventoryCountItemResponse>
    ): Map<Int, LocalInventoryInput> {
        if (dirtyInputs.isEmpty()) return dirtyInputs
        val newItemIds = newItems.map { it.id }.toSet()
        val allValid = dirtyInputs.keys.all { it in newItemIds }
        if (allValid) return dirtyInputs

        val codeToNewItem = newItems.associateBy { it.itemCode }
        val remapped = mutableMapOf<Int, LocalInventoryInput>()
        for ((_, input) in dirtyInputs) {
            val newItem = codeToNewItem[input.itemCode]
            if (newItem != null) {
                remapped[newItem.id] = input.copy(itemId = newItem.id)
            } else {
                Log.w("InventoryCount", "Remap: no match for itemCode=${input.itemCode}, dropping input")
            }
        }
        if (remapped.isNotEmpty()) {
            Log.i("InventoryCount", "Remapped ${remapped.size}/${dirtyInputs.size} dirtyInputs to new itemIds")
        }
        return remapped
    }

    private fun cacheKey(countId: Int): String = "$CACHE_PREFIX$countId"

    companion object {
        private const val CACHE_PREFIX = "inventory_count_"
        private const val BULK_SUBMIT_CHUNK_SIZE = 500
        private const val MAX_QUANTITY = 99999
    }
}
