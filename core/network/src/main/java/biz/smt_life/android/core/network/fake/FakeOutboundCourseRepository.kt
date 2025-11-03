package biz.smt_life.android.core.network.fake

import biz.smt_life.android.core.domain.model.ConfirmCourseRequest
import biz.smt_life.android.core.domain.model.HistoryFilter
import biz.smt_life.android.core.domain.model.HistoryStatus
import biz.smt_life.android.core.domain.model.OutboundCourse
import biz.smt_life.android.core.domain.model.OutboundCourseItem
import biz.smt_life.android.core.domain.model.OutboundHistoryEntry
import biz.smt_life.android.core.domain.model.RegisterItemRequest
import biz.smt_life.android.core.domain.repository.OutboundCourseRepository
import biz.smt_life.android.core.network.NetworkException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeOutboundCourseRepository @Inject constructor() : OutboundCourseRepository {

    private val mutex = Mutex()
    private val processedKeys = mutableSetOf<String>()

    // Mutable state for courses
    private val coursesState = MutableStateFlow(createInitialCourses())

    // Track update timestamps for history
    private val itemTimestamps = mutableMapOf<String, Instant>()

    companion object {
        private const val LATENCY_MS = 600L

        private fun createInitialCourses() = listOf(
            // Course A: 3 items, all unregistered
            OutboundCourse(
                id = "COURSE_A",
                courseName = "コースA",
                isMyAssignment = true,
                isConfirmed = false,
                items = listOf(
                    OutboundCourseItem(
                        id = "A1",
                        itemCode = "SAKE-001",
                        itemName = "純米大吟醸 1.8L",
                        customerName = "アミナ南越谷様",
                        capacity = "1800ML",
                        packSize = 6,
                        jan = "4901234567890",
                        area = "A-01",
                        location = "A-01-001",
                        orderQtyCase = 2,
                        orderQtyEach = 3,
                        outboundQtyCase = 2,
                        outboundQtyEach = 3,
                        isRegistered = false
                    ),
                    OutboundCourseItem(
                        id = "A2",
                        itemCode = "SAKE-002",
                        itemName = "本醸造 720ml",
                        customerName = "アミナ南越谷様",
                        capacity = "720ML",
                        packSize = 12,
                        jan = "4901234567891",
                        area = "A-02",
                        location = "A-02-005",
                        orderQtyCase = 1,
                        orderQtyEach = 6,
                        outboundQtyCase = 1,
                        outboundQtyEach = 6,
                        isRegistered = false
                    ),
                    OutboundCourseItem(
                        id = "A3",
                        itemCode = "SAKE-003",
                        itemName = "スパークリング清酒 500ml",
                        customerName = "アミナ南越谷様",
                        capacity = "500ML",
                        packSize = 24,
                        jan = "4901234567892",
                        area = "B-01",
                        location = "B-01-010",
                        orderQtyCase = 3,
                        orderQtyEach = 0,
                        outboundQtyCase = 3,
                        outboundQtyEach = 0,
                        isRegistered = false
                    )
                )
            ),
            // Course B: 2 items, 1 registered
            OutboundCourse(
                id = "COURSE_B",
                courseName = "コースB",
                isMyAssignment = true,
                isConfirmed = false,
                items = listOf(
                    OutboundCourseItem(
                        id = "B1",
                        itemCode = "SAKE-004",
                        itemName = "にごり酒 720ml",
                        customerName = "株式会社サケマル",
                        capacity = "720ML",
                        packSize = 12,
                        jan = "4901234567893",
                        area = "C-01",
                        location = "C-01-001",
                        orderQtyCase = 2,
                        orderQtyEach = 4,
                        outboundQtyCase = 2,
                        outboundQtyEach = 4,
                        isRegistered = true // Already registered
                    ),
                    OutboundCourseItem(
                        id = "B2",
                        itemCode = "SAKE-005",
                        itemName = "吟醸 1.8L",
                        customerName = "株式会社サケマル",
                        capacity = "1800ML",
                        packSize = 6,
                        jan = "4901234567894",
                        area = "C-02",
                        location = "C-02-003",
                        orderQtyCase = 1,
                        orderQtyEach = 0,
                        outboundQtyCase = 1,
                        outboundQtyEach = 0,
                        isRegistered = false
                    )
                )
            ),
            // Course C: 3 items, all done and confirmed
            OutboundCourse(
                id = "COURSE_C",
                courseName = "コースC",
                isMyAssignment = false,
                isConfirmed = true,
                items = listOf(
                    OutboundCourseItem(
                        id = "C1",
                        itemCode = "SAKE-006",
                        itemName = "特別純米 720ml",
                        customerName = "テスト商店",
                        capacity = "720ML",
                        packSize = 12,
                        jan = "4901234567895",
                        area = "D-01",
                        location = "D-01-001",
                        orderQtyCase = 1,
                        orderQtyEach = 6,
                        outboundQtyCase = 1,
                        outboundQtyEach = 6,
                        isRegistered = true
                    ),
                    OutboundCourseItem(
                        id = "C2",
                        itemCode = "SAKE-007",
                        itemName = "普通酒 1.8L",
                        customerName = "テスト商店",
                        capacity = "1800ML",
                        packSize = 6,
                        jan = "4901234567896",
                        area = "D-02",
                        location = "D-02-005",
                        orderQtyCase = 2,
                        orderQtyEach = 0,
                        outboundQtyCase = 2,
                        outboundQtyEach = 0,
                        isRegistered = true
                    ),
                    OutboundCourseItem(
                        id = "C3",
                        itemCode = "SAKE-008",
                        itemName = "生酒 720ml",
                        customerName = "テスト商店",
                        capacity = "720ML",
                        packSize = 12,
                        jan = "4901234567897",
                        area = "E-01",
                        location = "E-01-010",
                        orderQtyCase = 1,
                        orderQtyEach = 3,
                        outboundQtyCase = 1,
                        outboundQtyEach = 3,
                        isRegistered = true
                    )
                )
            )
        )
    }

    override fun getCourses(myAssignmentsOnly: Boolean): Flow<Result<List<OutboundCourse>>> {
        return coursesState.map { courses ->
            Result.success(
                if (myAssignmentsOnly) {
                    courses.filter { it.isMyAssignment }
                } else {
                    courses
                }
            )
        }
    }

    override fun getCourse(courseId: String): Flow<Result<OutboundCourse>> {
        return coursesState.map { courses ->
            val course = courses.find { it.id == courseId }
            if (course != null) {
                Result.success(course)
            } else {
                Result.failure(NetworkException.NotFound("Course not found: $courseId"))
            }
        }
    }

    override suspend fun registerItem(request: RegisterItemRequest): Result<OutboundCourse> {
        delay(LATENCY_MS)

        return mutex.withLock {
            // Idempotency check
            if (request.idempotencyKey in processedKeys) {
                return@withLock Result.failure(
                    NetworkException.Conflict("Duplicate idempotency key")
                )
            }

            val currentCourses = coursesState.value
            val courseIndex = currentCourses.indexOfFirst { it.id == request.courseId }

            if (courseIndex == -1) {
                return@withLock Result.failure(
                    NetworkException.NotFound("Course not found: ${request.courseId}")
                )
            }

            val course = currentCourses[courseIndex]
            val itemIndex = course.items.indexOfFirst { it.id == request.itemId }

            if (itemIndex == -1) {
                return@withLock Result.failure(
                    NetworkException.NotFound("Item not found: ${request.itemId}")
                )
            }

            // Validation
            val item = course.items[itemIndex]
            if (request.qtyCase < 0 || request.qtyEach < 0) {
                return@withLock Result.failure(
                    NetworkException.Validation(mapOf("qty" to "Quantity cannot be negative"))
                )
            }
            if (request.qtyCase > item.orderQtyCase || request.qtyEach > item.orderQtyEach) {
                return@withLock Result.failure(
                    NetworkException.Validation(mapOf("qty" to "Quantity exceeds order"))
                )
            }

            // Update item
            val updatedItem = item.copy(
                outboundQtyCase = request.qtyCase,
                outboundQtyEach = request.qtyEach,
                isRegistered = true
            )

            val updatedItems = course.items.toMutableList().apply {
                set(itemIndex, updatedItem)
            }

            val updatedCourse = course.copy(items = updatedItems)

            val updatedCourses = currentCourses.toMutableList().apply {
                set(courseIndex, updatedCourse)
            }

            coursesState.value = updatedCourses
            processedKeys.add(request.idempotencyKey)

            Result.success(updatedCourse)
        }
    }

    override suspend fun confirmCourse(request: ConfirmCourseRequest): Result<OutboundCourse> {
        delay(LATENCY_MS)

        return mutex.withLock {
            if (request.idempotencyKey in processedKeys) {
                return@withLock Result.failure(
                    NetworkException.Conflict("Duplicate idempotency key")
                )
            }

            val currentCourses = coursesState.value
            val courseIndex = currentCourses.indexOfFirst { it.id == request.courseId }

            if (courseIndex == -1) {
                return@withLock Result.failure(
                    NetworkException.NotFound("Course not found: ${request.courseId}")
                )
            }

            val course = currentCourses[courseIndex]

            // Validate all items are registered
            if (!course.isComplete) {
                return@withLock Result.failure(
                    NetworkException.Validation(
                        mapOf("course" to "Not all items are registered")
                    )
                )
            }

            val updatedCourse = course.copy(isConfirmed = true)

            val updatedCourses = currentCourses.toMutableList().apply {
                set(courseIndex, updatedCourse)
            }

            coursesState.value = updatedCourses
            processedKeys.add(request.idempotencyKey)

            Result.success(updatedCourse)
        }
    }

    override suspend fun unconfirmCourse(courseId: String): Result<OutboundCourse> {
        delay(LATENCY_MS)

        return mutex.withLock {
            val currentCourses = coursesState.value
            val courseIndex = currentCourses.indexOfFirst { it.id == courseId }

            if (courseIndex == -1) {
                return@withLock Result.failure(
                    NetworkException.NotFound("Course not found: $courseId")
                )
            }

            val course = currentCourses[courseIndex]
            val updatedCourse = course.copy(isConfirmed = false)

            val updatedCourses = currentCourses.toMutableList().apply {
                set(courseIndex, updatedCourse)
            }

            // Update timestamps for all items in the course
            val now = Instant.now()
            course.items.forEach { item ->
                val key = "${courseId}_${item.id}"
                itemTimestamps[key] = now
            }

            coursesState.value = updatedCourses

            Result.success(updatedCourse)
        }
    }

    override fun history(
        filter: HistoryFilter,
        courseId: String?
    ): Flow<List<OutboundHistoryEntry>> {
        return coursesState.map { courses ->
            val entries = mutableListOf<OutboundHistoryEntry>()
            val now = Instant.now()

            courses.forEach { course ->
                // Filter by courseId if specified
                if (courseId != null && course.id != courseId) return@forEach

                course.items.forEachIndexed { index, item ->
                    val status = when {
                        course.isConfirmed -> HistoryStatus.CONFIRMED
                        item.isRegistered -> HistoryStatus.REGISTERED_UNCONFIRMED
                        else -> HistoryStatus.UNREGISTERED
                    }

                    // Apply filter
                    val shouldInclude = when (filter.show) {
                        HistoryFilter.Show.UNCONFIRMED_ONLY ->
                            status == HistoryStatus.REGISTERED_UNCONFIRMED
                        HistoryFilter.Show.ALL -> true
                        HistoryFilter.Show.CONFIRMED_ONLY ->
                            status == HistoryStatus.CONFIRMED
                    }

                    if (shouldInclude) {
                        val key = "${course.id}_${item.id}"
                        // Generate a mock timestamp (within last 7 days) if not tracked
                        val timestamp = itemTimestamps.getOrPut(key) {
                            now.minus(Random.nextLong(0, 7), ChronoUnit.DAYS)
                                .minus(Random.nextLong(0, 24), ChronoUnit.HOURS)
                        }

                        entries.add(
                            OutboundHistoryEntry(
                                id = key,
                                courseId = course.id,
                                courseName = course.courseName,
                                itemId = item.id,
                                itemName = item.itemName,
                                customerName = item.customerName,
                                qtyCase = item.outboundQtyCase,
                                qtyEach = item.outboundQtyEach,
                                packSize = item.packSize,
                                jan = item.jan,
                                area = item.area,
                                location = item.location,
                                updatedAt = timestamp,
                                status = status
                            )
                        )
                    }
                }
            }

            // Sort by updatedAt descending (most recent first)
            entries.sortedByDescending { it.updatedAt }
        }
    }

    init {
        // Initialize timestamps for items that are already registered
        val now = Instant.now()
        coursesState.value.forEach { course ->
            course.items.forEach { item ->
                if (item.isRegistered || course.isConfirmed) {
                    val key = "${course.id}_${item.id}"
                    itemTimestamps[key] = now.minus(
                        Random.nextLong(1, 7),
                        ChronoUnit.DAYS
                    )
                }
            }
        }
    }
}
