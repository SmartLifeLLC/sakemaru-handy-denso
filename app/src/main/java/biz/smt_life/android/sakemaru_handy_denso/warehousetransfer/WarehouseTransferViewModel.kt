package biz.smt_life.android.sakemaru_handy_denso.warehousetransfer

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.WarehouseTransferApi
import biz.smt_life.android.core.network.model.JanCodeEntry
import biz.smt_life.android.core.network.model.WarehouseTransferStockItem
import biz.smt_life.android.core.network.model.WarehouseTransferSubmitItem
import biz.smt_life.android.core.network.model.WarehouseTransferSubmitRequest
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 倉庫移動（HANDY）ViewModel
 *
 * 1. 移動元倉庫（自店）の在庫リスト + JAN辞書を同期
 * 2. 商品スキャン → 数量入力 → 未送信履歴へ保存（同一商品は加算）
 * 3. 履歴画面 F4 → 通信チェック → 移動先倉庫選択 → 送信
 * 4. 送信成功時のみ未送信履歴を削除。失敗時は必ず保持する
 */
@HiltViewModel
class WarehouseTransferViewModel @Inject constructor(
    private val api: WarehouseTransferApi,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
    private val json: Json
) : ViewModel() {
    private val preferences = context.getSharedPreferences("warehouse_transfer_cache", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(WarehouseTransferState())
    val state: StateFlow<WarehouseTransferState> = _state.asStateFlow()

    private var lastToWarehouseId: Int? = null
    private var pendingUploadUuid: String? = null

    init {
        SoundUtils.init(context)
        val fromWarehouseId = tokenManager.getDefaultWarehouseId()
        _state.update { it.copy(fromWarehouseId = fromWarehouseId) }
        restoreCache(fromWarehouseId)
    }

    // ------------------------------------------------------------
    // Sync
    // ------------------------------------------------------------

    /**
     * 自店倉庫IDを再取得する（再ログイン等でTokenManagerが更新された場合に追従）
     */
    private fun resolveFromWarehouseId(): Int {
        val current = _state.value.fromWarehouseId
        val latest = tokenManager.getDefaultWarehouseId()
        if (latest > 0 && latest != current) {
            _state.update { it.copy(fromWarehouseId = latest) }
            if (current <= 0) restoreCache(latest)
            return latest
        }
        return if (current > 0) current else latest
    }

    fun syncAllItems() {
        val fromWarehouseId = resolveFromWarehouseId()
        if (fromWarehouseId <= 0) {
            _state.update { it.copy(error = "自店倉庫が設定されていません") }
            return
        }
        if (_state.value.syncing) return

        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _state.update { it.copy(error = "通信できません。WiFi接続を確認してください", message = null) }
                SoundUtils.playError()
                return@launch
            }

            _state.update { it.copy(syncing = true, error = null, message = "在庫データ取得中...") }
            val all = mutableListOf<WarehouseTransferStockItem>()
            var page = 1
            var lastPage = 1

            do {
                _state.update { it.copy(message = "取得中... ${all.size}件 (${page}/${lastPage}ページ)") }
                val response = runCatching { api.getStockItems(fromWarehouseId, page, 500) }
                    .getOrElse {
                        Log.e(TAG, "syncAllItems failed", it)
                        _state.update { state -> state.copy(syncing = false, error = it.message ?: "在庫データ取得に失敗しました", message = null) }
                        return@launch
                    }
                if (!response.isSuccess) {
                    _state.update { state ->
                        state.copy(syncing = false, error = response.result?.errorMessage ?: "在庫データ取得に失敗しました", message = null)
                    }
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
                    message = "商品 ${all.size} 件取得。JANコード取得中...",
                    error = null
                )
            }

            val syncedAt = System.currentTimeMillis()
            runCatching { api.getJanCodes(fromWarehouseId) }
                .onSuccess { response ->
                    val dict = if (response.isSuccess) response.result?.data?.janCodes.orEmpty() else emptyMap()
                    _state.update {
                        it.copy(
                            syncing = false,
                            janDictionary = if (dict.isNotEmpty()) dict else it.janDictionary,
                            syncedAt = syncedAt,
                            message = "同期完了（商品 ${all.size} 件 / JAN ${dict.size} 件）"
                        )
                    }
                }
                .onFailure {
                    Log.w(TAG, "getJanCodes failed", it)
                    _state.update { state -> state.copy(syncing = false, syncedAt = syncedAt, message = "商品 ${all.size} 件取得完了（JAN辞書は前回分）") }
                }

            persistCache()
        }
    }

    fun resetLocalData() {
        val fromWarehouseId = _state.value.fromWarehouseId
        preferences.edit { remove(cacheKey(fromWarehouseId)) }
        pendingUploadUuid = null
        _state.update {
            WarehouseTransferState(
                fromWarehouseId = fromWarehouseId,
                selectedTab = it.selectedTab,
                message = "データを初期化しました"
            )
        }
    }

    fun selectTab(tab: WarehouseTransferTab) {
        _state.update { it.copy(selectedTab = tab, error = null) }
    }

    // ------------------------------------------------------------
    // Scan
    // ------------------------------------------------------------

    fun setKeyword(value: String) {
        _state.update {
            it.copy(
                keyword = value,
                error = if (value.isNotBlank()) null else it.error,
                message = if (value.isNotBlank()) null else it.message
            )
        }
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
        val raw = _state.value.keyword.trim()
        val keyword = normalize(raw)
        if (keyword.isEmpty()) {
            _state.update { it.copy(error = "商品コード、JAN、バーコードを入力してください") }
            return
        }
        if (!_state.value.hasLocalData) {
            _state.update { it.copy(keyword = "", error = "先に在庫リストを同期してください") }
            SoundUtils.playError()
            return
        }
        _state.update { it.copy(keyword = "") }

        if (raw.length == 14 && raw.all { it.isDigit() }) {
            _state.update { it.copy(error = "ITFコードエラー", message = null) }
            SoundUtils.playError()
            return
        }

        fun applyResult(results: List<WarehouseTransferStockItem>, quantityType: String?, packageQuantity: Int?) {
            // 同一商品が複数在庫行（ロケーション違い等）で返る場合は商品ID単位で1件に絞る
            val distinct = results.distinctBy { it.localKey }
            if (distinct.isEmpty()) {
                _state.update { it.copy(selectedItem = null, items = emptyList(), error = "商品が見つかりません", message = null) }
                SoundUtils.playError()
                return
            }
            if (distinct.size != 1) {
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
                        error = "複数の商品が該当しました。商品CDで入力してください",
                        message = null
                    )
                }
                SoundUtils.playError()
                return
            }

            val singleItem = distinct.first()
            val existingInput = _state.value.dirtyInputs[singleItem.localKey]
            _state.update {
                it.copy(
                    items = distinct,
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

        // 1. JAN辞書
        val janEntries = janEntriesFor(raw, _state.value)
        if (!janEntries.isNullOrEmpty()) {
            val itemIds = janEntries.map { it.itemId }.toSet()
            val results = _state.value.allItems.filter { it.itemId in itemIds }
            if (results.isNotEmpty()) {
                val distinct = results.distinctBy { it.localKey }
                val packageQuantity = if (distinct.size == 1) {
                    janEntries.firstOrNull { it.itemId == distinct.first().itemId }?.packageQuantity
                } else {
                    null
                }
                applyResult(results, janEntries.first().quantityType, packageQuantity)
                return
            }
        }

        // 2. ローカル在庫リスト（商品CD / バーコード 完全一致）
        val keywordCandidates = searchCodeCandidates(raw).toSet()
        val localResults = _state.value.allItems.filter { item ->
            normalize(item.itemCode) in keywordCandidates
                || normalize(item.barcode ?: "") in keywordCandidates
        }
        if (localResults.isNotEmpty()) {
            applyResult(localResults, null, null)
            return
        }

        _state.update { it.copy(error = "商品が見つかりません", message = null) }
        SoundUtils.playError()
    }

    fun clearSelection() {
        val current = _state.value
        val item = current.selectedItem
        val restoredDirty = if (item != null) {
            val base = current.accumulatedBase
            if (base != null) current.dirtyInputs + (item.localKey to base) else current.dirtyInputs - item.localKey
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

    fun removeInput(localKey: String) {
        _state.update { it.copy(dirtyInputs = it.dirtyInputs - localKey) }
        persistCache()
    }

    fun confirmInput() {
        val current = _state.value
        val item = current.selectedItem ?: return
        val parsedCase = current.caseQuantity.toIntOrNull()
        val parsedPiece = current.pieceQuantity.toIntOrNull()
        if (parsedCase == null && parsedPiece == null) {
            _state.update { it.copy(error = "数量を入力してください") }
            SoundUtils.playError()
            return
        }
        if (current.totalPieces <= 0) {
            _state.update { it.copy(error = "数量を入力してください") }
            SoundUtils.playError()
            return
        }

        val savedInput = saveLocalInput()
        val input = savedInput ?: current.dirtyInputs[item.localKey]
        val overStock = input != null && item.availableTotalPieces in 1 until input.totalPieces
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
                message = if (input == null) {
                    "${item.itemName} の入力を相殺しました"
                } else {
                    "${item.itemName} を保存しました（総バラ: ${input.totalPieces}）"
                },
                error = if (overStock) "在庫（${item.availableTotalPieces}）を超える数量です。Web確定時に確認されます" else null
            )
        }
    }

    fun setCaseQuantity(value: String) {
        val filtered = normalizeQuantityInput(value)
        val currentDigits = _state.value.caseQuantity.count(Char::isDigit)
        if (filtered.count(Char::isDigit) - currentDigits >= 4) {
            _state.update { it.copy(caseQuantity = "") }
            SoundUtils.playErrorWithVibration(context)
            return
        }
        _state.update { it.copy(caseQuantity = filtered) }
    }

    fun setPieceQuantity(value: String) {
        val filtered = normalizeQuantityInput(value)
        val currentDigits = _state.value.pieceQuantity.count(Char::isDigit)
        if (filtered.count(Char::isDigit) - currentDigits >= 4) {
            _state.update { it.copy(pieceQuantity = "") }
            SoundUtils.playErrorWithVibration(context)
            return
        }
        _state.update { it.copy(pieceQuantity = filtered) }
    }

    fun toggleCaseQuantitySign() {
        _state.update { it.copy(caseQuantity = toggleQuantitySign(it.caseQuantity)) }
    }

    fun togglePieceQuantitySign() {
        _state.update { it.copy(pieceQuantity = toggleQuantitySign(it.pieceQuantity)) }
    }

    private fun saveLocalInput(): LocalWarehouseTransferInput? {
        val current = _state.value
        val item = current.selectedItem ?: return null
        val parsedCase = current.caseQuantity.toIntOrNull()
        val parsedPiece = current.pieceQuantity.toIntOrNull()
        if (parsedCase == null && parsedPiece == null) {
            return null
        }
        val incrementCase = parsedCase ?: 0
        val incrementPiece = parsedPiece ?: 0
        val base = current.accumulatedBase
        val existing = current.dirtyInputs[item.localKey]
        val packageQuantity = current.scanPackageQuantity ?: existing?.packageQuantity ?: scannedPackageQuantityFor(item, current)
        val capacityCase = packageQuantity ?: item.capacityCase.coerceAtLeast(1)
        val baseTotal = base?.totalPieces ?: 0
        val incrementTotal = (incrementCase * capacityCase) + incrementPiece
        val total = baseTotal + incrementTotal
        if (total <= 0) {
            _state.update { it.copy(dirtyInputs = it.dirtyInputs - item.localKey) }
            persistCache()
            return null
        }
        val canMergeCasePiece = base != null && base.packageQuantity == packageQuantity
        val caseQty = if (canMergeCasePiece) base.caseQuantity + incrementCase else incrementCase
        val pieceQty = if (canMergeCasePiece) base.pieceQuantity + incrementPiece else incrementPiece
        val input = LocalWarehouseTransferInput(
            localKey = item.localKey,
            itemId = item.itemId,
            itemCode = item.itemCode,
            itemName = item.itemName,
            barcode = item.barcode,
            realStockId = item.realStockId,
            locationNo = item.location?.locationNo,
            stockAllocationCode = item.stockAllocationCode,
            caseQuantity = caseQty,
            pieceQuantity = pieceQty,
            packageQuantity = packageQuantity,
            totalPieces = total,
            availableQuantityAtSync = item.availableTotalPieces,
            searchCode = scannedSearchCodeFor(item, current) ?: existing?.searchCode,
            requestUuid = existing?.requestUuid ?: base?.requestUuid ?: UUID.randomUUID().toString()
        )
        _state.update { it.copy(dirtyInputs = it.dirtyInputs + (item.localKey to input)) }
        persistCache()
        return input
    }

    // ------------------------------------------------------------
    // Submit (通信チェック → 移動先選択 → 送信)
    // ------------------------------------------------------------

    /**
     * 送信開始（履歴画面 F4）
     *
     * 通信できない場合はダイアログを開かずエラー表示し、未送信履歴はそのまま保持する。
     */
    fun startSubmit() {
        resolveFromWarehouseId()
        val current = _state.value
        if (current.submitting || current.destinationLoading) return
        if (current.fromWarehouseId <= 0) {
            _state.update { it.copy(error = "自店倉庫が設定されていません。再ログインしてください") }
            return
        }
        if (current.dirtyInputs.isEmpty()) {
            _state.update { it.copy(error = "未送信の入力はありません") }
            return
        }

        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _state.update { it.copy(error = "通信できません。WiFi接続後に再度送信してください（未送信履歴は保持されています）", message = null) }
                SoundUtils.playError()
                return@launch
            }

            _state.update { it.copy(destinationLoading = true, error = null, message = "移動先倉庫を取得中...") }
            val result = runCatching { api.getWarehouses(excludeWarehouseId = current.fromWarehouseId) }
            val response = result.getOrNull()
            if (response == null || !response.isSuccess) {
                Log.w(TAG, "getWarehouses failed", result.exceptionOrNull())
                _state.update {
                    it.copy(
                        destinationLoading = false,
                        error = failureMessageFor(result.exceptionOrNull(), "サーバーと通信できませんでした"),
                        message = null
                    )
                }
                SoundUtils.playError()
                return@launch
            }

            val warehouses = response.result?.data?.warehouses.orEmpty().filter { it.id != current.fromWarehouseId }
            if (warehouses.isEmpty()) {
                _state.update { it.copy(destinationLoading = false, error = "移動先倉庫がありません", message = null) }
                return@launch
            }

            val defaultTo = lastToWarehouseId?.takeIf { id -> warehouses.any { it.id == id } }
            _state.update {
                it.copy(
                    destinationLoading = false,
                    destinationDialogVisible = true,
                    warehouses = warehouses,
                    selectedToWarehouseId = defaultTo,
                    message = null,
                    error = null
                )
            }
        }
    }

    fun selectDestination(warehouseId: Int) {
        _state.update { it.copy(selectedToWarehouseId = warehouseId) }
    }

    fun dismissDestinationDialog() {
        _state.update { it.copy(destinationDialogVisible = false, error = null) }
    }

    /**
     * 移動先確定 → 送信
     *
     * - 500件単位で送信。各バッチの upload_uuid は成功するまで固定し、通信断後の再送で二重登録しない。
     * - 成功したバッチ分だけ未送信履歴から削除。失敗したバッチ以降は必ず保持する。
     */
    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val toWarehouse = current.selectedToWarehouse ?: run {
            _state.update { it.copy(error = "移動先倉庫を選択してください") }
            SoundUtils.playError()
            return
        }
        if (toWarehouse.id == current.fromWarehouseId) {
            _state.update { it.copy(error = "移動元と移動先は同一にできません") }
            SoundUtils.playError()
            return
        }
        val inputs = current.dirtyInputs.values.sortedBy { it.updatedAt }
        if (inputs.isEmpty()) {
            _state.update { it.copy(destinationDialogVisible = false, error = "未送信の入力はありません") }
            return
        }

        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _state.update {
                    it.copy(
                        destinationDialogVisible = false,
                        error = "通信できません。WiFi接続後に再度送信してください（未送信履歴は保持されています）",
                        message = null
                    )
                }
                SoundUtils.playError()
                return@launch
            }

            _state.update { it.copy(submitting = true, destinationDialogVisible = false, error = null, message = "送信中...") }
            lastToWarehouseId = toWarehouse.id

            val uploadUuidBase = pendingUploadUuid ?: UUID.randomUUID().toString().also {
                pendingUploadUuid = it
                persistCache()
            }
            val today = LocalDate.now().toString()
            val chunks = inputs.chunked(SUBMIT_CHUNK_SIZE)
            val sentKeys = mutableSetOf<String>()
            var acceptedCount = 0
            var missingCount = 0
            var lastCandidateId: Int? = null
            var lastCandidateNo: String? = null
            var failureMessage: String? = null

            for ((index, chunk) in chunks.withIndex()) {
                if (chunks.size > 1) {
                    _state.update { it.copy(message = "送信中... ${index + 1}/${chunks.size}") }
                }
                val request = WarehouseTransferSubmitRequest(
                    uploadUuid = if (chunks.size == 1) uploadUuidBase else "$uploadUuidBase-$index",
                    fromWarehouseId = current.fromWarehouseId,
                    toWarehouseId = toWarehouse.id,
                    processDate = today,
                    deliveredDate = today,
                    items = chunk.map { input ->
                        WarehouseTransferSubmitItem(
                            itemId = input.itemId,
                            itemCode = input.itemCode,
                            realStockId = input.realStockId,
                            stockAllocationCode = input.stockAllocationCode,
                            caseQuantity = input.caseQuantity,
                            pieceQuantity = input.pieceQuantity,
                            packageQuantity = input.packageQuantity,
                            quantity = input.totalPieces,
                            searchCode = input.searchCode,
                            requestUuid = input.requestUuid
                        )
                    }
                )

                val result = runCatching { api.submitCandidates(request) }
                val response = result.getOrNull()
                if (response == null) {
                    Log.e(TAG, "submit failed", result.exceptionOrNull())
                    failureMessage = failureMessageFor(result.exceptionOrNull(), "送信できませんでした")
                    break
                }
                if (!response.isSuccess) {
                    val serverMessage = response.result?.errorMessage
                    val fieldErrors = response.result?.errors?.values?.flatten()?.firstOrNull()
                    Log.w(TAG, "submit rejected: $serverMessage errors=${response.result?.errors}")
                    failureMessage = listOfNotNull(serverMessage ?: "送信できませんでした", fieldErrors).joinToString(" / ") + "（未送信履歴に残しました）"
                    break
                }

                val data = response.result?.data
                acceptedCount += data?.acceptedCount ?: 0
                missingCount += data?.missingItemIds?.size ?: 0
                lastCandidateId = data?.candidate?.id ?: lastCandidateId
                lastCandidateNo = data?.candidate?.candidateNo ?: lastCandidateNo
                sentKeys += chunk.map { it.localKey }
            }

            val allSent = failureMessage == null && sentKeys.size == inputs.size
            if (allSent) {
                pendingUploadUuid = null
            }

            val submitResult = if (sentKeys.isNotEmpty()) {
                WarehouseTransferSubmitResult(
                    candidateId = lastCandidateId,
                    candidateNo = lastCandidateNo,
                    toWarehouseName = "[${toWarehouse.code}] ${toWarehouse.name}",
                    itemCount = sentKeys.size,
                    totalQuantity = inputs.filter { it.localKey in sentKeys }.sumOf { it.totalPieces },
                    missingCount = missingCount
                )
            } else {
                null
            }

            if (allSent) SoundUtils.playSuccess() else SoundUtils.playError()

            _state.update { state ->
                state.copy(
                    submitting = false,
                    // 成功分のみ削除。失敗分は必ず残す
                    dirtyInputs = state.dirtyInputs - sentKeys,
                    selectedItem = null,
                    items = emptyList(),
                    caseQuantity = "",
                    pieceQuantity = "",
                    accumulatedBase = null,
                    lastSubmitResult = submitResult ?: state.lastSubmitResult,
                    message = when {
                        allSent && missingCount > 0 -> "${sentKeys.size}件送信しました（対象外商品 ${missingCount}件）。候補番号: ${lastCandidateNo ?: "-"}"
                        allSent -> "${sentKeys.size}件送信しました。候補番号: ${lastCandidateNo ?: "-"}"
                        sentKeys.isNotEmpty() -> "${sentKeys.size}/${inputs.size}件送信しました"
                        else -> null
                    },
                    error = failureMessage
                )
            }
            persistCache()
        }
    }

    // ------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------

    private fun restoreCache(fromWarehouseId: Int) {
        val raw = preferences.getString(cacheKey(fromWarehouseId), null) ?: return
        val cache = runCatching { json.decodeFromString<WarehouseTransferLocalCache>(raw) }.getOrNull() ?: return
        lastToWarehouseId = cache.lastToWarehouseId
        pendingUploadUuid = cache.pendingUploadUuid
        _state.update {
            it.copy(
                allItems = cache.items,
                janDictionary = cache.janDictionary,
                dirtyInputs = cache.dirtyInputs.associateBy { input -> input.localKey },
                syncedAt = cache.syncedAt,
                lastSubmitResult = cache.lastSubmitResult,
                message = if (cache.dirtyInputs.isNotEmpty()) "未送信 ${cache.dirtyInputs.size} 件を復元しました" else null
            )
        }
    }

    private fun persistCache() {
        val state = _state.value
        if (state.fromWarehouseId <= 0) return
        val cache = WarehouseTransferLocalCache(
            fromWarehouseId = state.fromWarehouseId,
            items = state.allItems,
            janDictionary = state.janDictionary,
            dirtyInputs = state.dirtyInputs.values.toList(),
            syncedAt = state.syncedAt,
            lastToWarehouseId = lastToWarehouseId,
            pendingUploadUuid = pendingUploadUuid,
            lastSubmitResult = state.lastSubmitResult
        )
        val encoded = json.encodeToString(cache)
        preferences.edit(commit = false) {
            putString(cacheKey(state.fromWarehouseId), encoded)
        }
    }

    private fun cacheKey(fromWarehouseId: Int): String = "$CACHE_PREFIX$fromWarehouseId"

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    /**
     * 例外種別に応じたユーザー向けメッセージ。未送信履歴が保持されることを必ず伝える。
     */
    private fun failureMessageFor(error: Throwable?, fallback: String): String {
        val reason = when (val mapped = error?.let { ErrorMapper.mapException(it) }) {
            is NetworkException.Unauthorized -> "認証が切れています。ログアウトして再ログインしてください"
            is NetworkException.Forbidden -> "権限がありません"
            is NetworkException.ServerError -> "サーバーエラーが発生しました"
            is NetworkException.NetworkError -> "通信できません。WiFi接続を確認してください"
            is NetworkException.ValidationError -> mapped.msg
            null -> fallback
            else -> fallback
        }
        return "$reason（未送信履歴は保持されています）"
    }

    private fun isNetworkAvailable(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun janEntriesFor(code: String, state: WarehouseTransferState): List<JanCodeEntry>? =
        searchCodeCandidates(code).firstNotNullOfOrNull { state.janDictionary[it] }

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

    private fun scannedSearchCodeFor(item: WarehouseTransferStockItem, state: WarehouseTransferState): String? {
        val code = state.scannedCode?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val entries = janEntriesFor(code, state)

        return if (entries.isNullOrEmpty() || entries.any { it.itemId == item.itemId }) code else null
    }

    private fun scannedPackageQuantityFor(item: WarehouseTransferStockItem, state: WarehouseTransferState): Int? {
        val code = state.scannedCode?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val entries = janEntriesFor(code, state) ?: return null

        return entries.firstOrNull { it.itemId == item.itemId }?.packageQuantity
    }

    private fun normalizeQuantityInput(value: String): String {
        var hasMinus = false
        val digits = StringBuilder()

        for (char in value) {
            when {
                char.isDigit() -> digits.append(char)
                char.isMinusSign() && !hasMinus && digits.isEmpty() -> hasMinus = true
            }
        }

        return if (hasMinus) "-$digits" else digits.toString()
    }

    private fun toggleQuantitySign(value: String): String {
        val normalized = normalizeQuantityInput(value)

        return when {
            normalized.startsWith("-") -> normalized.drop(1)
            normalized.isBlank() -> "-"
            else -> "-$normalized"
        }
    }

    private fun Char.isMinusSign(): Boolean =
        this == '-' || this == 'ー' || this == '－' || this == '−' || this == '―' || this == '–' || this == '—'

    private fun normalize(value: String): String =
        value.map {
            when (it) {
                in 'Ａ'..'Ｚ' -> it - 0xFEE0
                in 'ａ'..'ｚ' -> it - 0xFEE0
                in '０'..'９' -> it - 0xFEE0
                else -> it
            }
        }.joinToString("").lowercase()

    companion object {
        private const val TAG = "WarehouseTransfer"
        private const val CACHE_PREFIX = "warehouse_transfer_"
        private const val SUBMIT_CHUNK_SIZE = 500
    }
}
