package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.ItemStatus
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

/**
 * Picking History Screen (P22 - 出庫処理＞履歴).
 *
 * Two modes per spec:
 * - Editable mode: PICKING items exist → F2:戻る / F3:削除 / F4:確定
 * - Read-only mode: all COMPLETED/SHORTAGE → F2:戻る only + message + list
 *
 * Delete flow: tap card to select → F3 → confirmation dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingHistoryScreen(
    taskId: Int,
    onNavigateBack: () -> Unit,
    onHistoryConfirmed: () -> Unit,
    viewModel: PickingHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize viewModel with taskId
    LaunchedEffect(taskId) {
        viewModel.initialize(taskId)
    }

    // Show error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Delete confirmation dialog
    if (state.itemToDelete != null) {
        DeleteConfirmationDialog(
            item = state.itemToDelete!!,
            onConfirm = {
                viewModel.deleteHistoryItem(
                    item = state.itemToDelete!!,
                    onSuccess = { }
                )
            },
            onCancel = { viewModel.dismissDeleteDialog() }
        )
    }

    // Confirm-all dialog
    if (state.showConfirmDialog) {
        ConfirmAllDialog(
            pickingItemCount = state.pickingItemCount,
            isConfirming = state.isConfirming,
            onConfirm = {
                viewModel.confirmAll(onSuccess = onHistoryConfirmed)
            },
            onCancel = { viewModel.dismissConfirmDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isReadOnlyMode) "出庫処理（履歴）" else "出庫処理（履歴）"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            HistoryBottomBar(
                state = state,
                onBackClick = onNavigateBack,
                onDeleteClick = {
                    // F3: Delete selected item
                    val selected = state.selectedItem
                    if (selected != null && selected.status == ItemStatus.PICKING) {
                        viewModel.showDeleteDialog(selected)
                    }
                },
                onConfirmClick = { viewModel.showConfirmDialog() }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.task == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "タスクが見つかりません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            state.historyItems.isEmpty() && !state.isReadOnlyMode -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "出庫履歴がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                HistoryListContent(
                    state = state,
                    onItemClick = { item ->
                        if (state.isEditableMode && item.status == ItemStatus.PICKING) {
                            if (state.selectedItem?.id == item.id) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectItem(item)
                            }
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HistoryListContent(
    state: PickingHistoryState,
    onItemClick: (PickingTaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Read-only mode message
        if (state.isReadOnlyMode) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "すべての商品が確定済みです",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // History item cards
        items(state.historyItems, key = { it.id }) { item ->
            val isSelected = state.selectedItem?.id == item.id
            HistoryItemCard(
                item = item,
                isSelected = isSelected,
                isClickable = state.isEditableMode && item.status == ItemStatus.PICKING,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: PickingTaskItem,
    isSelected: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = isClickable,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Product name (13sp, Bold, wrap)
            Text(
                text = item.itemName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Slip + Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "伝票:${String.format("%03d", item.slipNumber)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.volume != null) {
                    Text(
                        text = item.volume!!,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // JAN code
            if (item.janCode != null) {
                Text(
                    text = "JAN: ${item.janCode}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Planned qty + Actual qty + Status badge
            val qtyLabel = when (item.plannedQtyType) {
                QuantityType.CASE -> "CASE"
                QuantityType.PIECE -> "PIECE"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "予定: ${String.format("%.2f", item.plannedQty)} $qtyLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "実績: ${String.format("%.2f", item.pickedQty)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(status = item.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ItemStatus) {
    val (text, color) = when (status) {
        ItemStatus.PENDING -> "未登録" to Color.Gray
        ItemStatus.PICKING -> "登録済み" to Color(0xFF1976D2) // Blue
        ItemStatus.COMPLETED -> "完了" to Color(0xFF388E3C) // Green
        ItemStatus.SHORTAGE -> "欠品" to Color(0xFFD32F2F) // Red
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Bottom bar per spec:
 * - Editable mode: F2:戻る / F3:削除 / F4:確定
 * - Read-only mode: F2:戻る only
 */
@Composable
private fun HistoryBottomBar(
    state: PickingHistoryState,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // F2: 戻る (always visible)
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("戻る(F2)")
            }

            if (state.isEditableMode) {
                // F3: 削除 (only in editable mode)
                OutlinedButton(
                    onClick = onDeleteClick,
                    enabled = state.hasSelection && !state.isDeleting,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("削除(F3)")
                }

                // F4: 確定
                Button(
                    onClick = onConfirmClick,
                    enabled = state.canConfirmAll,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isConfirming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("確定(F4)")
                    }
                }
            }
        }
    }
}

/**
 * Delete confirmation dialog per spec.
 */
@Composable
private fun DeleteConfirmationDialog(
    item: PickingTaskItem,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = null,
        text = {
            Column {
                Text("この商品の登録を取り消しますか？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.itemName,
                    fontWeight = FontWeight.Bold
                )
                if (item.volume != null) {
                    Text(
                        text = item.volume!!,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("削除")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * Confirm-all dialog per spec, with item count.
 */
@Composable
private fun ConfirmAllDialog(
    pickingItemCount: Int,
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isConfirming) onCancel() },
        title = null,
        text = {
            Column {
                Text("すべての登録商品を確定しますか？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "登録済み: ${pickingItemCount}件",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isConfirming
            ) {
                if (isConfirming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("確定")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isConfirming
            ) {
                Text("キャンセル")
            }
        }
    )
}
