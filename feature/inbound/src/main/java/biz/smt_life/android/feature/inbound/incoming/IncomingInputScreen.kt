package biz.smt_life.android.feature.inbound.incoming

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.ui.HardwareKeyHandler
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Input Screen for Incoming feature.
 * Allows user to input quantity, expiration date, and location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingInputScreen(
    onNavigateBack: () -> Unit,
    onSubmitSuccess: (Boolean) -> Unit,
    viewModel: IncomingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val quantityFocusRequester = remember { FocusRequester() }
    val pieceFocusRequester = remember { FocusRequester() }
    val expirationFocusRequester = remember { FocusRequester() }

    // Track current focused field index (0: case, 1: piece, 2: expiration)
    var currentFieldIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = listOf(quantityFocusRequester, pieceFocusRequester, expirationFocusRequester)

    fun moveInputFocus(delta: Int): Boolean {
        val targetIndex = (currentFieldIndex + delta).coerceIn(0, focusRequesters.lastIndex)
        if (targetIndex != currentFieldIndex) {
            SoundUtils.playBeep()
            currentFieldIndex = targetIndex
            focusRequesters[targetIndex].requestFocus()
            keyboardController?.hide()
        }
        return true
    }

    // Request focus on quantity field when displayed
    LaunchedEffect(Unit) {
        quantityFocusRequester.requestFocus()
        keyboardController?.hide()
    }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }

    HardwareKeyHandler { keyCode, _ ->
        when (keyCode) {
            AndroidKeyEvent.KEYCODE_F1 -> {
                SoundUtils.playBeep()
                showDatePicker = true
                true
            }
            AndroidKeyEvent.KEYCODE_F2 -> {
                SoundUtils.playBeep()
                onNavigateBack()
                true
            }
            AndroidKeyEvent.KEYCODE_F3 -> {
                SoundUtils.playBeep()
                if (viewModel.canSubmit()) {
                    viewModel.submitEntry(onSubmitSuccess)
                }
                true
            }
            AndroidKeyEvent.KEYCODE_ENTER,
            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER -> {
                false
            }
            AndroidKeyEvent.KEYCODE_DPAD_DOWN,
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
            AndroidKeyEvent.KEYCODE_NAVIGATE_NEXT,
            AndroidKeyEvent.KEYCODE_TAB -> {
                moveInputFocus(1)
            }
            AndroidKeyEvent.KEYCODE_DPAD_UP,
            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
            AndroidKeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                moveInputFocus(-1)
            }
            else -> false
        }
    }

    val schedule = state.selectedSchedule
    val product = state.selectedProduct
    val quantityWarning = viewModel.quantityWarningMessage()
    val pendingTotalPieceQuantity = if (schedule != null && product != null) {
        state.pendingInspectionDetails
            .filter { detail ->
                if (schedule.isUnplanned) {
                    detail.incomingScheduleId == null && detail.itemId == product.itemId
                } else {
                    detail.incomingScheduleId == schedule.id
                }
            }
            .sumOf { detail -> detail.totalPieceQuantity }
    } else {
        0
    }

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Show success message
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${state.selectedWarehouse?.name ?: ""} 入庫処理",
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            FunctionKeyBar(
                f1 = FunctionKey("賞味") { showDatePicker = true },
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = FunctionKey("登録") {
                    if (viewModel.canSubmit()) {
                        viewModel.submitEntry(onSubmitSuccess)
                    }
                },
                f4 = null
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (schedule == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("スケジュールが選択されていません")
            }
            return@Scaffold
        }

        // Loading overlay
        if (state.isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("処理中...")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false

                    when (event.key) {
                        Key.F2 -> {
                            SoundUtils.playBeep()
                            onNavigateBack()
                            true
                        }
                        Key.F3 -> {
                            SoundUtils.playBeep()
                            // F3 = 登録
                            if (viewModel.canSubmit()) {
                                viewModel.submitEntry(onSubmitSuccess)
                            }
                            true
                        }
                        Key.F1 -> {
                            SoundUtils.playBeep()
                            // F1 = 賞味期限カレンダー表示
                            showDatePicker = true
                            true
                        }
                        Key.DirectionDown, Key.DirectionRight, Key.Tab -> {
                            moveInputFocus(1)
                        }
                        Key.DirectionUp, Key.DirectionLeft -> {
                            moveInputFocus(-1)
                        }
                        else -> false
                    }
                }
        ) {
            // Product info header
            ProductInfoHeader(
                janCode = product?.primaryJanCode,
                itemCode = product?.itemCode,
                itemName = product?.itemName
            )

            HorizontalDivider()

            // Arrival date display
            ArrivalDateBar(
                orderDate = schedule.orderDate,
                expectedDate = schedule.expectedArrivalDate
                    ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                pendingTotalPieceQuantity = pendingTotalPieceQuantity
            )

            HorizontalDivider()

            // Date picker dialog
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState()
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    viewModel.onExpirationDateChange(
                                        date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    )
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("キャンセル")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Input form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity input (case/piece)
                CasePieceQuantityInputFields(
                    caseValue = state.inputCaseQuantity,
                    pieceValue = state.inputPieceQuantity,
                    onCaseValueChange = viewModel::onCaseQuantityChange,
                    onPieceValueChange = viewModel::onPieceQuantityChange,
                    expectedQuantity = schedule.remainingPieceQuantity ?: schedule.remainingQuantity,
                    capacityCase = schedule.capacityCase ?: product?.capacityCase,
                    isUnplanned = schedule.isUnplanned,
                    warningMessage = quantityWarning,
                    focusRequester = quantityFocusRequester,
                    pieceFocusRequester = pieceFocusRequester,
                    isCaseFocusTarget = currentFieldIndex == 0,
                    isPieceFocusTarget = currentFieldIndex == 1,
                    onCaseFocusChanged = { if (it) currentFieldIndex = 0 },
                    onPieceFocusChanged = { if (it) currentFieldIndex = 1 },
                    onMovePrevious = { moveInputFocus(-1) },
                    onMoveNext = { moveInputFocus(1) }
                )

                // Expiration date input with calendar
                ExpirationDateField(
                    value = state.inputExpirationDate,
                    onValueChange = viewModel::onExpirationDateChange,
                    onCalendarClick = { showDatePicker = true },
                    focusRequester = expirationFocusRequester,
                    isFocusTarget = currentFieldIndex == 2,
                    onFocusChanged = { if (it) currentFieldIndex = 2 },
                    onMovePrevious = { moveInputFocus(-1) },
                    onMoveNext = { moveInputFocus(1) }
                )

                // Location is fixed to the incoming default location.
                LocationDisplayField(
                    value = state.inputLocationSearch,
                    locationId = state.inputLocationId
                )
            }
        }
    }
}

@Composable
private fun ProductInfoHeader(
    janCode: String?,
    itemCode: String?,
    itemName: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // JAN code and item code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = janCode ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "(${itemCode ?: ""})",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Item name
        Text(
            text = itemName ?: "",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArrivalDateBar(
    orderDate: String?,
    expectedDate: String,
    pendingTotalPieceQuantity: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = listOfNotNull(
                    orderDate?.let { "発注日: $it" },
                    "予定日: $expectedDate"
                ).joinToString("  "),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "送信前 入荷総バラ数: $pendingTotalPieceQuantity",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpirationDateField(
    value: String,
    onValueChange: (String) -> Unit,
    onCalendarClick: () -> Unit,
    focusRequester: FocusRequester,
    isFocusTarget: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onMovePrevious: () -> Boolean,
    onMoveNext: () -> Boolean
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "賞味期限 (任意)",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HandyNumericEditText(
                value = value,
                onValueChange = onValueChange,
                placeholder = "YYYYMMDD",
                maxLength = 8,
                focusRequester = focusRequester,
                isFocusTarget = isFocusTarget,
                onFocusChanged = onFocusChanged,
                onMovePrevious = onMovePrevious,
                onMoveNext = onMoveNext,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "カレンダーを開く"
                )
            }
        }
    }
}

@Composable
private fun LocationDisplayField(
    value: String,
    locationId: Int?
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ロケ",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = value.ifBlank { "デフォルトロケ未設定" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = locationId?.let { "ロケID: $it / 変更不可" } ?: "変更不可",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CasePieceQuantityInputFields(
    caseValue: String,
    pieceValue: String,
    onCaseValueChange: (String) -> Unit,
    onPieceValueChange: (String) -> Unit,
    expectedQuantity: Int,
    capacityCase: Int?,
    isUnplanned: Boolean,
    warningMessage: String?,
    focusRequester: FocusRequester,
    pieceFocusRequester: FocusRequester,
    isCaseFocusTarget: Boolean,
    isPieceFocusTarget: Boolean,
    onCaseFocusChanged: (Boolean) -> Unit,
    onPieceFocusChanged: (Boolean) -> Unit,
    onMovePrevious: () -> Boolean,
    onMoveNext: () -> Boolean
) {
    val capacity = capacityCase?.takeIf { it > 1 } ?: 1
    val caseQty = caseValue.toIntOrNull() ?: 0
    val pieceQty = pieceValue.toIntOrNull() ?: 0
    val totalPiece = caseQty * capacity + pieceQty
    val isValid = totalPiece > 0

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isUnplanned) {
                    "入庫数量"
                } else {
                    "入庫予定 : ${formatCasePiece(expectedQuantity, capacityCase)}"
                },
                style = MaterialTheme.typography.labelMedium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HandyNumericEditText(
                value = caseValue,
                onValueChange = onCaseValueChange,
                label = "ケース",
                isError = (caseValue.isNotEmpty() || pieceValue.isNotEmpty()) && !isValid,
                focusRequester = focusRequester,
                isFocusTarget = isCaseFocusTarget,
                onFocusChanged = onCaseFocusChanged,
                onMovePrevious = onMovePrevious,
                onMoveNext = onMoveNext,
                modifier = Modifier.weight(1f)
            )
            HandyNumericEditText(
                value = pieceValue,
                onValueChange = onPieceValueChange,
                label = "バラ",
                isError = (caseValue.isNotEmpty() || pieceValue.isNotEmpty()) && !isValid,
                focusRequester = pieceFocusRequester,
                isFocusTarget = isPieceFocusTarget,
                onFocusChanged = onPieceFocusChanged,
                onMovePrevious = onMovePrevious,
                onMoveNext = onMoveNext,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "総バラ: $totalPiece",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        if ((caseValue.isNotEmpty() || pieceValue.isNotEmpty()) && !isValid) {
            Text(
                text = "1以上の数量を入力してください",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        warningMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun HandyNumericEditText(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    maxLength: Int? = null,
    isError: Boolean = false,
    focusRequester: FocusRequester,
    isFocusTarget: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    onMovePrevious: () -> Boolean,
    onMoveNext: () -> Boolean
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFocusChanged by rememberUpdatedState(onFocusChanged)
    val currentOnMovePrevious by rememberUpdatedState(onMovePrevious)
    val currentOnMoveNext by rememberUpdatedState(onMoveNext)
    val currentValue by rememberUpdatedState(value)
    val currentMaxLength by rememberUpdatedState(maxLength)
    val density = LocalDensity.current
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error.toArgb()
    } else {
        MaterialTheme.colorScheme.outline.toArgb()
    }
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val strokeWidth = (density.density * 1.5f).toInt().coerceAtLeast(1)

    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusRequester(focusRequester),
            factory = { context ->
                EditText(context).apply {
                    applyHandyNumericInputSettings()
                    hint = placeholder.orEmpty()
                    setText(normalizeNumericInput(value, maxLength))
                    setSelection(text.length)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(s: Editable?) {
                            val raw = s?.toString().orEmpty()
                            val normalized = normalizeNumericInput(raw, currentMaxLength)
                            if (raw != normalized) {
                                setText(normalized)
                                setSelection(normalized.length)
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
                            post {
                                selectAll()
                                hideSoftwareKeyboard(this)
                            }
                        }
                        currentOnFocusChanged(hasFocus)
                    }
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            performClick()
                            setShowSoftInputOnFocus(false)
                            requestFocus()
                            post {
                                selectAll()
                                hideSoftwareKeyboard(this)
                            }
                        }
                        true
                    }
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action != AndroidKeyEvent.ACTION_DOWN) {
                            return@setOnKeyListener false
                        }
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                            AndroidKeyEvent.KEYCODE_NAVIGATE_NEXT,
                            AndroidKeyEvent.KEYCODE_TAB -> currentOnMoveNext()
                            AndroidKeyEvent.KEYCODE_DPAD_UP,
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                            AndroidKeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> currentOnMovePrevious()
                            else -> false
                        }
                    }
                }
            },
            update = { editText ->
                val normalizedValue = normalizeNumericInput(value, maxLength)
                editText.setShowSoftInputOnFocus(false)
                editText.hint = placeholder.orEmpty()
                editText.setTextColor(textColor)
                editText.setHintTextColor(hintColor)
                editText.background = createHandyEditTextBackground(
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
                    strokeWidth = strokeWidth
                )
                if (editText.text.toString() != normalizedValue) {
                    editText.setText(normalizedValue)
                    if (editText.hasFocus()) {
                        editText.selectAll()
                    } else {
                        editText.setSelection(normalizedValue.length)
                    }
                }
                if (isFocusTarget && !editText.hasFocus()) {
                    editText.requestFocus()
                    editText.post {
                        editText.setShowSoftInputOnFocus(false)
                        editText.selectAll()
                        hideSoftwareKeyboard(editText)
                    }
                } else if (editText.hasFocus()) {
                    editText.post { hideSoftwareKeyboard(editText) }
                }
            }
        )
    }
}

private fun EditText.applyHandyNumericInputSettings() {
    inputType = InputType.TYPE_CLASS_NUMBER
    imeOptions = EditorInfo.IME_ACTION_NONE
    gravity = Gravity.CENTER_VERTICAL
    isSingleLine = true
    setSelectAllOnFocus(true)
    setShowSoftInputOnFocus(false)
    isFocusable = true
    isFocusableInTouchMode = true
    isCursorVisible = true
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
    setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
    setPadding(12, 0, 12, 0)
}

private fun normalizeNumericInput(value: String, maxLength: Int?): String {
    val digits = value.filter { it.isDigit() }
    return maxLength?.let { digits.take(it) } ?: digits
}

private fun createHandyEditTextBackground(
    backgroundColor: Int,
    borderColor: Int,
    strokeWidth: Int
): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(backgroundColor)
        setStroke(strokeWidth, borderColor)
        cornerRadius = 0f
    }
}

private fun hideSoftwareKeyboard(view: View) {
    val inputMethodManager = view.context
        .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
}

private fun formatCasePiece(quantity: Int, capacityCase: Int?): String {
    val capacity = capacityCase?.takeIf { it > 1 } ?: return "バラ $quantity"
    return "ケース ${quantity / capacity} / バラ ${quantity % capacity}"
}
