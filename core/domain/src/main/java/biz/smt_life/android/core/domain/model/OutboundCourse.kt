package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Picking course with assignment and completion state
 */
@Serializable
data class OutboundCourse(
    val id: String,
    val courseName: String,
    val isMyAssignment: Boolean,
    val items: List<OutboundCourseItem>,
    val isConfirmed: Boolean = false
) {
    val processedCount: Int
        get() = items.count { it.isRegistered }

    val totalCount: Int
        get() = items.size

    val isComplete: Boolean
        get() = processedCount == totalCount

    val currentItemIndex: Int
        get() = items.indexOfFirst { !it.isRegistered }.takeIf { it >= 0 } ?: 0
}
