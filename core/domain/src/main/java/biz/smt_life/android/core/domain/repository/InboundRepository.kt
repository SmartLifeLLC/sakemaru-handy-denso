package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.InboundConfirmResponse
import biz.smt_life.android.core.domain.model.InboundEntryDto
import biz.smt_life.android.core.domain.model.InboundEntryRequest
import biz.smt_life.android.core.domain.model.ItemDto

interface InboundRepository {
    suspend fun searchItems(query: String): Result<List<ItemDto>>
    suspend fun getItemByBarcode(barcode: String): Result<ItemDto?>
    suspend fun addEntry(request: InboundEntryRequest): Result<InboundEntryDto>
    suspend fun getHistory(): Result<List<InboundEntryDto>>
    suspend fun confirmEntries(ids: List<String>, idempotencyKey: String): Result<InboundConfirmResponse>
}
