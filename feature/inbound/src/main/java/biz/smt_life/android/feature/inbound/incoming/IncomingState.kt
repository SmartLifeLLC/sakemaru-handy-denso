package biz.smt_life.android.feature.inbound.incoming

import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.Location

/**
 * State for the entire Incoming feature flow.
 * Shared across all incoming screens.
 */
data class IncomingState(
    // Session data
    val pickerId: Int? = null,
    val pickerName: String? = null,

    // Warehouse selection
    val warehouses: List<IncomingWarehouse> = emptyList(),
    val selectedWarehouse: IncomingWarehouse? = null,
    val isLoadingWarehouses: Boolean = false,

    // Product list
    val products: List<IncomingProduct> = emptyList(),
    val searchQuery: String = "",
    val selectedProductIndex: Int = 0,
    val workingScheduleIds: Set<Int> = emptySet(),
    val isLoadingProducts: Boolean = false,
    val isSearching: Boolean = false,

    // Schedule list (for selected product)
    val selectedProduct: IncomingProduct? = null,
    val selectedScheduleIndex: Int = 0,

    // Input screen
    val selectedSchedule: IncomingSchedule? = null,
    val currentWorkItem: IncomingWorkItem? = null,
    val isFromHistory: Boolean = false,

    // Input form fields
    val inputQuantity: String = "",
    val inputExpirationDate: String = "",
    val inputLocationSearch: String = "",
    val inputLocationId: Int? = null,
    val inputLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val isLoadingLocations: Boolean = false,

    // History
    val historyItems: List<IncomingWorkItem> = emptyList(),
    val selectedHistoryIndex: Int = 0,
    val isLoadingHistory: Boolean = false,

    // General states
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * Current screen in the incoming flow.
 */
enum class IncomingScreen {
    WAREHOUSE_SELECTION,
    PRODUCT_LIST,
    SCHEDULE_LIST,
    INPUT,
    HISTORY
}
