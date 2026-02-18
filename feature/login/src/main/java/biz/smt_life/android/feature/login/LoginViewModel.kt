package biz.smt_life.android.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.ui.HostPreferences
import biz.smt_life.android.core.ui.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Login screen per Spec 2.1.0.
 * Handles authentication with staff code and password.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val hostPreferences: HostPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    val hostUrl = hostPreferences.baseUrl

    fun onStaffCodeChange(value: String) {
        _state.update { it.copy(staffCode = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, errorMessage = null) }
    }

    fun login() {
        val currentState = _state.value
        if (currentState.staffCode.isBlank() || currentState.password.isBlank()) {
            _state.update { it.copy(errorMessage = "スタッフコードとパスワードを入力してください") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.login(currentState.staffCode, currentState.password)
                .onSuccess { authResult ->
                    tokenManager.saveAuth(
                        token = authResult.token,
                        pickerId = authResult.pickerId,
                        pickerCode = authResult.pickerCode,
                        pickerName = authResult.pickerName,
                        defaultWarehouseId = authResult.defaultWarehouseId
                    )
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is NetworkException.Unauthorized -> "認証情報が無効です"
                        is NetworkException.NetworkError -> "ネットワーク接続に失敗しました"
                        is NetworkException.ServerError -> "サーバーエラーが発生しました"
                        is NetworkException.ValidationError -> error.msg
                        else -> error.message ?: "不明なエラー"
                    }
                    _state.update { it.copy(isLoading = false, errorMessage = message) }
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
