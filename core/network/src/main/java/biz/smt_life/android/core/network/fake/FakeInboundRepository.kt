package biz.smt_life.android.core.network.fake

import biz.smt_life.android.core.domain.model.InboundConfirmResponse
import biz.smt_life.android.core.domain.model.InboundEntryDto
import biz.smt_life.android.core.domain.model.InboundEntryRequest
import biz.smt_life.android.core.domain.model.ItemDto
import biz.smt_life.android.core.domain.repository.InboundRepository
import biz.smt_life.android.core.network.NetworkException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeInboundRepository @Inject constructor() : InboundRepository {

    private val mutex = Mutex()
    private val entries = mutableListOf<InboundEntryDto>()
    private val processedKeys = mutableSetOf<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("Asia/Tokyo")
    }

    companion object {
        private const val LATENCY_MS = 600L
        private val MOCK_ITEMS = listOf(
            ItemDto("I001", "SAKE-001", "Premium Sake 720ml", 12, "4901234567890"),
            ItemDto("I002", "SAKE-002", "Junmai Daiginjo 1.8L", 6, "4901234567891"),
            ItemDto("I003", "SAKE-003", "Honjozo 720ml", 12, "4901234567892"),
            ItemDto("I004", "SAKE-004", "Sparkling Sake 500ml", 24, "4901234567893"),
            ItemDto("I005", "SAKE-005", "Nigori Sake 720ml", 12, "4901234567894"),
            ItemDto("I006", "SAKE-006", "Ginjo Sake 1.8L", 6, "4901234567895"),
            ItemDto("I007", "SAKE-007", "Tokubetsu Junmai 720ml", 12, "4901234567896"),
            ItemDto("I008", "SAKE-008", "Daiginjo 720ml", 6, "4901234567897"),
            ItemDto("I009", "SAKE-009", "Nama Sake 500ml", 24, "4901234567898"),
            ItemDto("I010", "SAKE-010", "Aged Sake 720ml", 12, "4901234567899")
        )
    }

    override suspend fun searchItems(query: String): Result<List<ItemDto>> {
        delay(LATENCY_MS)

        val filtered = MOCK_ITEMS.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.code.contains(query, ignoreCase = true)
        }

        return Result.success(filtered)
    }

    override suspend fun getItemByBarcode(barcode: String): Result<ItemDto?> {
        delay(LATENCY_MS)

        val item = MOCK_ITEMS.firstOrNull { it.jan == barcode }
        return Result.success(item)
    }

    override suspend fun addEntry(request: InboundEntryRequest): Result<InboundEntryDto> {
        delay(LATENCY_MS)

        return mutex.withLock {
            // Idempotency check
            if (request.idempotencyKey in processedKeys) {
                val existing = entries.firstOrNull {
                    it.itemId == request.itemId &&
                    it.qtyCase == request.qtyCase &&
                    it.qtyEach == request.qtyEach
                }
                return@withLock if (existing != null) {
                    Result.success(existing)
                } else {
                    Result.failure(NetworkException.Conflict("Duplicate idempotency key"))
                }
            }

            // Validation
            val errors = mutableMapOf<String, String>()
            if (request.qtyCase < 0 || request.qtyEach < 0) {
                errors["qty"] = "Quantity must be non-negative"
            }
            if (request.qtyCase == 0 && request.qtyEach == 0) {
                errors["qty"] = "At least one quantity must be greater than 0"
            }
            if (request.labelCount !in 0..99) {
                errors["labelCount"] = "Label count must be between 0 and 99"
            }

            if (errors.isNotEmpty()) {
                return@withLock Result.failure(NetworkException.Validation(errors))
            }

            val item = MOCK_ITEMS.firstOrNull { it.id == request.itemId }
                ?: return@withLock Result.failure(NetworkException.NotFound("Item not found"))

            val entry = InboundEntryDto(
                id = "E${(entries.size + 1).toString().padStart(3, '0')}",
                itemId = request.itemId,
                itemName = item.name,
                qtyCase = request.qtyCase,
                qtyEach = request.qtyEach,
                expDate = request.expDate,
                labelCount = request.labelCount,
                status = "pending",
                createdAt = dateFormat.format(Date())
            )

            entries.add(entry)
            processedKeys.add(request.idempotencyKey)
            Result.success(entry)
        }
    }

    override suspend fun getHistory(): Result<List<InboundEntryDto>> {
        delay(LATENCY_MS)
        return mutex.withLock {
            Result.success(entries.toList().reversed())
        }
    }

    override suspend fun confirmEntries(
        ids: List<String>,
        idempotencyKey: String
    ): Result<InboundConfirmResponse> {
        delay(LATENCY_MS)

        return mutex.withLock {
            if (idempotencyKey in processedKeys) {
                return@withLock Result.failure(NetworkException.Conflict("Duplicate idempotency key"))
            }

            val updated = mutableListOf<InboundEntryDto>()
            ids.forEach { id ->
                val index = entries.indexOfFirst { it.id == id }
                if (index != -1) {
                    val confirmed = entries[index].copy(status = "confirmed")
                    entries[index] = confirmed
                    updated.add(confirmed)
                }
            }

            processedKeys.add(idempotencyKey)
            Result.success(InboundConfirmResponse(updated))
        }
    }
}
