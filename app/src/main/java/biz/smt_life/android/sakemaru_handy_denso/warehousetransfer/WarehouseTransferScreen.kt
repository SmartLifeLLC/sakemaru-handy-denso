package biz.smt_life.android.sakemaru_handy_denso.warehousetransfer

import android.content.Context
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.network.model.WarehouseTransferWarehouse
import biz.smt_life.android.core.ui.HardwareKeyHandler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val transferSyncTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")

private enum class TransferQuantityInputTarget { CASE, PIECE }

private val PrimaryBlue = Color(0xFF1976D2)
private val AccentOrange = Color(0xFFE65100)
private val AccentGreen = Color(0xFF2E7D32)

/**
 * 倉庫移動（メインメニュー 移動[3]）
 *
 * 操作感は棚卸し画面に合わせる。
 * F1: 符号切替 / F2: 戻る / F3: 入力確定 or データ同期 / F4: 履歴 or 送信
 */
@Composable
fun WarehouseTransferScreen(
    onNavigateBack: () -> Unit,
    viewModel: WarehouseTransferViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var showResetDialog by remember { mutableStateOf(false) }
    var lastF2BackAt by remember { mutableStateOf(0L) }
    var quantityInputTarget by remember { mutableStateOf(TransferQuantityInputTarget.PIECE) }

    fun handleHardwareKey(keyCode: Int): Boolean {
        if (state.destinationDialogVisible) {
            return when (keyCode) {
                AndroidKeyEvent.KEYCODE_F2 -> {
                    SoundUtils.playTick()
                    viewModel.dismissDestinationDialog()
                    true
                }
                AndroidKeyEvent.KEYCODE_F4 -> {
                    SoundUtils.playTick()
                    viewModel.submit()
                    true
                }
                else -> false
            }
        }
        return when (keyCode) {
            AndroidKeyEvent.KEYCODE_F1 -> {
                if (state.selectedItem == null) {
                    false
                } else {
                    SoundUtils.playTick()
                    when (quantityInputTarget) {
                        TransferQuantityInputTarget.CASE -> viewModel.toggleCaseQuantitySign()
                        TransferQuantityInputTarget.PIECE -> viewModel.togglePieceQuantitySign()
                    }
                    true
                }
            }
            AndroidKeyEvent.KEYCODE_F2 -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastF2BackAt < 500L) {
                    return true
                }
                lastF2BackAt = now
                SoundUtils.playTick()
                when {
                    state.selectedItem != null -> viewModel.clearSelection()
                    state.selectedTab == WarehouseTransferTab.HISTORY -> viewModel.selectTab(WarehouseTransferTab.SCAN)
                    state.selectedTab != WarehouseTransferTab.MENU -> viewModel.selectTab(WarehouseTransferTab.MENU)
                    else -> onNavigateBack()
                }
                true
            }
            AndroidKeyEvent.KEYCODE_F3 -> {
                if (state.selectedItem != null) {
                    viewModel.confirmInput()
                } else if (state.selectedTab == WarehouseTransferTab.MENU && !state.syncing) {
                    SoundUtils.playTick()
                    viewModel.syncAllItems()
                }
                true
            }
            AndroidKeyEvent.KEYCODE_F4 -> {
                if (state.selectedItem != null) {
                    false
                } else {
                    SoundUtils.playTick()
                    if (state.selectedTab == WarehouseTransferTab.HISTORY) {
                        viewModel.startSubmit()
                    } else {
                        viewModel.selectTab(WarehouseTransferTab.HISTORY)
                    }
                    true
                }
            }
            AndroidKeyEvent.KEYCODE_1,
            AndroidKeyEvent.KEYCODE_NUMPAD_1 -> {
                if (state.selectedTab == WarehouseTransferTab.MENU && state.selectedItem == null) {
                    SoundUtils.playTick()
                    viewModel.selectTab(WarehouseTransferTab.SCAN)
                    true
                } else {
                    false
                }
            }
            AndroidKeyEvent.KEYCODE_2,
            AndroidKeyEvent.KEYCODE_NUMPAD_2 -> {
                if (state.selectedTab == WarehouseTransferTab.MENU && state.selectedItem == null) {
                    SoundUtils.playTick()
                    viewModel.selectTab(WarehouseTransferTab.HISTORY)
                    true
                } else {
                    false
                }
            }
            AndroidKeyEvent.KEYCODE_3,
            AndroidKeyEvent.KEYCODE_NUMPAD_3 -> {
                if (state.selectedTab == WarehouseTransferTab.MENU && state.selectedItem == null) {
                    if (!state.syncing) {
                        SoundUtils.playTick()
                        viewModel.syncAllItems()
                    }
                    true
                } else {
                    false
                }
            }
            AndroidKeyEvent.KEYCODE_4,
            AndroidKeyEvent.KEYCODE_NUMPAD_4 -> {
                if (state.selectedTab == WarehouseTransferTab.MENU && state.selectedItem == null) {
                    if (!state.syncing) {
                        SoundUtils.playTick()
                        showResetDialog = true
                    }
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    HardwareKeyHandler { keyCode, _ -> handleHardwareKey(keyCode) }

    if (showResetDialog) {
        TransferResetConfirmDialog(
            dirtyCount = state.dirtyCount,
            onConfirm = {
                showResetDialog = false
                viewModel.resetLocalData()
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (state.destinationDialogVisible) {
        DestinationSelectDialog(
            state = state,
            onSelect = { SoundUtils.playTick(); viewModel.selectDestination(it) },
            onConfirm = { SoundUtils.playTick(); viewModel.submit() },
            onDismiss = { SoundUtils.playTick(); viewModel.dismissDestinationDialog() }
        )
    }

    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab != WarehouseTransferTab.SCAN) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.F1 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_F1)
                    Key.F2 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_F2)
                    Key.F3 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_F3)
                    Key.F4 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_F4)
                    Key.One, Key.NumPad1 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_1)
                    Key.Two, Key.NumPad2 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_2)
                    Key.Three, Key.NumPad3 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_3)
                    Key.Four, Key.NumPad4 -> handleHardwareKey(AndroidKeyEvent.KEYCODE_4)
                    else -> false
                }
            }
    ) {
        TransferHeader(state, onBack = {
            SoundUtils.playTick()
            when {
                state.selectedItem != null -> viewModel.clearSelection()
                state.selectedTab == WarehouseTransferTab.HISTORY -> viewModel.selectTab(WarehouseTransferTab.SCAN)
                state.selectedTab != WarehouseTransferTab.MENU -> viewModel.selectTab(WarehouseTransferTab.MENU)
                else -> onNavigateBack()
            }
        })

        if (state.loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.selectedItem != null) {
            ItemInputPanel(
                state = state,
                viewModel = viewModel,
                onQuantityInputFocused = { quantityInputTarget = it },
                modifier = Modifier.weight(1f)
            )
        } else when (state.selectedTab) {
            WarehouseTransferTab.MENU -> TransferMenuPanel(
                state,
                viewModel,
                onRequestReset = { showResetDialog = true },
                modifier = Modifier.weight(1f)
            )
            WarehouseTransferTab.SCAN -> Box(modifier = Modifier.weight(1f)) { ScanTab(state, viewModel) }
            WarehouseTransferTab.HISTORY -> Box(modifier = Modifier.weight(1f)) { HistoryTab(state, viewModel) }
        }
    }
}

// ─── Header ───

@Composable
private fun TransferHeader(state: WarehouseTransferState, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(onClick = onBack, color = Color.Transparent, modifier = Modifier.height(32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("← もどる", fontSize = 14.sp, color = PrimaryBlue)
                    Text(" (F2)", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Text(
                "倉庫移動",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.weight(1f).padding(start = 2.dp)
            )
            if (state.dirtyCount > 0) {
                Surface(shape = RoundedCornerShape(12.dp), color = AccentOrange) {
                    Text(
                        "未送信 ${state.dirtyCount}",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

// ─── Menu Panel ───

@Composable
private fun TransferMenuPanel(
    state: WarehouseTransferState,
    viewModel: WarehouseTransferViewModel,
    onRequestReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSyncText = formatTransferSyncTime(state.syncedAt)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "移動元: 自店倉庫（ID ${state.fromWarehouseId}）  在庫 ${state.allItems.size} 件",
            fontSize = 13.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuTile(
                color = PrimaryBlue,
                enabled = true,
                onClick = { SoundUtils.playTick(); viewModel.selectTab(WarehouseTransferTab.SCAN) }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(36.dp), tint = PrimaryBlue)
                Spacer(modifier = Modifier.height(8.dp))
                Text("(1)スキャン", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            MenuTile(
                color = Color(0xFF4CAF50),
                enabled = true,
                onClick = { SoundUtils.playTick(); viewModel.selectTab(WarehouseTransferTab.HISTORY) }
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(8.dp))
                Text("(2)履歴/送信", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                if (state.dirtyCount > 0) {
                    Text("未送信 ${state.dirtyCount}件", fontSize = 13.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MenuTile(
                color = Color(0xFFFF9800),
                enabled = !state.syncing,
                onClick = { SoundUtils.playTick(); viewModel.syncAllItems() }
            ) {
                if (state.syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("取得中...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                } else {
                    Text("(3)在庫", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("同期(F3)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(lastSyncText, fontSize = 12.sp, color = Color.DarkGray)
            }
            MenuTile(
                color = Color.Red,
                enabled = !state.syncing,
                borderColor = Color.Red,
                onClick = { SoundUtils.playTick(); onRequestReset() }
            ) {
                Text("(4)データ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text("初期化", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text("確認あり", fontSize = 13.sp, color = Color.DarkGray)
            }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp))
        }
        state.message?.let {
            Text(it, color = AccentGreen, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp))
        }
    }
}

@Composable
private fun MenuTile(
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    borderColor: Color = Color.LightGray,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(140.dp)
            .aspectRatio(1f)
            .border(1.dp, borderColor, shape),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = shape
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(color)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

// ─── Scan Tab ───

@Composable
private fun ScanTab(state: WarehouseTransferState, viewModel: WarehouseTransferViewModel) {
    val scanFocusRequester = remember { FocusRequester() }
    var scanEditText by remember { mutableStateOf<EditText?>(null) }

    fun refocusScanInput() {
        scanEditText?.post {
            scanEditText?.setShowSoftInputOnFocus(false)
            scanEditText?.requestFocus()
            scanEditText?.setSelection(scanEditText?.text?.length ?: 0)
            scanEditText?.let { hideSoftwareKeyboard(it) }
        }
    }

    LaunchedEffect(state.hasLocalData) {
        if (state.hasLocalData) {
            scanFocusRequester.requestFocus()
            refocusScanInput()
        }
    }

    LaunchedEffect(state.error, state.message, state.selectedItem, scanEditText) {
        if (state.hasLocalData && state.selectedItem == null) {
            refocusScanInput()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!state.hasLocalData) {
            Spacer(modifier = Modifier.weight(1f))
            Text("先に在庫リストを同期してください", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { SoundUtils.playTick(); viewModel.syncAllItems() },
                enabled = !state.syncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.syncing) "取得中..." else "在庫リスト同期")
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            return
        }

        Spacer(modifier = Modifier.height(12.dp))

        TransferNumericEditText(
            value = state.keyword,
            onValueChange = viewModel::setKeyword,
            label = "商品コード / JAN / バーコード",
            placeholder = "商品コード / JAN / バーコード",
            focusRequester = scanFocusRequester,
            isFocusTarget = true,
            selectAllOnFocus = false,
            textSizeSp = 20f,
            onEnter = {
                SoundUtils.playBeep()
                viewModel.scan()
                refocusScanInput()
                true
            },
            onEditTextReady = { scanEditText = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { SoundUtils.playTick(); viewModel.selectTab(WarehouseTransferTab.HISTORY) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("(F4) 履歴/送信", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (state.dirtyCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = AccentOrange) {
                    Text(
                        "${state.dirtyCount}",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = PrimaryBlue)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { SoundUtils.playTick(); viewModel.selectTab(WarehouseTransferTab.MENU) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("戻る", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("移動する商品をスキャンしてください", fontSize = 20.sp, color = Color.DarkGray)

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
        state.message?.let {
            Text(it, color = AccentGreen, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

// ─── Item Input Panel ───

@Composable
private fun ItemInputPanel(
    state: WarehouseTransferState,
    viewModel: WarehouseTransferViewModel,
    onQuantityInputFocused: (TransferQuantityInputTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val item = state.selectedItem ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "${item.volume ?: "-"}${displayVolumeUnit(item.volumeUnitLabel, item.volumeUnit)} × ${state.capacityCase}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Text("${item.itemCode}  ${item.barcode ?: ""}", fontSize = 16.sp, color = Color.DarkGray)
        Text(item.itemName, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        item.location?.locationNo?.takeIf { it.isNotBlank() }?.let {
            Text("ロケ: $it", fontSize = 14.sp, color = Color.Gray)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        val caseEnabled = state.capacityCase > 1
        val availableTotalPieces = item.availableTotalPieces
        val availableCaseQuantity = if (caseEnabled) availableTotalPieces / state.capacityCase else 0
        val availablePieceQuantity = if (caseEnabled) availableTotalPieces % state.capacityCase else availableTotalPieces
        val caseFocus = remember { FocusRequester() }
        val pieceFocus = remember { FocusRequester() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE3F2FD), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("移動元 利用可能在庫", fontSize = 14.sp, color = PrimaryBlue)
                Text(
                    "ケース $availableCaseQuantity | バラ $availablePieceQuantity",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("総バラ", fontSize = 14.sp, color = PrimaryBlue)
                Text("$availableTotalPieces", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)
            }
        }

        LaunchedEffect(item.localKey) {
            when {
                state.scanQuantityType == "CASE" && caseEnabled -> {
                    onQuantityInputFocused(TransferQuantityInputTarget.CASE)
                    caseFocus.requestFocus()
                }
                else -> {
                    onQuantityInputFocused(TransferQuantityInputTarget.PIECE)
                    pieceFocus.requestFocus()
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            TransferNumericEditText(
                value = state.caseQuantity,
                onValueChange = {
                    viewModel.setCaseQuantity(it)
                    if (it != state.caseQuantity) SoundUtils.playTick()
                },
                label = "ケース",
                enabled = caseEnabled,
                focusRequester = caseFocus,
                isFocusTarget = state.scanQuantityType == "CASE" && caseEnabled,
                textSizeSp = 24f,
                textBold = true,
                allowNegative = true,
                onFocused = { onQuantityInputFocused(TransferQuantityInputTarget.CASE) },
                onToggleNegative = viewModel::toggleCaseQuantitySign,
                onMoveNext = {
                    pieceFocus.requestFocus()
                    true
                },
                modifier = Modifier.weight(1f)
            )
            TransferNumericEditText(
                value = state.pieceQuantity,
                onValueChange = {
                    viewModel.setPieceQuantity(it)
                    if (it != state.pieceQuantity) SoundUtils.playTick()
                },
                label = "バラ",
                focusRequester = pieceFocus,
                isFocusTarget = state.scanQuantityType != "CASE" || !caseEnabled,
                textSizeSp = 24f,
                textBold = true,
                allowNegative = true,
                onFocused = { onQuantityInputFocused(TransferQuantityInputTarget.PIECE) },
                onToggleNegative = viewModel::togglePieceQuantitySign,
                onMovePrevious = {
                    if (caseEnabled) {
                        caseFocus.requestFocus()
                        true
                    } else {
                        false
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (state.previousTotalPieces != 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("入力済み総バラ（未送信）", fontSize = 14.sp, color = Color.Gray)
                Text("${state.previousTotalPieces}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }

        val overStock = availableTotalPieces > 0 && state.totalPieces > availableTotalPieces
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (overStock) Color(0xFFFFF3E0) else Color(0xFFF8F9FA), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("今回 総バラ", fontSize = 16.sp, color = Color.Gray)
                if (overStock) {
                    Text("在庫超過（Web確定時に確認）", fontSize = 12.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "${state.currentInputPieces}",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (overStock) AccentOrange else Color.Black
            )
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { SoundUtils.playTick(); viewModel.clearSelection() },
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text("(F2)戻る", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.confirmInput() },
                enabled = state.caseQuantity.toIntOrNull() != null || state.pieceQuantity.toIntOrNull() != null,
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text("(F3)確定", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── History Tab ───

@Composable
private fun HistoryTab(state: WarehouseTransferState, viewModel: WarehouseTransferViewModel) {
    var selectedHistoryItem by remember { mutableStateOf<LocalWarehouseTransferInput?>(null) }

    selectedHistoryItem?.let { item ->
        HistoryDetailDialog(item) { selectedHistoryItem = null }
    }

    val dirtyList = state.dirtyInputs.values.sortedByDescending { it.updatedAt }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("未送信", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF3E0)) {
                        Text(
                            "${dirtyList.size}件 / 総バラ ${state.dirtyTotalPieces}",
                            fontSize = 11.sp,
                            color = AccentOrange,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            if (dirtyList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("未送信の入力はありません", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                items(dirtyList, key = { "dirty_${it.localKey}" }) { input ->
                    DirtyHistoryCard(
                        input = input,
                        onClick = { selectedHistoryItem = input },
                        onRemove = { viewModel.removeInput(input.localKey) }
                    )
                }
            }

            state.lastSubmitResult?.let { result ->
                item {
                    LastSubmitResultCard(result, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        state.message?.let {
            Text(it, color = AccentGreen, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { SoundUtils.playTick(); viewModel.selectTab(WarehouseTransferTab.SCAN) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("(F2)戻る", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { SoundUtils.playTick(); viewModel.startSubmit() },
                enabled = dirtyList.isNotEmpty() && !state.submitting && !state.destinationLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(
                    when {
                        state.submitting -> "送信中..."
                        state.destinationLoading -> "通信確認中..."
                        else -> "(F4)送信"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LastSubmitResultCard(result: WarehouseTransferSubmitResult, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE8F5E9),
        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("前回の送信結果", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
            Text(
                "候補番号: ${result.candidateNo ?: "-"}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text("移動先: ${result.toWarehouseName ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
            Text(
                "${result.itemCount}商品 / 総バラ ${result.totalQuantity}" +
                    (if (result.missingCount > 0) " / 対象外 ${result.missingCount}" else ""),
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                Instant.ofEpochMilli(result.sentAt).atZone(ZoneId.systemDefault()).format(transferSyncTimeFormatter),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun DirtyHistoryCard(input: LocalWarehouseTransferInput, onClick: () -> Unit, onRemove: () -> Unit) {
    val overStock = input.availableQuantityAtSync != null && input.availableQuantityAtSync > 0 && input.totalPieces > input.availableQuantityAtSync
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(100.dp)
                    .background(AccentOrange)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0F0F0)) {
                        Text(input.itemCode, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                    }
                    input.locationNo?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 12.sp, color = Color.Gray)
                    }
                    if (overStock) {
                        Text("在庫超過", fontSize = 12.sp, color = AccentOrange, fontWeight = FontWeight.Bold)
                    }
                }
                Text(input.itemName, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("ケース", fontSize = 14.sp, color = Color.Gray)
                            Text("${input.caseQuantity}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("バラ", fontSize = 14.sp, color = Color.Gray)
                            Text("${input.pieceQuantity}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("総バラ", fontSize = 14.sp, color = AccentOrange)
                        Text("${input.totalPieces}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                    }
                }
            }
            IconButton(
                onClick = { SoundUtils.playTick(); onRemove() },
                modifier = Modifier.align(Alignment.CenterVertically).size(48.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "取り消し", tint = Color.Red, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// ─── Dialogs ───

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DestinationSelectDialog(
    state: WarehouseTransferState,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                Text("移動先倉庫を選択", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "未送信 ${state.dirtyCount}件 / 総バラ ${state.dirtyTotalPieces} を送信します",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.warehouses, key = { it.id }) { warehouse ->
                        WarehouseRow(
                            warehouse = warehouse,
                            selected = warehouse.id == state.selectedToWarehouseId,
                            onClick = { onSelect(warehouse.id) }
                        )
                    }
                }
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(8.dp)) {
                        Text("(F2)送信せず閉じる", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = state.selectedToWarehouseId != null && !state.submitting,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("(F4)送信する", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseRow(warehouse: WarehouseTransferWarehouse, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) Color(0xFFE3F2FD) else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) PrimaryBlue else Color.LightGray)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0F0F0)) {
                Text(warehouse.code, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(
                warehouse.name,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) PrimaryBlue else Color.Black,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailDialog(input: LocalWarehouseTransferInput, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(input.itemCode, fontSize = 18.sp, color = Color.DarkGray)
                Text(input.itemName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                input.locationNo?.takeIf { it.isNotBlank() }?.let {
                    Text("ロケーション: $it", fontSize = 16.sp, color = Color.Gray)
                }
                input.searchCode?.let {
                    Text("読取CD: $it", fontSize = 14.sp, color = Color.Gray)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ケース", fontSize = 14.sp, color = Color.Gray)
                        Text("${input.caseQuantity}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("バラ", fontSize = 14.sp, color = Color.Gray)
                        Text("${input.pieceQuantity}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("総バラ", fontSize = 14.sp, color = PrimaryBlue)
                        Text("${input.totalPieces}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
                input.availableQuantityAtSync?.let {
                    Text("同期時在庫: $it", fontSize = 14.sp, color = Color.Gray)
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text("閉じる", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TransferResetConfirmDialog(dirtyCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("データ初期化", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text(
                    "ローカルの在庫リストと未送信入力（${dirtyCount}件）を全て破棄します。未送信の入力は失われます。",
                    fontSize = 16.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("キャンセル")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("初期化")
                    }
                }
            }
        }
    }
}

// ─── Numeric EditText (棚卸し画面と同じ挙動) ───

@Composable
private fun TransferNumericEditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester,
    isFocusTarget: Boolean,
    selectAllOnFocus: Boolean = true,
    textSizeSp: Float = 20f,
    textBold: Boolean = false,
    allowNegative: Boolean = false,
    onFocused: (() -> Unit)? = null,
    onToggleNegative: (() -> Unit)? = null,
    onEnter: (() -> Boolean)? = null,
    onMovePrevious: (() -> Boolean)? = null,
    onMoveNext: (() -> Boolean)? = null,
    onEditTextReady: ((EditText) -> Unit)? = null
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnEnter by rememberUpdatedState(onEnter)
    val currentOnMovePrevious by rememberUpdatedState(onMovePrevious)
    val currentOnMoveNext by rememberUpdatedState(onMoveNext)
    val currentOnEditTextReady by rememberUpdatedState(onEditTextReady)
    val currentOnFocused by rememberUpdatedState(onFocused)
    val currentOnToggleNegative by rememberUpdatedState(onToggleNegative)
    val currentValue by rememberUpdatedState(value)
    val currentEnabled by rememberUpdatedState(enabled)

    Column(modifier = modifier) {
        label?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = it, fontSize = 14.sp, color = Color.DarkGray)
                if (allowNegative) {
                    Text(text = "F1(-)入力", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                }
            }
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusRequester(focusRequester),
            factory = { context ->
                EditText(context).apply {
                    applyTransferNumericInputSettings(textSizeSp, textBold, allowNegative)
                    isEnabled = enabled
                    hint = placeholder.orEmpty()
                    setText(normalizeTransferNumericInput(value, allowNegative))
                    setSelection(text.length)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(s: Editable?) {
                            if (tag == true) return

                            val raw = s?.toString().orEmpty()
                            val normalized = normalizeTransferNumericInput(raw, allowNegative)
                            if (raw != normalized) {
                                tag = true
                                setText(normalized)
                                setSelection(normalized.length)
                                tag = false
                                return
                            }
                            if (normalized != currentValue) {
                                currentOnValueChange(normalized)
                            }
                        }
                    })
                    setOnFocusChangeListener { _, hasFocus ->
                        setShowSoftInputOnFocus(false)
                        if (hasFocus) {
                            currentOnFocused?.invoke()
                            post {
                                applyTransferSelection(selectAllOnFocus)
                                hideSoftwareKeyboard(this)
                            }
                        }
                    }
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP && currentEnabled) {
                            performClick()
                            setShowSoftInputOnFocus(false)
                            requestFocus()
                            post {
                                applyTransferSelection(selectAllOnFocus)
                                hideSoftwareKeyboard(this)
                            }
                        }
                        true
                    }
                    setOnEditorActionListener { _, actionId, event ->
                        val enterHandler = currentOnEnter ?: return@setOnEditorActionListener false
                        val isEnter = event != null &&
                            (event.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                                event.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
                        if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                            if (event == null || event.action == AndroidKeyEvent.ACTION_DOWN) {
                                enterHandler()
                            } else {
                                true
                            }
                        } else {
                            false
                        }
                    }
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action != AndroidKeyEvent.ACTION_DOWN) {
                            return@setOnKeyListener false
                        }
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_F1 -> {
                                if (allowNegative) {
                                    currentOnToggleNegative?.invoke()
                                    true
                                } else {
                                    false
                                }
                            }
                            AndroidKeyEvent.KEYCODE_ENTER,
                            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER -> currentOnEnter?.invoke() ?: false
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                            AndroidKeyEvent.KEYCODE_NAVIGATE_NEXT,
                            AndroidKeyEvent.KEYCODE_TAB -> currentOnMoveNext?.invoke() ?: false
                            AndroidKeyEvent.KEYCODE_DPAD_UP,
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                            AndroidKeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> currentOnMovePrevious?.invoke() ?: false
                            else -> false
                        }
                    }
                    post {
                        setShowSoftInputOnFocus(false)
                        if (isFocusTarget && enabled) {
                            requestFocus()
                            hideSoftwareKeyboard(this)
                        }
                    }
                    currentOnEditTextReady?.invoke(this)
                }
            },
            update = { editText ->
                val normalizedValue = normalizeTransferNumericInput(value, allowNegative)
                editText.setShowSoftInputOnFocus(false)
                editText.isEnabled = enabled
                editText.hint = placeholder.orEmpty()
                editText.inputType = transferInputType(allowNegative)
                editText.imeOptions = EditorInfo.IME_ACTION_NONE
                editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
                editText.setTypeface(Typeface.MONOSPACE, if (textBold) Typeface.BOLD else Typeface.NORMAL)
                editText.background = createTransferEditTextBackground(
                    backgroundColor = if (enabled) AndroidColor.WHITE else AndroidColor.rgb(245, 245, 245),
                    borderColor = AndroidColor.rgb(158, 158, 158)
                )
                if (editText.text.toString() != normalizedValue) {
                    editText.tag = true
                    editText.setText(normalizedValue)
                    editText.applyTransferSelection(editText.hasFocus() && selectAllOnFocus)
                    editText.tag = false
                }
                if (isFocusTarget && enabled && !editText.hasFocus()) {
                    editText.requestFocus()
                    editText.post {
                        editText.setShowSoftInputOnFocus(false)
                        editText.applyTransferSelection(selectAllOnFocus)
                        hideSoftwareKeyboard(editText)
                    }
                } else if (editText.hasFocus()) {
                    editText.post { hideSoftwareKeyboard(editText) }
                }
            }
        )
    }
}

private fun EditText.applyTransferNumericInputSettings(textSizeSp: Float, textBold: Boolean, allowNegative: Boolean) {
    inputType = transferInputType(allowNegative)
    imeOptions = EditorInfo.IME_ACTION_NONE
    gravity = Gravity.CENTER_VERTICAL
    isSingleLine = true
    setSelectAllOnFocus(true)
    setShowSoftInputOnFocus(false)
    isFocusable = true
    isFocusableInTouchMode = true
    isCursorVisible = true
    setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
    setTypeface(Typeface.MONOSPACE, if (textBold) Typeface.BOLD else Typeface.NORMAL)
    setPadding(16, 0, 16, 0)
    background = createTransferEditTextBackground(
        backgroundColor = AndroidColor.WHITE,
        borderColor = AndroidColor.rgb(158, 158, 158)
    )
}

private fun EditText.applyTransferSelection(selectAllOnFocus: Boolean) {
    val currentText = text?.toString().orEmpty()
    if (selectAllOnFocus && !currentText.startsWith("-")) {
        selectAll()
    } else {
        setSelection(currentText.length)
    }
}

private fun transferInputType(allowNegative: Boolean): Int =
    InputType.TYPE_CLASS_NUMBER or (if (allowNegative) InputType.TYPE_NUMBER_FLAG_SIGNED else 0)

private fun normalizeTransferNumericInput(value: String, allowNegative: Boolean = false): String {
    if (!allowNegative) return value.filter { it.isDigit() }

    var hasMinus = false
    val digits = StringBuilder()
    for (char in value) {
        when {
            char.isDigit() -> digits.append(char)
            char.isTransferMinusSign() && !hasMinus && digits.isEmpty() -> hasMinus = true
        }
    }

    return if (hasMinus) "-$digits" else digits.toString()
}

private fun Char.isTransferMinusSign(): Boolean =
    this == '-' || this == 'ー' || this == '－' || this == '−' || this == '―' || this == '–' || this == '—'

private fun createTransferEditTextBackground(backgroundColor: Int, borderColor: Int): GradientDrawable =
    GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(backgroundColor)
        setStroke(2, borderColor)
        cornerRadius = 0f
    }

private fun hideSoftwareKeyboard(view: View) {
    val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
}

private fun formatTransferSyncTime(syncedAt: Long?): String {
    val validSyncedAt = syncedAt?.takeIf { it > 0 } ?: return "最終 --"
    return "最終 " + Instant.ofEpochMilli(validSyncedAt).atZone(ZoneId.systemDefault()).format(transferSyncTimeFormatter)
}

private fun displayVolumeUnit(label: String?, rawUnit: String?): String =
    label?.takeIf { it.isNotBlank() }
        ?: when (rawUnit?.trim()) {
            "MILLILITER" -> "ml"
            "GRAM" -> "g"
            "PIECE" -> "個"
            "PACK" -> "P"
            "SHEET" -> "枚"
            "BOTTLE" -> "本"
            "BAG" -> "袋"
            "GRAIN" -> "粒"
            "KILOGRAM" -> "Kg"
            "INCLUDED_QUANTITY" -> "内数"
            else -> rawUnit.orEmpty()
        }
