package biz.smt_life.android.feature.outbound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.OutboundCourse
import biz.smt_life.android.core.domain.model.OutboundCourseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundEntryScreen(
    courseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    viewModel: OutboundViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load course data when screen first appears
    LaunchedEffect(courseId) {
        if (state.selectedCourseId != courseId) {
            viewModel.selectCourse(courseId)
        }
    }

    // Show error/success messages
    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.selectedCourse?.let { course ->
                            "${course.courseName} (${state.currentItemIndex + 1}/${course.items.size})"
                        } ?: "出庫データ入力"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToHistory(courseId) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "履歴"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            OutboundEntryBottomBar(
                state = state,
                onPrevClick = viewModel::moveToPrevItem,
                onNextClick = viewModel::moveToNextItem,
                onPrimaryAction = {
                    when {
                        state.canUnconfirm -> viewModel.unconfirmCourse()
                        state.canConfirm -> viewModel.confirmCourse()
                        state.canRegister -> viewModel.registerItem()
                    }
                }
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
            state.currentItem != null && state.selectedCourse != null -> {
                OutboundEntryContent(
                    item = state.currentItem!!,
                    course = state.selectedCourse!!,
                    editQtyCase = state.editQtyCase,
                    editQtyEach = state.editQtyEach,
                    onQtyCaseChange = viewModel::onQtyCaseChange,
                    onQtyEachChange = viewModel::onQtyEachChange,
                    isProcessing = state.isProcessing,
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
                    Text("アイテムがありません")
                }
            }
        }
    }
}

@Composable
private fun OutboundEntryContent(
    item: OutboundCourseItem,
    course: OutboundCourse,
    editQtyCase: String,
    editQtyEach: String,
    onQtyCaseChange: (String) -> Unit,
    onQtyEachChange: (String) -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Item Header Card
        ItemHeaderCard(item = item)

        // Quantities Section
        QuantitiesSection(
            item = item,
            editQtyCase = editQtyCase,
            editQtyEach = editQtyEach,
            onQtyCaseChange = onQtyCaseChange,
            onQtyEachChange = onQtyEachChange,
            isProcessing = isProcessing
        )

        // Registration Status
        if (item.isRegistered) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "登録済み",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Course Progress
        CourseProgressCard(course = course)
    }
}

@Composable
private fun ItemHeaderCard(
    item: OutboundCourseItem,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Item Name
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Customer Name
            Text(
                text = "得意先: ${item.customerName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Specs Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "容量", value = item.capacity)
                InfoItem(label = "入数", value = "${item.packSize}個")
            }

            // Specs Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "JANコード", value = item.jan ?: "－")
            }

            HorizontalDivider()

            // Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "エリア", value = item.area)
                InfoItem(label = "ロケ", value = item.location)
            }

            // Image Button (if available)
            if (item.hasImage) {
                OutlinedButton(
                    onClick = { /* TODO: Show image */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("商品画像を見る")
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
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
private fun QuantitiesSection(
    item: OutboundCourseItem,
    editQtyCase: String,
    editQtyEach: String,
    onQtyCaseChange: (String) -> Unit,
    onQtyEachChange: (String) -> Unit,
    isProcessing: Boolean,
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

            // Order Quantities (Read-only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "発注ケース",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${item.orderQtyCase}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                OutlinedCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "発注バラ",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${item.orderQtyEach}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider()

            // Outbound Quantities (Editable)
            val isEditable = !item.isRegistered && !isProcessing

            Text(
                text = "出庫数量",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editQtyCase,
                    onValueChange = onQtyCaseChange,
                    label = { Text("ケース") },
                    enabled = isEditable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = editQtyEach,
                    onValueChange = onQtyEachChange,
                    label = { Text("バラ") },
                    enabled = isEditable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CourseProgressCard(
    course: OutboundCourse,
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
                    text = "コース進捗",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${course.processedCount} / ${course.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { course.processedCount.toFloat() / course.totalCount.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            if (course.isConfirmed) {
                Text(
                    text = "✓ 確定済み",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else if (course.isComplete) {
                Text(
                    text = "すべてのアイテムが登録されました",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun OutboundEntryBottomBar(
    state: OutboundUiState,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrimaryAction: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Previous Button
            OutlinedButton(
                onClick = onPrevClick,
                enabled = state.canMovePrev && !state.isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Text("前へ")
            }

            // Primary Action Button (Register / Confirm / Unconfirm)
            Button(
                onClick = onPrimaryAction,
                enabled = (state.canRegister || state.canConfirm || state.canUnconfirm) && !state.isProcessing,
                modifier = Modifier.weight(2f)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(state.primaryButtonLabel)
                }
            }

            // Next Button
            OutlinedButton(
                onClick = onNextClick,
                enabled = state.canMoveNext && !state.isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Text("次へ")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutboundEntryScreenPreview() {
    MaterialTheme {
        val sampleItem = OutboundCourseItem(
            id = "1",
            itemCode = "ITEM001",
            itemName = "サンプル商品名",
            customerName = "アミナ南越谷様",
            capacity = "1.8L",
            packSize = 6,
            jan = "4901234567890",
            area = "A-1",
            location = "R01-S02",
            orderQtyCase = 5,
            orderQtyEach = 3,
            outboundQtyCase = 0,
            outboundQtyEach = 0,
            isRegistered = false,
            imageUrl = "https://example.com/image.jpg"
        )

        val sampleCourse = OutboundCourse(
            id = "COURSE_A",
            courseName = "コースA",
            isMyAssignment = true,
            items = listOf(sampleItem),
            isConfirmed = false
        )

        OutboundEntryContent(
            item = sampleItem,
            course = sampleCourse,
            editQtyCase = "5",
            editQtyEach = "3",
            onQtyCaseChange = {},
            onQtyEachChange = {},
            isProcessing = false,
            modifier = Modifier
        )
    }
}
