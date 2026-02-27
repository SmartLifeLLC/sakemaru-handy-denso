package biz.smt_life.android.feature.outbound.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * ViewModel for Edit Picking screen (出庫検品 編集).
 *
 * Responsibilities:
 * - Load a specific item from a picking task
 * - Support ケース/バラ editing
 * - Save changes via updatePickingItem API
 */
@HiltViewModel
class EditPickingViewModel @Inject constructor(
    private val pickingTaskRepository: PickingTaskRepository,
    private val tokenManager: biz.smt_life.android.core.ui.TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(EditPickingState())
    val state: StateFlow<EditPickingState> = _state.asStateFlow()

    /**
     * Initialize with a specific item from a task.
     * Observes the task flow to get the item data.
     */
    fun initialize(itemResultId: Int, taskId: Int) {
        _state.update { it.copy(taskId = taskId, isLoading = true) }

        viewModelScope.launch {
            pickingTaskRepository.taskFlow(taskId).collect { task ->
                val item = task?.items?.find { it.id == itemResultId }
                if (item != null && _state.value.item?.id != item.id) {
                    // First time loading this item - set default values from pickedQty
                    val (cases, pieces) = computeDefaultQty(item)
                    _state.update {
                        it.copy(
                            item = item,
                            taskId = taskId,
                            inputCases = cases.toString(),
                            inputPieces = pieces.toString(),
                            isLoading = false
                        )
                    }
                } else if (item != null) {
                    // Item already loaded, just update the item reference
                    _state.update { it.copy(item = item, isLoading = false) }
                } else {
                    _state.update {
                        it.copy(isLoading = false, errorMessage = "商品が見つかりません")
                    }
                }
            }
        }
    }

    fun onInputCasesChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*$"))) {
            _state.update { it.copy(inputCases = value) }
        }
    }

    fun onInputPiecesChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*$"))) {
            _state.update { it.copy(inputPieces = value) }
        }
    }

    /**
     * Save changes by calling updatePickingItem API.
     * Converts ケース/バラ to total pieces before sending.
     */
    fun saveChanges(onSuccess: () -> Unit) {
        val currentState = _state.value
        val item = currentState.item ?: return
        val totalQty = currentState.totalPickedQty

        if (totalQty < 0) {
            _state.update { it.copy(errorMessage = "数量を正しく入力してください") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }

            pickingTaskRepository.updatePickingItem(
                resultId = item.id,
                pickedQty = totalQty,
                pickedQtyType = "PIECE"
            ).onSuccess {
                _state.update { it.copy(isSaving = false, saveSuccess = true) }
                onSuccess()
            }.onFailure { error ->
                _state.update { it.copy(isSaving = false, errorMessage = mapErrorMessage(error)) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Compute default ケース/バラ from item's pickedQty (current saved value).
     */
    private fun computeDefaultQty(item: PickingTaskItem): Pair<Int, Int> {
        val cf = item.capacityCase
        return if (cf != null && cf > 0) {
            Pair((item.pickedQty / cf).toInt(), (item.pickedQty.toInt() % cf))
        } else {
            Pair(0, item.pickedQty.toInt())
        }
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
