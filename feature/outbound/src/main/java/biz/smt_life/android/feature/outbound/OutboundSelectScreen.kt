package biz.smt_life.android.feature.outbound

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 2.5.1 出庫処理＞選択画面
 * Outbound Selection Screen with two main options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutboundSelectScreen(
    onNavigateToPickingList: () -> Unit,
    onNavigateToSlipEntry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("出庫処理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "戻る"
                        )
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Button(
                onClick = onNavigateToPickingList,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Text(
                    text = "ピッキングリスト選択",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Button(
                onClick = onNavigateToSlipEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Text(
                    text = "伝票入力",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewOutboundSelectScreen() {
    MaterialTheme {
        OutboundSelectScreen(
            onNavigateToPickingList = {},
            onNavigateToSlipEntry = {},
            onNavigateBack = {}
        )
    }
}
