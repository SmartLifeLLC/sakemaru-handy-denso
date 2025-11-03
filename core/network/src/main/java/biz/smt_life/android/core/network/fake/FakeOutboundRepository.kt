package biz.smt_life.android.core.network.fake

import biz.smt_life.android.core.domain.model.OutboundAddRequest
import biz.smt_life.android.core.domain.model.OutboundConfirmRequest
import biz.smt_life.android.core.domain.model.OutboundConfirmResponse
import biz.smt_life.android.core.domain.model.OutboundEntry
import biz.smt_life.android.core.domain.model.OutboundItem
import biz.smt_life.android.core.domain.model.OutboundSlip
import biz.smt_life.android.core.domain.model.PickingCourse
import biz.smt_life.android.core.domain.repository.OutboundRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeOutboundRepository @Inject constructor() : OutboundRepository {

    private val pickingCourses = MutableStateFlow(createInitialPickingCourses())
    private val pendingEntries = MutableStateFlow<List<OutboundEntry>>(emptyList())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object {
        private const val LATENCY_MS = 500L

        private fun createInitialPickingCourses() = listOf(
            PickingCourse(
                id = "COURSE_A",
                courseName = "コースA",
                isMyAssignment = true,
                done = 0,
                total = 3
            ),
            PickingCourse(
                id = "COURSE_B",
                courseName = "コースB",
                isMyAssignment = true,
                done = 1,
                total = 2
            ),
            PickingCourse(
                id = "COURSE_C",
                courseName = "コースC",
                isMyAssignment = false,
                done = 3,
                total = 3
            )
        )
    }

    override fun getPickingCourses(): Flow<Result<List<PickingCourse>>> {
        return pickingCourses.map { Result.success(it) }
    }

    override fun getSlipById(slipId: String): Flow<Result<OutboundSlip>> {
        return MutableStateFlow(
            Result.success(
                OutboundSlip(
                    id = slipId,
                    slipNumber = "SLIP-$slipId",
                    customerName = "サンプル得意先",
                    outboundDate = "2025-10-20",
                    done = 0,
                    total = 3,
                    status = "pending",
                    items = emptyList()
                )
            )
        )
    }

    override fun getPendingEntries(): Flow<Result<List<OutboundEntry>>> {
        return pendingEntries.map { Result.success(it) }
    }

    override suspend fun addEntry(request: OutboundAddRequest): Result<OutboundEntry> {
        delay(LATENCY_MS)

        val newEntry = OutboundEntry(
            id = UUID.randomUUID().toString(),
            slipId = request.slipId,
            itemId = request.itemId,
            itemName = "サンプル商品",
            qtyCase = request.qtyCase,
            qtyEach = request.qtyEach,
            course = request.course,
            status = "pending",
            createdAt = dateFormat.format(Date())
        )

        pendingEntries.value = pendingEntries.value + newEntry

        return Result.success(newEntry)
    }

    override suspend fun confirmEntries(request: OutboundConfirmRequest): Result<OutboundConfirmResponse> {
        delay(LATENCY_MS)

        val confirmedEntries = pendingEntries.value
            .filter { it.id in request.ids }
            .map { it.copy(status = "completed") }

        pendingEntries.value = pendingEntries.value.filter { it.id !in request.ids }

        return Result.success(
            OutboundConfirmResponse(
                updated = confirmedEntries
            )
        )
    }

    override suspend fun deleteEntry(id: String): Result<Unit> {
        delay(LATENCY_MS)

        pendingEntries.value = pendingEntries.value.filter { it.id != id }

        return Result.success(Unit)
    }
}
