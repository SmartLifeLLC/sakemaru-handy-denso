package biz.smt_life.android.feature.main

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse
import biz.smt_life.android.core.ui.HardwareKeyHandler

@Composable
fun MainRoute(
    onNavigateToInbound: () -> Unit,
    onNavigateToInboundWebView: (authKey: String, warehouseId: String) -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToOutboundInspection: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            onLogout()
        }
    }

    HardwareKeyHandler { keyCode, _ ->
        when (keyCode) {
            AndroidKeyEvent.KEYCODE_F2,
            AndroidKeyEvent.KEYCODE_BACK,
            AndroidKeyEvent.KEYCODE_ESCAPE,
            AndroidKeyEvent.KEYCODE_SOFT_RIGHT,
            AndroidKeyEvent.KEYCODE_MENU,
            AndroidKeyEvent.KEYCODE_BUTTON_B -> true
            else -> false
        }
    }

    MainScreen(
        state = state,
        onNavigateToInbound = onNavigateToInbound,
        onNavigateToInboundWebView = { authKey, warehouseId ->
            onNavigateToInboundWebView(authKey, warehouseId)
        },
        onNavigateToOutbound = onNavigateToOutbound,
        onNavigateToOutboundInspection = onNavigateToOutboundInspection,
        onNavigateToMove = onNavigateToMove,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToLocationSearch = onNavigateToLocationSearch,
        onLogoutClick = viewModel::logout,
        onRetry = viewModel::retry,
        onShowWarehouseDialog = viewModel::showWarehouseDialog,
        onDismissWarehouseDialog = viewModel::dismissWarehouseDialog,
        onSelectWarehouse = viewModel::selectWarehouse,
        onRefreshMaster = viewModel::refreshMasterData,
        onClearMasterUpdateMessage = viewModel::clearMasterUpdateMessage
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onNavigateToInbound: () -> Unit,
    onNavigateToInboundWebView: (authKey: String, warehouseId: String) -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToOutboundInspection: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetry: () -> Unit,
    onShowWarehouseDialog: () -> Unit = {},
    onDismissWarehouseDialog: () -> Unit = {},
    onSelectWarehouse: (IncomingWarehouse) -> Unit = {},
    onRefreshMaster: () -> Unit = {},
    onClearMasterUpdateMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (state) {
        is MainUiState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is MainUiState.Ready -> {
            ReadyContent(
                pickerCode = state.pickerCode,
                pickerName = state.pickerName,
                warehouse = state.warehouse,
                pendingCounts = state.pendingCounts,
                currentDate = state.currentDate,
                hostUrl = state.hostUrl,
                appVersion = state.appVersion,
                authKey = state.authKey,
                warehouseId = state.warehouseId,
                warehouses = state.warehouses,
                showWarehouseDialog = state.showWarehouseDialog,
                isMasterUpdating = state.isMasterUpdating,
                masterLastUpdatedAt = state.masterLastUpdatedAt,
                masterUpdateMessage = state.masterUpdateMessage,
                onShowWarehouseDialog = onShowWarehouseDialog,
                onDismissWarehouseDialog = onDismissWarehouseDialog,
                onSelectWarehouse = onSelectWarehouse,
                onRefreshMaster = onRefreshMaster,
                onClearMasterUpdateMessage = onClearMasterUpdateMessage,
                onNavigateToInbound = onNavigateToInbound,
                onNavigateToInboundWebView = onNavigateToInboundWebView,
                onNavigateToOutbound = onNavigateToOutbound,
                onNavigateToOutboundInspection = onNavigateToOutboundInspection,
                onNavigateToMove = onNavigateToMove,
                onNavigateToInventory = onNavigateToInventory,
                onNavigateToLocationSearch = onNavigateToLocationSearch,
                onLogoutClick = onLogoutClick,
                modifier = modifier
            )
        }

        is MainUiState.Error -> {
            ErrorContent(
                message = state.message ?: "不明なエラー",
                onRetry = onRetry,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReadyContent(
    pickerCode: String?,
    pickerName: String?,
    warehouse: Warehouse,
    pendingCounts: PendingCounts,
    currentDate: String,
    hostUrl: String,
    appVersion: String,
    authKey: String,
    warehouseId: String,
    warehouses: List<IncomingWarehouse>,
    showWarehouseDialog: Boolean,
    isMasterUpdating: Boolean,
    masterLastUpdatedAt: String?,
    masterUpdateMessage: String?,
    onShowWarehouseDialog: () -> Unit,
    onDismissWarehouseDialog: () -> Unit,
    onSelectWarehouse: (IncomingWarehouse) -> Unit,
    onRefreshMaster: () -> Unit,
    onClearMasterUpdateMessage: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToInboundWebView: (authKey: String, warehouseId: String) -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToOutboundInspection: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val screenBackground = Color(0xFFF4F8FF)
    val primaryTextColor = Color(0xFF0B2F63)
    val secondaryTextColor = Color(0xFF50657D)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(masterUpdateMessage) {
        masterUpdateMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            onClearMasterUpdateMessage()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウト") },
            text = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("ログアウト")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    if (showWarehouseDialog) {
        WarehouseSelectionDialog(
            warehouses = warehouses,
            warehouseId = warehouseId,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            onSelectWarehouse = onSelectWarehouse,
            onDismiss = onDismissWarehouseDialog
        )
    }

    val screenFocusRequester = remember { FocusRequester() }

    // Request focus when screen appears
    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            .padding(16.dp)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                when {
                    event.key == Key.One || event.key == Key.NumPad1 -> {
                        SoundUtils.playBeep()
                        onNavigateToInboundWebView(authKey, warehouseId)
                        true
                    }
                    event.key == Key.Two || event.key == Key.NumPad2 -> {
                        SoundUtils.playBeep()
                        onNavigateToOutbound()
                        true
                    }
                    event.key == Key.Three || event.key == Key.NumPad3 -> {
                        SoundUtils.playBeep()
                        onNavigateToMove()
                        true
                    }
                    event.key == Key.Four || event.key == Key.NumPad4 -> {
                        SoundUtils.playBeep()
                        onNavigateToInventory()
                        true
                    }
                    event.key == Key.Five || event.key == Key.NumPad5 -> {
                        SoundUtils.playBeep()
                        onNavigateToLocationSearch()
                        true
                    }
                    event.key == Key.Six || event.key == Key.NumPad6 -> {
                        SoundUtils.playBeep()
                        onNavigateToOutboundInspection()
                        true
                    }
                    event.key == Key.F1 -> {
                        SoundUtils.playBeep()
                        onNavigateToInboundWebView(authKey, warehouseId)
                        true
                    }
                    event.key == Key.F2 -> {
                        SoundUtils.playBeep()
                        true
                    }
                    event.key == Key.F4 -> {
                        SoundUtils.playBeep()
                        onNavigateToOutbound()
                        true
                    }
                    event.key == Key.F5 -> {
                        SoundUtils.playBeep()
                        onNavigateToMove()
                        true
                    }
                    event.key == Key.F6 -> {
                        SoundUtils.playBeep()
                        onNavigateToInventory()
                        true
                    }
                    event.key == Key.F7 -> {
                        SoundUtils.playBeep()
                        onNavigateToLocationSearch()
                        true
                    }
                    else -> false
                }
            },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
        // Header with picker info and warehouse
        Column(modifier = Modifier.fillMaxWidth()) {
            // Picker info
            if (pickerCode != null && pickerName != null) {
                Text(
                    text = "作業者: $pickerCode $pickerName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = warehouse.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                color = primaryTextColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main menu buttons - 2x2 grid + 1 centered
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: 入庫[1], 出庫[2]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuButton(
                    label = "入庫[1]",
                    count = pendingCounts.inbound,
                    topBorderColor = Color(0xFF2196F3), // Blue
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToInboundWebView(authKey, warehouseId)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                MenuButton(
                    label = "出庫[2]",
                    count = pendingCounts.outbound,
                    topBorderColor = Color(0xFFE91E63), // Pink/Red
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToOutbound()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Row 2: 移動[3], 棚卸し[4]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuButton(
                    label = "移動[3]",
                    count = 0,
                    topBorderColor = Color(0xFF9C27B0), // Purple
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToMove()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                MenuButton(
                    label = "棚卸し[4]",
                    count = pendingCounts.inventory,
                    topBorderColor = Color(0xFFFF9800), // Orange
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToInventory()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Row 3: ロケ検索[5], 出庫検品[6]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuButton(
                    label = "ロケ検索[5]",
                    count = null,
                    topBorderColor = Color(0xFF607D8B), // Blue Grey
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToLocationSearch()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                MenuButton(
                    label = "出庫検品[6]",
                    count = null,
                    topBorderColor = Color(0xFF1565C0), // Blue
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToOutboundInspection()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Row 4: shortcut keys intentionally disabled.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MenuButton(
                    label = "倉庫変更",
                    count = null,
                    topBorderColor = Color(0xFF0B5CAD),
                    onClick = {
                        SoundUtils.playBeep()
                        onShowWarehouseDialog()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                MenuButton(
                    label = "マスタ更新",
                    count = null,
                    subtitle = "最終: ${masterLastUpdatedAt ?: "未更新"}",
                    topBorderColor = Color(0xFF0097A7),
                    onClick = {
                        SoundUtils.playBeep()
                        onRefreshMaster()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        // Bottom info section
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryTextColor
                    )
                    Text(
                        text = hostUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .height(52.dp)
                        .focusProperties { canFocus = false },
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("終了", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appVersion,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
        }

        if (isMasterUpdating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.86f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = primaryTextColor)
                    Text(
                        text = "マスタ更新中",
                        color = primaryTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 56.dp)
        )
    }
}

@Composable
private fun WarehouseSelectionDialog(
    warehouses: List<IncomingWarehouse>,
    warehouseId: String,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onSelectWarehouse: (IncomingWarehouse) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedStartIndex = warehouses.indexOfFirst { it.id.toString() == warehouseId }
        .takeIf { it >= 0 } ?: 0
    var selectedIndex by remember(warehouses, warehouseId) { mutableStateOf(selectedStartIndex) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedStartIndex)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedIndex) {
        if (warehouses.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, warehouses.lastIndex))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4F8FF),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onKeyEvent false
                        }
                        when {
                            event.key == Key.DirectionDown -> {
                                if (warehouses.isNotEmpty()) {
                                    selectedIndex = (selectedIndex + 1).coerceAtMost(warehouses.lastIndex)
                                }
                                true
                            }
                            event.key == Key.DirectionUp -> {
                                if (warehouses.isNotEmpty()) {
                                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                }
                                true
                            }
                            event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.DirectionCenter -> {
                                warehouses.getOrNull(selectedIndex)?.let {
                                    SoundUtils.playBeep()
                                    onSelectWarehouse(it)
                                }
                                true
                            }
                            event.key == Key.F2 || event.key == Key.Back || event.key == Key.Escape -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Text(
                    text = "倉庫選択",
                    color = primaryTextColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "上下キーで移動 / Enterで決定",
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                if (warehouses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "倉庫リストを取得できませんでした",
                            color = primaryTextColor,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(warehouses, key = { _, wh -> wh.id }) { index, wh ->
                            val isCurrentWarehouse = wh.id.toString() == warehouseId
                            val isCursorSelected = index == selectedIndex
                            WarehouseSelectionRow(
                                warehouse = wh,
                                isCurrentWarehouse = isCurrentWarehouse,
                                isCursorSelected = isCursorSelected,
                                primaryTextColor = primaryTextColor,
                                onClick = {
                                    selectedIndex = index
                                    SoundUtils.playBeep()
                                    onSelectWarehouse(wh)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable(onClick = onDismiss)
                            .border(2.dp, Color(0xFF1565C0), RoundedCornerShape(0.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "F2:閉じる",
                                color = primaryTextColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseSelectionRow(
    warehouse: IncomingWarehouse,
    isCurrentWarehouse: Boolean,
    isCursorSelected: Boolean,
    primaryTextColor: Color,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCursorSelected -> Color(0xFF1565C0)
        isCurrentWarehouse -> Color(0xFF0097A7)
        else -> Color(0xFFD0D7E2)
    }
    val backgroundColor = when {
        isCursorSelected -> Color(0xFFE3F2FD)
        isCurrentWarehouse -> Color(0xFFE0F7FA)
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(0.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isCursorSelected) "▶" else "",
                color = Color(0xFF1565C0),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(18.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = warehouse.name,
                color = primaryTextColor,
                fontSize = 18.sp,
                fontWeight = if (isCurrentWarehouse || isCursorSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isCurrentWarehouse) {
                Text(
                    text = "選択中",
                    color = Color(0xFF00796B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "エラー: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("再試行")
        }
    }
}

/**
 * Menu button with colored top border accent.
 */
@Composable
private fun MenuButton(
    label: String,
    count: Int?,
    subtitle: String? = null,
    topBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(0.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .border(1.dp, Color.LightGray, shape),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = shape
    ) {
        Column {
            // Top colored border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(topBorderColor)
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = if (subtitle == null) 18.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                if (count != null) {
                    Text(
                        text = "(%02d)".format(count),
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenLoadingPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Loading,
            onNavigateToInbound = {},
            onNavigateToInboundWebView = { _, _ -> },
            onNavigateToOutbound = {},
            onNavigateToOutboundInspection = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenReadyPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Ready(
                pickerCode = "worker01",
                pickerName = "倉庫作業者",
                warehouse = Warehouse("001", "東京倉庫"),
                pendingCounts = PendingCounts(5, 12, 3),
                currentDate = "2024/10/07 Mon",
                hostUrl = "https://handy.click",
                appVersion = "Ver.1.7.0",
                authKey = "test_auth_key",
                warehouseId = "001"
            ),
            onNavigateToInbound = {},
            onNavigateToInboundWebView = { _, _ -> },
            onNavigateToOutbound = {},
            onNavigateToOutboundInspection = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenErrorPreview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Error("Network connection failed"),
            onNavigateToInbound = {},
            onNavigateToInboundWebView = { _, _ -> },
            onNavigateToOutbound = {},
            onNavigateToOutboundInspection = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}
