package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem

/**
 * Outbound Picking Screen (P21 - 出庫データ入力).
 * Displays current PENDING item and allows quantity input.
 *
 * Spec layout:
 * - TopAppBar: "出庫処理" + [◀ 1/5 ▶] item navigation
 * - Content: Course header, product info, quantity input
 * - BottomBar: F1:登録, F2:戻る, F3:履歴
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundPickingScreen(
    task: PickingTask,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onTaskCompleted: () -> Unit,
    viewModel: OutboundPickingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize viewModel with task
    LaunchedEffect(task.taskId) {
        viewModel.initialize(task)
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

    // Completion Dialog
    if (state.showCompletionDialog) {
        CompletionConfirmationDialog(
            isCompleting = state.isCompleting,
            onConfirm = {
                viewModel.completeTask(onSuccess = onTaskCompleted)
            },
            onCancel = {
                viewModel.dismissCompletionDialog()
                onNavigateToHistory()
            }
        )
    }

    // Image Viewer Dialog
    if (state.showImageDialog && state.currentItem != null) {
        ImageViewerDialog(
            images = state.currentItem!!.images,
            onDismiss = { viewModel.dismissImageDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出庫処理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    // [◀ 1/5 ▶] item navigation in TopAppBar
                    if (state.pendingItems.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.moveToPrevItem() },
                            enabled = state.canMovePrev && !state.isUpdating
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "前へ"
                            )
                        }
                        Text(
                            text = "${state.currentIndex + 1}/${state.pendingItems.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { viewModel.moveToNextItem() },
                            enabled = state.canMoveNext && !state.isUpdating
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "次へ"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            OutboundPickingBottomBar(
                state = state,
                onRegisterClick = viewModel::registerCurrentItem,
                onBackClick = onNavigateBack,
                onHistoryClick = onNavigateToHistory
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
            state.currentItem != null && state.originalTask != null -> {
                OutboundPickingContent(
                    state = state,
                    onPickedQtyChange = viewModel::onPickedQtyChange,
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("商品がありません")
                }
            }
        }
    }
}

@Composable
private fun OutboundPickingContent(
    state: OutboundPickingState,
    onPickedQtyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentItem = state.currentItem!!
    val originalTask = state.originalTask!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Course info header (2 lines per spec)
        CourseInfoHeader(
            courseName = originalTask.courseName,
            courseCode = originalTask.courseCode,
            pickingAreaName = originalTask.pickingAreaName,
            registeredCount = state.registeredCount,
            totalCount = state.totalCount
        )

        // Product information card (unified: slip, name, JAN, volume, capacity, packaging, temperature)
        ProductInfoCard(item = currentItem)

        // Quantity section
        QuantitySection(
            plannedQty = currentItem.plannedQty,
            quantityType = state.quantityTypeLabel,
            pickedQtyInput = state.pickedQtyInput,
            onPickedQtyChange = onPickedQtyChange,
            isUpdating = state.isUpdating
        )
    }
}

/**
 * Course info header per spec:
 * コース: 佐藤 尚紀 (910072)
 * エリア: エリアB  登録: 3/10
 */
@Composable
private fun CourseInfoHeader(
    courseName: String,
    courseCode: String,
    pickingAreaName: String,
    registeredCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "コース: $courseName ($courseCode)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "エリア: $pickingAreaName  登録: $registeredCount/$totalCount",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Unified product info card per spec:
 * 伝票, 商品名(折返し), JAN, 容量, 入数, 包装, 温度帯
 */
@Composable
private fun ProductInfoCard(
    item: PickingTaskItem,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Slip number
            Text(
                text = "伝票: ${String.format("%03d", item.slipNumber)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Product name (wrap)
            Text(
                text = item.itemName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

            // JAN code
            if (item.janCode != null) {
                InfoRow(label = "JAN", value = item.janCode!!, monospace = true)
            }

            // Volume + Capacity on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (item.volume != null) {
                    Text(
                        text = "容量: ${item.volume}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.capacityCase != null) {
                    Text(
                        text = "入数: ${item.capacityCase}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Packaging + Temperature on same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (item.packaging != null) {
                    Text(
                        text = "包装: ${item.packaging}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.temperatureType != null) {
                    Text(
                        text = "温度帯: ${item.temperatureType}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Quantity section per spec:
 * - Planned qty (read-only)
 * - Picking qty (editable input)
 */
@Composable
private fun QuantitySection(
    plannedQty: Double,
    quantityType: String,
    pickedQtyInput: String,
    onPickedQtyChange: (String) -> Unit,
    isUpdating: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Planned quantity (read-only)
            Text(
                text = "予定数量",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = quantityType,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = String.format("%.2f", plannedQty),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            // Picking quantity (editable)
            Text(
                text = "ピッキング数量",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = quantityType,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                OutlinedTextField(
                    value = pickedQtyInput,
                    onValueChange = onPickedQtyChange,
                    enabled = !isUpdating,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, monospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

/**
 * Bottom bar per spec: F1:登録 / F2:戻る / F3:履歴
 */
@Composable
private fun OutboundPickingBottomBar(
    state: OutboundPickingState,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
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
            // F1: 登録
            Button(
                onClick = onRegisterClick,
                enabled = state.canRegister,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("登録(F1)")
                }
            }

            // F2: 戻る
            OutlinedButton(
                onClick = onBackClick,
                enabled = !state.isUpdating,
                modifier = Modifier.weight(1f)
            ) {
                Text("戻る(F2)")
            }

            // F3: 履歴
            OutlinedButton(
                onClick = onHistoryClick,
                enabled = !state.isUpdating,
                modifier = Modifier.weight(1f)
            ) {
                Text("履歴(F3)")
            }
        }
    }
}

/**
 * Completion Confirmation Dialog (per spec P21).
 */
@Composable
private fun CompletionConfirmationDialog(
    isCompleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCompleting) onCancel() },
        title = null,
        text = {
            Text("すべての商品を登録しました。\n出庫処理を確定しますか？")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCompleting
            ) {
                if (isCompleting) {
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
                enabled = !isCompleting
            ) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * Image Viewer Dialog.
 */
@Composable
private fun ImageViewerDialog(
    images: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("商品画像") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (images.isEmpty()) {
                    Text(
                        text = "画像が登録されていません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "画像URL: ${images.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}
