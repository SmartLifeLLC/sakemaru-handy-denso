package biz.smt_life.android.feature.inbound.locationsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
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
import biz.smt_life.android.core.domain.model.ItemLocation
import biz.smt_life.android.core.domain.model.ItemLocationSearchResult
import biz.smt_life.android.core.domain.model.StockLocation
import biz.smt_life.android.core.domain.model.StockStatus
import biz.smt_life.android.core.ui.ScanKeyHandler
import biz.smt_life.android.feature.inbound.incoming.FunctionKey
import biz.smt_life.android.feature.inbound.incoming.FunctionKeyBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val searchFocusRequester = remember { FocusRequester() }

    ScanKeyHandler(onScan = viewModel::onScan)

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${state.warehouse?.name ?: state.warehouseId?.let { "倉庫 #$it" } ?: "倉庫未選択"} ロケ検索",
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
                f1 = FunctionKey("検索") { viewModel.searchNow() },
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = null,
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
                        Key.F1 -> {
                            viewModel.searchNow()
                            true
                        }
                        Key.F2 -> {
                            onNavigateBack()
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.moveSelectionUp()
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.moveSelectionDown()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            SearchInput(
                query = state.query,
                isLoading = state.isLoading,
                focusRequester = searchFocusRequester,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::searchNow
            )

            when {
                state.isLoading && state.results.isEmpty() -> LoadingContent()
                state.hasSearched && state.results.isEmpty() -> EmptyContent()
                state.results.isEmpty() -> PromptContent()
                else -> ResultContent(
                    state = state,
                    onSelectResult = viewModel::selectResult
                )
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .focusRequester(focusRequester),
        placeholder = { Text("スキャン/JAN/商品CD/商品名") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(22.dp)
                        .height(22.dp),
                    strokeWidth = 2.dp
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun ResultContent(
    state: LocationSearchState,
    onSelectResult: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state.results.size > 1) {
            item {
                Text(
                    text = "商品候補 ${state.results.size}件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            itemsIndexed(state.results, key = { _, result -> result.item.id }) { index, result ->
                CandidateRow(
                    result = result,
                    selected = index == state.selectedIndex,
                    onClick = { onSelectResult(index) }
                )
            }
        }

        state.selectedResult?.let { result ->
            item {
                DetailPanel(result = result)
            }
        }
    }
}

@Composable
private fun CandidateRow(
    result: ItemLocationSearchResult,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = result.item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MonoText(result.displayCode)
                result.item.primaryJanCode?.let { MonoText(it) }
            }
        }
    }
}

@Composable
private fun DetailPanel(result: ItemLocationSearchResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryLocationBanner(
            location = result.locations.suggested,
            itemName = result.item.name
        )

        Section("商品情報") {
            Text(
                text = result.item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            InfoLine("商品CD", result.displayCode)
            result.item.primaryJanCode?.let { InfoLine("JAN", it) }
            result.item.fullVolume?.let { InfoLine("容量", it) }
            result.item.packaging?.let { InfoLine("荷姿", it) }
            result.item.temperatureType?.let { InfoLine("温度帯", it) }
            result.item.itemQuantityCodes.firstOrNull()?.let { code ->
                InfoLine("社内JAN", listOfNotNull(code.productCode, code.ownCode).joinToString(" / "))
            }
        }

        Section("在庫状況") {
            StockStatusChip(result.stock.status)
            InfoLine("現在庫", result.stock.currentQuantity.toString())
            InfoLine("引当済", result.stock.reservedQuantity.toString())
            InfoLine("引当可能", result.stock.availableQuantity.toString())
            InfoLine("ロケ数", result.stock.locationCount.toString())
            result.stock.earliestExpirationDate?.let { InfoLine("最短期限", it) }
        }

        Section("推奨ロケ") {
            val suggested = result.locations.suggested
            if (suggested == null) {
                Text("推奨ロケなし", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SuggestedLocation(suggested)
            }
        }

        Section("在庫ロケ") {
            if (result.locations.stock.isEmpty()) {
                Text("在庫ロケなし", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                result.locations.stock.forEachIndexed { index, location ->
                    StockLocationRow(location)
                    if (index < result.locations.stock.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryLocationBanner(
    location: ItemLocation?,
    itemName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "推奨ロケ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = location?.code?.ifBlank { location.displayName } ?: "--",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            location?.displayName
                ?.takeIf { it.isNotBlank() && it != location.code }
                ?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = itemName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun SuggestedLocation(location: ItemLocation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = location.displayName,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        MonoText(location.code)
    }
}

@Composable
private fun StockLocationRow(location: StockLocation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.displayName,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MonoText(location.code)
        }
        Text(
            text = location.availableQuantity.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StockStatusChip(status: StockStatus) {
    val color = when (status) {
        StockStatus.IN_STOCK -> Color(0xFF2E7D32)
        StockStatus.RESERVED_ONLY -> Color(0xFFEF6C00)
        StockStatus.NO_STOCK -> Color(0xFFC62828)
        StockStatus.UNKNOWN -> Color(0xFF616161)
    }
    AssistChip(
        onClick = {},
        label = { Text(status.label, fontWeight = FontWeight.Bold) },
        modifier = Modifier.height(32.dp),
        leadingIcon = {
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(10.dp)
                    .background(color, RoundedCornerShape(5.dp))
            )
        }
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MonoText(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("該当商品なし", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PromptContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "商品CD・商品名・JAN・社内JANを入力またはスキャン",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
