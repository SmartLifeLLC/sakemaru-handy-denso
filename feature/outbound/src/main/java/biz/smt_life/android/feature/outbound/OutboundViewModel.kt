package biz.smt_life.android.feature.outbound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import biz.smt_life.android.core.domain.model.ConfirmCourseRequest
import biz.smt_life.android.core.domain.model.RegisterItemRequest
import biz.smt_life.android.core.domain.repository.OutboundCourseRepository
import biz.smt_life.android.core.network.IdempotencyKeyGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OutboundViewModel @Inject constructor(
    private val repository: OutboundCourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutboundUiState())
    val uiState: StateFlow<OutboundUiState> = _uiState.asStateFlow()

    init {
        loadCourses()
    }

    fun setMyAssignmentsFilter(myOnly: Boolean) {
        _uiState.update { it.copy(myAssignmentsOnly = myOnly) }
        loadCourses()
    }

    fun selectCourse(courseId: String) {
        _uiState.update { it.copy(selectedCourseId = courseId, isLoading = true) }
        viewModelScope.launch {
            repository.getCourse(courseId).collect { result ->
                result.onSuccess { course ->
                    val startIndex = course.currentItemIndex
                    val item = course.items.getOrNull(startIndex)
                    _uiState.update {
                        it.copy(
                            selectedCourse = course,
                            currentItemIndex = startIndex,
                            currentItem = item,
                            editQtyCase = item?.outboundQtyCase?.toString() ?: "0",
                            editQtyEach = item?.outboundQtyEach?.toString() ?: "0",
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load course"
                        )
                    }
                }
            }
        }
    }

    fun moveToPrevItem() {
        val state = _uiState.value
        if (!state.canMovePrev) return

        val newIndex = state.currentItemIndex - 1
        val item = state.selectedCourse?.items?.getOrNull(newIndex)
        _uiState.update {
            it.copy(
                currentItemIndex = newIndex,
                currentItem = item,
                editQtyCase = item?.outboundQtyCase?.toString() ?: "0",
                editQtyEach = item?.outboundQtyEach?.toString() ?: "0"
            )
        }
    }

    fun moveToNextItem() {
        val state = _uiState.value
        if (!state.canMoveNext) {
            _uiState.update { it.copy(errorMessage = "登録してください") }
            return
        }

        val newIndex = state.currentItemIndex + 1
        val item = state.selectedCourse?.items?.getOrNull(newIndex)
        _uiState.update {
            it.copy(
                currentItemIndex = newIndex,
                currentItem = item,
                editQtyCase = item?.outboundQtyCase?.toString() ?: "0",
                editQtyEach = item?.outboundQtyEach?.toString() ?: "0"
            )
        }
    }

    fun onQtyCaseChange(value: String) {
        _uiState.update { it.copy(editQtyCase = value) }
    }

    fun onQtyEachChange(value: String) {
        _uiState.update { it.copy(editQtyEach = value) }
    }

    fun registerItem() {
        val state = _uiState.value
        val courseId = state.selectedCourse?.id ?: return
        val itemId = state.currentItem?.id ?: return
        val qtyCase = state.editQtyCase.toIntOrNull() ?: 0
        val qtyEach = state.editQtyEach.toIntOrNull() ?: 0

        val request = RegisterItemRequest(
            courseId = courseId,
            itemId = itemId,
            qtyCase = qtyCase,
            qtyEach = qtyEach,
            idempotencyKey = IdempotencyKeyGenerator.generate()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            repository.registerItem(request)
                .onSuccess { updatedCourse ->
                    _uiState.update {
                        it.copy(
                            selectedCourse = updatedCourse,
                            currentItem = updatedCourse.items[state.currentItemIndex],
                            isProcessing = false,
                            successMessage = "登録しました"
                        )
                    }
                    // Reload courses to update list
                    loadCourses()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.message ?: "Failed to register"
                        )
                    }
                }
        }
    }

    fun confirmCourse() {
        val courseId = _uiState.value.selectedCourse?.id ?: return

        val request = ConfirmCourseRequest(
            courseId = courseId,
            idempotencyKey = IdempotencyKeyGenerator.generate()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            repository.confirmCourse(request)
                .onSuccess { updatedCourse ->
                    _uiState.update {
                        it.copy(
                            selectedCourse = updatedCourse,
                            isProcessing = false,
                            successMessage = "確定しました"
                        )
                    }
                    loadCourses()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.message ?: "Failed to confirm"
                        )
                    }
                }
        }
    }

    fun unconfirmCourse() {
        val courseId = _uiState.value.selectedCourse?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            repository.unconfirmCourse(courseId)
                .onSuccess { updatedCourse ->
                    _uiState.update {
                        it.copy(
                            selectedCourse = updatedCourse,
                            isProcessing = false,
                            successMessage = "確定を取り消しました"
                        )
                    }
                    loadCourses()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = error.message ?: "Failed to unconfirm"
                        )
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun loadCourses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getCourses(_uiState.value.myAssignmentsOnly).collect { result ->
                result.onSuccess { courses ->
                    _uiState.update { it.copy(courses = courses, isLoading = false) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load courses"
                        )
                    }
                }
            }
        }
    }
}
