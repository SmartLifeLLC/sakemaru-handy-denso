package biz.smt_life.android.feature.inbound.locationsearch

import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.ItemLocationSearchResult

data class LocationSearchState(
    val warehouseId: Int? = null,
    val warehouse: IncomingWarehouse? = null,
    val query: String = "",
    val results: List<ItemLocationSearchResult> = emptyList(),
    val selectedIndex: Int = 0,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedResult: ItemLocationSearchResult?
        get() = results.getOrNull(selectedIndex)
}
