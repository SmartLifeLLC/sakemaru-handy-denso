package biz.smt_life.android.feature.outbound.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.NetworkException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Outbound Picking (2.5.2 - Data Input Screen).
 * Handles picking item updates, navigation between items, and task completion.
 */
@HiltViewModel
class OutboundPickingViewModel @Inject constructor(
    private val pickingTaskRepository: PickingTaskRepository,
    private val tokenManager: biz.smt_life.android.core.ui.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(OutboundPickingState())
    val state: StateFlow<OutboundPickingState> = _state.asStateFlow()

    /**
     * Initialize the screen with a picking task.
     * Stores the original task and filters PENDING items for display (per spec 2.5.2).
     * Gets warehouse ID from TokenManager for server refresh operations.
     *
     * @param task The picking task to work with
     */
    fun initialize(task: PickingTask) {
        val warehouseId = tokenManager.getDefaultWarehouseId()

        _state.update {
            // Filter to only PENDING items (status-based filtering)
            val pendingItems = task.items.filter { item ->
                item.status == biz.smt_life.android.core.domain.model.ItemStatus.PENDING
            }

            if (pendingItems.isEmpty()) {
                // No PENDING items - should not happen if navigation is correct
                // Show error state
                return@update it.copy(
                    originalTask = task,
                    pendingItems = emptyList(),
                    currentIndex = 0,
                    pickedQtyInput = "",
                    isLoading = false,
                    warehouseId = warehouseId,
                    errorMessage = "登録可能な商品がありません"
                )
            }

            val currentItem = pendingItems.firstOrNull()
            // Default picked qty = planned qty per spec
            val defaultQty = currentItem?.plannedQty?.toString() ?: ""

            it.copy(
                originalTask = task,         // Store full task for counters
                pendingItems = pendingItems, // Store filtered PENDING items
                currentIndex = 0,
                pickedQtyInput = defaultQty,
                isLoading = false,
                warehouseId = warehouseId
            )
        }
    }

    /**
     * Update the picked quantity input field.
     */
    fun onPickedQtyChange(value: String) {
        // Allow only valid decimal numbers
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _state.update { it.copy(pickedQtyInput = value) }
        }
    }

    /**
     * Navigate to previous PENDING item (前へ F2).
     * Default quantity is planned qty for PENDING items.
     */
    fun moveToPrevItem() {
        _state.update { currentState ->
            if (currentState.canMovePrev) {
                val newIndex = currentState.currentIndex - 1
                val newItem = currentState.task?.items?.getOrNull(newIndex)
                // PENDING items: default to planned qty
                val newQty = newItem?.plannedQty?.toString() ?: ""

                currentState.copy(
                    currentIndex = newIndex,
                    pickedQtyInput = newQty,
                    errorMessage = null
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Navigate to next PENDING item (次へ F3).
     * Default quantity is planned qty for PENDING items.
     */
    fun moveToNextItem() {
        _state.update { currentState ->
            if (currentState.canMoveNext) {
                val newIndex = currentState.currentIndex + 1
                val newItem = currentState.task?.items?.getOrNull(newIndex)
                // PENDING items: default to planned qty
                val newQty = newItem?.plannedQty?.toString() ?: ""

                currentState.copy(
                    currentIndex = newIndex,
                    pickedQtyInput = newQty,
                    errorMessage = null
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Register the current item's picked quantity (登録(F1) button).
     * Calls POST /api/picking/tasks/{wms_picking_item_result_id}/update
     *
     * On success:
     * - Refresh task from server to get updated status and counts
     * - Update counters (registeredCount, totalCount) from refreshed task
     * - Remove registered item from PENDING list
     * - Move to next PENDING item or show completion dialog
     */
    fun registerCurrentItem() {
        val currentState = _state.value
        val currentItem = currentState.currentItem ?: return
        val originalTask = currentState.originalTask ?: return

        // Validate input
        val pickedQty = currentState.pickedQtyInput.toDoubleOrNull()
        if (pickedQty == null || pickedQty < 0.0) {
            _state.update { it.copy(errorMessage = "数量を正しく入力してください") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }

            // Step 1: Update the item
            pickingTaskRepository.updatePickingItem(
                resultId = currentItem.id,
                pickedQty = pickedQty,
                pickedQtyType = currentItem.plannedQtyType.name
            )
                .onSuccess { updatedItem ->
                    // Step 2: Refresh the task from server to get updated status and counts
                    // This is critical for counter updates!
                    refreshTaskFromServer(originalTask.taskId)
                }
                .onFailure { error ->
                    val errorMessage = mapErrorMessage(error)
                    _state.update { it.copy(isUpdating = false, errorMessage = errorMessage) }
                }
        }
    }

    /**
     * Refresh the task from server and update state.
     * Called after registration to ensure counters reflect latest server state.
     */
    private suspend fun refreshTaskFromServer(taskId: Int) {
        val warehouseId = _state.value.warehouseId

        pickingTaskRepository.refreshTask(taskId, warehouseId)
            .onSuccess { refreshedTask ->
                // Update state with refreshed task
                val newPendingItems = refreshedTask.items.filter { item ->
                    item.status == biz.smt_life.android.core.domain.model.ItemStatus.PENDING
                }

                _state.update { currentState ->
                    currentState.copy(
                        originalTask = refreshedTask,  // Updated task with new counts
                        pendingItems = newPendingItems,
                        isUpdating = false
                    )
                }

                // Move to next PENDING item or show completion dialog
                moveToNextPendingOrComplete()
            }
            .onFailure { error ->
                val errorMessage = mapErrorMessage(error)
                _state.update { it.copy(isUpdating = false, errorMessage = errorMessage) }
            }
    }

    /**
     * After registration and refresh, move to next PENDING item in the list.
     * The pendingItems list has been updated from the server refresh.
     * If no more PENDING items, show completion dialog.
     */
    private fun moveToNextPendingOrComplete() {
        val currentState = _state.value

        if (currentState.pendingItems.isEmpty()) {
            // All PENDING items processed, show completion dialog
            _state.update { it.copy(showCompletionDialog = true) }
        } else {
            // Stay at current index (or adjust if out of bounds)
            val newIndex = if (currentState.currentIndex >= currentState.pendingItems.size) {
                currentState.pendingItems.size - 1
            } else {
                currentState.currentIndex
            }

            val newItem = currentState.pendingItems.getOrNull(newIndex)
            val newQty = newItem?.plannedQty?.toString() ?: ""

            _state.update {
                it.copy(
                    currentIndex = newIndex,
                    pickedQtyInput = newQty
                )
            }
        }
    }

    /**
     * Show the completion confirmation dialog manually.
     * Used when user taps a "Complete" button.
     */
    fun showCompletionDialog() {
        _state.update { it.copy(showCompletionDialog = true) }
    }

    /**
     * Dismiss the completion confirmation dialog.
     */
    fun dismissCompletionDialog() {
        _state.update { it.copy(showCompletionDialog = false) }
    }

    /**
     * Complete the picking task (確定 button in dialog).
     * Calls POST /api/picking/tasks/{id}/complete
     * On success, navigates back to the course list.
     *
     * @return true if completion started successfully
     */
    fun completeTask(onSuccess: () -> Unit) {
        val task = _state.value.task ?: return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true, errorMessage = null) }

            pickingTaskRepository.completeTask(task.taskId)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isCompleting = false,
                            showCompletionDialog = false
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    val errorMessage = mapErrorMessage(error)
                    _state.update {
                        it.copy(
                            isCompleting = false,
                            errorMessage = errorMessage
                        )
                    }
                }
        }
    }

    /**
     * Show image viewer dialog (画像 F5 button).
     */
    fun showImageDialog() {
        _state.update { it.copy(showImageDialog = true) }
    }

    /**
     * Dismiss image viewer dialog.
     */
    fun dismissImageDialog() {
        _state.update { it.copy(showImageDialog = false) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Map exceptions to user-friendly Japanese error messages.
     */
    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.Conflict -> "データが競合しています。再度お試しください。"
            is NetworkException.ValidationError -> error.message ?: "入力エラーです。"
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラーが発生しました。"
            is NetworkException.Unknown -> "エラーが発生しました。"
            else -> error.message ?: "予期しないエラーが発生しました。"
        }
    }
}
