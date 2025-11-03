package biz.smt_life.android.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.repository.AuthRepository
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onStaffCodeChange(value: String) {
        _state.update { it.copy(staffCode = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val currentState = _state.value
        if (currentState.staffCode.isBlank() || currentState.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter staff code and password") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.login(currentState.staffCode, currentState.password)
                .onSuccess { authResult ->
                    tokenManager.saveToken(authResult.token)
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is NetworkException.Unauthorized -> "Invalid credentials"
                        is NetworkException.NetworkError -> "Network connection failed"
                        is NetworkException.ServerError -> "Server error. Please try again."
                        else -> error.message ?: "Unknown error"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
