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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTaskItem
import biz.smt_life.android.core.domain.model.QuantityType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Design colors
private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFF92400E)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber600 = Color(0xFFD97706)
private val Amber700 = Color(0xFFB45309)
private val Neutral100 = Color(0xFFF5F5F4)
private val Neutral200 = Color(0xFFE5E7EB)
private val Neutral400 = Color(0xFFA1A1AA)
private val Neutral500 = Color(0xFF6B7280)
private val Neutral600 = Color(0xFF4B5563)
private val Neutral900 = Color(0xFF171717)
private val Emerald600 = Color(0xFF059669)
private val Emerald700 = Color(0xFF047857)
private val Red600 = Color(0xFFDC2626)
private val Red700 = Color(0xFFB91C1C)
private val Blue700 = Color(0xFF1D4ED8)

/**
 * Picking History Screen (P22 - 出庫処理＞履歴).
 *
 * Shows registered items with 受注/出荷 comparison in ケース/バラ format.
 * Items are clickable to navigate to the edit screen.
 * Color-coded: emerald for match, red for shortage, amber for excess.
 */
@Composable
fun PickingHistoryScreen(
    taskId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (itemResultId: Int) -> Unit,
    onHistoryConfirmed: () -> Unit,
    viewModel: PickingHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskId) {
        viewModel.initialize(taskId)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    // Confirm-all dialog
    if (state.showConfirmDialog) {
        ConfirmAllDialog(
            pickingItemCount = state.pickingItemCount,
            isConfirming = state.isConfirming,
            onConfirm = { viewModel.confirmAll(onSuccess = onHistoryConfirmed) },
            onCancel = { viewModel.dismissConfirmDialog() }
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
            HistoryHeader()

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.task == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("タスクが見つかりません", color = Neutral500)
                        }
                    }
                    state.historyItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("履歴なし", fontSize = 11.sp, color = Neutral500)
                        }
                    }
                    else -> {
                        HistoryListContent(
                            state = state,
                            onItemClick = { item -> onNavigateToEdit(item.id) }
                        )
                    }
                }
            }

            // Footer
            HistoryFooter(
                state = state,
                onBackClick = onNavigateBack,
                onConfirmClick = { viewModel.showConfirmDialog() },
                onListClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun HistoryHeader() {
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
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AmberBorder)
    )
}

@Composable
private fun HistoryListContent(
    state: PickingHistoryState,
    onItemClick: (PickingTaskItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header text
        item {
            Text(
                text = "履歴（${state.historyItems.size}件）",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Neutral600,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        items(state.historyItems, key = { it.id }) { item ->
            HistoryItemCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    item: PickingTaskItem,
    onClick: () -> Unit
) {
    // Compute ケース/バラ for order
    val orderCases: Int
    val orderPieces: Int
    when (item.plannedQtyType) {
        QuantityType.CASE -> {
            orderCases = item.plannedQty.toInt()
            orderPieces = 0
        }
        QuantityType.PIECE -> {
            val cf = item.capacityCase
            if (cf != null && cf > 0) {
                orderCases = (item.plannedQty / cf).toInt()
                orderPieces = (item.plannedQty.toInt() % cf)
            } else {
                orderCases = 0
                orderPieces = item.plannedQty.toInt()
            }
        }
    }

    // Compute ケース/バラ for shipped
    val shippedCases: Int
    val shippedPieces: Int
    val cf = item.capacityCase
    if (cf != null && cf > 0) {
        shippedCases = (item.pickedQty / cf).toInt()
        shippedPieces = (item.pickedQty.toInt() % cf)
    } else {
        shippedCases = 0
        shippedPieces = item.pickedQty.toInt()
    }

    // Compare totals for color
    val orderTotal = when (item.plannedQtyType) {
        QuantityType.CASE -> item.plannedQty * (item.capacityCase ?: 1)
        QuantityType.PIECE -> item.plannedQty
    }
    val shippedTotal = item.pickedQty
    val shippedColor = when {
        shippedTotal == orderTotal -> Emerald600
        shippedTotal < orderTotal -> Red600
        else -> Amber600
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Neutral200, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        // Row 1: Product name + chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.itemName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("\u25B6", fontSize = 12.sp, color = Neutral400)
        }

        // Row 2: Code + SKU info
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val janCode = item.janCode
            if (janCode != null) {
                Text(
                    text = janCode,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Neutral500
                )
            }
            val sku = listOfNotNull(
                item.volume,
                item.capacityCase?.let { "${it}本入" }
            ).joinToString("/")
            if (sku.isNotEmpty()) {
                Text(text = sku, fontSize = 9.sp, color = Neutral400)
            }
        }

        Spacer(Modifier.height(4.dp))

        // 受注 row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "受注",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Neutral500
            )
            Text(
                text = "${orderCases}ケース ${orderPieces}バラ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Neutral500
            )
        }

        // 出荷 row (color-coded)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "出荷",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = shippedColor
            )
            Text(
                text = "${shippedCases}ケース ${shippedPieces}バラ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = shippedColor
            )
        }
    }
}

@Composable
private fun HistoryFooter(
    state: PickingHistoryState,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
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
            label = "前へ",
            keyHint = "F3",
            backgroundColor = Red700,
            keyColor = Color(0xFFFCA5A5),
            enabled = false,
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = if (state.isConfirming) "..." else "送信",
            keyHint = "F1",
            backgroundColor = Emerald700,
            keyColor = Color(0xFF6EE7B7),
            enabled = state.canConfirmAll,
            onClick = onConfirmClick,
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
            Button(onClick = onConfirm, enabled = !isConfirming) {
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
            TextButton(onClick = onCancel, enabled = !isConfirming) { Text("キャンセル") }
        }
    )
}
