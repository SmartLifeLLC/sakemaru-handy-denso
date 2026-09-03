package biz.smt_life.android.feature.inbound.incoming

import android.graphics.Color as AndroidColor
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingProduct
import biz.smt_life.android.core.ui.HardwareKeyHandler

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
    val listState = rememberLazyListState()

    fun selectCurrentProductOrPrompt() {
        val product = viewModel.selectCurrentProduct()
        if (product != null) {
            onProductSelected()
        } else {
            viewModel.promptItemMasterRefreshIfSearchMissing()
        }
    }

    HardwareKeyHandler { keyCode, _ ->
        when (keyCode) {
            AndroidKeyEvent.KEYCODE_F1 -> {
                SoundUtils.playBeep()
                viewModel.syncIncomingData()
                true
            }
            AndroidKeyEvent.KEYCODE_F2 -> {
                SoundUtils.playBeep()
                onNavigateBack()
                true
            }
            AndroidKeyEvent.KEYCODE_F3 -> {
                SoundUtils.playBeep()
                viewModel.searchCurrentProductQuery()
                true
            }
            AndroidKeyEvent.KEYCODE_F4 -> {
                SoundUtils.playBeep()
                onNavigateToHistory()
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                SoundUtils.playBeep()
                viewModel.moveProductSelectionUp()
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                SoundUtils.playBeep()
                viewModel.moveProductSelectionDown()
                true
            }
            else -> false
        }
    }

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
                title = state.selectedWarehouse?.name ?: "作業倉庫確認中"
            )
        },
        bottomBar = {
            FunctionKeyBar(
                f1 = FunctionKey("入予取得", viewModel::syncIncomingData),
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = FunctionKey("検索", viewModel::searchCurrentProductQuery),
                f4 = FunctionKey("履歴", onNavigateToHistory)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                    when (event.key) {
                        Key.F1 -> {
                            SoundUtils.playBeep()
                            viewModel.syncIncomingData()
                            true
                        }
                        Key.F2 -> {
                            SoundUtils.playBeep()
                            onNavigateBack()
                            true
                        }
                        Key.F3 -> {
                            SoundUtils.playBeep()
                            viewModel.searchCurrentProductQuery()
                            true
                        }
                        Key.F4 -> {
                            SoundUtils.playBeep()
                            onNavigateToHistory()
                            true
                        }
                        Key.DirectionUp -> {
                            SoundUtils.playBeep()
                            viewModel.moveProductSelectionUp()
                            true
                        }
                        Key.DirectionDown -> {
                            SoundUtils.playBeep()
                            viewModel.moveProductSelectionDown()
                            true
                        }
                        Key.Enter -> {
                            SoundUtils.playBeep()
                            selectCurrentProductOrPrompt()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            SyncStatusBar(
                isSyncing = state.isSyncingIncomingData,
                isSending = state.isSyncingInspectionBatch,
                isSyncingItemMaster = state.isSyncingItemMaster,
                lastSyncedAt = state.lastSyncedAt,
                pendingCount = state.pendingInspectionDetails.size,
                syncResultMessage = state.syncResultMessage
            )

            // Search bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSubmitCode = viewModel::onProductBarcodeScan,
                onEmptySubmit = { selectCurrentProductOrPrompt() },
                isSearching = state.isSearching,
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
                                "入荷予定の取得が必要です。F1 入予取得を実施してください。"
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
    title: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
    isSyncing: Boolean,
    isSending: Boolean,
    isSyncingItemMaster: Boolean,
    lastSyncedAt: String?,
    pendingCount: Int,
    syncResultMessage: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        isSyncing -> "同期中..."
                        isSyncingItemMaster -> "商品マスタ更新中..."
                        isSending -> "送信中..."
                        else -> "入荷予定同期時刻 : ${lastSyncedAt ?: "-"}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = syncResultMessage ?: "未送信: ${pendingCount}件",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (pendingCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmitCode: (String) -> Unit,
    onEmptySubmit: () -> Unit,
    isSearching: Boolean,
    modifier: Modifier = Modifier
) {
    val currentOnQueryChange = rememberUpdatedState(onQueryChange)
    val currentOnSubmitCode = rememberUpdatedState(onSubmitCode)
    val currentOnEmptySubmit = rememberUpdatedState(onEmptySubmit)

    Surface(
        modifier = modifier.height(52.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                factory = { context ->
                    EditText(context).apply {
                        setSingleLine(true)
                        hint = "JAN/商品CD"
                        inputType = InputType.TYPE_CLASS_NUMBER
                        imeOptions = EditorInfo.IME_ACTION_SEARCH
                        setShowSoftInputOnFocus(false)
                        setTextColor(AndroidColor.rgb(23, 32, 51))
                        setHintTextColor(AndroidColor.rgb(84, 110, 122))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        setPadding(8, 0, 8, 0)
                        background = null
                        setIncludeFontPadding(false)

                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                if (tag == true) return
                                currentOnQueryChange.value(s?.toString().orEmpty().filter { it.isDigit() })
                            }
                            override fun afterTextChanged(s: Editable?) = Unit
                        })

                        fun clearTextWithoutSearch() {
                            tag = true
                            setText("")
                            tag = false
                        }

                        fun submitCurrentText(): Boolean {
                            val rawCode = text?.toString().orEmpty()
                            val numericCode = rawCode.filter { it.isDigit() }
                            SoundUtils.playBeep()
                            clearTextWithoutSearch()
                            if (numericCode.isNotBlank()) {
                                currentOnSubmitCode.value(rawCode)
                            } else {
                                currentOnEmptySubmit.value()
                            }
                            post {
                                setShowSoftInputOnFocus(false)
                                requestFocus()
                            }
                            return true
                        }

                        setOnEditorActionListener { _, actionId, event ->
                            val isEnter = event != null &&
                                (event.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                                    event.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
                            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                                if (event == null || event.action == AndroidKeyEvent.ACTION_DOWN) {
                                    submitCurrentText()
                                } else {
                                    true
                                }
                            } else {
                                false
                            }
                        }

                        setOnKeyListener { _, keyCode, event ->
                            when (keyCode) {
                                AndroidKeyEvent.KEYCODE_ENTER,
                                AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                                AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                    if (event.action == AndroidKeyEvent.ACTION_DOWN) {
                                        submitCurrentText()
                                    } else {
                                        true
                                    }
                                }
                                else -> false
                            }
                        }

                        post {
                            setShowSoftInputOnFocus(false)
                            requestFocus()
                        }
                    }
                },
                update = { editText ->
                    editText.setShowSoftInputOnFocus(false)
                    if (editText.text?.toString().orEmpty() != query) {
                        editText.tag = true
                        editText.setText(query)
                        editText.setSelection(editText.text?.length ?: 0)
                        editText.tag = false
                    }
                    if (!editText.hasFocus()) {
                        editText.post { editText.requestFocus() }
                    }
                }
            )

            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: IncomingProduct,
    isSelected: Boolean,
    isWorking: Boolean,
    onClick: () -> Unit
) {
    val selectedBackgroundColor = Color(0xFF0D47A1)
    val selectedAccentColor = Color(0xFFFFD54F)
    val primaryTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val itemNameColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
    val secondaryTextColor = if (isSelected) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clickable(onClick = onClick),
        border = if (isSelected) BorderStroke(3.dp, selectedAccentColor) else null,
        color = when {
            isSelected -> selectedBackgroundColor
            isWorking -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .width(if (isSelected) 12.dp else 6.dp)
                    .fillMaxHeight()
                    .background(
                        when {
                            isSelected -> selectedAccentColor
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
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = product.itemCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor,
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
                    color = itemNameColor,
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
                        color = secondaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (product.hasFaxOrder()) {
                        QuantityBadge(
                            label = "FAX",
                            quantity = null,
                            containerColor = Color(0xFFE3F2FD),
                            contentColor = Color(0xFF0D47A1)
                        )
                    }

                    if (product.hasEosOrder()) {
                        QuantityBadge(
                            label = "EOS",
                            quantity = null,
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFB71C1C)
                        )
                    }

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

private fun IncomingProduct.hasFaxOrder(): Boolean {
    return schedules.any { !it.isUnplanned && !it.isEosOrder() }
}

private fun IncomingProduct.hasEosOrder(): Boolean {
    return schedules.any { it.isEosOrder() }
}

private fun biz.smt_life.android.core.domain.model.IncomingSchedule.isEosOrder(): Boolean {
    return inspectionPolicy == "EOS_HISTORY_ONLY" ||
        inspectionPolicy == "EOS_ALREADY_CONFIRMED" ||
        isEosSent ||
        orderSource.equals("EOS", ignoreCase = true) ||
        orderSourceLabel?.contains("EOS", ignoreCase = true) == true
}
