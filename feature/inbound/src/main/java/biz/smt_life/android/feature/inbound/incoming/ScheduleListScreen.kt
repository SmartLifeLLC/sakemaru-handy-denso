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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(
                        items = product.schedules,
                        key = { _, schedule -> schedule.id }
                    ) { index, schedule ->
                        ScheduleListItem(
                            schedule = schedule,
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
private fun ProductSummaryHeader(
    product: biz.smt_life.android.core.domain.model.IncomingProduct
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
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

        // Item name
        Text(
            text = product.itemName,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Volume and capacity
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (product.fullVolume != null) {
                Text(
                    text = "容量: ${product.fullVolume}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (product.capacityCase != null) {
                Text(
                    text = "入数: ${product.capacityCase}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "合計入荷予定数",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$totalRemaining / $totalExpected",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun ScheduleListItem(
    schedule: IncomingSchedule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val canWork = schedule.status.canStartWork

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canWork, onClick = onClick)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    !canWork -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left side: Schedule info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Warehouse name
            Text(
                text = schedule.warehouseName ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Expected date
            schedule.expectedArrivalDate?.let { dateStr ->
                val formattedDate = formatDateForDisplay(dateStr)
                Text(
                    text = "予定日: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier
                            .width(16.dp)
                            .height(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ロケ: ${location.fullDisplayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status badge
            if (!canWork) {
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = schedule.status)
            }
        }

        // Right side: Quantity button
        QuantityButton(
            remainingQuantity = schedule.remainingQuantity,
            expectedQuantity = schedule.expectedQuantity,
            enabled = canWork,
            onClick = onClick
        )
    }
}

@Composable
private fun QuantityButton(
    remainingQuantity: Int,
    expectedQuantity: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "予定数",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = remainingQuantity.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
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
 * Format date string for display (MM月DD日).
 */
private fun formatDateForDisplay(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("MM月dd日"))
    } catch (e: Exception) {
        dateStr
    }
}
