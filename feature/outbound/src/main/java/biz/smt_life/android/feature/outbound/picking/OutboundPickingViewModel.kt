package biz.smt_life.android.feature.outbound.picking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.QuantityType
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutboundPickingViewModel @Inject constructor(
    private val pickingTaskRepository: PickingTaskRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(OutboundPickingState())
    val state: StateFlow<OutboundPickingState> = _state.asStateFlow()

    fun initialize(task: PickingTask) {
        val warehouseId = tokenManager.getDefaultWarehouseId()
        val pendingItems = task.items.filter { it.status == ItemStatus.PENDING }

        if (pendingItems.isEmpty()) {
            _state.update {
                it.copy(
                    originalTask = task,
                    pendingItems = emptyList(),
                    currentIndex = 0,
                    isLoading = false,
                    warehouseId = warehouseId,
                    errorMessage = "登録可能な商品がありません"
                )
            }
            return
        }

        val currentItem = pendingItems.first()
        val (defaultCases, defaultPieces) = computeDefaultQty(currentItem)

        _state.update {
            it.copy(
                originalTask = task,
                pendingItems = pendingItems,
                currentIndex = 0,
                inputCases = defaultCases.toString(),
                inputPieces = defaultPieces.toString(),
                isLoading = false,
                warehouseId = warehouseId
            )
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

    // Legacy compatibility
    fun onPickedQtyChange(value: String) {
        // No-op: replaced by onInputCasesChange / onInputPiecesChange
    }

    fun moveToPrevItem() {
        _state.update { currentState ->
            if (currentState.canMovePrev) {
                val newIndex = currentState.currentIndex - 1
                val newItem = currentState.pendingItems.getOrNull(newIndex)
                val (cases, pieces) = computeDefaultQty(newItem)
                currentState.copy(
                    currentIndex = newIndex,
                    inputCases = cases.toString(),
                    inputPieces = pieces.toString(),
                    errorMessage = null
                )
            } else currentState
        }
    }

    fun moveToNextItem() {
        _state.update { currentState ->
            if (currentState.canMoveNext) {
                val newIndex = currentState.currentIndex + 1
                val newItem = currentState.pendingItems.getOrNull(newIndex)
                val (cases, pieces) = computeDefaultQty(newItem)
                currentState.copy(
                    currentIndex = newIndex,
                    inputCases = cases.toString(),
                    inputPieces = pieces.toString(),
                    errorMessage = null
                )
            } else currentState
        }
    }

    fun registerCurrentItem() {
        val currentState = _state.value
        val currentItem = currentState.currentItem ?: return
        val originalTask = currentState.originalTask ?: return
        val totalQty = currentState.totalPickedQty

        if (totalQty < 0) {
            _state.update { it.copy(errorMessage = "数量を正しく入力してください") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUpdating = true, errorMessage = null) }

            pickingTaskRepository.updatePickingItem(
                resultId = currentItem.id,
                pickedQty = totalQty,
                pickedQtyType = "PIECE"
            ).onSuccess {
                refreshTaskFromServer(originalTask.taskId)
            }.onFailure { error ->
                _state.update { it.copy(isUpdating = false, errorMessage = mapErrorMessage(error)) }
            }
        }
    }

    private suspend fun refreshTaskFromServer(taskId: Int) {
        val warehouseId = _state.value.warehouseId

        pickingTaskRepository.refreshTask(taskId, warehouseId)
            .onSuccess { refreshedTask ->
                val newPendingItems = refreshedTask.items.filter { it.status == ItemStatus.PENDING }

                _state.update { currentState ->
                    currentState.copy(
                        originalTask = refreshedTask,
                        pendingItems = newPendingItems,
                        isUpdating = false
                    )
                }
                moveToNextPendingOrComplete()
            }
            .onFailure { error ->
                _state.update { it.copy(isUpdating = false, errorMessage = mapErrorMessage(error)) }
            }
    }

    private fun moveToNextPendingOrComplete() {
        val currentState = _state.value

        if (currentState.pendingItems.isEmpty()) {
            _state.update { it.copy(showCompletionDialog = true) }
        } else {
            val newIndex = if (currentState.currentIndex >= currentState.pendingItems.size) {
                currentState.pendingItems.size - 1
            } else {
                currentState.currentIndex
            }
            val newItem = currentState.pendingItems.getOrNull(newIndex)
            val (cases, pieces) = computeDefaultQty(newItem)

            _state.update {
                it.copy(
                    currentIndex = newIndex,
                    inputCases = cases.toString(),
                    inputPieces = pieces.toString()
                )
            }
        }
    }

    fun showCompletionDialog() {
        _state.update { it.copy(showCompletionDialog = true) }
    }

    fun dismissCompletionDialog() {
        _state.update { it.copy(showCompletionDialog = false) }
    }

    fun completeTask(onSuccess: () -> Unit) {
        val taskId = _state.value.originalTask?.taskId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true, errorMessage = null) }

            pickingTaskRepository.completeTask(taskId)
                .onSuccess {
                    _state.update { it.copy(isCompleting = false, showCompletionDialog = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _state.update { it.copy(isCompleting = false, errorMessage = mapErrorMessage(error)) }
                }
        }
    }

    fun showImageDialog() {
        _state.update { it.copy(showImageDialog = true) }
    }

    fun dismissImageDialog() {
        _state.update { it.copy(showImageDialog = false) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Compute default ケース/バラ from item's plannedQty.
     */
    private fun computeDefaultQty(item: biz.smt_life.android.core.domain.model.PickingTaskItem?): Pair<Int, Int> {
        if (item == null) return Pair(0, 0)
        return when (item.plannedQtyType) {
            QuantityType.CASE -> Pair(item.plannedQty.toInt(), 0)
            QuantityType.PIECE -> {
                val cf = item.capacityCase
                if (cf != null && cf > 0) {
                    Pair((item.plannedQty / cf).toInt(), (item.plannedQty.toInt() % cf))
                } else {
                    Pair(0, item.plannedQty.toInt())
                }
            }
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
