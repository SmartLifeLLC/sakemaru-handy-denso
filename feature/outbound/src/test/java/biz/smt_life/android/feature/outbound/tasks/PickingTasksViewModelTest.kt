package biz.smt_life.android.feature.outbound.tasks

import app.cash.turbine.test
import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType
import biz.smt_life.android.core.domain.repository.PickingTaskRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PickingTasksViewModel.
 * Tests state transitions and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PickingTasksViewModelTest {

    private lateinit var viewModel: PickingTasksViewModel
    private lateinit var repository: PickingTaskRepository
    private lateinit var tokenManager: TokenManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk()
        tokenManager = mockk()

        // Default mock values
        every { tokenManager.getDefaultWarehouseId() } returns TEST_WAREHOUSE_ID
        every { tokenManager.getPickerId() } returns TEST_PICKER_ID
    }

    @Test
    fun `initial state loads My tasks on init`() = runTest {
        // Given
        coEvery { repository.getMyAreaTasks(TEST_WAREHOUSE_ID, TEST_PICKER_ID) } returns
                Result.success(listOf(createTestTask()))

        // When
        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.tasksState is TaskListState.Success)
        assertEquals(1, (state.tasksState as TaskListState.Success).tasks.size)
    }

    @Test
    fun `empty list shows Empty state`() = runTest {
        // Given
        coEvery { repository.getMyAreaTasks(TEST_WAREHOUSE_ID, TEST_PICKER_ID) } returns
                Result.success(emptyList())

        // When
        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.tasksState is TaskListState.Empty)
    }

    @Test
    fun `network error shows Error state with message`() = runTest {
        // Given
        coEvery { repository.getMyAreaTasks(TEST_WAREHOUSE_ID, TEST_PICKER_ID) } returns
                Result.failure(NetworkException.NetworkError("Connection failed"))

        // When
        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.tasksState is TaskListState.Error)
        assertTrue((state.tasksState as TaskListState.Error).message.contains("ネットワーク"))
    }

    @Test
    fun `unauthorized error shows error with re-login message`() = runTest {
        // Given
        coEvery { repository.getMyAreaTasks(TEST_WAREHOUSE_ID, TEST_PICKER_ID) } returns
                Result.failure(NetworkException.Unauthorized())

        // When
        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.tasksState is TaskListState.Error)
        assertTrue((state.tasksState as TaskListState.Error).message.contains("再ログイン"))
    }

    @Test
    fun `missing warehouse ID shows error state`() = runTest {
        // Given
        every { tokenManager.getDefaultWarehouseId() } returns -1

        // When
        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("倉庫情報"))
    }

    @Test
    fun `missing picker ID shows error for My tasks`() = runTest {
        // Given
        every { tokenManager.getPickerId() } returns -1

        // When
        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.tasksState is TaskListState.Error)
        assertTrue((state.tasksState as TaskListState.Error).message.contains("ピッカー"))
    }

    @Test
    fun `refresh reloads current tab data`() = runTest {
        // Given
        coEvery { repository.getMyAreaTasks(TEST_WAREHOUSE_ID, TEST_PICKER_ID) } returns
                Result.success(emptyList()) andThen
                Result.success(listOf(createTestTask()))

        viewModel = PickingTasksViewModel(repository, tokenManager)
        advanceUntilIdle()

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertTrue(state.tasksState is TaskListState.Success)
    }

    @Test
    fun `state transitions from Loading to Success`() = runTest {
        // Given
        coEvery { repository.getMyAreaTasks(TEST_WAREHOUSE_ID, TEST_PICKER_ID) } returns
                Result.success(listOf(createTestTask()))

        // When & Then
        viewModel = PickingTasksViewModel(repository, tokenManager)

        viewModel.state.test {
            val initial = awaitItem()
            assertTrue(initial.tasksState is TaskListState.Loading)

            advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertTrue(loaded.tasksState is TaskListState.Success)
        }
    }

    // Helper functions

    private fun createTestTask(
        taskId: Int = 1,
        courseName: String = "Test Course",
        courseCode: String = "TC001",
        totalItems: Int = 10,
        completedItems: Int = 5
    ): PickingTask {
        val items = List(totalItems) { index ->
            PickingTaskItem(
                id = index + 1,
                itemId = 1000 + index,
                itemName = "Test Item $index",
                plannedQtyType = QuantityType.CASE,
                plannedQty = 2.0,
                pickedQty = if (index < completedItems) 2.0 else 0.0,
                slipNumber = 100,
                janCode = "1234567",
                volume = "volume",
                capacityCase = 10,
                packaging = "package",
                temperatureType = "temp",
                images = listOf("image1", "image2"),
                status = ItemStatus.COMPLETED,
                walkingOrder = 1
            )
        }

        return PickingTask(
            taskId = taskId,
            waveId = 5,
            courseName = courseName,
            courseCode = courseCode,
            pickingAreaName = "Area B",
            pickingAreaCode = "B",
            items = items,
//            totalItems = totalItems,
//            completedItems = completedItems,
//            progressText = "$completedItems/$totalItems"
        )
    }

    companion object {
        private const val TEST_WAREHOUSE_ID = 991
        private const val TEST_PICKER_ID = 2
    }
}
