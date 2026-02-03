package biz.smt_life.android.feature.login

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import biz.smt_life.android.core.designsystem.component.HandyTextField
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.designsystem.util.SoundUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Login Screen per Spec 2.1.0.
 * Shows staff code/password fields, version info, date, and host URL.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    appVersion: String = "1.0",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val hostUrl by viewModel.hostUrl.collectAsState(initial = "")

    // Get today's date in Asia/Tokyo timezone
    val today = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"))
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.JAPAN))
        } else {
            java.text.SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Tokyo")
            }.format(java.util.Date())
        }
    }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DENSOハンディ",
                        fontSize = 20.sp
                    ) },
                actions = {
                    IconButton(onClick = {
                        SoundUtils.playBeep()
                        onNavigateToSettings()
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Main content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "倉庫管理システム",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HandyTextField(
                    value = state.staffCode,
                    onValueChange = viewModel::onStaffCodeChange,
                    label = "スタッフコード",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

//                Spacer(modifier = Modifier.height(4.dp))

                HandyTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "パスワード",
                    enabled = !state.isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.login()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        SoundUtils.playBeep()
                        viewModel.login()
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("ログイン", fontSize = 16.sp)
                    }
                }
            }

            // Footer per Spec 2.1.0: Version, Date, Host
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "バージョン $appVersion",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = today,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = hostUrl,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    HandyTheme {
        LoginScreenContent(
            staffCode = "",
            password = "",
            isLoading = false,
            errorMessage = null,
            appVersion = "1.0.0",
            today = "2026/01/26",
            hostUrl = "https://api.example.com",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen - Loading")
@Composable
private fun LoginScreenLoadingPreview() {
    HandyTheme {
        LoginScreenContent(
            staffCode = "staff001",
            password = "password",
            isLoading = true,
            errorMessage = null,
            appVersion = "1.0.0",
            today = "2026/01/26",
            hostUrl = "https://api.example.com",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen - Error")
@Composable
private fun LoginScreenErrorPreview() {
    HandyTheme {
        LoginScreenContent(
            staffCode = "staff001",
            password = "wrongpass",
            isLoading = false,
            errorMessage = "Invalid staff code or password",
            appVersion = "1.0.0",
            today = "2026/01/26",
            hostUrl = "https://api.example.com",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Login Screen - Dark")
@Composable
private fun LoginScreenDarkPreview() {
    HandyTheme(darkTheme = true) {
        LoginScreenContent(
            staffCode = "",
            password = "",
            isLoading = false,
            errorMessage = null,
            appVersion = "1.0.0",
            today = "2026/01/26",
            hostUrl = "https://api.example.com",
            onStaffCodeChange = {},
            onPasswordChange = {},
            onLogin = {},
            onNavigateToSettings = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    staffCode: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    appVersion: String,
    today: String,
    hostUrl: String,
    onStaffCodeChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DENSOハンディ",
                        fontSize = 20.sp
                    ) },
                actions = {
                    IconButton(onClick = {
                        SoundUtils.playBeep()
                        onNavigateToSettings()
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Main content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "倉庫管理システム",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HandyTextField(
                    value = staffCode,
                    onValueChange = onStaffCodeChange,
                    label = "スタッフコード",
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

//                Spacer(modifier = Modifier.height(4.dp))

                HandyTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "パスワード",
                    enabled = !isLoading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLogin()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        SoundUtils.playBeep()
                        onLogin()
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("ログイン", fontSize = 16.sp)
                    }
                }
            }

            // Footer per Spec 2.1.0: Version, Date, Host
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "バージョン $appVersion",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = today,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = hostUrl,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
