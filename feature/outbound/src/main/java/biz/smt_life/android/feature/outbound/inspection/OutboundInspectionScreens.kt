package biz.smt_life.android.feature.outbound.inspection

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.OutboundInspectionCourse
import biz.smt_life.android.core.domain.model.OutboundInspectionFloor
import biz.smt_life.android.core.domain.model.OutboundInspectionItem
import biz.smt_life.android.core.domain.model.OutboundInspectionPeriod
import biz.smt_life.android.core.domain.model.OutboundInspectionQuantity

private val Blue = Color(0xFF1565C0)
private val BlueDark = Color(0xFF0D47A1)
private val BlueLight = Color(0xFFE3F2FD)
private val Green = Color(0xFF2E7D32)
private val GreenLight = Color(0xFFE8F5E9)
private val Red = Color(0xFFC62828)
private val RedLight = Color(0xFFFFEBEE)
private val GrayBorder = Color(0xFFB0BEC5)
private val TextMain = Color(0xFF172033)
private val TextSub = Color(0xFF546E7A)

@Composable
fun OutboundInspectionPeriodScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCourse: () -> Unit,
    viewModel: OutboundInspectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var selectedPeriodIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun selectPeriod(index: Int) {
        if (state.isLoading) return
        selectedPeriodIndex = index
        SoundUtils.playTick()
        viewModel.loadSnapshot(
            period = if (index == 0) OutboundInspectionPeriod.Morning else OutboundInspectionPeriod.Afternoon,
            onSuccess = onNavigateToCourse
        )
    }

    fun movePeriodSelection(delta: Int) {
        if (state.isLoading) return
        val nextIndex = (selectedPeriodIndex + delta).coerceIn(0, 1)
        if (nextIndex != selectedPeriodIndex) {
            SoundUtils.playTick()
            selectedPeriodIndex = nextIndex
        }
    }

    InspectionShell(
        title = "出庫検品",
        subtitle = "午前・午後を選択",
        onBack = onNavigateBack,
        footer = {
            FunctionFooter(
                f1 = "データリセット" to {
                    if (!state.isLoading) {
                        SoundUtils.playTick()
                        viewModel.resetSnapshot()
                    }
                },
                f2 = "戻る" to onNavigateBack
            )
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.One, Key.NumPad1 -> {
                        selectPeriod(0)
                        true
                    }
                    Key.Two, Key.NumPad2 -> {
                        selectPeriod(1)
                        true
                    }
                    Key.Three, Key.NumPad3, Key.DirectionLeft -> {
                        if (!state.isLoading) {
                            SoundUtils.playTick()
                            viewModel.moveWorkingDate(-1)
                        }
                        true
                    }
                    Key.Four, Key.NumPad4, Key.DirectionRight -> {
                        if (!state.isLoading) {
                            SoundUtils.playTick()
                            viewModel.moveWorkingDate(1)
                        }
                        true
                    }
                    Key.F1 -> {
                        if (!state.isLoading) {
                            SoundUtils.playTick()
                            viewModel.resetSnapshot()
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        movePeriodSelection(-1)
                        true
                    }
                    Key.DirectionDown -> {
                        movePeriodSelection(1)
                        true
                    }
                    Key.Enter -> {
                        selectPeriod(selectedPeriodIndex)
                        true
                    }
                    Key.F2 -> {
                        SoundUtils.playTick()
                        onNavigateBack()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkingDateSelector(
                    workingDate = state.workingDate,
                    enabled = !state.isLoading,
                    onPrevious = {
                        SoundUtils.playTick()
                        viewModel.moveWorkingDate(-1)
                    },
                    onNext = {
                        SoundUtils.playTick()
                        viewModel.moveWorkingDate(1)
                    }
                )
                PeriodButton(
                    label = "午前 [1]",
                    description = "",
                    selected = selectedPeriodIndex == 0,
                    enabled = !state.isLoading,
                    onClick = {
                        selectPeriod(0)
                    }
                )
                PeriodButton(
                    label = "午後 [2]",
                    description = "",
                    selected = selectedPeriodIndex == 1,
                    enabled = !state.isLoading,
                    onClick = {
                        selectPeriod(1)
                    }
                )
                Text(
                    text = "↑↓:選択 / Enter:決定",
                    fontSize = 11.sp,
                    color = TextSub,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                state.errorMessage?.let {
                    ErrorPanel(it)
                }
            }
            if (state.isLoading) {
                LoadingOverlay("出庫検品データ取得中")
            }
        }
    }
}

@Composable
fun OutboundInspectionCourseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFloor: () -> Unit,
    viewModel: OutboundInspectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var selectedCourseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state.courses.size) {
        selectedCourseIndex = selectedCourseIndex.coerceIn(0, (state.courses.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(selectedCourseIndex, state.courses.size) {
        if (state.courses.isNotEmpty()) {
            listState.animateScrollToItem(selectedCourseIndex)
        }
    }

    fun moveCourseSelection(delta: Int) {
        if (state.courses.isEmpty()) return
        val nextIndex = (selectedCourseIndex + delta).coerceIn(0, state.courses.lastIndex)
        if (nextIndex != selectedCourseIndex) {
            SoundUtils.playTick()
            selectedCourseIndex = nextIndex
        }
    }

    fun selectHighlightedCourse() {
        state.courses.getOrNull(selectedCourseIndex)?.let { course ->
            SoundUtils.playTick()
            viewModel.selectCourse(course)
            onNavigateToFloor()
        }
    }

    InspectionShell(
        title = "出庫検品",
        subtitle = "配送コース選択",
        onBack = onNavigateBack,
        footer = { FunctionFooter(f2 = "戻る" to onNavigateBack) },
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        moveCourseSelection(-1)
                        true
                    }
                    Key.DirectionDown -> {
                        moveCourseSelection(1)
                        true
                    }
                    Key.Enter -> {
                        selectHighlightedCourse()
                        true
                    }
                    Key.F2 -> {
                        SoundUtils.playTick()
                        onNavigateBack()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            SnapshotSummary(state)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.courses.isEmpty()) {
                EmptyPanel("配送コースがありません")
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(state.courses, key = { _, course -> course.deliveryCourseId }) { index, course ->
                        CourseRow(course, selected = index == selectedCourseIndex) {
                            selectedCourseIndex = index
                            SoundUtils.playTick()
                            viewModel.selectCourse(course)
                            onNavigateToFloor()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OutboundInspectionFloorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    viewModel: OutboundInspectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    InspectionShell(
        title = "出庫検品",
        subtitle = "1F / 2F / YX 選択",
        onBack = onNavigateBack,
        footer = { FunctionFooter(f2 = "戻る" to onNavigateBack) },
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.One, Key.NumPad1 -> state.floors.getOrNull(0)?.let {
                        SoundUtils.playTick()
                        viewModel.selectFloor(it)
                        onNavigateToScan()
                        true
                    } ?: false
                    Key.Two, Key.NumPad2 -> state.floors.getOrNull(1)?.let {
                        SoundUtils.playTick()
                        viewModel.selectFloor(it)
                        onNavigateToScan()
                        true
                    } ?: false
                    Key.Three, Key.NumPad3 -> state.floors.getOrNull(2)?.let {
                        SoundUtils.playTick()
                        viewModel.selectFloor(it)
                        onNavigateToScan()
                        true
                    } ?: false
                    Key.F2 -> {
                        SoundUtils.playTick()
                        onNavigateBack()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.selectedCourse?.let { course ->
                InfoPanel(
                    title = course.displayName,
                    text = "検品する階層を選択してください。"
                )
            }
            if (state.floors.isEmpty()) {
                EmptyPanel("フロアデータがありません")
            } else {
                state.floors.forEachIndexed { index, floor ->
                    FloorButton(
                        floor = floor,
                        keyLabel = "[${index + 1}]",
                        onClick = {
                            SoundUtils.playTick()
                            viewModel.selectFloor(floor)
                            onNavigateToScan()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OutboundInspectionScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: OutboundInspectionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPickingList by remember { mutableStateOf(false) }
    var scanInput by remember { mutableStateOf("") }
    var scanEditText by remember { mutableStateOf<EditText?>(null) }

    fun submitScan(rawCode: String) {
        val code = rawCode.trim()
        if (code.isEmpty()) return

        scanInput = ""
        when (viewModel.handleScan(code)) {
            is OutboundInspectionScanResult.Success -> SoundUtils.playSuccess()
            is OutboundInspectionScanResult.NotFound -> SoundUtils.playErrorWithVibration(context)
            is OutboundInspectionScanResult.DuplicateCode -> SoundUtils.playErrorWithVibration(context)
            is OutboundInspectionScanResult.AlreadyScanned -> Unit
            OutboundInspectionScanResult.Waiting -> Unit
        }
    }

    fun confirmScanResult() {
        if (state.scanResult !is OutboundInspectionScanResult.Waiting) {
            SoundUtils.playTick()
            viewModel.confirmScanResult()
        }
    }

    fun navigateBackWithSound() {
        SoundUtils.playTick()
        onNavigateBack()
    }

    LaunchedEffect(context) {
        SoundUtils.init(context.applicationContext)
    }

    LaunchedEffect(showPickingList, state.scanResult, scanEditText) {
        if (!showPickingList) {
            scanEditText?.post {
                scanEditText?.setShowSoftInputOnFocus(false)
                scanEditText?.requestFocus()
            }
        }
    }

    if (showPickingList) {
        PickingListDialog(
            courseName = state.selectedCourse?.displayName ?: "コース未選択",
            floorLabel = state.selectedFloor?.floorLabel ?: "フロア未選択",
            items = state.selectedFloor?.items.orEmpty(),
            onDismiss = { showPickingList = false }
        )
    }

    InspectionShell(
        title = state.selectedCourse?.displayName ?: "コース未選択",
        subtitle = state.selectedFloor?.floorLabel ?: "スキャン待機",
        onBack = onNavigateBack,
        showBackButton = false,
        emphasizedSubtitle = true,
        footer = {
            FunctionFooter(
                f1 = "確認" to { confirmScanResult() },
                f2 = "戻る" to { navigateBackWithSound() }
            )
        },
        modifier = Modifier
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.F1 -> {
                        confirmScanResult()
                        true
                    }
                    Key.F2 -> {
                        navigateBackWithSound()
                        true
                    }
                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanInputField(
                value = scanInput,
                onValueChange = { value -> scanInput = value },
                onSubmit = { submitScan(it) },
                onConfirm = { confirmScanResult() },
                onBack = { navigateBackWithSound() },
                requestFocus = !showPickingList,
                onCreated = { scanEditText = it },
                modifier = Modifier.fillMaxWidth()
            )
            ScanResultPanel(state.scanResult)
            PickingListButton(
                itemCount = state.selectedFloor?.items?.size ?: 0,
                onClick = {
                    SoundUtils.playTick()
                    showPickingList = true
                }
            )
            Text(
                text = "スキャンまたは手入力後Enter / F1:確認 / F2:戻る",
                fontSize = 11.sp,
                color = TextSub,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    requestFocus: Boolean,
    onCreated: (EditText) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnValueChange = rememberUpdatedState(onValueChange)
    val currentOnSubmit = rememberUpdatedState(onSubmit)
    val currentOnConfirm = rememberUpdatedState(onConfirm)
    val currentOnBack = rememberUpdatedState(onBack)

    AndroidView(
        modifier = modifier.height(56.dp),
        factory = { context ->
            EditText(context).apply {
                val density = resources.displayMetrics.density
                setSingleLine(true)
                hint = "商品コード / JAN / バーコード"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_ACTION_SEARCH
                setShowSoftInputOnFocus(false)
                setTextColor(AndroidColor.rgb(23, 32, 51))
                setHintTextColor(AndroidColor.rgb(84, 110, 122))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(AndroidColor.WHITE)
                    setStroke((1 * density).toInt().coerceAtLeast(1), AndroidColor.rgb(21, 101, 192))
                }
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        currentOnValueChange.value(s?.toString().orEmpty())
                    }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
                fun submitCurrentText(): Boolean {
                    val rawCode = text?.toString().orEmpty()
                    if (rawCode.isNotBlank()) {
                        currentOnSubmit.value(rawCode)
                        text?.clear()
                    }
                    post {
                        setShowSoftInputOnFocus(false)
                        requestFocus()
                    }
                    return true
                }
                setOnEditorActionListener { _, actionId, event ->
                    val isEnter = event != null &&
                        (event.keyCode == AndroidKeyEvent.KEYCODE_ENTER || event.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
                    if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                        if (event == null || event.action == AndroidKeyEvent.ACTION_DOWN) {
                            submitCurrentText()
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }
                setOnKeyListener { _, keyCode, event ->
                    when (keyCode) {
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            if (event.action == AndroidKeyEvent.ACTION_DOWN) {
                                submitCurrentText()
                            } else {
                                true
                            }
                        }
                        AndroidKeyEvent.KEYCODE_F1 -> {
                            if (event.action == AndroidKeyEvent.ACTION_DOWN) {
                                currentOnConfirm.value()
                            }
                            true
                        }
                        AndroidKeyEvent.KEYCODE_F2 -> {
                            if (event.action == AndroidKeyEvent.ACTION_DOWN) {
                                currentOnBack.value()
                            }
                            true
                        }
                        else -> false
                    }
                }
                onCreated(this)
            }
        },
        update = { editText ->
            editText.setShowSoftInputOnFocus(false)
            if (editText.text?.toString().orEmpty() != value) {
                editText.setText(value)
                editText.setSelection(editText.text?.length ?: 0)
            }
            if (requestFocus && !editText.hasFocus()) {
                editText.post { editText.requestFocus() }
            }
        }
    )
}

@Composable
private fun InspectionShell(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    emphasizedSubtitle: Boolean = false,
    footer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAFD))
    ) {
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            shape = RectangleShape
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    Text(
                        text = "← もどる",
                        fontSize = 13.sp,
                        color = Blue,
                        modifier = Modifier
                            .clickable { SoundUtils.playTick(); onBack() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = if (showBackButton) 4.dp else 0.dp),
                    horizontalAlignment = if (showBackButton) Alignment.Start else Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = BlueDark, textAlign = TextAlign.Center)
                    Text(subtitle, fontSize = if (emphasizedSubtitle) 17.sp else 10.sp, color = TextSub, textAlign = TextAlign.Center)
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
        footer()
    }
}

@Composable
private fun FunctionFooter(
    f1: Pair<String, () -> Unit>? = null,
    f2: Pair<String, () -> Unit>? = null
) {
    Surface(color = Color.White, shadowElevation = 4.dp, shape = RectangleShape) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FooterKey("F1", f1, modifier = Modifier.weight(1f))
            FooterKey("F2", f2, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FooterKey(
    keyName: String,
    action: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier
) {
    val enabled = action != null
    Surface(
        modifier = modifier
            .height(36.dp)
            .border(1.dp, if (enabled) Blue else GrayBorder, RectangleShape)
            .clickable(enabled = enabled) { action?.second?.invoke() },
        color = if (enabled) BlueLight else Color(0xFFF5F5F5),
        shape = RectangleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (action == null) keyName else "$keyName:${action.first}",
                fontSize = 12.sp,
                fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) BlueDark else Color.Gray
            )
        }
    }
}

@Composable
private fun PeriodButton(
    label: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    SquareButton(
        title = label,
        subtitle = description,
        selected = selected,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun WorkingDateSelector(
    workingDate: String,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateMoveButton(
            label = "前日[3]",
            enabled = enabled,
            onClick = onPrevious,
            modifier = Modifier.weight(1f)
        )
        Surface(
            modifier = Modifier
                .height(52.dp)
                .weight(1.5f)
                .border(1.dp, Blue, RectangleShape),
            color = BlueLight,
            shape = RectangleShape
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("作業日", fontSize = 10.sp, color = TextSub)
                Text(workingDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BlueDark)
            }
        }
        DateMoveButton(
            label = "翌日[4]",
            enabled = enabled,
            onClick = onNext,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateMoveButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .border(1.dp, if (enabled) Blue else GrayBorder, RectangleShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) Color.White else Color(0xFFF5F5F5),
        shadowElevation = if (enabled) 1.dp else 0.dp,
        shape = RectangleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) BlueDark else Color.Gray
            )
        }
    }
}

@Composable
private fun SquareButton(
    title: String,
    subtitle: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(2.dp, if (selected && enabled) BlueDark else if (enabled) Blue else GrayBorder, RectangleShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected && enabled) BlueLight else if (enabled) Color.White else Color(0xFFF5F5F5),
        shadowElevation = if (enabled) 1.dp else 0.dp,
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (enabled) BlueDark else Color.Gray)
            if (subtitle.isNotBlank()) {
                Text(subtitle, fontSize = 11.sp, color = TextSub)
            }
        }
    }
}

@Composable
private fun CourseRow(
    course: OutboundInspectionCourse,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (selected) BlueDark else GrayBorder, RectangleShape)
            .clickable(onClick = onClick),
        color = if (selected) BlueLight else Color.White,
        shadowElevation = 1.dp,
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = course.deliveryCourseName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(course.deliveryCourseCode, fontSize = 11.sp, color = TextSub)
                Text("${course.floors.size}フロア", fontSize = 11.sp, color = TextSub)
                Text("${course.summary.itemCount}商品", fontSize = 11.sp, color = TextSub)
            }
        }
    }
}

@Composable
private fun FloorButton(
    floor: OutboundInspectionFloor,
    keyLabel: String,
    onClick: () -> Unit
) {
    SquareButton(
        title = "${floor.floorLabel} $keyLabel",
        subtitle = "${floor.summary.itemCount}商品 / 総バラ ${floor.summary.totalPieces}",
        onClick = onClick
    )
}

@Composable
private fun SnapshotSummary(state: OutboundInspectionState) {
    val snapshot = state.snapshot ?: return
    InfoPanel(
        title = "${snapshot.periodLabel} / ${snapshot.businessDate}",
        text = "${snapshot.source.groupNo}  ${snapshot.warehouse.name}"
    )
}


@Composable
private fun PickingListButton(
    itemCount: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.dp, Blue, RectangleShape)
            .clickable(onClick = onClick),
        color = Color.White,
        shadowElevation = 1.dp,
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ピッキングリスト", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlueDark)
            Text("${itemCount}商品", fontSize = 12.sp, color = TextSub)
        }
    }
}

@Composable
private fun PickingListDialog(
    courseName: String,
    floorLabel: String,
    items: List<OutboundInspectionItem>,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Blue, RectangleShape)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.F2) {
                        SoundUtils.playTick()
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
            color = Color.White,
            shape = RectangleShape
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ピッキングリスト", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueDark)
                        Text("$courseName / $floorLabel", fontSize = 10.sp, color = TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(
                        modifier = Modifier
                            .border(1.dp, Blue, RectangleShape)
                            .clickable(onClick = onDismiss),
                        color = BlueLight,
                        shape = RectangleShape
                    ) {
                        Text("F2:閉じる", fontSize = 12.sp, color = BlueDark, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                HorizontalDivider(color = GrayBorder)
                if (items.isEmpty()) {
                    Text("対象商品がありません", fontSize = 13.sp, color = TextSub, modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items, key = { it.inspectionItemId }) { item ->
                            PickingListItemRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickingListItemRow(item: OutboundInspectionItem) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, GrayBorder, RectangleShape),
        color = Color(0xFFFAFCFF),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.itemName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text("棚番: ${item.location.locationCode}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BlueDark)
            Text("注文: ${item.orderedQuantity.displayText}", fontSize = 11.sp, color = TextMain)
            Text("ピック: ${item.plannedQuantity.displayText}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BlueDark)
        }
    }
}

@Composable
private fun ScanResultPanel(result: OutboundInspectionScanResult) {
    when (result) {
        OutboundInspectionScanResult.Waiting -> {
            StatusPanel(
                title = "スキャン待機中",
                message = "商品コードを読み取ってください。",
                background = BlueLight,
                color = BlueDark
            )
        }
        is OutboundInspectionScanResult.Success -> {
            ItemResultPanel(
                title = "対象商品です",
                item = result.item,
                background = GreenLight,
                color = Green,
                extraMessage = null
            )
        }
        is OutboundInspectionScanResult.AlreadyScanned -> {
            ItemResultPanel(
                title = "対象商品です",
                item = result.item,
                background = Color.White,
                color = Green,
                extraMessage = "すでにスキャンしています。"
            )
        }
        is OutboundInspectionScanResult.NotFound -> {
            StatusPanel(
                title = "商品が違います",
                message = result.rawCode,
                background = RedLight,
                color = Red
            )
        }
        is OutboundInspectionScanResult.DuplicateCode -> {
            StatusPanel(
                title = "JANが複数商品にひもづいています。",
                message = result.items.joinToString(" / ") { it.itemName },
                background = RedLight,
                color = Red
            )
        }
    }
}

@Composable
private fun ItemResultPanel(
    title: String,
    item: OutboundInspectionItem,
    background: Color,
    color: Color,
    extraMessage: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(2.dp, color, RectangleShape),
        color = background,
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(item.itemName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("棚番: ${item.location.locationCode}", fontSize = 12.sp, color = TextSub)
            QuantityRow("ピッキング数量", item.plannedQuantity, emphasized = true)
            extraMessage?.let {
                Text(it, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Red)
            }
        }
    }
}

@Composable
private fun QuantityRow(
    label: String,
    quantity: OutboundInspectionQuantity,
    emphasized: Boolean = false
) {
    val labelSize = if (emphasized) 14.sp else 11.sp
    val valueSize = if (emphasized) 22.sp else 13.sp
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, GrayBorder, RectangleShape),
        color = Color.White,
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(7.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = labelSize, color = TextSub)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ケース ${quantity.caseQty}", fontSize = valueSize, fontWeight = FontWeight.Bold, color = TextMain)
                Text("バラ ${quantity.pieceQty}", fontSize = valueSize, fontWeight = FontWeight.Bold, color = TextMain)
            }
        }
    }
}

@Composable
private fun StatusPanel(
    title: String,
    message: String,
    background: Color,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp).border(2.dp, color, RectangleShape),
        color = background,
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(10.dp))
            Text(message, fontSize = 13.sp, color = TextMain, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoPanel(title: String, text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF90CAF9), RectangleShape),
        color = Color.White,
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BlueDark)
            Text(text, fontSize = 11.sp, color = TextSub)
        }
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, Red, RectangleShape),
        color = RedLight,
        shape = RectangleShape
    ) {
        Text(message, fontSize = 12.sp, color = Red, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, fontSize = 14.sp, color = TextSub)
    }
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(color = Blue)
            Text(message, fontSize = 13.sp, color = BlueDark, fontWeight = FontWeight.Bold)
        }
    }
}
