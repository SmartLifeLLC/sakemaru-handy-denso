package biz.smt_life.android.feature.inbound.incoming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.designsystem.util.SoundUtils
import biz.smt_life.android.core.domain.model.IncomingWarehouse

/**
 * Warehouse Selection Screen for Incoming feature.
 * Displays a list of warehouses for the user to select.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("倉庫選択", fontSize = 14.sp) },
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
                f1 = null,
                f2 = FunctionKey("戻る", onNavigateBack),
                f3 = null,
                f4 = FunctionKey("ログアウト", onLogout)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
            .height(40.dp),
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
 * Data class for function key.
 */
data class FunctionKey(
    val label: String,
    val onClick: () -> Unit
)

/**
 * Function key bar at the bottom of the screen.
 * Displays F1-F4 buttons in a row.
 */
@Composable
fun FunctionKeyBar(
    f1: FunctionKey? = null,
    f2: FunctionKey? = null,
    f3: FunctionKey? = null,
    f4: FunctionKey? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FunctionKeyButton(keyName = "F1", functionKey = f1, modifier = Modifier.weight(1f))
        FunctionKeyButton(keyName = "F2", functionKey = f2, modifier = Modifier.weight(1f))
        FunctionKeyButton(keyName = "F3", functionKey = f3, modifier = Modifier.weight(1f))
        FunctionKeyButton(keyName = "F4", functionKey = f4, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FunctionKeyButton(
    keyName: String,
    functionKey: FunctionKey?,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            SoundUtils.playBeep()
            functionKey?.onClick?.invoke()
        },
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp),
        enabled = functionKey != null,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (functionKey != null) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            contentColor = if (functionKey != null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = keyName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = functionKey?.label ?: "--",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
