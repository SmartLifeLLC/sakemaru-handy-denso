package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask

/**
 * Outbound Picking Screen (2.5.2 - 出庫データ入力).
 * Displays current picking item and allows quantity input.
 *
 * @param task The picking task to work with
 * @param onNavigateBack Navigate back to course list or previous screen
 * @param onNavigateToCourseList Navigate back to course list (コース(F6) button)
 * @param onNavigateToHistory Navigate to picking history (履歴(F7) button)
 * @param onNavigateToMain Navigate to main menu (ホーム(F8) button)
 * @param onTaskCompleted Callback when task is successfully completed (確定)
 * @param viewModel ViewModel for this screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundPickingScreen(
    task: PickingTask,
    onNavigateBack: () -> Unit,
    onNavigateToCourseList: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMain: () -> Unit,
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
                title = { Text("出庫データ入力") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る(F4)"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToMain) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "ホーム(F8)"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            OutboundPickingBottomBar(
                state = state,
                onPrevClick = viewModel::moveToPrevItem,
                onNextClick = viewModel::moveToNextItem,
                onRegisterClick = viewModel::registerCurrentItem,
                onImageClick = { viewModel.showImageDialog() },
                onCourseClick = onNavigateToCourseList,
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
            state.currentItem != null && state.task != null -> {
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
    val task = state.task!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Course Header (use counters from state, not filtered task)
        CourseHeaderCard(
            courseName = "${task.courseName}",
            pickingAreaName = task.pickingAreaName,
            registeredCount = state.registeredCount, // From originalTask, not filtered
            totalCount = state.totalCount             // From originalTask, not filtered
        )

        // Item Information Card
        ItemInformationCard(
            itemName = currentItem.itemName,
            slipNumber = currentItem.slipNumber.toString()
        )

        // Quantity Input Card
        QuantityInputCard(
            plannedQty = currentItem.plannedQty,
            quantityType = state.quantityTypeLabel,
            pickedQtyInput = state.pickedQtyInput,
            onPickedQtyChange = onPickedQtyChange,
            isUpdating = state.isUpdating,
            formatQuantity = state::formatQuantity
        )

        // Product details card (容量, 入数, JAN)
        ProductDetailsCard(currentItem)
    }
}

@Composable
private fun CourseHeaderCard(
    courseName: String,
    pickingAreaName: String,
    registeredCount: Int,  // Changed from processedCount
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "コース",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$registeredCount / $totalCount",  // Use registeredCount
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = courseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "エリア: $pickingAreaName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { if (totalCount > 0) registeredCount.toFloat() / totalCount.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ItemInformationCard(
    itemName: String,
    slipNumber: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "商品",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "伝票番号",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = slipNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuantityInputCard(
    plannedQty: Double,
    quantityType: String,
    pickedQtyInput: String,
    onPickedQtyChange: (String) -> Unit,
    isUpdating: Boolean,
    formatQuantity: (Double, String) -> String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "数量",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Planned Quantity (Read-only)
            OutlinedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "発注数量",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = String.format("%.1f %s", plannedQty, quantityType),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider()

            // Picked Quantity (Editable)
            Text(
                text = "出庫数量",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = pickedQtyInput,
                onValueChange = onPickedQtyChange,
                label = { Text("出庫数量 ($quantityType)") },
                enabled = !isUpdating,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("数量を入力してください。不足の場合は0を入力。")
                }
            )
        }
    }
}

@Composable
private fun ProductDetailsCard(
    item: biz.smt_life.android.core.domain.model.PickingTaskItem,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "商品情報",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            // Volume (容量)
            if (item.volume != null) {
                InfoRow(label = "容量", value = item.volume!!)
            } else {
                InfoRow(label = "容量", value = "—")
            }

            // Capacity per case (入数)
            if (item.capacityCase != null) {
                InfoRow(label = "入数", value = "${item.capacityCase} 個/ケース")
            } else {
                InfoRow(label = "入数", value = "—")
            }

            // JAN code
            if (item.janCode != null) {
                InfoRow(label = "JAN", value = item.janCode!!)
            } else {
                InfoRow(label = "JAN", value = "—")
            }
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
private fun OutboundPickingBottomBar(
    state: OutboundPickingState,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onImageClick: () -> Unit,
    onCourseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: Register, Prev, Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 登録(F1)
                Button(
                    onClick = onRegisterClick,
                    enabled = state.canRegister,
                    modifier = Modifier.weight(2f)
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

                // 前へ(F2)
                OutlinedButton(
                    onClick = onPrevClick,
                    enabled = state.canMovePrev && !state.isUpdating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("前へ(F2)")
                }

                // 次へ(F3)
                OutlinedButton(
                    onClick = onNextClick,
                    enabled = state.canMoveNext && !state.isUpdating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("次へ(F3)")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Image, Course, History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 画像(F5) - Show image viewer if images are available
                OutlinedButton(
                    onClick = onImageClick,
                    enabled = state.hasImages && !state.isUpdating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("画像(F5)")
                }

                // コース(F6)
                OutlinedButton(
                    onClick = onCourseClick,
                    enabled = !state.isUpdating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("コース(F6)")
                }

                // 履歴(F7)
                OutlinedButton(
                    onClick = onHistoryClick,
                    enabled = !state.isUpdating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("履歴(F7)")
                }
            }
        }
    }
}

/**
 * Completion Confirmation Dialog (per spec 2.5.2).
 * Message: すべての商品登録を完了しました。確定しますか？
 * Buttons: 確定 (complete task) / キャンセル (navigate to history)
 */
@Composable
private fun CompletionConfirmationDialog(
    isCompleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCompleting) onCancel() },
        title = { Text("完了確認") },
        text = { Text("すべての商品登録を完了しました。確定しますか？") },
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
 * Image Viewer Dialog (画像 F5).
 * Shows product images from the server in a simple dialog.
 * For now, displays the first image. Can be enhanced to show thumbnails/carousel.
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
                    // Display first image
                    // Note: In production, use Coil or Glide to load images from URL
                    Text(
                        text = "画像URL: ${images.first()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "画像の表示にはCoilまたはGlideの実装が必要です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    // TODO: Implement image loading with Coil
                    // AsyncImage(
                    //     model = images.first(),
                    //     contentDescription = "商品画像",
                    //     modifier = Modifier
                    //         .fillMaxWidth()
                    //         .heightIn(max = 400.dp)
                    // )
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
