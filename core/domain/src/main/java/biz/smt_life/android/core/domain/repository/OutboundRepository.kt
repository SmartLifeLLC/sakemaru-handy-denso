package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.OutboundAddRequest
import biz.smt_life.android.core.domain.model.OutboundConfirmRequest
import biz.smt_life.android.core.domain.model.OutboundConfirmResponse
import biz.smt_life.android.core.domain.model.OutboundEntry
import biz.smt_life.android.core.domain.model.OutboundSlip
import biz.smt_life.android.core.domain.model.PickingCourse
import kotlinx.coroutines.flow.Flow

interface OutboundRepository {
    /**
     * Get all picking courses (both my assignments and all courses)
     */
    fun getPickingCourses(): Flow<Result<List<PickingCourse>>>

    /**
     * Get slip by identification ID
     */
    fun getSlipById(slipId: String): Flow<Result<OutboundSlip>>

    /**
     * Get pending outbound entries for history
     */
    fun getPendingEntries(): Flow<Result<List<OutboundEntry>>>

    /**
     * Add outbound entry
     */
    suspend fun addEntry(request: OutboundAddRequest): Result<OutboundEntry>

    /**
     * Confirm (finalize) outbound entries
     */
    suspend fun confirmEntries(request: OutboundConfirmRequest): Result<OutboundConfirmResponse>

    /**
     * Delete pending entry
     */
    suspend fun deleteEntry(id: String): Result<Unit>
}
