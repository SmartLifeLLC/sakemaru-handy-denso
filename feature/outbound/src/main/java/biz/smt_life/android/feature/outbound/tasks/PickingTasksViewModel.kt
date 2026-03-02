package biz.smt_life.android.feature.outbound.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * ViewModel for Picking Tasks screen per spec 2.5.1 出庫処理.
 * Shows only "My tasks" (私の担当) - tasks assigned to current picker.
 *
 * Responsibilities:
 * - Load tasks from repository (picker-filtered)
 * - Pull-to-refresh
 * - Cache latest successful data in memory
 * - Handle 401/403 by exposing session error
 */
@HiltViewModel
class PickingTasksViewModel @Inject constructor(
    private val repository: PickingTaskRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(PickingTasksState())
    val state: StateFlow<PickingTasksState> = _state.asStateFlow()

    init {
        initializeSessionData()
        observeRepositoryTasks()
    }

    /**
     * Observe repository's tasksFlow to keep UI state synchronized.
     * This ensures that when tasks are updated anywhere (data input, history),
     * the course list counters are automatically updated.
     */
    private fun observeRepositoryTasks() {
        viewModelScope.launch {
            repository.tasksFlow.collect { tasks ->
                // Update task state if we've already loaded tasks
                _state.update { currentState ->
                    if (currentState.tasksState is TaskListState.Success ||
                        currentState.tasksState is TaskListState.Empty
                    ) {
                        // Filter out confirmed (fully processed) tasks
                        val unconfirmedTasks = tasks.filter { !it.isFullyProcessed }
                        val newState = if (unconfirmedTasks.isEmpty()) {
                            TaskListState.Empty
                        } else {
                            TaskListState.Success(unconfirmedTasks)
                        }
                        currentState.copy(tasksState = newState)
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    /**
     * Initialize warehouse ID and picker ID from session.
     * If missing, set error state.
     */
    private fun initializeSessionData() {
        val warehouseId = tokenManager.getDefaultWarehouseId()
        val pickerId = tokenManager.getPickerId()

        if (warehouseId <= 0) {
            _state.update {
                it.copy(
                    errorMessage = "倉庫情報が見つかりません。再ログインしてください。",
                    tasksState = TaskListState.Error("倉庫情報が見つかりません")
                )
            }
            return
        }

        _state.update { it.copy(warehouseId = warehouseId, pickerId = pickerId) }

        // Load My tasks
        loadMyAreaTasks()
    }

    /**
     * Refresh tasks.
     */
    fun refresh() {
        loadMyAreaTasks()
    }

    /**
     * Load tasks for "My tasks" (filtered by picker).
     */
    fun loadMyAreaTasks() {
        val warehouseId = _state.value.warehouseId ?: return
        val pickerId = _state.value.pickerId

        if (pickerId == null || pickerId <= 0) {
            _state.update {
                it.copy(tasksState = TaskListState.Error("ピッカー情報が見つかりません"))
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(tasksState = TaskListState.Loading) }

            repository.getMyAreaTasks(warehouseId, pickerId)
                .onSuccess { tasks ->
                    // Filter out confirmed (fully processed) tasks
                    val unconfirmedTasks = tasks.filter { !it.isFullyProcessed }
                    val newState = if (unconfirmedTasks.isEmpty()) {
                        TaskListState.Empty
                    } else {
                        TaskListState.Success(unconfirmedTasks)
                    }
                    _state.update { it.copy(tasksState = newState) }
                }
                .onFailure { error ->
                    val message = mapErrorMessage(error)
                    _state.update { it.copy(tasksState = TaskListState.Error(message)) }
                }
        }
    }

    /**
     * Handle task selection with status-based navigation (per spec 2.5.1).
     * Navigation logic:
     * - If pendingCount > 0: navigate to Data Input (2.5.2)
     * - If pendingCount == 0 && pickingCount > 0: navigate to History (2.5.3) editable
     * - If all items COMPLETED/SHORTAGE: navigate to History (2.5.3) read-only
     *
     * Calls POST /api/picking/tasks/{id}/start if not already started.
     *
     * @param task The task to select
     * @param onNavigateToDataInput Callback to navigate to Data Input screen
     * @param onNavigateToHistory Callback to navigate to History screen
     * @param onError Callback with error message
     */
    fun selectTask(
        task: biz.smt_life.android.core.domain.model.PickingTask,
        onNavigateToDataInput: (biz.smt_life.android.core.domain.model.PickingTask) -> Unit,
        onNavigateToHistory: (biz.smt_life.android.core.domain.model.PickingTask) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Store the selected task for the next screen
            _state.update { it.copy(selectedTask = task) }

            // Start the task if not already started (server will be idempotent)
            repository.startTask(task.taskId)
                .onSuccess {
                    // Determine navigation based on task status
                    when {
                        task.hasUnregisteredItems -> {
                            // PENDING items exist → navigate to Data Input
                            onNavigateToDataInput(task)
                        }
                        task.hasPickingItems -> {
                            // Only PICKING items exist → navigate to History (editable)
                            onNavigateToHistory(task)
                        }
                        task.isFullyProcessed -> {
                            // All COMPLETED/SHORTAGE → navigate to History (read-only)
                            onNavigateToHistory(task)
                        }
                        else -> {
                            // Fallback: navigate to Data Input
                            onNavigateToDataInput(task)
                        }
                    }
                }
                .onFailure { error ->
                    val message = mapErrorMessage(error)
                    onError(message)
                }
        }
    }

    /**
     * Legacy method for backwards compatibility.
     * Use selectTask() for status-based navigation.
     */
    @Deprecated("Use selectTask() instead", ReplaceWith("selectTask(task, onSuccess, onSuccess, onError)"))
    fun startTask(
        task: biz.smt_life.android.core.domain.model.PickingTask,
        onSuccess: (biz.smt_life.android.core.domain.model.PickingTask) -> Unit,
        onError: (String) -> Unit
    ) {
        selectTask(task, onSuccess, onSuccess, onError)
    }

    /**
     * Clear the selected task.
     */
    fun clearSelectedTask() {
        _state.update { it.copy(selectedTask = null) }
    }

    /**
     * Map exception to user-friendly Japanese error message.
     */
    private fun mapErrorMessage(error: Throwable): String {
        return when (error) {
            is NetworkException.Unauthorized -> "認証エラー。再ログインしてください。"
            is NetworkException.Forbidden -> "アクセス権限がありません。"
            is NetworkException.NotFound -> "データが見つかりません。"
            is NetworkException.NetworkError -> "ネットワークエラー。接続を確認してください。"
            is NetworkException.ServerError -> "サーバーエラー。しばらくしてから再度お試しください。"
            else -> error.message ?: "エラーが発生しました。"
        }
    }
}
