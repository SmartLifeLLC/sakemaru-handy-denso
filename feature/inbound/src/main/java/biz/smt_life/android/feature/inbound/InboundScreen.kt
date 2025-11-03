package biz.smt_life.android.feature.inbound

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import biz.smt_life.android.core.ui.ScanKeyHandler
import biz.smt_life.android.feature.inbound.component.HistoryBottomSheet
import biz.smt_life.android.feature.inbound.component.ItemSearchBar
import biz.smt_life.android.feature.inbound.component.QtyInputSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboundScreen(
    viewModel: InboundViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle keyboard wedge barcode scanner
    ScanKeyHandler(onScan = viewModel::onBarcodeScan)

    // Show snackbar for messages
    LaunchedEffect(state.errorMessage, state.successMessage) {
        val message = state.errorMessage ?: state.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbound") },
                actions = {
                    TextButton(onClick = viewModel::toggleHistorySheet) {
                        Text("History (${state.history.size})")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ItemSearchBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearch = viewModel::searchItems,
                searchResults = state.searchResults,
                onItemSelect = viewModel::selectItem,
                isSearching = state.isSearching,
                modifier = Modifier.fillMaxWidth()
            )

            // DEV: Simulate Scan Button (will be removed for production)
            if (BuildConfig.DEBUG) {
                OutlinedButton(
                    onClick = {
                        // Simulate scanning a barcode
                        viewModel.onBarcodeScan("4901234567890")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔍 DEV: Simulate Scan (Premium Sake)")
                }
            }

            QtyInputSection(
                selectedItem = state.selectedItem,
                qtyCase = state.qtyCase,
                onQtyCaseChange = viewModel::onQtyCaseChange,
                qtyEach = state.qtyEach,
                onQtyEachChange = viewModel::onQtyEachChange,
                expirationDate = state.expirationDate,
                onExpirationDateChange = viewModel::onExpirationDateChange,
                labelCount = state.labelCount,
                onLabelCountChange = viewModel::onLabelCountChange,
                fieldErrors = state.fieldErrors,
                onAddEntry = viewModel::addEntry,
                isAdding = state.isAdding,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.selectedItem != null) {
                OutlinedButton(
                    onClick = viewModel::clearSelection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Selection")
                }
            }
        }

        if (state.showHistory) {
            HistoryBottomSheet(
                history = state.history,
                selectedEntries = state.selectedEntries,
                onToggleSelection = viewModel::toggleEntrySelection,
                onConfirmSelected = viewModel::confirmSelected,
                onDismiss = viewModel::toggleHistorySheet,
                isConfirming = state.isConfirming,
                isLoading = state.isLoadingHistory
            )
        }
    }
}
