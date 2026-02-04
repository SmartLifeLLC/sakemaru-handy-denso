package biz.smt_life.android.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.PendingCounts
import biz.smt_life.android.core.domain.model.Warehouse

@Composable
fun MainRoute(
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToInboundWebView: (authKey: String, warehouseId: String) -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Listen for logout event
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            onLogout()
        }
    }

    MainScreen(
        state = state,
        onNavigateToWarehouseSettings = onNavigateToWarehouseSettings,
        onNavigateToInbound = onNavigateToInbound,
        onNavigateToInboundWebView = { authKey, warehouseId ->
            onNavigateToInboundWebView(authKey, warehouseId)
        },
        onNavigateToOutbound = onNavigateToOutbound,
        onNavigateToMove = onNavigateToMove,
        onNavigateToInventory = onNavigateToInventory,
        onNavigateToLocationSearch = onNavigateToLocationSearch,
        onLogoutClick = viewModel::logout,
        onRetry = viewModel::retry
    )
}

@Composable
fun MainScreen(
    state: MainUiState,
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToInboundWebView: (authKey: String, warehouseId: String) -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetry: () -> Unit,
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
                onNavigateToWarehouseSettings = onNavigateToWarehouseSettings,
                onNavigateToInbound = onNavigateToInbound,
                onNavigateToInboundWebView = onNavigateToInboundWebView,
                onNavigateToOutbound = onNavigateToOutbound,
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
    onNavigateToWarehouseSettings: () -> Unit,
    onNavigateToInbound: () -> Unit,
    onNavigateToInboundWebView: (authKey: String, warehouseId: String) -> Unit,
    onNavigateToOutbound: () -> Unit,
    onNavigateToMove: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Logout confirmation dialog
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

    val screenFocusRequester = remember { FocusRequester() }

    // Request focus when screen appears
    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                when (event.key) {
                    Key.F1 -> {
                        SoundUtils.playBeep()
                        onNavigateToInboundWebView(authKey, warehouseId)
                        true
                    }
                    Key.F2 -> {
                        showLogoutDialog = true
                        true
                    }
                    Key.F4 -> {
                        SoundUtils.playBeep()
                        onNavigateToOutbound()
                        true
                    }
                    Key.F5 -> {
                        SoundUtils.playBeep()
                        onNavigateToMove()
                        true
                    }
                    Key.F6 -> {
                        SoundUtils.playBeep()
                        onNavigateToInventory()
                        true
                    }
                    Key.F7 -> {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Warehouse section with settings icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onNavigateToWarehouseSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "倉庫設定"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main menu buttons - 2x2 grid + 1 centered
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1: 入庫 [F1], 出庫 [F4]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    label = "入庫 [F1]",
                    count = pendingCounts.inbound,
                    topBorderColor = Color(0xFF2196F3), // Blue
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToInboundWebView(authKey, warehouseId)
                    },
                    modifier = Modifier.weight(1f)
                )

                MenuButton(
                    label = "出庫 [F4]",
                    count = pendingCounts.outbound,
                    topBorderColor = Color(0xFFE91E63), // Pink/Red
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToOutbound()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: 移動, 棚卸
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    label = "移動",
                    count = 0,
                    topBorderColor = Color(0xFF9C27B0), // Purple
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToMove()
                    },
                    modifier = Modifier.weight(1f)
                )

                MenuButton(
                    label = "棚卸",
                    count = pendingCounts.inventory,
                    topBorderColor = Color(0xFFFF9800), // Orange
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToInventory()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: ロケ検索, 終了 [F2]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuButton(
                    label = "ロケ検索",
                    count = null,
                    topBorderColor = Color(0xFF607D8B), // Blue Grey
                    onClick = {
                        SoundUtils.playBeep()
                        onNavigateToLocationSearch()
                    },
                    modifier = Modifier.weight(1f)
                )

                MenuButton(
                    label = "終了 [F2]",
                    count = null,
                    topBorderColor = Color(0xFF795548), // Brown
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom info section
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = hostUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "ログアウト"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appVersion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    topBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Surface(
        modifier = modifier
            .aspectRatio(1.5f)
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (count != null) {
                    Text(
                        text = "(%02d)".format(count),
                        fontSize = 16.sp,
                        color = Color.DarkGray
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
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToInboundWebView = { _, _ -> },
            onNavigateToOutbound = {},
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
                appVersion = "Ver.1.1.1",
                authKey = "test_auth_key",
                warehouseId = "001"
            ),
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToInboundWebView = { _, _ -> },
            onNavigateToOutbound = {},
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
            onNavigateToWarehouseSettings = {},
            onNavigateToInbound = {},
            onNavigateToInboundWebView = { _, _ -> },
            onNavigateToOutbound = {},
            onNavigateToMove = {},
            onNavigateToInventory = {},
            onNavigateToLocationSearch = {},
            onLogoutClick = {},
            onRetry = {}
        )
    }
}
