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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.Location
import java.time.LocalDate
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

    val expirationFocusRequester = remember { FocusRequester() }
    val locationFocusRequester = remember { FocusRequester() }
    val quantityFocusRequester = remember { FocusRequester() }

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
                f1 = FunctionKey("自動") { viewModel.setQuantityToExpected() },
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
                        Key.DirectionDown, Key.Tab -> {
                            // Move focus to next field
                            true
                        }
                        Key.DirectionUp -> {
                            // Move focus to previous field
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

            // Input form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expiration date input
                InputField(
                    icon = Icons.Default.DateRange,
                    label = "賞味期限 (任意)",
                    value = state.inputExpirationDate,
                    onValueChange = viewModel::onExpirationDateChange,
                    placeholder = "YYYY-MM-DD",
                    keyboardType = KeyboardType.Number,
                    focusRequester = expirationFocusRequester
                )

                // Location input with autocomplete
                LocationInputField(
                    value = state.inputLocationSearch,
                    onValueChange = viewModel::onLocationSearchChange,
                    suggestions = state.locationSuggestions,
                    isLoading = state.isLoadingLocations,
                    onLocationSelected = viewModel::selectLocation,
                    focusRequester = locationFocusRequester
                )

                // Quantity input
                QuantityInputField(
                    value = state.inputQuantity,
                    onValueChange = viewModel::onQuantityChange,
                    expectedQuantity = schedule.remainingQuantity,
                    focusRequester = quantityFocusRequester
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

@Composable
private fun InputField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
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
    focusRequester: FocusRequester
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
    focusRequester: FocusRequester
) {
    val currentQty = value.toIntOrNull() ?: 0
    val isValid = currentQty > 0 && currentQty <= expectedQuantity

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
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
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
                imeAction = ImeAction.Done
            )
        )
    }
}
