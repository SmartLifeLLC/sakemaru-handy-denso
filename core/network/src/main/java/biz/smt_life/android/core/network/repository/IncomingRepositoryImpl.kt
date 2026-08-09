package biz.smt_life.android.core.network.repository

import android.content.Context
import biz.smt_life.android.core.domain.model.IncomingInspectionBatchSyncData
import biz.smt_life.android.core.domain.model.IncomingInspectionBatchSyncResult
import biz.smt_life.android.core.domain.model.IncomingInspectionDetailData
import biz.smt_life.android.core.domain.model.IncomingInspectionDetailSyncResult
import biz.smt_life.android.core.domain.model.IncomingItemMaster
import biz.smt_life.android.core.domain.model.IncomingItemQuantityCode
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingQuantityType
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWarehouseSummary
import biz.smt_life.android.core.domain.model.IncomingSnapshot
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.ItemLocation
import biz.smt_life.android.core.domain.model.ItemLocationSearchResult
import biz.smt_life.android.core.domain.model.ItemQuantityCode
import biz.smt_life.android.core.domain.model.ItemSearchCode
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.LocationSearchItem
import biz.smt_life.android.core.domain.model.LocationSearchLocations
import biz.smt_life.android.core.domain.model.LocationSearchStock
import biz.smt_life.android.core.domain.model.LocationSearchWarehouse
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.StockLocation
import biz.smt_life.android.core.domain.model.StockStatus
import biz.smt_life.android.core.domain.model.UpdateWorkItemData
import biz.smt_life.android.core.domain.model.WorkItemSchedule
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.IncomingApi
import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingInspectionBatchSyncRequest
import biz.smt_life.android.core.network.model.IncomingInspectionBatchSyncResponse
import biz.smt_life.android.core.network.model.IncomingInspectionDetailRequest
import biz.smt_life.android.core.network.model.IncomingInspectionDetailResponse
import biz.smt_life.android.core.network.model.IncomingItemMasterResponse
import biz.smt_life.android.core.network.model.IncomingProductResponse
import biz.smt_life.android.core.network.model.IncomingScheduleResponse
import biz.smt_life.android.core.network.model.IncomingSnapshotItemResponse
import biz.smt_life.android.core.network.model.IncomingSnapshotResponse
import biz.smt_life.android.core.network.model.IncomingSnapshotScheduleResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.ItemLocationResponse
import biz.smt_life.android.core.network.model.ItemLocationSearchResponse
import biz.smt_life.android.core.network.model.ItemQuantityCodeResponse
import biz.smt_life.android.core.network.model.ItemSearchCodeResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.LocationSearchItemResponse
import biz.smt_life.android.core.network.model.LocationSearchLocationsResponse
import biz.smt_life.android.core.network.model.LocationSearchStockResponse
import biz.smt_life.android.core.network.model.LocationSearchWarehouseResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.StockLocationResponse
import biz.smt_life.android.core.network.model.UpdateWorkItemRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import biz.smt_life.android.core.network.model.WorkItemScheduleResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IncomingRepository.
 * Maps API responses to domain models and handles errors.
 */
@Singleton
class IncomingRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val incomingApi: IncomingApi,
    private val errorMapper: ErrorMapper,
    private val json: Json
) : IncomingRepository {
    private val itemMasterPreferences = context.getSharedPreferences(
        ITEM_MASTER_CACHE_NAME,
        Context.MODE_PRIVATE
    )

    // ============================================================
    // Warehouse Operations
    // ============================================================

    override suspend fun getWarehouses(): Result<List<IncomingWarehouse>> {
        return try {
            val response = incomingApi.getWarehouses()

            if (response.isSuccess && response.result?.data != null) {
                val warehouses = response.result.data.map { it.toDomainModel() }
                Result.success(warehouses)
            } else {
                val errorMessage = extractErrorMessage(response.result, "倉庫一覧の取得に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    // ============================================================
    // Schedule Operations
    // ============================================================

    override suspend fun getSchedules(
        warehouseId: Int,
        search: String?
    ): Result<List<IncomingProduct>> {
        return try {
            val response = incomingApi.getSchedules(
                warehouseId = warehouseId,
                search = search
            )

            if (response.isSuccess && response.result?.data != null) {
                val products = response.result.data.map { it.toDomainModel() }
                Result.success(products)
            } else {
                val errorMessage = extractErrorMessage(response.result, "入庫予定一覧の取得に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun getScheduleDetail(id: Int): Result<IncomingProduct> {
        return try {
            val response = incomingApi.getScheduleDetail(id)

            if (response.isSuccess && response.result?.data != null) {
                val product = response.result.data.toDomainModel()
                Result.success(product)
            } else {
                val errorMessage = extractErrorMessage(response.result, "入庫予定詳細の取得に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    // ============================================================
    // Work Item Operations
    // ============================================================

    override suspend fun getWorkItems(
        warehouseId: Int,
        pickerId: Int?,
        status: String?,
        fromDate: String?,
        toDate: String?,
        limit: Int?
    ): Result<List<IncomingWorkItem>> {
        return try {
            val response = incomingApi.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = status,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit
            )

            if (response.isSuccess && response.result?.data != null) {
                val workItems = response.result.data.map { it.toDomainModel() }
                Result.success(workItems)
            } else {
                val errorMessage = extractErrorMessage(response.result, "作業履歴の取得に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun getWorkingScheduleIds(
        warehouseId: Int,
        pickerId: Int
    ): Result<Set<Int>> {
        return try {
            val response = incomingApi.getWorkItems(
                warehouseId = warehouseId,
                pickerId = pickerId,
                status = "WORKING"
            )

            if (response.isSuccess && response.result?.data != null) {
                val scheduleIds = response.result.data
                    .map { it.incomingScheduleId }
                    .toSet()
                Result.success(scheduleIds)
            } else {
                Result.success(emptySet())
            }
        } catch (e: Exception) {
            // Return empty set on error to avoid blocking product list
            Result.success(emptySet())
        }
    }

    override suspend fun startWork(data: StartWorkData): Result<IncomingWorkItem> {
        return try {
            val request = StartWorkRequest(
                incomingScheduleId = data.incomingScheduleId,
                pickerId = data.pickerId,
                warehouseId = data.warehouseId
            )

            val response = incomingApi.startWork(request)

            if (response.isSuccess && response.result?.data != null) {
                val workItem = response.result.data.toDomainModel()
                Result.success(workItem)
            } else if (response.code == "ALREADY_WORKING" && response.result?.data != null) {
                // Already working - return the existing work item
                val workItem = response.result.data.toDomainModel()
                Result.success(workItem)
            } else {
                val errorMessage = extractErrorMessage(response.result, "作業開始に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun updateWorkItem(id: Int, data: UpdateWorkItemData): Result<IncomingWorkItem> {
        return try {
            val request = UpdateWorkItemRequest(
                workQuantity = data.workQuantity,
                workArrivalDate = data.workArrivalDate,
                workExpirationDate = data.workExpirationDate,
                locationId = data.locationId
            )

            val response = incomingApi.updateWorkItem(id, request)

            if (response.isSuccess && response.result?.data != null) {
                val workItem = response.result.data.toDomainModel()
                Result.success(workItem)
            } else {
                val errorMessage = extractErrorMessage(response.result, "作業データの更新に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun completeWorkItem(id: Int): Result<Unit> {
        return try {
            val response = incomingApi.completeWorkItem(id)

            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                val errorMessage = extractErrorMessage(response.result, "入庫の確定に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun cancelWorkItem(id: Int): Result<Unit> {
        return try {
            val response = incomingApi.cancelWorkItem(id)

            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                val errorMessage = extractErrorMessage(response.result, "キャンセルに失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    // ============================================================
    // Location Operations
    // ============================================================

    override suspend fun searchLocations(
        warehouseId: Int,
        search: String?,
        limit: Int?
    ): Result<List<Location>> {
        return try {
            val response = incomingApi.searchLocations(
                warehouseId = warehouseId,
                search = search,
                limit = limit
            )

            if (response.isSuccess && response.result?.data != null) {
                val locations = response.result.data.map { it.toDomainModel() }
                Result.success(locations)
            } else {
                val errorMessage = extractErrorMessage(response.result, "ロケーション検索に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun searchItemLocations(
        warehouseId: Int,
        search: String,
        limit: Int?
    ): Result<List<ItemLocationSearchResult>> {
        return try {
            val response = incomingApi.searchItemLocations(
                warehouseId = warehouseId,
                search = search,
                limit = limit
            )

            if (response.isSuccess && response.result?.data != null) {
                val results = response.result.data.map { it.toDomainModel() }
                Result.success(results)
            } else {
                val errorMessage = extractErrorMessage(response.result, "ロケ検索に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun getIncomingSnapshot(
        warehouseId: Int,
        inspectionDate: String?
    ): Result<IncomingSnapshot> {
        return try {
            val response = incomingApi.getIncomingSnapshot(
                warehouseId = warehouseId,
                inspectionDate = inspectionDate
            )

            if (response.isSuccess && response.result?.data != null) {
                val cachedMaster = loadCachedIncomingItemMaster(warehouseId)?.products.orEmpty()
                Result.success(response.result.data.toDomainModel(warehouseId, cachedMaster))
            } else {
                val errorMessage = extractErrorMessage(response.result, "入荷検品データの同期に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun ensureIncomingItemMaster(warehouseId: Int): Result<IncomingItemMaster> {
        val cached = loadCachedIncomingItemMasterCache(warehouseId)
        if (cached != null && cached.cachedDate == LocalDate.now().toString()) {
            return Result.success(cached.response.toDomainModel(warehouseId))
        }

        val refreshed = refreshIncomingItemMaster(warehouseId)
        return if (refreshed.isFailure && cached != null) {
            Result.success(cached.response.toDomainModel(warehouseId))
        } else {
            refreshed
        }
    }

    override suspend fun refreshIncomingItemMaster(warehouseId: Int): Result<IncomingItemMaster> {
        return try {
            val response = incomingApi.getIncomingItemMaster(warehouseId)

            if (response.isSuccess && response.result?.data != null) {
                persistIncomingItemMaster(warehouseId, response.result.data)
                Result.success(response.result.data.toDomainModel(warehouseId))
            } else {
                val errorMessage = extractErrorMessage(response.result, "商品マスタの取得に失敗しました")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun syncIncomingInspectionBatch(
        data: IncomingInspectionBatchSyncData
    ): Result<IncomingInspectionBatchSyncResult> {
        return try {
            val response = incomingApi.syncIncomingInspectionBatch(data.toRequest())

            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDomainModel())
            } else {
                val errorMessage = extractErrorMessage(response.result, "入荷検品結果の送信に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    // ============================================================
    // Helper Functions
    // ============================================================

    private fun <T> extractErrorMessage(
        result: ApiEnvelope.ResultBlock<T>?,
        fallbackMessage: String
    ): String {
        val primaryMessage = result?.errorMessage
        val detailedErrors = result?.errors
            ?.values
            ?.flatten()
            ?.joinToString(separator = "\n")

        return when {
            !primaryMessage.isNullOrBlank() && !detailedErrors.isNullOrBlank() ->
                "$primaryMessage\n$detailedErrors"
            !primaryMessage.isNullOrBlank() ->
                primaryMessage
            !detailedErrors.isNullOrBlank() ->
                detailedErrors
            else ->
                fallbackMessage
        }
    }

    private fun loadCachedIncomingItemMaster(warehouseId: Int): IncomingItemMaster? {
        return loadCachedIncomingItemMasterCache(warehouseId)?.response?.toDomainModel(warehouseId)
    }

    private fun loadCachedIncomingItemMasterCache(warehouseId: Int): IncomingItemMasterCache? {
        val raw = itemMasterPreferences.getString(itemMasterCacheKey(warehouseId), null) ?: return null
        return runCatching { json.decodeFromString<IncomingItemMasterCache>(raw) }
            .getOrNull()
            ?.takeIf { it.warehouseId == warehouseId }
    }

    private fun persistIncomingItemMaster(warehouseId: Int, response: IncomingItemMasterResponse) {
        val cache = IncomingItemMasterCache(
            warehouseId = warehouseId,
            cachedDate = LocalDate.now().toString(),
            response = response
        )
        itemMasterPreferences
            .edit()
            .putString(itemMasterCacheKey(warehouseId), json.encodeToString(cache))
            .apply()
    }

    private fun itemMasterCacheKey(warehouseId: Int): String = "$ITEM_MASTER_CACHE_PREFIX$warehouseId"

    // ============================================================
    // Mapping Functions
    // ============================================================

    private fun WarehouseResponse.toDomainModel(): IncomingWarehouse {
        return IncomingWarehouse(
            id = id,
            code = code,
            name = name,
            kanaName = kanaName,
            outOfStockOption = outOfStockOption
        )
    }

    private fun LocationResponse.toDomainModel(): Location {
        return Location(
            id = id,
            code1 = code1,
            code2 = code2,
            code3 = code3,
            name = name,
            displayName = displayName
        )
    }

    private fun IncomingSnapshotResponse.toDomainModel(
        workWarehouseId: Int,
        cachedMasterProducts: List<IncomingProduct>
    ): IncomingSnapshot {
        val scheduleGroups = schedules
            .mapNotNull { schedule ->
                val item = schedule.item ?: return@mapNotNull null
                item.id to schedule.toDomainModel()
            }
            .groupBy({ it.first }, { it.second })

        val scheduleItemMap = linkedMapOf<Int, IncomingSnapshotItemResponse>()
        schedules.forEach { schedule ->
            schedule.item?.let { scheduleItemMap[it.id] = it }
        }
        val cachedMasterById = cachedMasterProducts.associateBy { it.itemId }

        val products = scheduleItemMap.values.map { item ->
            val productSchedules = scheduleGroups[item.id].orEmpty()
            val schedulesForProduct = productSchedules.ifEmpty { listOf(item.toUnplannedSchedule(workWarehouseId)) }
            val expectedTotal = schedulesForProduct.sumOf { it.expectedPieceQuantity ?: it.expectedQuantity }
            val receivedTotal = schedulesForProduct.sumOf { it.receivedPieceQuantity ?: it.receivedQuantity }
            val remainingTotal = schedulesForProduct.sumOf { it.remainingPieceQuantity ?: it.remainingQuantity }

            val baseProduct = cachedMasterById[item.id] ?: item.toIncomingProduct(
                schedules = emptyList(),
                expectedTotal = 0,
                receivedTotal = 0,
                remainingTotal = 0
            )

            baseProduct.withSchedules(
                schedules = schedulesForProduct,
                expectedTotal = expectedTotal,
                receivedTotal = receivedTotal,
                remainingTotal = remainingTotal
            )
        }

        return IncomingSnapshot(
            inspectionDate = inspectionDate,
            generatedAt = generatedAt,
            warehouse = warehouse?.toDomainModel(),
            matchingWarehouseIds = rules?.matchingWarehouseIds.orEmpty(),
            products = products,
            locations = locations.map { it.toDomainModel() }
        )
    }

    private fun IncomingItemMasterResponse.toDomainModel(warehouseId: Int): IncomingItemMaster {
        return IncomingItemMaster(
            warehouseId = warehouse?.id ?: warehouseId,
            masterDate = masterDate.ifBlank { LocalDate.now().toString() },
            generatedAt = generatedAt,
            products = items.map { item ->
                item.toIncomingProduct(
                    schedules = listOf(item.toUnplannedSchedule(warehouseId)),
                    expectedTotal = 0,
                    receivedTotal = 0,
                    remainingTotal = 0
                )
            }
        )
    }

    private fun IncomingProduct.withSchedules(
        schedules: List<IncomingSchedule>,
        expectedTotal: Int,
        receivedTotal: Int,
        remainingTotal: Int
    ): IncomingProduct {
        return copy(
            totalExpectedQuantity = expectedTotal,
            totalReceivedQuantity = receivedTotal,
            totalRemainingQuantity = remainingTotal,
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

    private fun IncomingSnapshotItemResponse.toIncomingProduct(
        schedules: List<IncomingSchedule>,
        expectedTotal: Int,
        receivedTotal: Int,
        remainingTotal: Int
    ): IncomingProduct {
        val codeValues = searchCodes.mapNotNull { it.code.takeIf { code -> code.isNotBlank() } }
        val janCodeValues = searchCodes
            .filter { it.codeType.equals("JAN", ignoreCase = true) }
            .mapNotNull { it.code.takeIf { code -> code.isNotBlank() } }
            .ifEmpty { codeValues }

        return IncomingProduct(
            itemId = id,
            itemCode = code,
            itemName = name,
            searchCode = codeValues.firstOrNull(),
            searchCodes = codeValues,
            itemQuantityCodes = itemQuantityCodes.map { it.toIncomingItemQuantityCode() },
            janCodes = janCodeValues,
            volume = volume,
            volumeUnit = volumeUnit,
            capacityCase = capacityCase,
            packaging = packaging,
            temperatureType = temperatureType,
            defaultLocation = defaultLocation?.toDomainModel(),
            totalExpectedQuantity = expectedTotal,
            totalReceivedQuantity = receivedTotal,
            totalRemainingQuantity = remainingTotal,
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

    private fun IncomingSnapshotItemResponse.toUnplannedSchedule(workWarehouseId: Int): IncomingSchedule {
        return IncomingSchedule(
            id = -id,
            warehouseId = workWarehouseId,
            warehouseName = null,
            isUnplanned = true,
            orderSource = "APP_UNPLANNED",
            orderSourceLabel = "予定なし入荷",
            inspectionPolicy = "APP_CONFIRM_ALLOWED",
            status = IncomingScheduleStatus.PENDING,
            location = defaultLocation?.toDomainModel(),
            capacityCase = capacityCase
        )
    }

    private fun IncomingSnapshotScheduleResponse.toDomainModel(): IncomingSchedule {
        val quantity = quantity
        return IncomingSchedule(
            id = id,
            warehouseId = warehouseId,
            warehouseName = warehouse?.name,
            slipNumber = slipNumber,
            orderSource = orderSource,
            orderSourceLabel = orderSourceLabel,
            inspectionPolicy = inspectionPolicy,
            isEosSent = isEosSent,
            orderDate = orderDate,
            contractorId = contractor?.id,
            contractorName = contractor?.name,
            expectedQuantity = quantity?.expectedQuantity ?: 0,
            receivedQuantity = quantity?.receivedQuantity ?: 0,
            remainingQuantity = quantity?.remainingQuantity ?: 0,
            expectedPieceQuantity = quantity?.expectedPieceQuantity,
            receivedPieceQuantity = quantity?.receivedPieceQuantity,
            remainingPieceQuantity = quantity?.remainingPieceQuantity,
            capacityCase = quantity?.capacityCase ?: item?.capacityCase,
            quantityType = IncomingQuantityType.fromString(quantity?.quantityType),
            expectedArrivalDate = expectedArrivalDate,
            status = IncomingScheduleStatus.fromString(status),
            location = location?.toDomainModel()
        )
    }

    private fun ItemQuantityCodeResponse.toIncomingItemQuantityCode(): IncomingItemQuantityCode {
        return IncomingItemQuantityCode(
            productCode = productCode,
            ownCode = ownCode,
            quantityCode = quantityCode,
            quantity = quantity
        )
    }

    private fun IncomingProductResponse.toDomainModel(): IncomingProduct {
        return IncomingProduct(
            itemId = itemId,
            itemCode = itemCode,
            itemName = itemName,
            searchCode = searchCode,
            janCodes = janCodes,
            volume = volume,
            volumeUnit = volumeUnit,
            capacityCase = capacityCase,
            packaging = packaging,
            temperatureType = temperatureType,
            images = images,
            defaultLocation = defaultLocation?.toDomainModel(),
            totalExpectedQuantity = totalExpectedQuantity,
            totalReceivedQuantity = totalReceivedQuantity,
            totalRemainingQuantity = totalRemainingQuantity,
            warehouses = warehouses.map { it.toDomainModel() },
            schedules = schedules.map { it.toDomainModel() }
        )
    }

    private fun biz.smt_life.android.core.network.model.IncomingWarehouseSummaryResponse.toDomainModel(): IncomingWarehouseSummary {
        return IncomingWarehouseSummary(
            warehouseId = warehouseId,
            warehouseCode = warehouseCode,
            warehouseName = warehouseName,
            expectedQuantity = expectedQuantity,
            receivedQuantity = receivedQuantity,
            remainingQuantity = remainingQuantity
        )
    }

    private fun IncomingScheduleResponse.toDomainModel(): IncomingSchedule {
        return IncomingSchedule(
            id = id,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            slipNumber = slipNumber,
            orderDate = orderDate,
            expectedQuantity = expectedQuantity,
            receivedQuantity = receivedQuantity,
            remainingQuantity = remainingQuantity,
            expectedPieceQuantity = expectedPieceQuantity,
            receivedPieceQuantity = receivedPieceQuantity,
            remainingPieceQuantity = remainingPieceQuantity,
            quantityType = IncomingQuantityType.fromString(quantityType),
            expectedArrivalDate = expectedArrivalDate,
            expirationDate = expirationDate,
            status = IncomingScheduleStatus.fromString(status),
            location = location?.toDomainModel()
        )
    }

    private fun IncomingWorkItemResponse.toDomainModel(): IncomingWorkItem {
        return IncomingWorkItem(
            id = id,
            incomingScheduleId = incomingScheduleId,
            pickerId = pickerId,
            warehouseId = warehouseId,
            locationId = locationId,
            location = location?.toDomainModel(),
            workQuantity = workQuantity,
            workArrivalDate = workArrivalDate,
            workExpirationDate = workExpirationDate,
            status = IncomingWorkStatus.fromString(status),
            startedAt = startedAt,
            schedule = schedule?.toDomainModel()
        )
    }

    private fun WorkItemScheduleResponse.toDomainModel(): WorkItemSchedule {
        return WorkItemSchedule(
            id = id,
            itemId = itemId,
            itemCode = itemCode,
            itemName = itemName,
            janCodes = janCodes,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            expectedQuantity = expectedQuantity,
            receivedQuantity = receivedQuantity,
            remainingQuantity = remainingQuantity,
            quantityType = IncomingQuantityType.fromString(quantityType),
            expectedArrivalDate = expectedArrivalDate,
            status = IncomingScheduleStatus.fromString(status)
        )
    }

    private fun ItemLocationSearchResponse.toDomainModel(): ItemLocationSearchResult {
        return ItemLocationSearchResult(
            item = item.toDomainModel(),
            warehouse = warehouse.toDomainModel(),
            stock = stock.toDomainModel(),
            locations = locations.toDomainModel()
        )
    }

    private fun LocationSearchItemResponse.toDomainModel(): LocationSearchItem {
        return LocationSearchItem(
            id = id,
            code = code,
            name = name,
            kana = kana,
            volume = volume,
            volumeUnit = volumeUnit,
            capacityCase = capacityCase,
            capacityCarton = capacityCarton,
            packaging = packaging,
            temperatureType = temperatureType,
            usesExpirationDate = usesExpirationDate,
            images = images,
            searchCodes = searchCodes.map { it.toDomainModel() },
            janCodes = janCodes,
            itemQuantityCodes = itemQuantityCodes.map { it.toDomainModel() }
        )
    }

    private fun ItemSearchCodeResponse.toDomainModel(): ItemSearchCode {
        return ItemSearchCode(
            code = code,
            codeType = codeType,
            quantityType = quantityType,
            priority = priority
        )
    }

    private fun ItemQuantityCodeResponse.toDomainModel(): ItemQuantityCode {
        return ItemQuantityCode(
            productCode = productCode,
            ownCode = ownCode,
            quantityCode = quantityCode,
            quantity = quantity,
            canOrder = canOrder
        )
    }

    private fun LocationSearchWarehouseResponse.toDomainModel(): LocationSearchWarehouse {
        return LocationSearchWarehouse(
            id = id,
            code = code,
            name = name,
            kanaName = kanaName
        )
    }

    private fun LocationSearchStockResponse.toDomainModel(): LocationSearchStock {
        return LocationSearchStock(
            status = StockStatus.fromString(status),
            hasStock = hasStock,
            lotCount = lotCount,
            locationCount = locationCount,
            currentQuantity = currentQuantity,
            reservedQuantity = reservedQuantity,
            availableQuantity = availableQuantity,
            earliestExpirationDate = earliestExpirationDate,
            latestExpirationDate = latestExpirationDate
        )
    }

    private fun LocationSearchLocationsResponse.toDomainModel(): LocationSearchLocations {
        return LocationSearchLocations(
            suggested = suggested?.toDomainModel(),
            defaultLocation = defaultLocation?.toDomainModel(),
            stock = stock.map { it.toDomainModel() }
        )
    }

    private fun ItemLocationResponse.toDomainModel(): ItemLocation {
        return ItemLocation(
            id = id,
            warehouseId = warehouseId,
            floorId = floorId,
            code = code,
            displayName = displayName.ifBlank { code },
            name = name,
            source = source,
            isNoLocation = isNoLocation
        )
    }

    private fun StockLocationResponse.toDomainModel(): StockLocation {
        return StockLocation(
            id = id,
            warehouseId = warehouseId,
            floorId = floorId,
            code = code,
            displayName = displayName.ifBlank { code },
            name = name,
            source = source,
            isNoLocation = isNoLocation,
            lotCount = lotCount,
            currentQuantity = currentQuantity,
            reservedQuantity = reservedQuantity,
            availableQuantity = availableQuantity
        )
    }

    private fun IncomingInspectionBatchSyncData.toRequest(): IncomingInspectionBatchSyncRequest {
        return IncomingInspectionBatchSyncRequest(
            clientBatchUuid = clientBatchUuid,
            warehouseId = warehouseId,
            inspectionDate = inspectionDate,
            inspectedAt = inspectedAt,
            pickerId = pickerId,
            deviceId = deviceId,
            appVersion = appVersion,
            details = details.map { it.toRequest() }
        )
    }

    private fun IncomingInspectionDetailData.toRequest(): IncomingInspectionDetailRequest {
        return IncomingInspectionDetailRequest(
            clientLineUuid = clientLineUuid,
            incomingScheduleId = incomingScheduleId,
            itemId = itemId,
            itemCode = itemCode,
            itemName = itemName,
            scannedCode = scannedCode,
            slipNumber = slipNumber,
            contractorId = contractorId,
            locationId = locationId,
            caseQuantity = caseQuantity,
            pieceQuantity = pieceQuantity,
            capacityCase = capacityCase,
            totalPieceQuantity = totalPieceQuantity,
            expirationDate = expirationDate,
            inspectedAt = inspectedAt
        )
    }

    private fun IncomingInspectionBatchSyncResponse.toDomainModel(): IncomingInspectionBatchSyncResult {
        return IncomingInspectionBatchSyncResult(
            status = batch.status,
            totalDetailCount = batch.totalDetailCount,
            successCount = batch.successCount,
            historyOnlyCount = batch.historyOnlyCount,
            reviewCount = batch.reviewCount,
            errorCount = batch.errorCount,
            details = details.map { it.toDomainModel() }
        )
    }

    private fun IncomingInspectionDetailResponse.toDomainModel(): IncomingInspectionDetailSyncResult {
        return IncomingInspectionDetailSyncResult(
            clientLineUuid = clientLineUuid,
            incomingScheduleId = incomingScheduleId,
            itemId = itemId,
            itemCode = itemCode,
            itemName = itemName,
            inspectionPolicy = inspectionPolicy,
            resultStatus = resultStatus,
            reviewReason = reviewReason,
            inspectedTotalPieceQuantity = inspectedTotalPieceQuantity,
            appliedPieceQuantity = appliedPieceQuantity,
            shortagePieceQuantity = shortagePieceQuantity
        )
    }

    private companion object {
        const val ITEM_MASTER_CACHE_NAME = "incoming_item_master_cache"
        const val ITEM_MASTER_CACHE_PREFIX = "incoming_item_master_v2_"
    }
}

@Serializable
private data class IncomingItemMasterCache(
    val warehouseId: Int,
    val cachedDate: String,
    val response: IncomingItemMasterResponse
)
