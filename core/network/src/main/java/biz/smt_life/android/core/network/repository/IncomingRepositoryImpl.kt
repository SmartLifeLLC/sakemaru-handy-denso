package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingQuantityType
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWarehouseSummary
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.domain.model.StartWorkData
import biz.smt_life.android.core.domain.model.UpdateWorkItemData
import biz.smt_life.android.core.domain.model.WorkItemSchedule
import biz.smt_life.android.core.domain.repository.IncomingRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.IncomingApi
import biz.smt_life.android.core.network.model.ApiEnvelope
import biz.smt_life.android.core.network.model.IncomingProductResponse
import biz.smt_life.android.core.network.model.IncomingScheduleResponse
import biz.smt_life.android.core.network.model.IncomingWorkItemResponse
import biz.smt_life.android.core.network.model.LocationResponse
import biz.smt_life.android.core.network.model.StartWorkRequest
import biz.smt_life.android.core.network.model.UpdateWorkItemRequest
import biz.smt_life.android.core.network.model.WarehouseResponse
import biz.smt_life.android.core.network.model.WorkItemScheduleResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of IncomingRepository.
 * Maps API responses to domain models and handles errors.
 */
@Singleton
class IncomingRepositoryImpl @Inject constructor(
    private val incomingApi: IncomingApi,
    private val errorMapper: ErrorMapper
) : IncomingRepository {

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
            expectedQuantity = expectedQuantity,
            receivedQuantity = receivedQuantity,
            remainingQuantity = remainingQuantity,
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
}
