package biz.smt_life.android.sakemaru_handy_denso.inventory

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils

@Composable
fun InventoryCountScreen(
    onNavigateBack: () -> Unit,
    viewModel: InventoryCountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab != InventoryTab.SCAN) {
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
                    Key.F2 -> {
                        SoundUtils.playTick()
                        when {
                            state.selectedItem != null -> viewModel.clearSelection()
                            state.selectedTab == InventoryTab.HISTORY -> viewModel.selectTab(InventoryTab.SCAN)
                            state.selectedTab != InventoryTab.MENU && state.hasInstruction -> viewModel.selectTab(InventoryTab.MENU)
                            else -> onNavigateBack()
                        }
                        true
                    }
                    Key.F3 -> {
                        if (state.selectedItem != null) {
                            viewModel.confirmInput()
                        } else {
                            SoundUtils.playTick()
                            viewModel.loadInstructions()
                        }
                        true
                    }
                    Key.F4 -> {
                        SoundUtils.playTick()
                        if (state.selectedTab == InventoryTab.SCAN && state.selectedItem == null) {
                            viewModel.selectTab(InventoryTab.HISTORY)
                        } else {
                            viewModel.finishRound()
                        }
                        true
                    }
                    Key.One, Key.NumPad1 -> {
                        if (state.selectedTab == InventoryTab.MENU && state.hasInstruction && state.selectedItem == null) {
                            SoundUtils.playTick()
                            viewModel.selectTab(InventoryTab.SCAN)
                            true
                        } else false
                    }
                    Key.Two, Key.NumPad2 -> {
                        if (state.selectedTab == InventoryTab.MENU && state.hasInstruction && state.selectedItem == null) {
                            SoundUtils.playTick()
                            viewModel.selectTab(InventoryTab.HISTORY)
                            true
                        } else false
                    }
                    else -> false
                }
            }
    ) {
        InventoryHeader(state, onBack = {
            SoundUtils.playTick()
            when {
                state.selectedItem != null -> viewModel.clearSelection()
                state.selectedTab == InventoryTab.HISTORY -> viewModel.selectTab(InventoryTab.SCAN)
                state.selectedTab != InventoryTab.MENU && state.hasInstruction -> viewModel.selectTab(InventoryTab.MENU)
                else -> onNavigateBack()
            }
        })

        if (state.loading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!state.hasInstruction) {
            InstructionCheckPanel(state, viewModel, modifier = Modifier.weight(1f))
        } else if (state.selectedItem != null) {
            ItemInputPanel(state, viewModel, modifier = Modifier.weight(1f))
        } else when (state.selectedTab) {
            InventoryTab.MENU -> InventoryMenuPanel(viewModel, modifier = Modifier.weight(1f))
            InventoryTab.SCAN -> {
                Box(modifier = Modifier.weight(1f)) { ScanTab(state, viewModel) }
            }
            InventoryTab.HISTORY -> {
                Box(modifier = Modifier.weight(1f)) { HistoryTab(state, viewModel) }
            }
            InventoryTab.SETTINGS -> {
                Box(modifier = Modifier.weight(1f)) { SettingsTab(state, viewModel) }
            }
            else -> InventoryMenuPanel(viewModel, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InventoryHeader(state: InventoryCountState, onBack: () -> Unit) {
    val count = state.selectedCount
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                color = Color.Transparent,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("← もどる", fontSize = 14.sp, color = Color(0xFF1976D2))
                    Text(" (F2)", fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (count != null) {
                Text(
                    "第${state.countRound}回目",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f).padding(start = 2.dp)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InstructionCheckPanel(
    state: InventoryCountState,
    viewModel: InventoryCountViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val shape = RoundedCornerShape(8.dp)
        Surface(
            onClick = { SoundUtils.playTick(); viewModel.loadInstructions() },
            enabled = !state.loading,
            modifier = Modifier
                .width(200.dp)
                .aspectRatio(1.5f)
                .border(1.dp, Color.LightGray, shape),
            color = Color.White,
            shadowElevation = 2.dp,
            shape = shape
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFFFF9800))
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("棚卸し指示確認", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("[F3]", fontSize = 16.sp, color = Color.DarkGray)
                }
            }
        }
        state.error?.let {
            Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

// ─── Menu Panel ───

@Composable
private fun InventoryMenuPanel(
    viewModel: InventoryCountViewModel,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                onClick = { SoundUtils.playTick(); viewModel.selectTab(InventoryTab.SCAN) },
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(1f)
                    .border(1.dp, Color.LightGray, shape),
                color = Color.White,
                shadowElevation = 2.dp,
                shape = shape
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF1976D2))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("(1)スキャン", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
            Surface(
                onClick = { SoundUtils.playTick(); viewModel.selectTab(InventoryTab.HISTORY) },
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(1f)
                    .border(1.dp, Color.LightGray, shape),
                color = Color.White,
                shadowElevation = 2.dp,
                shape = shape
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF4CAF50))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("(2)履歴", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { SoundUtils.playTick(); viewModel.selectTab(InventoryTab.SETTINGS) },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text("設定", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

// ─── Settings Tab ───

@Composable
private fun SettingsTab(state: InventoryCountState, viewModel: InventoryCountViewModel) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        ResetConfirmDialog(
            onConfirm = { showResetDialog = false; viewModel.resetLocalData() },
            onDismiss = { showResetDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("設定", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider()

        Button(
            onClick = { SoundUtils.playTick(); viewModel.finishRound() },
            enabled = state.dirtyCount > 0 && !state.submitting,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (state.submitting) "送信中..."
                else "送信（未送信 ${state.dirtyCount}件）",
                fontSize = 15.sp
            )
        }

        HorizontalDivider()

        OutlinedButton(
            onClick = { SoundUtils.playTick(); viewModel.syncAllItems() },
            enabled = !state.syncing,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.syncing) "取得中..." else "棚卸しデータ再取得", fontSize = 15.sp)
        }

        OutlinedButton(
            onClick = { SoundUtils.playTick(); showResetDialog = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("データ初期化", fontSize = 15.sp, color = Color.Red)
        }
    }
}

// ─── Scan Tab ───

@Composable
private fun ScanTab(state: InventoryCountState, viewModel: InventoryCountViewModel) {
    val scanFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.hasLocalData) {
        if (state.hasLocalData) {
            scanFocusRequester.requestFocus()
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
            Text("棚卸しデータを取得してください", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { SoundUtils.playTick(); viewModel.syncAllItems() },
                enabled = !state.syncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.syncing) "取得中..." else "棚卸しデータ取得")
            }
            Spacer(modifier = Modifier.weight(1f))
            return
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.keyword,
            onValueChange = viewModel::setKeyword,
            label = { Text("商品コード / JAN / バーコード") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    SoundUtils.playBeep()
                    viewModel.scan()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(scanFocusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                        SoundUtils.playBeep()
                        viewModel.scan()
                        true
                    } else {
                        false
                    }
                }
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFF1976D2)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("商品をスキャンしてください", fontSize = 20.sp, color = Color.DarkGray)

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
        state.message?.let {
            Text(it, color = Color(0xFF2E7D32), fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { SoundUtils.playTick(); viewModel.selectTab(InventoryTab.HISTORY) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.6f).height(56.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("(F4) 履歴", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (state.dirtyCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE65100)) {
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
    }
}

// ─── Item Input Panel ───

@Composable
private fun ItemInputPanel(
    state: InventoryCountState,
    viewModel: InventoryCountViewModel,
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
        // Packaging — prominent at top
        Text(
            "${item.volume ?: "-"}${displayVolumeUnit(item.volumeUnitLabel, item.volumeUnit)} × ${state.capacityCase}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )

        // Item info
        Text("${item.itemCode}  ${item.barcode ?: ""}", fontSize = 16.sp, color = Color.DarkGray)
        Text(item.itemName, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2)

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        // Input fields
        val caseEnabled = state.capacityCase > 1
        val caseFocus = remember { FocusRequester() }
        val pieceFocus = remember { FocusRequester() }

        LaunchedEffect(item.id) {
            when {
                state.scanQuantityType == "CASE" && caseEnabled -> caseFocus.requestFocus()
                !caseEnabled -> pieceFocus.requestFocus()
                else -> pieceFocus.requestFocus()
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.caseQuantity,
                onValueChange = {
                    viewModel.setCaseQuantity(it)
                    if (it != state.caseQuantity) SoundUtils.playTick()
                },
                label = { Text("ケース", fontSize = 14.sp) },
                enabled = caseEnabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.None
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(caseFocus)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionRight -> { pieceFocus.requestFocus(); true }
                            Key.DirectionDown -> { pieceFocus.requestFocus(); true }
                            else -> false
                        }
                    }
            )
            OutlinedTextField(
                value = state.pieceQuantity,
                onValueChange = {
                    viewModel.setPieceQuantity(it)
                    if (it != state.pieceQuantity) SoundUtils.playTick()
                },
                label = { Text("バラ", fontSize = 14.sp) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.None
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(pieceFocus)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionLeft -> { caseFocus.requestFocus(); true }
                            Key.DirectionUp -> { caseFocus.requestFocus(); true }
                            else -> false
                        }
                    }
            )
        }

        // 入力済み総バラ
        if (state.previousTotalPieces > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("入力済み総バラ", fontSize = 14.sp, color = Color.Gray)
                Text("${state.previousTotalPieces}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }

        // 今回の総バラ
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("今回 総バラ", fontSize = 16.sp, color = Color.Gray)
            Text("${state.currentInputPieces}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { SoundUtils.playTick(); viewModel.clearSelection() },
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text("(F2)戻る", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.confirmInput() },
                enabled = state.caseQuantity.isNotBlank() || state.pieceQuantity.isNotBlank(),
                modifier = Modifier.weight(1f).height(64.dp)
            ) {
                Text("(F3)確定", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
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

// ─── History Tab ───

@Composable
private fun HistoryTab(state: InventoryCountState, viewModel: InventoryCountViewModel) {
    var selectedHistoryItem by remember { mutableStateOf<LocalInventoryInput?>(null) }

    selectedHistoryItem?.let { item ->
        HistoryDetailDialog(item) { selectedHistoryItem = null }
    }

    val dirtyList = state.dirtyInputs.values
        .filter { it.countRound == state.countRound }
        .sortedByDescending { it.updatedAt }

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
            // 未送信セクション
            if (dirtyList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("未送信", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF3E0)) {
                            Text(
                                "${dirtyList.size}件",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                items(dirtyList, key = { "dirty_${it.itemId}" }) { input ->
                    DirtyHistoryCard(
                        input = input,
                        onClick = { selectedHistoryItem = input },
                        onRemove = { viewModel.removeInput(input.itemId) }
                    )
                }
            }

            // 送信済みセクション
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (dirtyList.isNotEmpty()) 8.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("送信済み", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF0F0F0)) {
                        Text(
                            "${state.sentHistory.size}件",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            if (state.sentHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("送信済みの履歴はありません", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                items(state.sentHistory, key = { "sent_${it.requestUuid}" }) { input ->
                    HistoryCard(input, onClick = { selectedHistoryItem = input })
                }
            }
        }
    }
}

@Composable
private fun DirtyHistoryCard(input: LocalInventoryInput, onClick: () -> Unit, onRemove: () -> Unit) {
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
                    .background(Color(0xFFE65100))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0F0F0)) {
                    Text(input.itemCode, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
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
                        Text("総バラ", fontSize = 14.sp, color = Color(0xFFE65100))
                        Text("${input.totalPieces}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
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

@Composable
private fun HistoryCard(input: LocalInventoryInput, onClick: () -> Unit) {
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
                    .background(Color(0xFF1976D2))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF0F0F0)) {
                    Text(input.itemCode, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
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
                        Text("総バラ", fontSize = 14.sp, color = Color(0xFF1976D2))
                        Text("${input.totalPieces}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDetailDialog(input: LocalInventoryInput, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(input.itemCode, fontSize = 18.sp, color = Color.DarkGray)
                Text(input.itemName, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                if (input.locationNo != null) {
                    Text("ロケーション: ${input.locationNo}", fontSize = 16.sp, color = Color.Gray)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ケース", fontSize = 14.sp, color = Color.Gray)
                        Text("${input.caseQuantity}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("バラ", fontSize = 14.sp, color = Color.Gray)
                        Text("${input.pieceQuantity}", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("総バラ", fontSize = 14.sp, color = Color(0xFF1976D2))
                        Text("${input.totalPieces}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                }

                Text(
                    "第${input.countRound}回目",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("閉じる", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ResetConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("データ初期化", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text(
                    "ローカルの棚卸しデータを全て破棄し、サーバーから最新データを取得します。未送信の入力は失われます。",
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

// ─── Bottom Navigation ───

@Composable
private fun InventoryBottomBar(selectedTab: InventoryTab, onSelect: (InventoryTab) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp,
        modifier = Modifier.height(72.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == InventoryTab.SCAN,
            onClick = { onSelect(InventoryTab.SCAN) },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(30.dp)) },
            label = { Text("スキャン", fontSize = 15.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1976D2),
                selectedTextColor = Color(0xFF1976D2),
                indicatorColor = Color(0xFFE8F0FE)
            )
        )
        NavigationBarItem(
            selected = selectedTab == InventoryTab.HISTORY,
            onClick = { onSelect(InventoryTab.HISTORY) },
            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(30.dp)) },
            label = { Text("履歴", fontSize = 15.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1976D2),
                selectedTextColor = Color(0xFF1976D2),
                indicatorColor = Color(0xFFE8F0FE)
            )
        )
    }
}
