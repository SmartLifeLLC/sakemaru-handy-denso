package biz.smt_life.android.core.domain.repository

import biz.smt_life.android.core.domain.model.OutboundInspectionPeriod
import biz.smt_life.android.core.domain.model.OutboundInspectionSnapshot

interface OutboundInspectionRepository {
    suspend fun getSnapshot(
        warehouseId: Int,
        period: OutboundInspectionPeriod,
        workingDate: String
    ): Result<OutboundInspectionSnapshot>
}
