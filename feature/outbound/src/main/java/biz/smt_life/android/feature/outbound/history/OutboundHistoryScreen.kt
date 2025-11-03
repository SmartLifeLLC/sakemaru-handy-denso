package biz.smt_life.android.feature.outbound.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.HistoryFilter
import biz.smt_life.android.core.domain.model.HistoryStatus
import biz.smt_life.android.core.domain.model.OutboundHistoryEntry
import biz.smt_life.android.feature.outbound.history.OutboundHistoryContract.Effect
import biz.smt_life.android.feature.outbound.history.OutboundHistoryContract.Event
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (courseId: String, itemId: String) -> Unit,
    viewModel: OutboundHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var dialogEntry by remember { mutableStateOf<OutboundHistoryEntry?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is Effect.NavigateToEntry -> {
                    onNavigateToEntry(effect.courseId, effect.itemId)
                }
                is Effect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is Effect.ShowUnconfirmDialog -> {
                    dialogEntry = effect.entry
                }
            }
        }
    }

    // Unconfirm dialog
    if (dialogEntry != null) {
        AlertDialog(
            onDismissRequest = { dialogEntry = null },
            title = { Text("確定取消") },
            text = { Text("確定を取消しますか？\n取消後、再度編集が可能になります。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val entry = dialogEntry!!
                        dialogEntry = null
                        viewModel.onEvent(Event.UnconfirmConfirmed(entry.courseId))
                    }
                ) {
                    Text("取消")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogEntry = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出庫履歴") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter chips
            FilterChipsRow(
                currentFilter = state.filter,
                onFilterChanged = { viewModel.onEvent(Event.FilterChanged(it)) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "エラー: ${state.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                state.entries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getEmptyMessage(state.filter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    HistoryList(
                        entries = state.entries,
                        onRowTapped = { viewModel.onEvent(Event.RowTapped(it)) },
                        onUnconfirmTapped = { viewModel.onEvent(Event.UnconfirmRequested(it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    currentFilter: HistoryFilter,
    onFilterChanged: (HistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter.show == HistoryFilter.Show.UNCONFIRMED_ONLY,
            onClick = { onFilterChanged(HistoryFilter(HistoryFilter.Show.UNCONFIRMED_ONLY)) },
            label = { Text("未確定のみ") }
        )

        FilterChip(
            selected = currentFilter.show == HistoryFilter.Show.ALL,
            onClick = { onFilterChanged(HistoryFilter(HistoryFilter.Show.ALL)) },
            label = { Text("全て") }
        )

        FilterChip(
            selected = currentFilter.show == HistoryFilter.Show.CONFIRMED_ONLY,
            onClick = { onFilterChanged(HistoryFilter(HistoryFilter.Show.CONFIRMED_ONLY)) },
            label = { Text("確定のみ") }
        )
    }
}

@Composable
private fun HistoryList(
    entries: List<OutboundHistoryEntry>,
    onRowTapped: (OutboundHistoryEntry) -> Unit,
    onUnconfirmTapped: (OutboundHistoryEntry) -> Unit
) {
    // Group by course
    val grouped = entries.groupBy { it.courseId to it.courseName }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        grouped.forEach { (courseInfo, courseEntries) ->
            // Course header
            item(key = "header_${courseInfo.first}") {
                Text(
                    text = "${courseInfo.second} (${courseInfo.first})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Items in course
            items(
                items = courseEntries,
                key = { it.id }
            ) { entry ->
                HistoryCard(
                    entry = entry,
                    onRowTapped = { onRowTapped(entry) },
                    onUnconfirmTapped = { onUnconfirmTapped(entry) }
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: OutboundHistoryEntry,
    onRowTapped: () -> Unit,
    onUnconfirmTapped: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Item name & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                StatusBadge(status = entry.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer
            InfoRow(label = "得意先", value = entry.customerName)

            // Quantities
            val totalEach = entry.qtyCase * entry.packSize + entry.qtyEach
            InfoRow(
                label = "数量",
                value = "ケース: ${entry.qtyCase} / バラ: ${entry.qtyEach} (合計: ${totalEach}個)"
            )

            // Location & Area
            if (entry.location.isNotEmpty() && entry.area.isNotEmpty()) {
                InfoRow(label = "ロケ/エリア", value = "${entry.location} / ${entry.area}")
            }

            // Updated at
            val formatter = DateTimeFormatter
                .ofPattern("yyyy/MM/dd HH:mm", Locale.JAPAN)
                .withZone(ZoneId.of("Asia/Tokyo"))
            InfoRow(
                label = "最終更新",
                value = formatter.format(entry.updatedAt)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRowTapped,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("編集へ")
                }

                if (entry.status == HistoryStatus.CONFIRMED) {
                    OutlinedButton(
                        onClick = onUnconfirmTapped,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("確定取消")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun StatusBadge(status: HistoryStatus) {
    val (text, color) = when (status) {
        HistoryStatus.UNREGISTERED -> "未登録" to MaterialTheme.colorScheme.error
        HistoryStatus.REGISTERED_UNCONFIRMED -> "登録済み" to MaterialTheme.colorScheme.tertiary
        HistoryStatus.CONFIRMED -> "確定済み" to MaterialTheme.colorScheme.primary
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

private fun getEmptyMessage(filter: HistoryFilter): String {
    return when (filter.show) {
        HistoryFilter.Show.UNCONFIRMED_ONLY -> "未確定の履歴がありません"
        HistoryFilter.Show.ALL -> "履歴がありません"
        HistoryFilter.Show.CONFIRMED_ONLY -> "確定済みの履歴がありません"
    }
}
