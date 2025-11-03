package biz.smt_life.android.feature.outbound.history

import biz.smt_life.android.core.domain.model.HistoryFilter
import biz.smt_life.android.core.domain.model.OutboundHistoryEntry

/**
 * Contract for Outbound History screen
 */
object OutboundHistoryContract {
    data class State(
        val entries: List<OutboundHistoryEntry> = emptyList(),
        val filter: HistoryFilter = HistoryFilter.Default,
        val courseIdFilter: String? = null,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    sealed interface Event {
        data class FilterChanged(val filter: HistoryFilter) : Event
        data class RowTapped(val entry: OutboundHistoryEntry) : Event
        data class UnconfirmRequested(val entry: OutboundHistoryEntry) : Event
        data class UnconfirmConfirmed(val courseId: String) : Event
    }

    sealed interface Effect {
        data class NavigateToEntry(val courseId: String, val itemId: String) : Effect
        data class ShowToast(val message: String) : Effect
        data class ShowUnconfirmDialog(val entry: OutboundHistoryEntry) : Effect
    }
}
