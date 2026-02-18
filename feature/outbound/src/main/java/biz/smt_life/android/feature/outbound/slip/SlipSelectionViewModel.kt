package biz.smt_life.android.feature.outbound.slip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.repository.OutboundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlipSelectionViewModel @Inject constructor(
    private val repository: OutboundRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SlipSelectionState())
    val state: StateFlow<SlipSelectionState> = _state.asStateFlow()

    init {
        loadSlips()
    }

    private fun loadSlips() {
        viewModelScope.launch {
            repository.getSlips().collect { result ->
                result.onSuccess { slips ->
                    _state.update { it.copy(isLoading = false, slips = slips) }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "エラーが発生しました"
                        )
                    }
                }
            }
        }
    }

    /**
     * 伝票を選択する。同じIDをタップした場合は選択解除。
     */
    fun selectSlip(slipId: String) {
        _state.update { currentState ->
            if (currentState.selectedSlipId == slipId) {
                currentState.copy(selectedSlipId = null)
            } else {
                currentState.copy(selectedSlipId = slipId)
            }
        }
    }

    /**
     * 確定ボタン押下。選択されたslipId（またはNO_SLIP_ID）をコールバックに渡す。
     */
    fun confirmSelection(onConfirm: (String?) -> Unit) {
        SoundUtils.playBeep()
        onConfirm(_state.value.selectedSlipId)
    }
}
