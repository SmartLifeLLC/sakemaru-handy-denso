package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.PickingApi
import biz.smt_life.android.core.network.model.PickingTaskResponse
import biz.smt_life.android.core.network.model.UpdatePickingRequest
import biz.smt_life.android.core.network.model.UpdatePickingResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PickingTaskRepository.
 * Maps API responses to domain models and handles errors.
 *
 * SINGLE SOURCE OF TRUTH:
 * Maintains an in-memory StateFlow of all tasks.
 * All mutations (update, refresh) update this flow to keep all screens synchronized.
 */
@Singleton
class PickingTaskRepositoryImpl @Inject constructor(
    private val pickingApi: PickingApi,
    private val errorMapper: ErrorMapper
) : PickingTaskRepository {

    // Single source of truth for all picking tasks
    private val _tasksFlow = MutableStateFlow<List<PickingTask>>(emptyList())
    override val tasksFlow: StateFlow<List<PickingTask>> = _tasksFlow.asStateFlow()

    override fun taskFlow(taskId: Int): Flow<PickingTask?> {
        return tasksFlow.map { tasks ->
            tasks.find { it.taskId == taskId }
        }
    }

    override suspend fun getMyAreaTasks(warehouseId: Int, pickerId: Int): Result<List<PickingTask>> {
        return try {
            val response = pickingApi.getPickingTasks(
                warehouseId = warehouseId,
                pickerId = pickerId
            )

            if (response.isSuccess && response.result?.data != null) {
                val tasks = response.result.data.map { it.toDomainModel() }
                // Update shared state flow
                _tasksFlow.value = tasks
                Result.success(tasks)
            } else {
                val errorMessage = response.result?.errorMessage ?: "Failed to fetch tasks"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun getAllTasks(warehouseId: Int): Result<List<PickingTask>> {
        return try {
            val response = pickingApi.getPickingTasks(
                warehouseId = warehouseId,
                pickerId = null // No picker filter for "All Courses"
            )

            if (response.isSuccess && response.result?.data != null) {
                val tasks = response.result.data.map { it.toDomainModel() }
                // Update shared state flow
                _tasksFlow.value = tasks
                Result.success(tasks)
            } else {
                val errorMessage = response.result?.errorMessage ?: "Failed to fetch tasks"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun startTask(taskId: Int): Result<Unit> {
        return try {
            val response = pickingApi.startPickingTask(
                id = taskId,
                idempotencyKey = UUID.randomUUID().toString()
            )

            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                val errorMessage = extractErrorMessage(response.result, "タスクの開始に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun updatePickingItem(
        resultId: Int,
        pickedQty: Double,
        pickedQtyType: String
    ): Result<PickingTaskItem> {
        return try {
            val request = UpdatePickingRequest(
                pickedQty = pickedQty.toString(),
                pickedQtyType = pickedQtyType
            )

            val response = pickingApi.updatePickingResult(
                resultId = resultId,
                idempotencyKey = UUID.randomUUID().toString(),
                request = request
            )

            if (response.isSuccess && response.result?.data != null) {
                // API returns only minimal fields (id, picked_qty, status).
                // ViewModel will merge this with existing item data.
                val updatedItem = PickingTaskItem(
                    id = response.result.data.wmsPickingItemResultId,
                    itemId = 0, // Not provided by API - preserved from local state
                    itemName = "", // Not provided by API - preserved from local state
                    janCode = null,
                    volume = null,
                    capacityCase = null,
                    packaging = null,
                    temperatureType = null,
                    images = emptyList(),
                    plannedQtyType = QuantityType.fromString(pickedQtyType), // From request
                    plannedQty = 0.0, // Not provided by API - preserved from local state
                    pickedQty = response.result.data.pickedQty.toDoubleOrNull() ?: pickedQty,
                    status = biz.smt_life.android.core.domain.model.ItemStatus.fromString(response.result.data.status),
                    walkingOrder = 0,
                    slipNumber = 0 // Not provided by API - preserved from local state
                )
                Result.success(updatedItem)
            } else {
                val errorMessage = extractErrorMessage(response.result, "出庫数量の更新に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun completeTask(taskId: Int): Result<Unit> {
        return try {
            val response = pickingApi.completePickingTask(
                id = taskId,
                idempotencyKey = UUID.randomUUID().toString()
            )

            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                val errorMessage = extractErrorMessage(response.result, "タスクの完了に失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun cancelPickingItem(
        resultId: Int,
        taskId: Int,
        warehouseId: Int
    ): Result<Unit> {
        return try {
            val response = pickingApi.cancelPickingItem(
                resultId = resultId,
                idempotencyKey = UUID.randomUUID().toString()
            )

            if (response.isSuccess) {
                // After successful cancel, refresh the task to update tasksFlow
                // This ensures all observers (history, data input, course list) see the update
                refreshTask(taskId, warehouseId)
                Result.success(Unit)
            } else {
                val errorMessage = extractErrorMessage(response.result, "出庫履歴のキャンセルに失敗しました")
                Result.failure(NetworkException.ValidationError(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    override suspend fun refreshTask(taskId: Int, warehouseId: Int): Result<PickingTask> {
        return try {
            // Fetch all tasks for the warehouse and find the matching one
            val response = pickingApi.getPickingTasks(
                warehouseId = warehouseId,
                pickerId = null
            )

            if (response.isSuccess && response.result?.data != null) {
                val tasks = response.result.data.map { it.toDomainModel() }

                // Update shared state flow with ALL tasks
                _tasksFlow.value = tasks

                val matchingTask = tasks.find { it.taskId == taskId }

                if (matchingTask != null) {
                    Result.success(matchingTask)
                } else {
                    // Task not in server response - check if we have a cached version
                    val cachedTask = tasksFlow.value.find { it.taskId == taskId }
                    if (cachedTask != null) {
                        // Log warning but return cached task to avoid breaking UI
                        android.util.Log.w(
                            "PickingTaskRepository",
                            "Task $taskId not found in server response, using cached version"
                        )
                        Result.success(cachedTask)
                    } else {
                        Result.failure(Exception("タスクが見つかりません。リストを更新してください。"))
                    }
                }
            } else {
                val errorMessage = response.result?.errorMessage ?: "Failed to refresh task"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val mappedException = errorMapper.mapException(e)
            Result.failure(mappedException)
        }
    }

    /**
     * Extracts error message from API response result block.
     * Combines error_message and detailed errors into a single user-friendly message.
     *
     * @param result The result block from API envelope
     * @param fallbackMessage Default message if no error details found
     * @return Combined error message
     */
    private fun <T> extractErrorMessage(
        result: biz.smt_life.android.core.network.model.ApiEnvelope.ResultBlock<T>?,
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

    /**
     * Maps API response to domain model.
     * Includes all extended fields from the updated API schema.
     * Counters are computed properties in PickingTask, not stored statically.
     */
    private fun PickingTaskResponse.toDomainModel(): PickingTask {
        val items = pickingList.map { item ->
            PickingTaskItem(
                id = item.wmsPickingItemResultId,
                itemId = item.itemId,
                itemName = item.itemName,
                janCode = item.janCode,
                volume = item.volume,
                capacityCase = item.capacityCase,
                packaging = item.packaging,
                temperatureType = item.temperatureType,
                images = item.images,
                plannedQtyType = QuantityType.fromString(item.plannedQtyType),
                plannedQty = item.plannedQty.toDoubleOrNull() ?: 0.0,
                pickedQty = item.pickedQty.toDoubleOrNull() ?: 0.0,
                status = biz.smt_life.android.core.domain.model.ItemStatus.fromString(item.status),
                walkingOrder = item.walkingOrder,
                slipNumber = item.slipNumber
            )
        }

        return PickingTask(
            taskId = wave.wmsPickingTaskId,
            waveId = wave.wmsWaveId,
            courseName = course.name,
            courseCode = course.code,
            pickingAreaName = pickingArea.name,
            pickingAreaCode = pickingArea.code,
            items = items
            // totalItems, registeredCount, etc. are now computed properties
        )
    }

}
