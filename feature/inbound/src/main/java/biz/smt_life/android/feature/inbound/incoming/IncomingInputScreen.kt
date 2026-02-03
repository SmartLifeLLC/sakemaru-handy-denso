package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.Location
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
    onSubmitSuccess: () -> Unit,
    viewModel: IncomingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    val quantityFocusRequester = remember { FocusRequester() }
    val expirationFocusRequester = remember { FocusRequester() }
    val locationFocusRequester = remember { FocusRequester() }

    // Track current focused field index (0: quantity, 1: expiration, 2: location)
    var currentFieldIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = listOf(quantityFocusRequester, expirationFocusRequester, locationFocusRequester)

    // Request focus on quantity field when displayed
    LaunchedEffect(Unit) {
        quantityFocusRequester.requestFocus()
    }

    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }


    val schedule = state.selectedSchedule
    val product = state.selectedProduct

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
                    when (event.key) {
                        Key.F2 -> {
                            onNavigateBack()
                            true
                        }
                        Key.F3 -> {
                            // F3 = 登録
                            if (viewModel.canSubmit()) {
                                viewModel.submitEntry(onSubmitSuccess)
                            }
                            true
                        }
                        Key.F1 -> {
                            // F1 = 賞味期限カレンダー表示
                            showDatePicker = true
                            true
                        }
                        Key.DirectionDown, Key.Tab -> {
                            // Move focus to next field
                            if (currentFieldIndex < focusRequesters.size - 1) {
                                currentFieldIndex++
                                focusRequesters[currentFieldIndex].requestFocus()
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            // Move focus to previous field
                            if (currentFieldIndex > 0) {
                                currentFieldIndex--
                                focusRequesters[currentFieldIndex].requestFocus()
                            }
                            true
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
                arrivalDate = schedule.expectedArrivalDate
                    ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
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
                // Quantity input (first)
                QuantityInputField(
                    value = state.inputQuantity,
                    onValueChange = viewModel::onQuantityChange,
                    expectedQuantity = schedule.remainingQuantity,
                    focusRequester = quantityFocusRequester,
                    onFocusChanged = { if (it) currentFieldIndex = 0 }
                )

                // Expiration date input with calendar
                ExpirationDateField(
                    value = state.inputExpirationDate,
                    onValueChange = viewModel::onExpirationDateChange,
                    onCalendarClick = { showDatePicker = true },
                    focusRequester = expirationFocusRequester,
                    onFocusChanged = { if (it) currentFieldIndex = 1 }
                )

                // Location input with autocomplete
                LocationInputField(
                    value = state.inputLocationSearch,
                    onValueChange = viewModel::onLocationSearchChange,
                    suggestions = state.locationSuggestions,
                    isLoading = state.isLoadingLocations,
                    onLocationSelected = viewModel::selectLocation,
                    focusRequester = locationFocusRequester,
                    onFocusChanged = { if (it) currentFieldIndex = 2 }
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
private fun ArrivalDateBar(arrivalDate: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "入荷日: $arrivalDate",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                )
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
    onFocusChanged: (Boolean) -> Unit
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

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onCalendarClick) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "カレンダーを開く"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
    }
}

@Composable
private fun LocationInputField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<Location>,
    isLoading: Boolean,
    onLocationSelected: (Location) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit
) {
    var showSuggestions by remember { mutableStateOf(false) }

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

        Box {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    showSuggestions = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                        if (!focusState.isFocused) {
                            showSuggestions = false
                        }
                    },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(24.dp)
                                .height(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                placeholder = { Text("ロケーション検索") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Suggestions dropdown
            DropdownMenu(
                expanded = showSuggestions && suggestions.isNotEmpty(),
                onDismissRequest = { showSuggestions = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                suggestions.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Text(location.displayName ?: location.fullDisplayName)
                        },
                        onClick = {
                            onLocationSelected(location)
                            showSuggestions = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityInputField(
    value: String,
    onValueChange: (String) -> Unit,
    expectedQuantity: Int,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit
) {
    val currentQty = value.toIntOrNull() ?: 0
    val isValid = currentQty > 0 && currentQty <= expectedQuantity

    // Use TextFieldValue for select-all on focus
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(0, value.length)))
    }

    // Update when external value changes
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, TextRange(0, value.length))
        }
    }

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
                text = "入庫予定 : $expectedQuantity",
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onValueChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                    // Select all when gaining focus
                    if (focusState.isFocused) {
                        textFieldValue = textFieldValue.copy(
                            selection = TextRange(0, textFieldValue.text.length)
                        )
                    }
                },
            placeholder = { Text("入庫数量") },
            singleLine = true,
            isError = value.isNotEmpty() && !isValid,
            supportingText = if (value.isNotEmpty() && !isValid) {
                {
                    Text(
                        text = when {
                            currentQty <= 0 -> "1以上の数量を入力してください"
                            currentQty > expectedQuantity -> "予定数を超えています"
                            else -> ""
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
    }
}
