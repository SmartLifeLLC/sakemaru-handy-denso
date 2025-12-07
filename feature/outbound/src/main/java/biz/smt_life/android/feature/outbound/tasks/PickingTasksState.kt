package biz.smt_life.android.feature.outbound.tasks

import biz.smt_life.android.core.domain.model.PickingTask

/**
 * UI state for Picking Tasks screen per spec 2.5.1 出庫処理.
 * Manages state for two tabs: My Area and All Courses.
 */
data class PickingTasksState(
    val activeTab: PickingTab = PickingTab.MY_AREA,
    val myAreaState: TaskListState = TaskListState.Loading,
    val allCoursesState: TaskListState = TaskListState.Loading,
    val warehouseId: Int? = null,
    val pickerId: Int? = null,
    val errorMessage: String? = null,
    val selectedTask: PickingTask? = null // Task selected for navigation to picking screen
) {
    /**
     * Get the state for the currently active tab.
     */
    val currentTabState: TaskListState
        get() = when (activeTab) {
            PickingTab.MY_AREA -> myAreaState
            PickingTab.ALL_COURSES -> allCoursesState
        }
}

/**
 * Tab selection for Picking Tasks screen.
 */
enum class PickingTab {
    MY_AREA,      // 担当エリア - Tasks assigned to current picker
    ALL_COURSES   // 全コース - All tasks in warehouse
}

/**
 * State for each tab's task list.
 */
sealed interface TaskListState {
    data object Loading : TaskListState
    data object Empty : TaskListState
    data class Success(val tasks: List<PickingTask>) : TaskListState
    data class Error(val message: String) : TaskListState
}
