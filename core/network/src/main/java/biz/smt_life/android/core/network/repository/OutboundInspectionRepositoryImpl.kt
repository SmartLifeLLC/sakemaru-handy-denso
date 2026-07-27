package biz.smt_life.android.core.network.repository

import biz.smt_life.android.core.domain.model.OutboundInspectionPeriod
import biz.smt_life.android.core.domain.model.OutboundInspectionSnapshot
import biz.smt_life.android.core.domain.repository.OutboundInspectionRepository
import biz.smt_life.android.core.network.ErrorMapper
import biz.smt_life.android.core.network.NetworkException
import biz.smt_life.android.core.network.api.OutboundInspectionApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboundInspectionRepositoryImpl @Inject constructor(
    private val outboundInspectionApi: OutboundInspectionApi,
    private val errorMapper: ErrorMapper
) : OutboundInspectionRepository {

    override suspend fun getSnapshot(
        warehouseId: Int,
        period: OutboundInspectionPeriod,
        workingDate: String
    ): Result<OutboundInspectionSnapshot> {
        return try {
            val response = outboundInspectionApi.getSnapshot(
                warehouseId = warehouseId,
                period = period.apiValue,
                workingDate = workingDate
            )

            if (response.isSuccess && response.result?.data != null) {
                Result.success(response.result.data.toDomain())
            } else {
                Result.failure(
                    NetworkException.NotFound(
                        response.result?.errorMessage ?: "出庫検品データを取得できませんでした"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(errorMapper.mapException(e))
        }
    }
}
