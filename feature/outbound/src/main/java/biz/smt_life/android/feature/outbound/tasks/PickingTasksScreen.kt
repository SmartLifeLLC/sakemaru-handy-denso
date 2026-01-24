package biz.smt_life.android.feature.outbound.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import kotlinx.coroutines.launch

/**
 * Picking Tasks screen per spec 2.5.1 出庫処理 > ピッキングリスト選択.
 *
 * Features:
 * - Shows only "My tasks" (私の担当) - tasks assigned to current picker
 * - List of tasks with progress (e.g., "5/10")
 * - Status chip based on completion
 * - Status-based navigation:
 *   - PENDING items → Data Input screen
 *   - Only PICKING items → History screen (editable)
 *   - All COMPLETED/SHORTAGE → History screen (read-only)
 * - Pull-to-refresh
 * - Empty and error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingTasksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataInput: (taskId: Int) -> Unit,
    onNavigateToHistory: (taskId: Int) -> Unit,
    viewModel: PickingTasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var isStartingTask by remember { mutableStateOf(false) }

    // Show error messages
    LaunchedEffect(Unit) {
        // Handle errors via snackbar if needed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出庫処理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Content
            when (state.tasksState) {
                is TaskListState.Loading -> LoadingContent()
                is TaskListState.Empty -> EmptyContent()
                is TaskListState.Error -> ErrorContent(
                    message = (state.tasksState as TaskListState.Error).message,
                    onRetry = { viewModel.refresh() }
                )
                is TaskListState.Success -> {
                    val tasks = (state.tasksState as TaskListState.Success).tasks
                    TaskListContent(
                        tasks = tasks,
                        isRefreshing = false,
                        onRefresh = { viewModel.refresh() },
                        onTaskClick = { task ->
                            // Status-based navigation (per spec 2.5.1)
                            isStartingTask = true
                            viewModel.selectTask(
                                task = task,
                                onNavigateToDataInput = { selectedTask ->
                                    isStartingTask = false
                                    onNavigateToDataInput(selectedTask.taskId)
                                },
                                onNavigateToHistory = { selectedTask ->
                                    isStartingTask = false
                                    onNavigateToHistory(selectedTask.taskId)
                                },
                                onError = { errorMessage ->
                                    isStartingTask = false
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        snackbarHostState.showSnackbar(
                                            message = errorMessage,
                                            duration = androidx.compose.material3.SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        },
                        isStartingTask = isStartingTask
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "該当データがありません",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}

@Composable
private fun TaskListContent(
    tasks: List<PickingTask>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTaskClick: (PickingTask) -> Unit,
    isStartingTask: Boolean = false
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tasks, key = { it.taskId }) { task ->
                PickingTaskCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    enabled = !isStartingTask
                )
            }
        }
    }
}

@Composable
private fun PickingTaskCard(
    task: PickingTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Course name and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "(${task.progressText})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Course code
            Text(
                text = "コード: ${task.courseCode}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Picking area
            Text(
                text = "フロア: ${task.pickingAreaName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status chip
            StatusChip(task = task)
        }
    }
}

@Composable
private fun StatusChip(task: PickingTask) {
    val (text, containerColor) = when {
        task.isCompleted -> "完了" to MaterialTheme.colorScheme.primaryContainer
        task.isInProgress -> "進行中" to MaterialTheme.colorScheme.secondaryContainer
        else -> "未着手" to MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
