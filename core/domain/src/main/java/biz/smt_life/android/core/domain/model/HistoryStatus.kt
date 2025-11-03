package biz.smt_life.android.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Status of an outbound history entry
 */
@Serializable
enum class HistoryStatus {
    UNREGISTERED,           // 未登録
    REGISTERED_UNCONFIRMED, // 登録済み未確定
    CONFIRMED               // 確定済み
}
