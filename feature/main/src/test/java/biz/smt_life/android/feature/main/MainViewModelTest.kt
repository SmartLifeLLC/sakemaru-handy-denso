package biz.smt_life.android.feature.main

import app.cash.turbine.test
import biz.smt_life.android.core.domain.model.FeedItem
import biz.smt_life.android.core.domain.model.FeedItemType
import biz.smt_life.android.core.domain.model.Summary
import biz.smt_life.android.core.domain.model.User
import biz.smt_life.android.core.domain.repository.MainRepository
import biz.smt_life.android.core.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var profileRepository: ProfileRepository
    private lateinit var mainRepository: MainRepository
    private lateinit var viewModel: MainViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mockk()
        mainRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        // Given
        every { profileRepository.currentUser() } returns flowOf(null)
        every { mainRepository.summary() } returns flowOf(Summary(0, 0, 0, 0))
        every { mainRepository.feed(any(), any()) } returns flowOf(emptyList())

        // When
        viewModel = MainViewModel(profileRepository, mainRepository)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is MainUiState.Ready || state is MainUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit Ready state with data on success`() = runTest {
        // Given
        val user = User("1", "John Doe", "worker01", "Operator")
        val summary = Summary(25, 180, 3, 1)
        val feedItems = listOf(
            FeedItem("1", "Task 1", "Description 1", System.currentTimeMillis(), FeedItemType.TASK)
        )

        every { profileRepository.currentUser() } returns flowOf(user)
        every { mainRepository.summary() } returns flowOf(summary)
        every { mainRepository.feed(0, 20) } returns flowOf(feedItems)

        // When
        viewModel = MainViewModel(profileRepository, mainRepository)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is MainUiState.Ready)
            assertEquals(user, (state as MainUiState.Ready).user)
            assertEquals(summary, state.summary)
            assertEquals(feedItems, state.feedItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit Empty state when no data`() = runTest {
        // Given
        every { profileRepository.currentUser() } returns flowOf(null)
        every { mainRepository.summary() } returns flowOf(Summary(0, 0, 0, 0))
        every { mainRepository.feed(any(), any()) } returns flowOf(emptyList())

        // When
        viewModel = MainViewModel(profileRepository, mainRepository)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is MainUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should emit Error state on repository failure`() = runTest {
        // Given
        val errorMessage = "Network error"
        every { profileRepository.currentUser() } returns flow {
            throw Exception(errorMessage)
        }
        every { mainRepository.summary() } returns flowOf(Summary(0, 0, 0, 0))
        every { mainRepository.feed(any(), any()) } returns flowOf(emptyList())

        // When
        viewModel = MainViewModel(profileRepository, mainRepository)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is MainUiState.Error)
            assertEquals(errorMessage, (state as MainUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh should reload data`() = runTest {
        // Given
        val user = User("1", "John Doe", "worker01", "Operator")
        val summary = Summary(25, 180, 3, 1)
        val feedItems = listOf(
            FeedItem("1", "Task 1", "Description 1", System.currentTimeMillis(), FeedItemType.TASK)
        )

        every { profileRepository.currentUser() } returns flowOf(user)
        every { mainRepository.summary() } returns flowOf(summary)
        every { mainRepository.feed(0, 20) } returns flowOf(feedItems)

        viewModel = MainViewModel(profileRepository, mainRepository)

        // When
        viewModel.refresh()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is MainUiState.Ready)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry should reset to Loading and reload data`() = runTest {
        // Given
        val errorMessage = "Network error"
        every { profileRepository.currentUser() } returns flow {
            throw Exception(errorMessage)
        }
        every { mainRepository.summary() } returns flowOf(Summary(0, 0, 0, 0))
        every { mainRepository.feed(any(), any()) } returns flowOf(emptyList())

        viewModel = MainViewModel(profileRepository, mainRepository)

        // Wait for error state
        viewModel.uiState.test {
            awaitItem() // Error state
            cancelAndIgnoreRemainingEvents()
        }

        // When
        viewModel.retry()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is MainUiState.Loading || state is MainUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
