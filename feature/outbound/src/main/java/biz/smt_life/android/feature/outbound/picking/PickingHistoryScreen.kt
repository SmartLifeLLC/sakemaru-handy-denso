package biz.smt_life.android.feature.outbound.picking

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType

/**
 * Picking History Screen (2.5.3 - 出庫処理＞履歴).
 *
 * Two modes:
 * - Editable mode: show PICKING items with delete (F3) and confirm-all (F4) buttons
 * - Read-only mode: all items COMPLETED/SHORTAGE, no action buttons
 *
 * @param taskId The picking task ID to show history for
 * @param onNavigateBack Navigate back to previous screen
 * @param onHistoryConfirmed Callback when user confirms all (navigate back to course list)
 * @param viewModel ViewModel for this screen
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

    // Initialize viewModel with taskId - it will observe the repository flow
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
                    onSuccess = onNavigateBack
                )
            },
            onCancel = { viewModel.dismissDeleteDialog() }
        )
    }

    // Confirm-all dialog
    if (state.showConfirmDialog) {
        ConfirmAllDialog(
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
                title = { Text("出庫履歴") },
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
            if (state.isEditableMode && state.historyItems.isNotEmpty()) {
                HistoryBottomBar(
                    onConfirmAllClick = { viewModel.showConfirmDialog() },
                    canConfirm = state.canConfirmAll
                )
            }
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
            state.historyItems.isEmpty() && state.isReadOnlyMode -> {
                // Read-only mode with no PICKING items (all completed)
                ReadOnlyModeContent(
                    task = state.task!!,
                    modifier = Modifier.padding(padding)
                )
            }
            state.historyItems.isEmpty() -> {
                // No history items at all
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
                    onDeleteClick = { viewModel.showDeleteDialog(it) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HistoryListContent(
    state: PickingHistoryState,
    onDeleteClick: (PickingTaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with course info
        if (state.task != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.task.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "フロア: ${state.task.pickingAreaName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.isReadOnlyMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "確定済み（参照のみ）",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // List of history items
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.historyItems, key = { it.id }) { item ->
                HistoryItemCard(
                    item = item,
                    onDeleteClick = { onDeleteClick(item) },
                    showDeleteButton = state.isEditableMode && !state.isDeleting
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: PickingTaskItem,
    onDeleteClick: () -> Unit,
    showDeleteButton: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Item name
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Slip number
            InfoRow(label = "伝票番号", value = item.slipNumber.toString())

            // Volume and capacity (if available)
            if (item.volume != null || item.capacityCase != null) {
                val spec = buildString {
                    if (item.volume != null) append(item.volume)
                    if (item.capacityCase != null) {
                        if (isNotEmpty()) append(" / ")
                        append("入数: ${item.capacityCase}")
                    }
                }
                InfoRow(label = "規格", value = spec)
            }

            // JAN code (if available)
            if (item.janCode != null) {
                InfoRow(label = "JAN", value = item.janCode!!)
            }

            // Get quantity type
            val qtyLabel = when (item.plannedQtyType) {
                QuantityType.CASE -> "ケース"
                QuantityType.PIECE -> "バラ"
            }

            InfoRow(
                label = "予定数量",
                value = String.format("%.1f %s", item.plannedQty, qtyLabel)
            )

            InfoRow(
                label = "出庫数量",
                value = String.format("%.1f %s", item.pickedQty, qtyLabel)
            )

            // Status badge
            StatusBadge(status = item.status)

            // Delete button (only in editable mode)
            if (showDeleteButton) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("削除(F3)")
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyModeContent(
    task: PickingTask,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "すべての商品が確定済みです",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = task.courseName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "履歴は参照のみ可能です。変更はできません。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusBadge(status: biz.smt_life.android.core.domain.model.ItemStatus) {
    val (text, color) = when (status) {
        biz.smt_life.android.core.domain.model.ItemStatus.PENDING -> "未登録" to MaterialTheme.colorScheme.error
        biz.smt_life.android.core.domain.model.ItemStatus.PICKING -> "登録済み" to MaterialTheme.colorScheme.tertiary
        biz.smt_life.android.core.domain.model.ItemStatus.COMPLETED -> "完了" to MaterialTheme.colorScheme.primary
        biz.smt_life.android.core.domain.model.ItemStatus.SHORTAGE -> "欠品" to MaterialTheme.colorScheme.error
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HistoryBottomBar(
    onConfirmAllClick: () -> Unit,
    canConfirm: Boolean,
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onConfirmAllClick,
                enabled = canConfirm,
                modifier = Modifier.widthIn(min = 200.dp)
            ) {
                Text("確定(F4)")
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    item: PickingTaskItem,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("削除確認") },
        text = {
            Column {
                Text("以下の履歴を削除しますか？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.itemName,
                    fontWeight = FontWeight.Bold
                )
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

@Composable
private fun ConfirmAllDialog(
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isConfirming) onCancel() },
        title = { Text("確定確認") },
        text = { Text("すべての出庫履歴を確定しますか？\n確定後は変更できません。") },
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
