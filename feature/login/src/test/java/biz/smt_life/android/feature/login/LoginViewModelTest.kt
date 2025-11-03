package biz.smt_life.android.feature.login

import biz.smt_life.android.core.domain.model.AuthResult
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.TokenManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        tokenManager = mockk(relaxed = true)
        viewModel = LoginViewModel(authRepository, tokenManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success saves token and updates state`() = runTest {
        val authResult = AuthResult("token", "S001", "Worker", "operator")
        coEvery { authRepository.login("worker01", "1234") } returns Result.success(authResult)

        viewModel.onStaffCodeChange("worker01")
        viewModel.onPasswordChange("1234")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { tokenManager.saveToken("token") }
        assertTrue(viewModel.state.value.isSuccess)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `login failure shows error message`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(
            NetworkException.Unauthorized("Invalid credentials")
        )

        viewModel.onStaffCodeChange("wrong")
        viewModel.onPasswordChange("wrong")
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isSuccess)
    }

    @Test
    fun `login with blank fields shows validation error`() = runTest {
        viewModel.login()

        assertEquals("Please enter staff code and password", viewModel.state.value.errorMessage)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }
}
