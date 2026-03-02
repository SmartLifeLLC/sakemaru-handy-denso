package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import biz.smt_life.android.core.domain.model.PickingTaskItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Design colors
private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFF92400E)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber400 = Color(0xFFFBBF24)
private val Amber600 = Color(0xFFD97706)
private val Amber700 = Color(0xFFB45309)
private val Amber900 = Color(0xFF78350F)
private val OrangeBg = Color(0xFFFFF7ED)
private val OrangeBorder = Color(0xFFFED7AA)
private val Orange600 = Color(0xFFEA580C)
private val Orange800 = Color(0xFF9A3412)
private val BlueBg = Color(0xFFEFF6FF)
private val BlueBorder = Color(0xFFBFDBFE)
private val Blue600 = Color(0xFF2563EB)
private val Blue800 = Color(0xFF1E40AF)
private val Neutral100 = Color(0xFFF5F5F4)
private val Neutral200 = Color(0xFFE5E7EB)
private val Neutral300 = Color(0xFFD4D4D8)
private val Neutral400 = Color(0xFFA1A1AA)
private val Neutral500 = Color(0xFF6B7280)
private val Neutral600 = Color(0xFF4B5563)
private val Neutral700 = Color(0xFF374151)
private val Neutral800 = Color(0xFF262626)
private val Neutral900 = Color(0xFF171717)
private val Purple700 = Color(0xFF7C3AED)
private val Emerald700 = Color(0xFF047857)
private val Blue700 = Color(0xFF1D4ED8)
private val Red700 = Color(0xFFB91C1C)

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

    LaunchedEffect(task.taskId) {
        viewModel.initialize(task)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    if (state.showCompletionDialog) {
        CompletionConfirmationDialog(
            isCompleting = state.isCompleting,
            onConfirm = { viewModel.completeTask(onSuccess = onTaskCompleted) },
            onCancel = {
                viewModel.dismissCompletionDialog()
                onNavigateToHistory()
            }
        )
    }

    if (state.showImageDialog && state.currentItem != null) {
        ImageViewerDialog(
            images = state.currentItem!!.images,
            onDismiss = { viewModel.dismissImageDialog() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Neutral100
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Amber header
            DataInputHeader(state = state)

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.currentItem != null && state.originalTask != null -> {
                        DataInputContent(
                            state = state,
                            onInputCasesChange = viewModel::onInputCasesChange,
                            onInputPiecesChange = viewModel::onInputPiecesChange
                        )
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("商品がありません")
                        }
                    }
                }
            }

            // Footer
            DataInputFooter(
                state = state,
                onBackClick = onNavigateBack,
                onImageClick = { viewModel.showImageDialog() },
                onRegisterClick = { viewModel.registerCurrentItem() },
                onHistoryClick = onNavigateToHistory
            )
        }
    }
}

@Composable
private fun DataInputHeader(state: OutboundPickingState) {
    val currentItem = state.currentItem
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberBg)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("出庫", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AmberText)
            Text(
                text = today,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Amber700,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .background(Amber100, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // Slip number
        if (currentItem != null) {
            Text(
                text = String.format("%03d", currentItem.slipNumber),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AmberText,
                fontFamily = FontFamily.Monospace
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(AmberBorder))
}

@Composable
private fun DataInputContent(
    state: OutboundPickingState,
    onInputCasesChange: (String) -> Unit,
    onInputPiecesChange: (String) -> Unit
) {
    val currentItem = state.currentItem!!
    val originalTask = state.originalTask!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Course row: コース名 + 作業進行(X/Y)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlueBg, RoundedCornerShape(4.dp))
                .border(1.dp, BlueBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = originalTask.courseName,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Blue800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${state.registeredCount}/${state.totalCount}",
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Blue600,
                fontFamily = FontFamily.Monospace
            )
        }

        // Product info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberBg, RoundedCornerShape(8.dp))
                .border(1.dp, AmberBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = currentItem.itemName,
                fontSize = 16.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Amber900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (currentItem.janCode != null) {
                Text(
                    text = currentItem.janCode!!,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp),
                    lineHeight = 12.sp,
                    color = Amber700
                )
            }
            val details = listOfNotNull(
                currentItem.volume,
                currentItem.capacityCase?.let { "${it}本入" },
                currentItem.packaging
            ).joinToString(" / ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    color = Amber700
                )
            }
            // 得意先名
            Text(
                text = "得意先名：",
                fontSize = 12.sp,
                lineHeight = 12.sp,
                color = Amber700
            )
        }

        // Location info (moved here: between product info and quantity input)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangeBg, RoundedCornerShape(4.dp))
                .border(1.dp, OrangeBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ロケーション", fontSize = 10.sp, color = Orange600)
            Text(
                text = "---",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Orange800,
                fontFamily = FontFamily.Monospace
            )
        }

        // Quantity input section (reference image layout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Neutral200, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ケース input
            Column {
                Text(
                    text = "ケース（受注数：${state.orderCases}）",
                    fontSize = 12.sp,
                    color = Neutral700,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.dp, Neutral300, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = state.inputCases,
                        onValueChange = onInputCasesChange,
                        enabled = !state.isUpdating,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            // バラ input
            Column {
                Text(
                    text = "バラ（受注数：${state.orderPieces}）",
                    fontSize = 12.sp,
                    color = Neutral700,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.dp, Neutral300, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = state.inputPieces,
                        onValueChange = onInputPiecesChange,
                        enabled = !state.isUpdating,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }

        }
    }
}

@Composable
private fun DataInputFooter(
    state: OutboundPickingState,
    onBackClick: () -> Unit,
    onImageClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Neutral900)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FooterButton("戻る", "F4", Neutral600, Color(0xFFD4D4D4), onClick = onBackClick, modifier = Modifier.weight(1f))
        FooterButton("画像", "F3", Purple700, Color(0xFFC4B5FD), enabled = state.hasImages && !state.isUpdating, onClick = onImageClick, modifier = Modifier.weight(1f))
        FooterButton(
            label = if (state.isUpdating) "..." else "登録",
            keyHint = "F1",
            backgroundColor = Emerald700,
            keyColor = Color(0xFF6EE7B7),
            enabled = state.canRegister,
            onClick = onRegisterClick,
            modifier = Modifier.weight(1f)
        )
        FooterButton("履歴", "F2", Blue700, Color(0xFF93C5FD), enabled = !state.isUpdating, onClick = onHistoryClick, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FooterButton(
    label: String,
    keyHint: String,
    backgroundColor: Color,
    keyColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Text(keyHint, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = keyColor.copy(alpha = if (enabled) 1f else 0.4f))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = if (enabled) 1f else 0.4f))
    }
}

@Composable
private fun CompletionConfirmationDialog(
    isCompleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isCompleting) onCancel() },
        title = null,
        text = { Text("すべての商品を登録しました。\n出庫処理を確定しますか？") },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isCompleting) {
                if (isCompleting) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("確定")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isCompleting) { Text("キャンセル") }
        }
    )
}

@Composable
private fun ImageViewerDialog(images: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("商品画像") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (images.isEmpty()) {
                    Text("画像が登録されていません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("画像URL: ${images.first()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}
