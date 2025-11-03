package biz.smt_life.android.feature.inbound

import androidx.compose.ui.text.input.TextFieldValue
import biz.smt_life.android.core.domain.model.InboundEntryDto
import biz.smt_life.android.core.domain.model.ItemDto

data class InboundState(
    val searchQuery: String = "",
    val barcodeInput: String = "",
    val searchResults: List<ItemDto> = emptyList(),
    val selectedItem: ItemDto? = null,

    val qtyCase: String = "0",
    val qtyEach: String = "0",
    val expirationDate: TextFieldValue = TextFieldValue(""),
    val labelCount: String = "0",

    val history: List<InboundEntryDto> = emptyList(),
    val selectedEntries: Set<String> = emptySet(),
    val showHistory: Boolean = false,

    val isSearching: Boolean = false,
    val isAdding: Boolean = false,
    val isLoadingHistory: Boolean = false,
    val isConfirming: Boolean = false,

    val errorMessage: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val successMessage: String? = null
)
