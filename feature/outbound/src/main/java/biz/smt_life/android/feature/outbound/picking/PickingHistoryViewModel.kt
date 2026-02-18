package biz.smt_life.android.feature.outbound.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * ViewModel for Picking History screen (P22 - 出庫処理＞履歴).
 *
 * Responsibilities:
 * - Observe and display non-PENDING items from the task via repository flow
 * - Support confirm-all (complete task) when PICKING items exist
 * - Automatically refresh when task data changes in repository
 */
@HiltViewModel
class PickingHistoryViewModel @Inject constructor(
    private val pickingTaskRepository: PickingTaskRepository,
    private val tokenManager: biz.smt_life.android.core.ui.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(PickingHistoryState())
    val state: StateFlow<PickingHistoryState> = _state.asStateFlow()

    /**
     * Initialize the screen with a task ID.
     * Observes the task from repository's flow to ensure latest data is always shown.
     */
    fun initialize(taskId: Int) {
        viewModelScope.launch {
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

    fun showConfirmDialog() {
        _state.update { it.copy(showConfirmDialog = true) }
    }

    fun dismissConfirmDialog() {
        _state.update { it.copy(showConfirmDialog = false) }
    }

    /**
     * Confirm all items (送信 F1).
     * Calls POST /api/picking/tasks/{id}/complete
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
                    _state.update { it.copy(isConfirming = false, errorMessage = mapErrorMessage(error)) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun mapErrorMessage(error: Throwable): String = when (error) {
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
