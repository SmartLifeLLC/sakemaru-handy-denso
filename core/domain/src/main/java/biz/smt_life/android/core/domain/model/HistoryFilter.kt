package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Filter for history list
 */
@Serializable
data class HistoryFilter(val show: Show) {
    @Serializable
    enum class Show {
        UNCONFIRMED_ONLY, // 未確定のみ
        ALL,              // 全て
        CONFIRMED_ONLY    // 確定のみ
    }

    companion object {
        val Default = HistoryFilter(Show.UNCONFIRMED_ONLY)
    }
}
