package biz.smt_life.android.feature.inbound.incoming

import android.view.KeyEvent as AndroidKeyEvent
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import biz.smt_life.android.core.domain.model.IncomingSchedule
import biz.smt_life.android.core.domain.model.IncomingScheduleStatus
import biz.smt_life.android.core.ui.HardwareKeyHandler
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Schedule List Screen for Incoming feature.
 * Displays schedules for a selected product.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    onNavigateBack: () -> Unit,
    onScheduleSelected: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: IncomingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val product = state.selectedProduct
    val visibleSchedules = viewModel.visibleSchedulesForSelectedProduct()

    HardwareKeyHandler { keyCode, _ ->
        when (keyCode) {
            AndroidKeyEvent.KEYCODE_F2 -> {
                SoundUtils.playBeep()
                onNavigateBack()
                true
            }
            AndroidKeyEvent.KEYCODE_F3 -> {
                SoundUtils.playBeep()
                onNavigateToHistory()
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                SoundUtils.playBeep()
                viewModel.moveScheduleSelectionUp()
                true
            }
            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                SoundUtils.playBeep()
                viewModel.moveScheduleSelectionDown()
                true
            }
            AndroidKeyEvent.KEYCODE_ENTER,
            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
            AndroidKeyEvent.KEYCODE_DPAD_CENTER,
            AndroidKeyEvent.KEYCODE_TAB -> {
                SoundUtils.playBeep()
                val schedule = viewModel.selectCurrentSchedule()
                if (schedule != null) onScheduleSelected()
                true
            }
            else -> false
        }
    }

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Scroll to selected item
    LaunchedEffect(state.selectedScheduleIndex, visibleSchedules.size) {
        if (visibleSchedules.isNotEmpty()) {
            listState.animateScrollToItem(state.selectedScheduleIndex.coerceIn(0, visibleSchedules.lastIndex))
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
                f1 = null,
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = FunctionKey("履歴", onNavigateToHistory),
                f4 = null
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("商品が選択されていません")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                    when (event.key) {
                        Key.F2 -> {
                            SoundUtils.playBeep()
                            onNavigateBack()
                            true
                        }
                        Key.F3 -> {
                            SoundUtils.playBeep()
                            onNavigateToHistory()
                            true
                        }
                        Key.DirectionUp -> {
                            SoundUtils.playBeep()
                            viewModel.moveScheduleSelectionUp()
                            true
                        }
                        Key.DirectionDown -> {
                            SoundUtils.playBeep()
                            viewModel.moveScheduleSelectionDown()
                            true
                        }
                        Key.Enter, Key.Tab -> {
                            SoundUtils.playBeep()
                            val schedule = viewModel.selectCurrentSchedule()
                            if (schedule != null) onScheduleSelected()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            // Product summary header
            ProductSummaryHeader(product = product)

            HorizontalDivider()

            // Total expected quantity
            TotalQuantityBar(
                totalExpected = product.totalExpectedQuantity,
                totalRemaining = product.totalRemainingQuantity
            )

            HorizontalDivider()

            // Schedule list
            if (visibleSchedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未入力の入荷予定がありません\n履歴から削除すると再表示されます",
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
                        items = visibleSchedules,
                        key = { _, schedule -> schedule.id }
                    ) { index, schedule ->
                        ScheduleListItem(
                            schedule = schedule,
                            capacityCase = product.capacityCase,
                            pendingTotalPieceQuantity = state.pendingInspectionDetails
                                .filter { detail ->
                                    if (schedule.isUnplanned) {
                                        detail.incomingScheduleId == null && detail.itemId == product.itemId
                                    } else {
                                        detail.incomingScheduleId == schedule.id
                                    }
                                }
                                .sumOf { detail -> detail.totalPieceQuantity },
                            isSelected = index == state.selectedScheduleIndex,
                            onClick = {
                                SoundUtils.playBeep()
                                if (viewModel.selectSchedule(schedule)) {
                                    onScheduleSelected()
                                }
                            }
                        )
                        if (index < visibleSchedules.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
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
                onClick = {
                    SoundUtils.playBeep()
                    onNavigateBack()
                },
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
private fun ProductSummaryHeader(
    product: biz.smt_life.android.core.domain.model.IncomingProduct
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // JAN code and item code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.primaryJanCode ?: "",
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = product.itemCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Item name
        Text(
            text = product.itemName,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "規格: ${product.packaging?.takeIf { it.isNotBlank() } ?: "-"}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TotalQuantityBar(
    totalExpected: Int,
    totalRemaining: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "合計入荷予定",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "総バラ数",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = totalRemaining.toString(),
                        fontSize = 26.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ $totalExpected",
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "残 / 予定",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScheduleListItem(
    schedule: IncomingSchedule,
    capacityCase: Int?,
    pendingTotalPieceQuantity: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isEosOrder = schedule.isEosOrder()
    val canWork = (schedule.status.canStartWork || schedule.isUnplanned) && !isEosOrder
    val totalPieceQuantity = schedule.remainingPieceQuantity ?: schedule.remainingQuantity
    val casePiece = splitCasePiece(totalPieceQuantity, schedule.capacityCase ?: capacityCase)
    val caseQuantity = casePiece?.first ?: 0
    val pieceQuantity = casePiece?.second ?: totalPieceQuantity
    val selectedBackgroundColor = Color(0xFF0D47A1)
    val selectedAccentColor = Color(0xFFFFD54F)
    val primaryTextColor = if (isSelected) {
        Color.White
    } else if (isEosOrder) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryTextColor = if (isSelected) {
        Color(0xFFE3F2FD)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val alertTextColor = if (isSelected) selectedAccentColor else MaterialTheme.colorScheme.error

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clickable(enabled = canWork, onClick = onClick),
        border = if (isSelected) BorderStroke(3.dp, selectedAccentColor) else null,
        color = when {
            isSelected -> selectedBackgroundColor
            !canWork -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            isEosOrder -> MaterialTheme.colorScheme.error
                            !canWork -> MaterialTheme.colorScheme.outline
                            else -> Color.Transparent
                        }
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${schedule.warehouseName ?: if (schedule.isUnplanned) "予定なし入荷" else "-"} - ${schedule.orderTypeText()}",
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "発注日: ${schedule.orderDate?.let { formatDateForDisplay(it) } ?: "-"}   納品日: ${schedule.expectedArrivalDate?.let { formatDateForDisplay(it) } ?: "-"}",
                    fontSize = 17.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ケース $caseQuantity  |  バラ $pieceQuantity",
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "総バラ数 $totalPieceQuantity",
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (pendingTotalPieceQuantity > 0) {
                    Text(
                        text = "送信前 入荷総バラ数 $pendingTotalPieceQuantity",
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) selectedAccentColor else MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                schedule.location?.let { location ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = secondaryTextColor
                        )
                        Text(
                            text = location.fullDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isEosOrder) {
                    Text(
                        text = "EOS発注は入荷確定処理不可",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = alertTextColor,
                        maxLines = 1
                    )
                } else if (!canWork) {
                    StatusBadge(status = schedule.status)
                }
            }
        }
    }
}

@Composable
private fun QuantitySummary(
    remainingQuantity: Int,
    expectedQuantity: Int,
    capacityCase: Int?,
    isUnplanned: Boolean,
    enabled: Boolean
) {
    val remaining = splitCasePiece(remainingQuantity, capacityCase)
    val expected = splitCasePiece(expectedQuantity, capacityCase)

    Surface(
        modifier = Modifier.width(128.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isUnplanned) "予定なし" else "残総バラ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (isUnplanned) {
                Text(
                    text = "入力",
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = "総バラ入力",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            } else {
                Text(
                    text = remainingQuantity.toString(),
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                if (remaining == null) {
                    Text(
                        text = "バラ $remainingQuantity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ケース ${remaining.first}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            text = "バラ ${remaining.second}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
            if (!isUnplanned) {
                val expectedText = expected?.let { "${it.first}/${it.second}" } ?: expectedQuantity.toString()
                Text(
                    text = "予定総バラ $expectedQuantity",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (expected != null) {
                    Text(
                        text = "予定 $expectedText",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: IncomingScheduleStatus) {
    val (text, color) = when (status) {
        IncomingScheduleStatus.CONFIRMED -> "確定済" to MaterialTheme.colorScheme.primary
        IncomingScheduleStatus.TRANSMITTED -> "連携済" to MaterialTheme.colorScheme.secondary
        IncomingScheduleStatus.CANCELLED -> "キャンセル" to MaterialTheme.colorScheme.error
        else -> return
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Format date string for display (MM/DD).
 */
private fun formatDateForDisplay(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("MM/dd"))
    } catch (e: Exception) {
        dateStr
    }
}

private fun IncomingSchedule.isEosOrder(): Boolean {
    return inspectionPolicy == "EOS_HISTORY_ONLY" ||
        inspectionPolicy == "EOS_ALREADY_CONFIRMED" ||
        isEosSent ||
        orderSource.equals("EOS", ignoreCase = true) ||
        orderSourceLabel?.contains("EOS", ignoreCase = true) == true
}

private fun IncomingSchedule.orderTypeText(): String {
    if (isEosOrder()) return "EOS発注"
    if (isUnplanned) return "予定なし入荷"
    return "FAX発注"
}

private fun splitCasePiece(quantity: Int, capacityCase: Int?): Pair<Int, Int>? {
    val capacity = capacityCase?.takeIf { it > 1 } ?: return null
    return quantity / capacity to quantity % capacity
}
