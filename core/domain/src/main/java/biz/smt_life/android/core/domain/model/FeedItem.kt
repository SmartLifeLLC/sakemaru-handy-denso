package biz.smt_life.android.core.domain.model

data class FeedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long,
    val type: FeedItemType = FeedItemType.TASK
)

enum class FeedItemType {
    TASK,
    NOTIFICATION,
    ALERT
}
