package biz.smt_life.android.feature.inbound.incoming

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingInspectionDetailData
import biz.smt_life.android.core.domain.model.Location
import biz.smt_life.android.core.ui.HardwareKeyHandler
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * History screen for incoming inspection.
 * Displays local pending inspection details before they are sent to the server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProductList: () -> Unit,
    onEditWorkItem: () -> Unit,
    viewModel: IncomingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pendingDetails = state.pendingInspectionDetails
    fun removeSelectedAndReturnToList() {
        if (state.isSyncingInspectionBatch) return
        if (viewModel.removeSelectedPendingInspectionDetail()) {
            onNavigateToProductList()
        }
    }
    fun submitPendingData() {
        if (!state.isSyncingInspectionBatch) {
            viewModel.syncInspectionBatch()
        }
    }

    HardwareKeyHandler { keyCode, _ ->
        when (keyCode) {
            AndroidKeyEvent.KEYCODE_F1 -> {
                SoundUtils.playBeep()
                removeSelectedAndReturnToList()
                true
            }
            AndroidKeyEvent.KEYCODE_F2 -> {
                SoundUtils.playBeep()
                onNavigateBack()
                true
            }
            AndroidKeyEvent.KEYCODE_F3 -> {
                SoundUtils.playBeep()
                onNavigateToProductList()
                true
            }
            AndroidKeyEvent.KEYCODE_F4 -> {
                SoundUtils.playBeep()
                submitPendingData()
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                SoundUtils.playBeep()
                viewModel.moveHistorySelectionUp()
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                SoundUtils.playBeep()
                viewModel.moveHistorySelectionDown()
                true
            }
            else -> false
        }
    }

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

    LaunchedEffect(state.selectedHistoryIndex, pendingDetails.size) {
        if (pendingDetails.isNotEmpty()) {
            listState.animateScrollToItem(state.selectedHistoryIndex.coerceIn(0, pendingDetails.lastIndex))
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
                f1 = if (state.isSyncingInspectionBatch) null else FunctionKey("削除", ::removeSelectedAndReturnToList),
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = FunctionKey("リスト", onNavigateToProductList),
                f4 = FunctionKey("送信", ::submitPendingData)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                        when (event.key) {
                            Key.F1 -> {
                                SoundUtils.playBeep()
                                removeSelectedAndReturnToList()
                                true
                            }
                            Key.F2 -> {
                                SoundUtils.playBeep()
                                onNavigateBack()
                                true
                            }
                            Key.F3 -> {
                                SoundUtils.playBeep()
                                onNavigateToProductList()
                                true
                            }
                            Key.F4 -> {
                                SoundUtils.playBeep()
                                submitPendingData()
                                true
                            }
                            Key.DirectionUp -> {
                                SoundUtils.playBeep()
                                viewModel.moveHistorySelectionUp()
                                true
                            }
                            Key.DirectionDown -> {
                                SoundUtils.playBeep()
                                viewModel.moveHistorySelectionDown()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "送信前データ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${pendingDetails.size}件",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                if (pendingDetails.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "送信前の検品データがありません",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        itemsIndexed(
                            items = pendingDetails,
                            key = { _, detail -> detail.clientLineUuid }
                        ) { index, detail ->
                            PendingInspectionDetailListItem(
                                detail = detail,
                                location = state.syncedLocations.firstOrNull { it.id == detail.locationId },
                                isSelected = index == state.selectedHistoryIndex,
                                onClick = {
                                    SoundUtils.playBeep()
                                    viewModel.selectPendingInspectionDetail(index)
                                }
                            )
                            if (index < pendingDetails.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            if (state.isSyncingInspectionBatch) {
                SendingOverlay()
            }
        }
    }
}

@Composable
private fun SendingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "送信中...",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "しばらくお待ちください",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PendingInspectionDetailListItem(
    detail: IncomingInspectionDetailData,
    location: Location?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val locationText = location?.displayName ?: location?.fullDisplayName ?: detail.locationId?.toString() ?: "-"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            Color(0xFF0D47A1)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = detail.scannedCode ?: detail.itemCode ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail.slipNumber ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = detail.itemName ?: "商品名なし",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "ロケ:$locationText  期限:${detail.expirationDate?.let { formatDateShort(it) } ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "ケース ${detail.caseQuantity} | バラ ${detail.pieceQuantity} | 総バラ ${detail.totalPieceQuantity}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDateShort(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("MM/dd"))
    } catch (e: Exception) {
        dateStr
    }
}
