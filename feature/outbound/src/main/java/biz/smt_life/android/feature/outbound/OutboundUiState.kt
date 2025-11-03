package biz.smt_life.android.feature.outbound

import biz.smt_life.android.core.domain.model.OutboundCourse
import biz.smt_life.android.core.domain.model.OutboundCourseItem

data class OutboundUiState(
    val courses: List<OutboundCourse> = emptyList(),
    val selectedCourseId: String? = null,
    val selectedCourse: OutboundCourse? = null,
    val currentItemIndex: Int = 0,
    val currentItem: OutboundCourseItem? = null,
    val myAssignmentsOnly: Boolean = true,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Editable quantities for current item
    val editQtyCase: String = "0",
    val editQtyEach: String = "0"
) {
    val canMovePrev: Boolean
        get() = currentItemIndex > 0

    val canMoveNext: Boolean
        get() = currentItem?.isRegistered == true &&
                currentItemIndex < (selectedCourse?.items?.size?.minus(1) ?: 0)

    val canRegister: Boolean
        get() = currentItem?.isRegistered != true && !isProcessing

    val canConfirm: Boolean
        get() = selectedCourse?.isComplete == true &&
                selectedCourse?.isConfirmed != true &&
                !isProcessing

    val canUnconfirm: Boolean
        get() = selectedCourse?.isConfirmed == true && !isProcessing

    val primaryButtonLabel: String
        get() = when {
            selectedCourse?.isConfirmed == true -> "確定取消"
            canConfirm -> "確定"
            currentItem?.isRegistered == true -> "登録済み"
            else -> "登録"
        }
}
