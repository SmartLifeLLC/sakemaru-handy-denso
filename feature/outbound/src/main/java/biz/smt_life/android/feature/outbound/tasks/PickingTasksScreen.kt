package biz.smt_life.android.feature.outbound.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import biz.smt_life.android.core.domain.model.PickingTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Design colors from standalone HTML
private val AmberBg = Color(0xFFFFFBEB)       // amber-50
private val AmberBorder = Color(0xFFFDE68A)   // amber-200
private val AmberText = Color(0xFF92400E)     // amber-800
private val Amber600 = Color(0xFFD97706)      // amber-600
private val Amber700 = Color(0xFFB45309)      // amber-700
private val Amber100 = Color(0xFFFEF3C7)      // amber-100
private val Amber500 = Color(0xFFF59E0B)      // amber-500
private val EmeraldBg = Color(0xFFECFDF5)     // emerald-50
private val EmeraldBorder = Color(0xFF6EE7B7) // emerald-300
private val Emerald600 = Color(0xFF059669)    // emerald-600
private val Emerald800 = Color(0xFF065F46)    // emerald-800
private val Emerald100 = Color(0xFFD1FAE5)    // emerald-100
private val Neutral200 = Color(0xFFE5E7EB)    // neutral-200
private val Neutral500 = Color(0xFF6B7280)    // neutral-500
private val Neutral600 = Color(0xFF4B5563)    // neutral-600
private val Neutral900 = Color(0xFF171717)    // neutral-900
private val Purple700 = Color(0xFF7C3AED)     // purple-700
private val Blue700 = Color(0xFF1D4ED8)       // blue-700
private val Emerald700 = Color(0xFF047857)    // emerald-700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingTasksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDataInput: (taskId: Int) -> Unit,
    onNavigateToHistory: (taskId: Int) -> Unit,
    viewModel: PickingTasksViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isStartingTask by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F4) // neutral-100
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Amber header
            CourseSelectionHeader(state = state)

            // Content
            Box(modifier = Modifier.weight(1f)) {
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
                                        CoroutineScope(Dispatchers.Main).launch {
                                            snackbarHostState.showSnackbar(
                                                message = errorMessage,
                                                duration = SnackbarDuration.Short
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

            // Footer bar
            CourseSelectionFooter(onNavigateBack = onNavigateBack)
        }
    }
}

@Composable
private fun CourseSelectionHeader(state: PickingTasksState) {
    val tasks = (state.tasksState as? TaskListState.Success)?.tasks ?: emptyList()
    val completedCount = tasks.count { it.isCompleted }
    val incompleteCount = tasks.size - completedCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmberBg)
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "出庫 - コース選択",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AmberText
            )
        }

        if (tasks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed badge
                Text(
                    text = "完了$completedCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald800,
                    modifier = Modifier
                        .background(Emerald100, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                // Incomplete badge
                Text(
                    text = "未完$incompleteCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber700,
                    modifier = Modifier
                        .background(Amber100, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
    // Bottom border line
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AmberBorder)
    )
}

@Composable
private fun CourseSelectionFooter(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Neutral900)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // F4: 戻る (active)
        FooterButton(
            label = "戻る",
            keyHint = "F4",
            backgroundColor = Neutral600,
            keyColor = Color(0xFFD4D4D4), // neutral-300
            onClick = onNavigateBack,
            modifier = Modifier.weight(1f)
        )
        // F3: 画像 (disabled)
        FooterButton(
            label = "画像",
            keyHint = "F3",
            backgroundColor = Purple700,
            keyColor = Color(0xFFC4B5FD), // purple-300
            enabled = false,
            modifier = Modifier.weight(1f)
        )
        // F1: 確定 (disabled)
        FooterButton(
            label = "確定",
            keyHint = "F1",
            backgroundColor = Emerald700,
            keyColor = Color(0xFF6EE7B7), // emerald-300
            enabled = false,
            modifier = Modifier.weight(1f)
        )
        // F2: 履歴 (disabled)
        FooterButton(
            label = "履歴",
            keyHint = "F2",
            backgroundColor = Blue700,
            keyColor = Color(0xFF93C5FD), // blue-300
            enabled = false,
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
            .clickable(enabled = enabled) { onClick() }
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

@OptIn(ExperimentalMaterial3Api::class)
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
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tasks, key = { it.taskId }) { task ->
                CourseCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    enabled = !isStartingTask
                )
            }
        }
    }
}

@Composable
private fun CourseCard(
    task: PickingTask,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val isCompleted = task.isCompleted
    val remainingCount = task.totalItems - task.registeredCount

    val cardBg = if (isCompleted) EmeraldBg else Color.White
    val cardBorder = if (isCompleted) EmeraldBorder else Neutral200
    val nameColor = if (isCompleted) Emerald800 else Color.Unspecified

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(6.dp)
    ) {
        // Row 1: Icon + Course name + Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "完了",
                        tint = Emerald600,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    // Truck icon placeholder - use text since we don't have the icon
                    Text(
                        text = "🚚",
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = task.courseName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status badge
            if (isCompleted) {
                Text(
                    text = "完了",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald600,
                    modifier = Modifier
                        .background(Emerald100, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            } else {
                Text(
                    text = "残${remainingCount}件",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber700,
                    modifier = Modifier
                        .background(Amber100, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: Progress text + Progress bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "検品: ${task.registeredCount}/${task.totalItems}",
                fontSize = 9.sp,
                color = Neutral500
            )

            if (!isCompleted && task.totalItems > 0) {
                LinearProgressIndicator(
                    progress = { task.registeredCount.toFloat() / task.totalItems.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Amber500,
                    trackColor = Neutral200
                )
            }
        }
    }
}
