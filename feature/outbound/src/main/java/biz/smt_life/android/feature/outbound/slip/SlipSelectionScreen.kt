package biz.smt_life.android.feature.outbound.slip

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.OutboundSlip

// ── ルート（ViewModel接続） ────────────────────────────────────────────────

@Composable
fun SlipSelectionRoute(
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
    onConfirm: (String?) -> Unit,
    viewModel: SlipSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SlipSelectionScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToMain = onNavigateToMain,
        onSlipSelected = viewModel::selectSlip,
        onConfirm = { viewModel.confirmSelection(onConfirm) }
    )
}

// ── メイン画面 ────────────────────────────────────────────────────────────

@Composable
fun SlipSelectionScreen(
    state: SlipSelectionState,
    onNavigateBack: () -> Unit,
    onNavigateToMain: () -> Unit,
    onSlipSelected: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.F1 -> { onConfirm(); true }
                        Key.F4 -> { SoundUtils.playBeep(); onNavigateBack(); true }
                        Key.F8 -> { SoundUtils.playBeep(); onNavigateToMain(); true }
                        else -> false
                    }
                } else false
            }
    ) {
        SlipHeader(
            title = "伝票選択",
            onBack = { SoundUtils.playBeep(); onNavigateBack() },
            onMain = { SoundUtils.playBeep(); onNavigateToMain() }
        )

        SlipGuideText(text = "伝票を選択してください")

        if (state.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(state.slips, key = { it.id }) { slip ->
                    SlipCardItem(
                        slip = slip,
                        isSelected = state.selectedSlipId == slip.id,
                        onClick = { onSlipSelected(slip.id) }
                    )
                }
                item {
                    NoSlipCardItem(
                        isSelected = state.selectedSlipId == NO_SLIP_ID,
                        onClick = { onSlipSelected(NO_SLIP_ID) }
                    )
                }
            }
        }

        SlipConfirmFooter(onConfirm = onConfirm)
    }
}

// ── ヘッダー ──────────────────────────────────────────────────────────────

@Composable
private fun SlipHeader(
    title: String,
    onBack: () -> Unit,
    onMain: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF1A233A))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SlipHeaderButton(text = "戻る\n[F4]", onClick = onBack)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        SlipHeaderButton(text = "メイン\n[F8]", onClick = onMain)
    }
}

@Composable
private fun SlipHeaderButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "headerBtnScale"
    )
    val bottomBorder by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 5.dp,
        label = "headerBtnBottom"
    )
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color.White,
            0.3f to Color.White,
            1.0f to Color(0xFFB0B8C1)
        )
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .widthIn(min = 65.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF6C757D))
    ) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = bottomBorder)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (isPressed) Modifier.background(Color(0xFFA0A8B1))
                    else Modifier.background(gradient)
                )
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .padding(vertical = 4.dp, horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212529),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── 案内テキスト ──────────────────────────────────────────────────────────

@Composable
private fun SlipGuideText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .drawBehind {
                drawLine(
                    color = Color(0xFFDDDDDD),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212529),
            textAlign = TextAlign.Center
        )
    }
}

// ── 伝票カード ────────────────────────────────────────────────────────────

@Composable
private fun SlipCardItem(
    slip: OutboundSlip,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "slipCardScale"
    )
    val unprocessed = slip.total - slip.done

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(cardScale)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFFFFF5F5) else Color.White)
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFFE74C3C) else Color(0xFFCCCCCC),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = slip.slipNumber,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212529),
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("仕入先:", fontSize = 12.sp, color = Color(0xFF666666))
                    Text(
                        text = slip.customerName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("予定日:", fontSize = 12.sp, color = Color(0xFF666666))
                    Text(
                        text = slip.outboundDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("未処理:", fontSize = 12.sp, color = Color(0xFF666666))
                    Text(
                        text = "$unprocessed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFE74C3C)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── 伝票無しカード ────────────────────────────────────────────────────────

@Composable
private fun NoSlipCardItem(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "noSlipCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(cardScale)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFFFFF5F5) else Color(0xFFEEEEEE))
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFFE74C3C) else Color(0xFFCCCCCC),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null) { onClick() }
                .heightIn(min = 50.dp)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "伝票無し",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212529),
                textAlign = TextAlign.Center
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopStart)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFE74C3C)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── フッター（確定ボタン） ────────────────────────────────────────────────

@Composable
private fun SlipConfirmFooter(onConfirm: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "confirmBtnScale"
    )
    val bottomBorder by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 8.dp,
        label = "confirmBtnBottom"
    )
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color.White,
            0.3f to Color.White,
            1.0f to Color(0xFFB0B8C1)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .drawBehind {
                drawLine(
                    color = Color(0xFFCCCCCC),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2196F3))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 3.dp, end = 3.dp, top = 3.dp, bottom = bottomBorder)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF6C757D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isPressed) Modifier.background(Color(0xFFA0A8B1))
                            else Modifier.background(gradient)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onConfirm() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "確定",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212529)
                    )
                    Text(
                        text = " [F1]",
                        fontSize = 14.sp,
                        color = Color(0xFF444444)
                    )
                }
            }
        }
    }
}
