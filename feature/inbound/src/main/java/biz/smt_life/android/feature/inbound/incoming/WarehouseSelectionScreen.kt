package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingWarehouse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Design colors from HTML prototype (Emerald theme for incoming)
val EmeraldBg = Color(0xFFECFDF5)           // emerald-50
val EmeraldBorder = Color(0xFF6EE7B7)       // emerald-300
val EmeraldBorder200 = Color(0xFFA7F3D0)    // emerald-200
val Emerald100 = Color(0xFFD1FAE5)          // emerald-100
val Emerald400 = Color(0xFF34D399)          // emerald-400
val Emerald600 = Color(0xFF059669)          // emerald-600
val Emerald700 = Color(0xFF047857)          // emerald-700
val Emerald800 = Color(0xFF065F46)          // emerald-800
val Emerald900 = Color(0xFF064E3B)          // emerald-900
val IncomingNeutral100 = Color(0xFFF5F5F4)  // neutral-100
val IncomingNeutral200 = Color(0xFFE5E7EB)  // neutral-200
val IncomingNeutral500 = Color(0xFF6B7280)  // neutral-500
val IncomingNeutral600 = Color(0xFF4B5563)  // neutral-600
val IncomingNeutral900 = Color(0xFF171717)  // neutral-900
val IncomingPurple700 = Color(0xFF7C3AED)   // purple-700
val IncomingRed700 = Color(0xFFB91C1C)      // red-700
val IncomingBlue700 = Color(0xFF1D4ED8)     // blue-700

/**
 * Warehouse Selection Screen for Incoming feature.
 * Displays a list of warehouses for the user to select.
 */
@Composable
fun WarehouseSelectionScreen(
    onNavigateBack: () -> Unit,
    onWarehouseSelected: () -> Unit,
    onLogout: () -> Unit,
    viewModel: IncomingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load warehouses on first composition
    LaunchedEffect(Unit) {
        viewModel.loadWarehouses()
    }

    // Show error message
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = IncomingNeutral100,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Emerald header
            IncomingHeader()

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onKeyEvent { event ->
                        when (event.key) {
                            Key.F2 -> {
                                onNavigateBack()
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                when {
                    state.isLoadingWarehouses -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.warehouses.isEmpty() -> {
                        Text(
                            text = "倉庫がありません",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.warehouses, key = { it.id }) { warehouse ->
                                WarehouseButton(
                                    warehouse = warehouse,
                                    onClick = {
                                        viewModel.selectWarehouse(warehouse)
                                        onWarehouseSelected()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Footer bar
            FunctionKeyBar(
                f4 = FunctionKey("戻る", onNavigateBack, IncomingNeutral600),
                f3 = null,
                f1 = null,
                f2 = FunctionKey("ログアウト", onLogout, IncomingBlue700, Color(0xFF93C5FD))
            )
        }
    }
}

@Composable
private fun WarehouseButton(
    warehouse: IncomingWarehouse,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = {
            SoundUtils.playBeep()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, Color.Gray),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Text(
            text = warehouse.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Common emerald header for all incoming screens.
 */
@Composable
fun IncomingHeader(
    subtitle: String? = null
) {
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.JAPANESE)
    val dateText = "${today.year}/${today.monthValue}/${today.dayOfMonth}（$dayOfWeek）"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmeraldBg)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "入庫",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Emerald800
            )
            Text(
                text = dateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Emerald700,
                modifier = Modifier
                    .background(Emerald100, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = Emerald800
            )
        }
    }
    // Bottom border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(EmeraldBorder200)
    )
}

/**
 * Data class for function key.
 */
data class FunctionKey(
    val label: String,
    val onClick: () -> Unit,
    val backgroundColor: Color = IncomingNeutral600,
    val keyColor: Color = Color(0xFFD4D4D4) // neutral-300
)

/**
 * Function key bar at the bottom of the screen.
 * P20-style footer with neutral-900 background and colored buttons.
 * Button order: F4 | F3 | F1 | F2 (matching HTML prototype layout)
 */
@Composable
fun FunctionKeyBar(
    f4: FunctionKey? = null,
    f3: FunctionKey? = null,
    f1: FunctionKey? = null,
    f2: FunctionKey? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(IncomingNeutral900)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FooterButton(
            label = f4?.label ?: "--",
            keyHint = "F4",
            backgroundColor = f4?.backgroundColor ?: IncomingNeutral600,
            keyColor = f4?.keyColor ?: Color(0xFFD4D4D4),
            enabled = f4 != null,
            onClick = { f4?.onClick?.invoke() },
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = f3?.label ?: "--",
            keyHint = "F3",
            backgroundColor = f3?.backgroundColor ?: IncomingPurple700,
            keyColor = f3?.keyColor ?: Color(0xFFC4B5FD),
            enabled = f3 != null,
            onClick = { f3?.onClick?.invoke() },
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = f1?.label ?: "--",
            keyHint = "F1",
            backgroundColor = f1?.backgroundColor ?: Emerald700,
            keyColor = f1?.keyColor ?: Color(0xFF6EE7B7),
            enabled = f1 != null,
            onClick = { f1?.onClick?.invoke() },
            modifier = Modifier.weight(1f)
        )
        FooterButton(
            label = f2?.label ?: "--",
            keyHint = "F2",
            backgroundColor = f2?.backgroundColor ?: IncomingBlue700,
            keyColor = f2?.keyColor ?: Color(0xFF93C5FD),
            enabled = f2 != null,
            onClick = { f2?.onClick?.invoke() },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FooterButton(
    label: String,
    keyHint: String,
    backgroundColor: Color,
    keyColor: Color = Color(0xFFD4D4D4),
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) {
                SoundUtils.playBeep()
                onClick()
            }
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
