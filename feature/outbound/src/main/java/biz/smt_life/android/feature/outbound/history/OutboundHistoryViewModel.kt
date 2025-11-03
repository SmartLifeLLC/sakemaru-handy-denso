package biz.smt_life.android.feature.outbound.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.HistoryFilter
import biz.smt_life.android.core.domain.model.HistoryStatus
import biz.smt_life.android.core.domain.repository.OutboundCourseRepository
import biz.smt_life.android.feature.outbound.history.OutboundHistoryContract.Effect
import biz.smt_life.android.feature.outbound.history.OutboundHistoryContract.Event
import biz.smt_life.android.feature.outbound.history.OutboundHistoryContract.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutboundHistoryViewModel @Inject constructor(
    private val repository: OutboundCourseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val courseIdArg: String? = savedStateHandle["courseId"]

    private val _filter = MutableStateFlow(HistoryFilter.Default)
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val historyEntries = _filter
        .flatMapLatest { filter ->
            repository.history(
                filter = filter,
                courseId = courseIdArg
            )
                .onStart {
                    _isLoading.value = true
                    _error.value = null
                }
                .catch { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "Unknown error"
                }
                .map { entries ->
                    _isLoading.value = false
                    entries
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val state: StateFlow<State> = combine(
        _filter,
        historyEntries,
        _isLoading,
        _error
    ) { filter, entries, isLoading, error ->
        State(
            entries = entries,
            filter = filter,
            courseIdFilter = courseIdArg,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State(courseIdFilter = courseIdArg)
    )

    fun onEvent(event: Event) {
        when (event) {
            is Event.FilterChanged -> {
                _filter.value = event.filter
            }

            is Event.RowTapped -> {
                if (event.entry.status == HistoryStatus.CONFIRMED) {
                    viewModelScope.launch {
                        _effects.send(Effect.ShowUnconfirmDialog(event.entry))
                    }
                } else {
                    viewModelScope.launch {
                        _effects.send(
                            Effect.NavigateToEntry(
                                courseId = event.entry.courseId,
                                itemId = event.entry.itemId
                            )
                        )
                    }
                }
            }

            is Event.UnconfirmRequested -> {
                viewModelScope.launch {
                    _effects.send(Effect.ShowUnconfirmDialog(event.entry))
                }
            }

            is Event.UnconfirmConfirmed -> {
                viewModelScope.launch {
                    _isLoading.value = true

                    repository.unconfirmCourse(event.courseId)
                        .onSuccess {
                            _effects.send(Effect.ShowToast("確定を取消しました"))
                            // History will auto-refresh via Flow
                        }
                        .onFailure { e ->
                            _isLoading.value = false
                            _error.value = e.message ?: "Unknown error"
                            _effects.send(
                                Effect.ShowToast("取消に失敗しました: ${e.message}")
                            )
                        }
                }
            }
        }
    }
}
