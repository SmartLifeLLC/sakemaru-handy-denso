package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingProduct

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

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Scroll to selected item
    LaunchedEffect(state.selectedProductIndex) {
        if (state.products.isNotEmpty()) {
            listState.animateScrollToItem(state.selectedProductIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${state.selectedWarehouse?.name ?: ""} 入庫処理",
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            FunctionKeyBar(
                f1 = FunctionKey("検索") { searchFocusRequester.requestFocus() },
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = FunctionKey("履歴", onNavigateToHistory),
                f4 = null
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
                        Key.F2 -> {
                            onNavigateBack()
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
                            if (product != null) onProductSelected()
                            true
                        }
                        else -> false
                    }
                }
        ) {
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
                                "検索結果がありません"
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
        modifier = modifier.focusRequester(focusRequester),
        placeholder = { Text("JAN/商品コード/商品名") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(24.dp)
                        .height(24.dp),
                    strokeWidth = 2.dp
                )
            }
        },
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
            .clickable(onClick = onClick),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            isWorking -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // First row: JAN code and item code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.primaryJanCode ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = product.itemCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Second row: Item name
            Text(
                text = product.itemName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Third row: Quantity info
            if (product.totalRemainingQuantity > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuantityBadge(
                        label = "残",
                        value = product.totalRemainingQuantity,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (product.totalReceivedQuantity > 0) {
                        QuantityBadge(
                            label = "済",
                            value = product.totalReceivedQuantity,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Working indicator
            if (isWorking) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "作業中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun QuantityBadge(
    label: String,
    value: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Text(
            text = ": $value",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}
