package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.ui.ScanKeyHandler

/**
 * Product List Screen for Incoming feature.
 * Displays a list of products with pending incoming schedules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onNavigateBack: () -> Unit,
    onProductSelected: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: IncomingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val searchFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    ScanKeyHandler(
        onScan = viewModel::onProductBarcodeScan,
        onScanStart = viewModel::prepareProductBarcodeScan
    )

    LaunchedEffect(Unit) {
        viewModel.ensureDefaultWarehouseSelected()
    }

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccess()
        }
    }

    if (state.showItemMasterRefreshPrompt) {
        ItemMasterRefreshDialog(
            searchCode = state.pendingItemMasterRefreshCode ?: state.searchQuery,
            isSyncing = state.isSyncingItemMaster,
            onRefresh = {
                SoundUtils.playBeep()
                viewModel.refreshItemMasterForMissingProduct()
            },
            onDismiss = viewModel::dismissItemMasterRefreshPrompt
        )
    }

    // Scroll to selected item
    LaunchedEffect(state.selectedProductIndex) {
        if (state.products.isNotEmpty()) {
            listState.animateScrollToItem(state.selectedProductIndex)
        }
    }

    Scaffold(
        topBar = {
            IncomingCompactTopBar(
                title = "${state.selectedWarehouse?.name ?: ""} 入庫処理",
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            FunctionKeyBar(
                f1 = FunctionKey("同期", viewModel::syncIncomingData),
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = FunctionKey("送信", viewModel::syncInspectionBatch),
                f4 = FunctionKey("検索") { searchFocusRequester.requestFocus() }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.F1 -> {
                            viewModel.syncIncomingData()
                            true
                        }
                        Key.F2 -> {
                            onNavigateBack()
                            true
                        }
                        Key.F3 -> {
                            viewModel.syncInspectionBatch()
                            true
                        }
                        Key.F4 -> {
                            searchFocusRequester.requestFocus()
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.moveProductSelectionUp()
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.moveProductSelectionDown()
                            true
                        }
                        Key.Enter -> {
                            val product = viewModel.selectCurrentProduct()
                            if (product != null) {
                                onProductSelected()
                            } else {
                                viewModel.promptItemMasterRefreshIfSearchMissing()
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            SyncStatusBar(
                warehouseName = state.selectedWarehouse?.name,
                hasSynced = state.hasSyncedIncomingData,
                isSyncing = state.isSyncingIncomingData,
                isSending = state.isSyncingInspectionBatch,
                isSyncingItemMaster = state.isSyncingItemMaster,
                lastSyncedAt = state.lastSyncedAt,
                itemMasterSyncedDate = state.itemMasterSyncedDate,
                pendingCount = state.pendingInspectionDetails.size,
                syncResultMessage = state.syncResultMessage,
                onSync = viewModel::syncIncomingData
            )

            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                isSearching = state.isSearching,
                focusRequester = searchFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // Product list
            when {
                state.isLoadingProducts -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.products.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) {
                                "商品が見つかりません。Enterで最新マスタを確認できます。"
                            } else if (!state.hasSyncedIncomingData) {
                                "データ同期を押してください"
                            } else {
                                "入庫予定がありません"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        itemsIndexed(
                            items = state.products,
                            key = { _, product -> product.itemId }
                        ) { index, product ->
                            ProductListItem(
                                product = product,
                                isSelected = index == state.selectedProductIndex,
                                isWorking = state.workingScheduleIds.any { scheduleId ->
                                    product.schedules.any { it.id == scheduleId }
                                },
                                onClick = {
                                    SoundUtils.playBeep()
                                    viewModel.selectProduct(product)
                                    onProductSelected()
                                }
                            )
                            if (index < state.products.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingCompactTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ItemMasterRefreshDialog(
    searchCode: String,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("商品が見つかりません") },
        text = {
            Text(
                text = "ローカルの商品マスタに「$searchCode」が見つかりません。最新マスタを取得しますか？"
            )
        },
        confirmButton = {
            TextButton(
                onClick = onRefresh,
                enabled = !isSyncing
            ) {
                Text(if (isSyncing) "取得中" else "取得する")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSyncing
            ) {
                Text("閉じる")
            }
        }
    )
}

@Composable
private fun SyncStatusBar(
    warehouseName: String?,
    hasSynced: Boolean,
    isSyncing: Boolean,
    isSending: Boolean,
    isSyncingItemMaster: Boolean,
    lastSyncedAt: String?,
    itemMasterSyncedDate: String?,
    pendingCount: Int,
    syncResultMessage: String?,
    onSync: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = warehouseName ?: "作業倉庫確認中",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        isSyncing -> "同期中..."
                        isSyncingItemMaster -> "商品マスタ更新中..."
                        isSending -> "送信中..."
                        hasSynced -> "同期: ${lastSyncedAt ?: "-"} / マスタ: ${itemMasterSyncedDate ?: "-"}"
                        else -> "未同期"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = syncResultMessage ?: "未送信: ${pendingCount}件",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pendingCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = {
                    SoundUtils.playBeep()
                    onSync()
                },
                modifier = Modifier
                    .width(82.dp)
                    .height(38.dp),
                enabled = warehouseName != null && !isSyncing && !isSending && !isSyncingItemMaster,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isSyncing || isSyncingItemMaster) "同期中" else "同期",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(52.dp)
            .focusRequester(focusRequester),
        placeholder = {
            Text(
                text = "JAN/商品CD/商品名",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp),
                    strokeWidth = 2.dp
                )
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { /* Already debounced */ })
    )
}

@Composable
private fun ProductListItem(
    product: IncomingProduct,
    isSelected: Boolean,
    isWorking: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clickable(onClick = onClick),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            isWorking -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isWorking -> MaterialTheme.colorScheme.tertiary
                            else -> Color.Transparent
                        }
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.primaryJanCode ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = product.itemCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(86.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.itemName,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildProductSpecText(product),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (product.totalRemainingQuantity > 0) {
                        QuantityBadge(
                            label = "残総バラ",
                            quantity = product.totalRemainingQuantity,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    if (product.totalReceivedQuantity > 0) {
                        QuantityBadge(
                            label = "済",
                            quantity = product.totalReceivedQuantity,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    if (isWorking) {
                        QuantityBadge(
                            label = "作業中",
                            quantity = null,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityBadge(
    label: String,
    quantity: Int?,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = if (quantity == null) label else "$label $quantity",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

private fun buildProductSpecText(product: IncomingProduct): String {
    val spec = product.packaging
        ?.takeIf { it.isNotBlank() }
        ?: "-"
    return "規格 $spec"
}
