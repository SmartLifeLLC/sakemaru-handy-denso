package biz.smt_life.android.feature.outbound

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import biz.smt_life.android.core.domain.model.OutboundCourse

/**
 * 2.5.2 出庫処理＞ピッキングリスト選択
 * Picking List Selection with My Assignments / All toggle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickingListScreen(
    onSelectCourse: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: OutboundViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ピッキングリスト選択") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "戻る")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = if (state.myAssignmentsOnly) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.myAssignmentsOnly,
                    onClick = { viewModel.setMyAssignmentsFilter(true) },
                    text = { Text("私の担当") }
                )
                Tab(
                    selected = !state.myAssignmentsOnly,
                    onClick = { viewModel.setMyAssignmentsFilter(false) },
                    text = { Text("全体") }
                )
            }

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.courses.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("コースがありません")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.courses,
                            key = { it.id }
                        ) { course ->
                            CourseCard(
                                course = course,
                                isSelected = course.id == state.selectedCourseId,
                                onClick = {
                                    viewModel.selectCourse(course.id)
                                    onSelectCourse(course.id)
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
private fun CourseCard(
    course: OutboundCourse,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                course.isConfirmed -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${course.processedCount}/${course.totalCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (course.isConfirmed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "完了",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewPickingListScreen() {
    MaterialTheme {
        PickingListScreen(
            onSelectCourse = {},
            onNavigateBack = {}
        )
    }
}
