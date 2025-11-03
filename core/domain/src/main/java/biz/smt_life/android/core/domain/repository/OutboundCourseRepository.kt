package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.ConfirmCourseRequest
import biz.smt_life.android.core.domain.model.HistoryFilter
import biz.smt_life.android.core.domain.model.OutboundCourse
import biz.smt_life.android.core.domain.model.OutboundHistoryEntry
import biz.smt_life.android.core.domain.model.RegisterItemRequest
import kotlinx.coroutines.flow.Flow

/**
 * Repository for outbound course operations
 */
interface OutboundCourseRepository {
    /**
     * Get all courses, optionally filtered to current user's assignments
     */
    fun getCourses(myAssignmentsOnly: Boolean): Flow<Result<List<OutboundCourse>>>

    /**
     * Get a specific course by ID
     */
    fun getCourse(courseId: String): Flow<Result<OutboundCourse>>

    /**
     * Register an item (mark as picked with quantities)
     */
    suspend fun registerItem(request: RegisterItemRequest): Result<OutboundCourse>

    /**
     * Confirm entire course (mark as completed)
     */
    suspend fun confirmCourse(request: ConfirmCourseRequest): Result<OutboundCourse>

    /**
     * Unconfirm a course (revert to editable state)
     */
    suspend fun unconfirmCourse(courseId: String): Result<OutboundCourse>

    /**
     * Get history of outbound entries with filtering
     */
    fun history(
        filter: HistoryFilter,
        courseId: String? = null
    ): Flow<List<OutboundHistoryEntry>>
}
