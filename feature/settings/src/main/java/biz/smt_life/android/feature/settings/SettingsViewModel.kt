package biz.smt_life.android.feature.settings

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
        val hostUrl = currentState.hostUrl.trim()

        // Validation
        if (hostUrl.isBlank()) {
            _state.update { it.copy(errorMessage = "Host URL cannot be empty") }
            return
        }

        if (!hostUrl.startsWith("http://") && !hostUrl.startsWith("https://")) {
            _state.update { it.copy(errorMessage = "Host URL must start with http:// or https://") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                hostPreferences.setBaseUrl(hostUrl)
                _state.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Host URL saved successfully",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save: ${e.message}",
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
