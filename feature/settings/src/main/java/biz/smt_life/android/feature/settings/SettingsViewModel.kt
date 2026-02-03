package biz.smt_life.android.feature.settings

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.ui.HostPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 * Manages host URL configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val hostPreferences: HostPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadCurrentHost()
    }

    private fun loadCurrentHost() {
        viewModelScope.launch {
            hostPreferences.baseUrl.collect { url ->
                _state.update { it.copy(hostUrl = url) }
            }
        }
    }

    fun onHostUrlChange(value: String) {
        _state.update { it.copy(hostUrl = value, errorMessage = null, successMessage = null) }
    }

    fun saveHostUrl() {
        val currentState = _state.value
        var hostUrl = currentState.hostUrl.trim()

        // Validation
        if (hostUrl.isBlank()) {
            _state.update { it.copy(errorMessage = "ホストURLを入力してください") }
            return
        }

        if (!Patterns.WEB_URL.matcher(hostUrl).matches()) {
            _state.update { it.copy(errorMessage = "無効なURL形式です") }
            return
        }

        if (!hostUrl.endsWith("/")) {
            hostUrl += "/"
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                hostPreferences.setBaseUrl(hostUrl)
                _state.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "ホストURLを保存しました",
                        errorMessage = null,
                        hostUrl = hostUrl
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "保存に失敗しました: ${e.message}",
                        successMessage = null
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
