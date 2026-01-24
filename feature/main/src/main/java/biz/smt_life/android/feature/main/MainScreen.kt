package biz.smt_life.android.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.theme.HandyTheme
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
                message = state.message,
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
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header with picker info and warehouse
        Column(modifier = Modifier.fillMaxWidth()) {
            // Picker info
            if (pickerCode != null && pickerName != null) {
                Text(
                    text = "Worker: $pickerCode $pickerName",
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

        // Main menu buttons
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigateToInboundWebView(authKey, warehouseId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
//                    text = "入庫処理(${pendingCounts.inbound.toString().padStart(2, '0')})",
                    text = "入庫処理",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = onNavigateToOutbound,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
//                    text = "出庫処理(${pendingCounts.outbound.toString().padStart(2, '0')})",
                    text = "出庫処理",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = onNavigateToMove,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "移動処理",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = onNavigateToInventory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
//                    text = "棚卸処理(${pendingCounts.inventory.toString().padStart(2, '0')})",
                    text = "棚卸処理",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = onNavigateToLocationSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ロケ検索",
                    modifier = Modifier.padding(vertical = 8.dp)
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
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
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
                pickerName = "Warehouse Worker",
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
