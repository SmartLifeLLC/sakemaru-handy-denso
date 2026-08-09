package biz.smt_life.android.feature.inbound.incoming

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Scroll to selected item
    LaunchedEffect(state.selectedScheduleIndex) {
        if (product?.schedules?.isNotEmpty() == true) {
            listState.animateScrollToItem(state.selectedScheduleIndex)
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
                    when (event.key) {
                        Key.F2 -> {
                            onNavigateBack()
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.moveScheduleSelectionUp()
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.moveScheduleSelectionDown()
                            true
                        }
                        Key.Enter, Key.Tab -> {
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
            if (product.schedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "入庫予定がありません",
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
                        items = product.schedules,
                        key = { _, schedule -> schedule.id }
                    ) { index, schedule ->
                        ScheduleListItem(
                            schedule = schedule,
                            capacityCase = product.capacityCase,
                            isSelected = index == state.selectedScheduleIndex,
                            onClick = {
                                SoundUtils.playBeep()
                                viewModel.selectSchedule(schedule)
                                onScheduleSelected()
                            }
                        )
                        if (index < product.schedules.lastIndex) {
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Text(
                text = product.itemCode,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Item name
        Text(
            text = product.itemName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Packaging and total piece count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "規格: ${product.packaging?.takeIf { it.isNotBlank() } ?: "-"}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "残総バラ: ${product.totalRemainingQuantity}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
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
            Text(
                text = "合計総バラ",
                style = MaterialTheme.typography.bodySmall
            )
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "残総バラ $totalRemaining",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "予定総バラ $totalExpected",
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
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val canWork = schedule.status.canStartWork || schedule.isUnplanned

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canWork, onClick = onClick),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            !canWork -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left side: Schedule info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Warehouse name
                Text(
                    text = schedule.warehouseName ?: if (schedule.isUnplanned) "予定なし入荷" else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (schedule.inspectionPolicy == "EOS_HISTORY_ONLY" || schedule.isEosSent) {
                    Text(
                        text = "EOS履歴のみ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                } else if (schedule.isUnplanned) {
                    Text(
                        text = "予定なし入荷",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }

                // Order and expected dates
                val orderDateText = schedule.orderDate?.let { "発注 ${formatDateForDisplay(it)}" }
                val expectedDateText = schedule.expectedArrivalDate?.let { "予定 ${formatDateForDisplay(it)}" }
                if (orderDateText != null || expectedDateText != null) {
                    Text(
                        text = listOfNotNull(orderDateText, expectedDateText).joinToString("  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Location
                schedule.location?.let { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = location.fullDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status badge
                if (!canWork) {
                    Spacer(modifier = Modifier.height(2.dp))
                    StatusBadge(status = schedule.status)
                }
            }

            // Right side: Quantity summary
            QuantitySummary(
                remainingQuantity = schedule.remainingPieceQuantity ?: schedule.remainingQuantity,
                expectedQuantity = schedule.expectedPieceQuantity ?: schedule.expectedQuantity,
                capacityCase = capacityCase,
                isUnplanned = schedule.isUnplanned,
                enabled = canWork
            )
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
        modifier = Modifier.width(108.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = if (isUnplanned) "予定なし" else "残総バラ",
                style = MaterialTheme.typography.labelSmall
            )
            if (isUnplanned) {
                Text(
                    text = "入力",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                if (remaining == null) {
                    Text(
                        text = "バラ $remainingQuantity",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "ケース ${remaining.first}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = "バラ ${remaining.second}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
            if (!isUnplanned) {
                val expectedText = expected?.let { "${it.first}/${it.second}" } ?: expectedQuantity.toString()
                Text(
                    text = "予定総バラ $expectedQuantity",
                    style = MaterialTheme.typography.labelSmall,
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

private fun splitCasePiece(quantity: Int, capacityCase: Int?): Pair<Int, Int>? {
    val capacity = capacityCase?.takeIf { it > 1 } ?: return null
    return quantity / capacity to quantity % capacity
}
