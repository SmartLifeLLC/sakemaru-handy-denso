package biz.smt_life.android.feature.main

sealed interface MainAction {
    data object Retry : MainAction
    data object Refresh : MainAction
    data class ClickFeedItem(val itemId: String) : MainAction
}
