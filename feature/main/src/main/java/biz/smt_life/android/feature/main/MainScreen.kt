package biz.smt_life.android.feature.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
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
        is MainUiState.Loading -> LoadingContent(modifier = modifier)
        is MainUiState.Ready -> ReadyContent(
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
        is MainUiState.Error -> ErrorContent(
            message = state.message ?: "不明なエラー",
            onRetry = onRetry,
            modifier = modifier
        )
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
    var currentPage by remember { mutableStateOf(1) }
    val screenFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("ログアウト") },
            text = { Text("ログアウトしますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogoutClick()
                }) { Text("ログアウト") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("キャンセル") }
            }
        )
    }

    val displayName = buildString {
        if (pickerCode != null) append(pickerCode)
        if (pickerName != null) {
            if (isNotEmpty()) append(" ")
            append(pickerName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .focusRequester(screenFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (currentPage == 1) {
                    when (event.key) {
                        Key.F1 -> { SoundUtils.playBeep(); onNavigateToInboundWebView(authKey, warehouseId); true }
                        Key.F2 -> { SoundUtils.playBeep(); onNavigateToOutbound(); true }
                        Key.F3 -> { currentPage = 2; true }
                        Key.F6 -> { SoundUtils.playBeep(); onNavigateToWarehouseSettings(); true }
                        Key.F7 -> { showLogoutDialog = true; true }
                        else -> false
                    }
                } else {
                    when (event.key) {
                        Key.F1 -> { SoundUtils.playBeep(); true } // 発注 stub
                        Key.F2 -> { SoundUtils.playBeep(); onNavigateToMove(); true }
                        Key.F3 -> { SoundUtils.playBeep(); onNavigateToInventory(); true }
                        Key.F4 -> { SoundUtils.playBeep(); onNavigateToLocationSearch(); true }
                        Key.F6 -> { SoundUtils.playBeep(); onNavigateToWarehouseSettings(); true }
                        Key.F7 -> { showLogoutDialog = true; true }
                        Key.F8 -> { currentPage = 1; true }
                        else -> false
                    }
                }
            }
    ) {
        // ─── Navbar ──────────────────────────────────────────
        MainNavbar(
            pickerName = displayName,
            onLogout = { showLogoutDialog = true }
        )

        // ─── SubHeader ───────────────────────────────────────
        MainSubHeader(
            currentDate = currentDate,
            warehouseName = warehouse.name,
            onWarehouseChange = {
                SoundUtils.playBeep()
                onNavigateToWarehouseSettings()
            }
        )

        // ─── Menu Cards ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (currentPage == 1) 16.dp else 12.dp)
        ) {
            if (currentPage == 1) {
                MainMenuCard(
                    text = "入荷処理 [F1]",
                    count = "(%02d)".format(pendingCounts.inbound),
                    bottomBorderColor = Color(0xFF2196F3),
                    isPage1 = true,
                    onClick = { SoundUtils.playBeep(); onNavigateToInboundWebView(authKey, warehouseId) }
                )
                MainMenuCard(
                    text = "出荷処理 [F2]",
                    count = "(%02d)".format(pendingCounts.outbound),
                    bottomBorderColor = Color(0xFFF44336),
                    isPage1 = true,
                    onClick = { SoundUtils.playBeep(); onNavigateToOutbound() }
                )
                MainMenuCard(
                    text = "その他 [F3]",
                    count = "...",
                    bottomBorderColor = Color(0xFF607D8B),
                    isPage1 = true,
                    onClick = { currentPage = 2 }
                )
            } else {
                MainMenuCard(
                    text = "発注処理 [F1]",
                    count = "",
                    bottomBorderColor = Color(0xFF2196F3),
                    isPage1 = false,
                    onClick = { SoundUtils.playBeep() }
                )
                MainMenuCard(
                    text = "移動処理 [F2]",
                    count = "",
                    bottomBorderColor = Color(0xFFFF9800),
                    isPage1 = false,
                    onClick = { SoundUtils.playBeep(); onNavigateToMove() }
                )
                MainMenuCard(
                    text = "棚卸処理 [F3]",
                    count = "(%02d)".format(pendingCounts.inventory),
                    bottomBorderColor = Color(0xFF4CAF50),
                    isPage1 = false,
                    onClick = { SoundUtils.playBeep(); onNavigateToInventory() }
                )
                MainMenuCard(
                    text = "ロケ検索 [F4]",
                    count = "",
                    bottomBorderColor = Color(0xFF9C27B0),
                    isPage1 = false,
                    onClick = { SoundUtils.playBeep(); onNavigateToLocationSearch() }
                )
                MainMenuCard(
                    text = "戻る [F8]",
                    count = "",
                    bottomBorderColor = Color(0xFF607D8B),
                    isPage1 = false,
                    onClick = { currentPage = 1 }
                )
            }
        }

        // ─── Footer ──────────────────────────────────────────
        MainFooter(appVersion = appVersion, hostUrl = hostUrl)
    }
}

// ─── Navbar ──────────────────────────────────────────────────

@Composable
private fun MainNavbar(
    pickerName: String,
    onLogout: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFF1A233A))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pickerName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isPressed) Color(0xFFB71C1C) else Color(0xFFD32F2F))
                .border(1.dp, Color(0xFFB71C1C), RoundedCornerShape(4.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onLogout
                )
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ログアウト【F7】",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─── SubHeader ───────────────────────────────────────────────

@Composable
private fun MainSubHeader(
    currentDate: String,
    warehouseName: String,
    onWarehouseChange: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = Color(0xFFCCCCCC),
                    start = Offset(0f, size.height - strokeWidth / 2),
                    end = Offset(size.width, size.height - strokeWidth / 2),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentDate,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF444444)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPressed) Color(0xFF004494) else Color(0xFF0056B3))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onWarehouseChange
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "倉庫名変更 [F6]",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE9ECEF))
                .border(2.dp, Color(0xFFCED4DA), RoundedCornerShape(4.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = warehouseName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Menu Card ───────────────────────────────────────────────

@Composable
private fun MainMenuCard(
    text: String,
    count: String,
    bottomBorderColor: Color,
    isPage1: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        label = "cardScale"
    )

    val outerShape = RoundedCornerShape(12.dp)
    val innerShape = RoundedCornerShape(10.dp)
    val sideBorderColor = if (isPressed) Color(0xFF495057) else Color(0xFF6C757D)
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.3f to Color.White,
            1.0f to Color(0xFFB0B8C1)
        )
    )

    val textSize = if (isPage1) 22.sp else 20.sp
    val textPadding = if (isPage1) 8.dp else 4.dp

    // 3층 구조: 最外=下ボーダー色, 中=上/左/右ボーダー色, 内=グラデーション
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clip(outerShape)
            .background(bottomBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 10.dp)
                .clip(innerShape)
                .background(sideBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp) // 内側コンテンツmin高さ (外側ボーダー分を除く)
                    .then(
                        if (isPressed) Modifier.background(Color(0xFFA0A8B1))
                        else Modifier.background(gradient)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212529),
                    modifier = Modifier.padding(start = textPadding),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.White,
                            offset = Offset(1f, 1f),
                            blurRadius = 0f
                        )
                    )
                )
                if (count.isNotEmpty()) {
                    Text(
                        text = count,
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(end = textPadding),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.White,
                                offset = Offset(1f, 1f),
                                blurRadius = 0f
                            )
                        )
                    )
                }
            }
        }
    }
}

// ─── Footer ──────────────────────────────────────────────────

@Composable
private fun MainFooter(appVersion: String, hostUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F2F5))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Ver: $appVersion | $hostUrl",
            fontSize = 11.sp,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Error ───────────────────────────────────────────────────

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
        Button(onClick = onRetry) { Text("再試行") }
    }
}

// ─── Preview ─────────────────────────────────────────────────

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

@Preview(showBackground = true, name = "Main - Page1")
@Composable
private fun MainScreenPage1Preview() {
    HandyTheme {
        MainScreen(
            state = MainUiState.Ready(
                pickerCode = "worker01",
                pickerName = "倉庫作業者",
                warehouse = Warehouse("001", "酒丸本社"),
                pendingCounts = PendingCounts(5, 12, 3),
                currentDate = "2026/02/23(月)",
                hostUrl = "https://wms.lw-hana.net",
                appVersion = "1.1.1",
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
