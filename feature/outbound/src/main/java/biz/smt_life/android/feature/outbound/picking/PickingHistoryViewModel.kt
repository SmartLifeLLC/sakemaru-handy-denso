package biz.smt_life.android.feature.outbound.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
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
 * ViewModel for Picking History screen (2.5.3 - 出庫処理＞履歴).
 *
 * Responsibilities:
 * - Observe and display PICKING items from the task via repository flow
 * - Support editable mode (cancel, confirm-all) when PICKING items exist
 * - Support read-only mode when all items are COMPLETED/SHORTAGE
 * - Automatically refresh when task data changes in repository
 */
@HiltViewModel
class PickingHistoryViewModel @Inject constructor(
    private val pickingTaskRepository: PickingTaskRepository,
    private val tokenManager: biz.smt_life.android.core.ui.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(PickingHistoryState())
    val state: StateFlow<PickingHistoryState> = _state.asStateFlow()

    private var currentTaskId: Int = 0
    private var currentWarehouseId: Int = 0

    /**
     * Initialize the screen with a task ID.
     * Observes the task from repository's flow to ensure latest data is always shown.
     *
     * @param taskId The picking task ID to display history for
     */
    fun initialize(taskId: Int) {
        currentTaskId = taskId
        currentWarehouseId = tokenManager.getDefaultWarehouseId()

        viewModelScope.launch {
            // Observe the task flow from repository
            pickingTaskRepository.taskFlow(taskId).collect { task ->
                _state.update {
                    it.copy(
                        task = task,
                        isLoading = false,
                        errorMessage = if (task == null) "タスクが見つかりません" else null
                    )
                }
            }
        }
    }

    /**
     * Show confirmation dialog for deleting a single history item.
     */
    fun showDeleteDialog(item: PickingTaskItem) {
        _state.update { it.copy(itemToDelete = item) }
    }

    /**
     * Dismiss delete confirmation dialog.
     */
    fun dismissDeleteDialog() {
        _state.update { it.copy(itemToDelete = null) }
    }

    /**
     * Cancel a single history item (削除 F3).
     * This reverts the item's status from PICKING back to PENDING.
     *
     * Calls POST /api/picking/tasks/{wms_picking_item_result_id}/cancel
     * After successful cancellation, the repository refreshes the task,
     * and the item disappears from history (since it's now PENDING).
     */
    fun deleteHistoryItem(item: PickingTaskItem, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, errorMessage = null, itemToDelete = null) }

            pickingTaskRepository.cancelPickingItem(
                resultId = item.id,
                taskId = currentTaskId,
                warehouseId = currentWarehouseId
            )
                .onSuccess {
                    // After successful cancel, the repository has refreshed the task.
                    // The taskFlow will emit the updated task where this item's status is now PENDING.
                    // The historyItems computed property will automatically exclude it.
                    _state.update { it.copy(isDeleting = false) }
                    // Don't call onSuccess - stay on history screen to see remaining items
                }
                .onFailure { error ->
                    val errorMessage = mapErrorMessage(error)
                    _state.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = "キャンセルに失敗しました。$errorMessage"
                        )
                    }
                }
        }
    }

    /**
     * Show confirmation dialog for confirming all items.
     */
    fun showConfirmDialog() {
        _state.update { it.copy(showConfirmDialog = true) }
    }

    /**
     * Dismiss confirm dialog.
     */
    fun dismissConfirmDialog() {
        _state.update { it.copy(showConfirmDialog = false) }
    }

    /**
     * Confirm all items (確定 F4).
     * Calls POST /api/picking/tasks/{id}/complete
     *
     * On success:
     * - All PICKING items transition to COMPLETED/SHORTAGE
     * - Navigate back or show success message
     */
    fun confirmAll(onSuccess: () -> Unit) {
        val task = _state.value.task ?: return

        viewModelScope.launch {
            _state.update { it.copy(isConfirming = true, errorMessage = null, showConfirmDialog = false) }

            pickingTaskRepository.completeTask(task.taskId)
                .onSuccess {
                    _state.update { it.copy(isConfirming = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    val errorMessage = mapErrorMessage(error)
                    _state.update { it.copy(isConfirming = false, errorMessage = errorMessage) }
                }
        }
    }

    /**
     * Select an item in the history list (for F3 delete).
     */
    fun selectItem(item: PickingTaskItem) {
        _state.update { it.copy(selectedItem = item) }
    }

    /**
     * Clear selection.
     */
    fun clearSelection() {
        _state.update { it.copy(selectedItem = null) }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Map exceptions to user-friendly Japanese error messages.
     * For ValidationError, use the detailed message from the API response.
     */
    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.Conflict -> "データが競合しています。再度お試しください。"
            is NetworkException.ValidationError -> {
                // Use the message from the exception, which contains the API's error_message + errors
                error.message ?: "入力エラーです。"
            }
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラーが発生しました。"
            is NetworkException.Unknown -> "エラーが発生しました。"
            else -> error.message ?: "予期しないエラーが発生しました。"
        }
    }
}
