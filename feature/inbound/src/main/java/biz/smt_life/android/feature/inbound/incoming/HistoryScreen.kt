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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingWorkItem
import biz.smt_life.android.core.domain.model.IncomingWorkStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * History Screen for Incoming feature.
 * Displays today's incoming work history.
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

    // Load history on first composition
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    // Scroll to selected item
    LaunchedEffect(state.selectedHistoryIndex) {
        if (state.historyItems.isNotEmpty()) {
            listState.animateScrollToItem(state.selectedHistoryIndex)
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
                f3 = FunctionKey("リスト", onNavigateToProductList),
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
                        Key.F2 -> {
                            onNavigateBack()
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.moveHistorySelectionUp()
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.moveHistorySelectionDown()
                            true
                        }
                        Key.Enter -> {
                            val items = state.historyItems
                            val index = state.selectedHistoryIndex
                            if (index >= 0 && index < items.size) {
                                if (viewModel.selectHistoryItem(items[index])) {
                                    onEditWorkItem()
                                }
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "本日の入庫履歴",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }

            HorizontalDivider()

            // Content
            when {
                state.isLoadingHistory -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.historyItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "本日の入庫履歴がありません",
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
                            items = state.historyItems,
                            key = { _, item -> item.id }
                        ) { index, workItem ->
                            HistoryListItem(
                                workItem = workItem,
                                isSelected = index == state.selectedHistoryIndex,
                                onClick = {
                                    SoundUtils.playBeep()
                                    if (viewModel.selectHistoryItem(workItem)) {
                                        onEditWorkItem()
                                    }
                                }
                            )
                            if (index < state.historyItems.lastIndex) {
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
private fun HistoryListItem(
    workItem: IncomingWorkItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val schedule = workItem.schedule
    val canEdit = workItem.status.canEdit

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit, onClick = onClick),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            !canEdit -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // First row: JAN code and item code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = schedule?.primaryJanCode ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = schedule?.itemCode ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Second row: Item name
            Text(
                text = schedule?.itemName ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Third row: Warehouse name
            Text(
                text = schedule?.warehouseName ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Fourth row: Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val expectedDate = schedule?.expectedArrivalDate?.let { formatDateShort(it) } ?: ""
                val arrivalDate = workItem.workArrivalDate?.let { formatDateShort(it) } ?: ""

                Text(
                    text = "予定:$expectedDate  入庫:$arrivalDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Fifth row: Quantity and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                StatusBadge(status = workItem.status)

                // Quantity
                Text(
                    text = workItem.workQuantity.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: IncomingWorkStatus) {
    val (text, color) = when (status) {
        IncomingWorkStatus.WORKING -> "作業中" to MaterialTheme.colorScheme.tertiary
        IncomingWorkStatus.COMPLETED -> "完了" to MaterialTheme.colorScheme.primary
        IncomingWorkStatus.CANCELLED -> "キャンセル" to MaterialTheme.colorScheme.error
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
 * Format date string for short display (MM/DD).
 */
private fun formatDateShort(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("MM/dd"))
    } catch (e: Exception) {
        dateStr
    }
}
