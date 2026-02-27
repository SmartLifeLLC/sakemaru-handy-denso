package biz.smt_life.android.feature.outbound.picking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Design colors
private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFF92400E)
private val Amber400 = Color(0xFFFBBF24)
private val Amber700 = Color(0xFFB45309)
private val Amber900 = Color(0xFF78350F)
private val Neutral100 = Color(0xFFF5F5F4)
private val Neutral200 = Color(0xFFE5E7EB)
private val Neutral300 = Color(0xFFD4D4D8)
private val Neutral400 = Color(0xFFA1A1AA)
private val Neutral500 = Color(0xFF6B7280)
private val Neutral600 = Color(0xFF4B5563)
private val Neutral700 = Color(0xFF374151)
private val Neutral800 = Color(0xFF262626)
private val Neutral900 = Color(0xFF171717)
private val Emerald700 = Color(0xFF047857)
private val Red700 = Color(0xFFB91C1C)
private val Blue700 = Color(0xFF1D4ED8)

/**
 * Edit Picking Screen (出庫検品 編集).
 *
 * Allows editing ケース/バラ quantities for a single history item.
 * No delete button per user requirement.
 */
@Composable
fun EditPickingScreen(
    itemResultId: Int,
    taskId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: EditPickingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(itemResultId, taskId) {
        viewModel.initialize(itemResultId, taskId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
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
            EditHeader()

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.item != null -> {
                        EditContent(
                            state = state,
                            onInputCasesChange = viewModel::onInputCasesChange,
                            onInputPiecesChange = viewModel::onInputPiecesChange
                        )
                    }
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("商品が見つかりません", color = Neutral500)
                        }
                    }
                }
            }

            // Footer
            EditFooter(
                state = state,
                onBackClick = onNavigateBack,
                onHistoryBackClick = onNavigateToHistory,
                onSaveClick = { viewModel.saveChanges(onSuccess = onSaveSuccess) },
                onListClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun EditHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberBg)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("出庫", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AmberText)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AmberBorder)
    )
}

@Composable
private fun EditContent(
    state: EditPickingState,
    onInputCasesChange: (String) -> Unit,
    onInputPiecesChange: (String) -> Unit
) {
    val item = state.item!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Section title
        Text(
            text = "出庫検品 編集",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Neutral600,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // Product info card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmberBg, RoundedCornerShape(8.dp))
                .border(1.dp, AmberBorder, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = item.itemName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Amber900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val janCode = item.janCode
            if (janCode != null) {
                Text(
                    text = janCode,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Neutral800,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                text = "伝票${String.format("%03d", item.slipNumber)}",
                fontSize = 10.sp,
                color = Amber700,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Quantity section: 受注数 / 出荷数
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Neutral200, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 受注数 (read-only)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "受注数",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Neutral500,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                // ケース
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Neutral100, RoundedCornerShape(4.dp))
                        .border(1.dp, Neutral300, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ケース", fontSize = 9.sp, color = Neutral400)
                    Text(
                        text = "${state.orderCases}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Neutral700
                    )
                }
                Spacer(Modifier.height(2.dp))
                // バラ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Neutral100, RoundedCornerShape(4.dp))
                        .border(1.dp, Neutral300, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("バラ", fontSize = 9.sp, color = Neutral400)
                    Text(
                        text = "${state.orderPieces}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Neutral700
                    )
                }
            }

            // 出荷数 (editable)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "出荷数",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber700,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                // ケース input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .border(1.dp, Amber400, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "ケース",
                        fontSize = 9.sp,
                        color = Amber400,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    BasicTextField(
                        value = state.inputCases,
                        onValueChange = onInputCasesChange,
                        enabled = !state.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                // バラ input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .border(1.dp, Amber400, RoundedCornerShape(4.dp))
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "バラ",
                        fontSize = 9.sp,
                        color = Amber400,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    BasicTextField(
                        value = state.inputPieces,
                        onValueChange = onInputPiecesChange,
                        enabled = !state.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp)
                    )
                }
            }
        }

        // NO delete button (per user requirement)
    }
}

@Composable
private fun EditFooter(
    state: EditPickingState,
    onBackClick: () -> Unit,
    onHistoryBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onListClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Neutral900)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FooterButton(
            label = "戻る",
            keyHint = "F4",
            backgroundColor = Neutral600,
            keyColor = Color(0xFFD4D4D4),
            onClick = onBackClick,
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = "戻る",
            keyHint = "F3",
            backgroundColor = Red700,
            keyColor = Color(0xFFFCA5A5),
            onClick = onHistoryBackClick,
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = if (state.isSaving) "..." else "保存",
            keyHint = "F1",
            backgroundColor = Emerald700,
            keyColor = Color(0xFF6EE7B7),
            enabled = state.canSave,
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = "一覧",
            keyHint = "F2",
            backgroundColor = Blue700,
            keyColor = Color(0xFF93C5FD),
            onClick = onListClick,
            modifier = Modifier.weight(1f)
        )
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
        Text(
            text = keyHint,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = keyColor.copy(alpha = if (enabled) 1f else 0.4f)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}
