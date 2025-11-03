package biz.smt_life.android.core.domain.model

data class PendingCounts(
    val inbound: Int = 0,
    val outbound: Int = 0,
    val inventory: Int = 0
)
