package biz.smt_life.android.feature.login

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.designsystem.util.SoundUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Login Screen per designs-p01-0023.md.
 * Card-centered layout with gradient buttons and custom input fields.
 */
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

    LoginScreenContent(
        staffCode = state.staffCode,
        password = state.password,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        appVersion = appVersion,
        today = today,
        hostUrl = hostUrl,
        onStaffCodeChange = viewModel::onStaffCodeChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLogin = {
            SoundUtils.playBeep()
            focusManager.clearFocus()
            viewModel.login()
        },
        onNavigateToSettings = {
            SoundUtils.playBeep()
            onNavigateToSettings()
        }
    )
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, Color(0xFFE0E0E0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ─── ヘッダー ───────────────────────────
                Text(
                    text = "物流管理システム",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A233A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                Text(
                    text = appVersion,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ─── 入力フィールド ──────────────────────
                LoginInputField(
                    value = staffCode,
                    onValueChange = onStaffCodeChange,
                    placeholder = "担当者コード",
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LoginInputField(
                    value = password,
                    onValueChange = onPasswordChange,
                    placeholder = "パスワード",
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

                // ─── エラーメッセージ ────────────────────
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ─── ログインボタン ──────────────────────
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF007BFF)
                        )
                    }
                } else {
                    LoginGradientButton(
                        text = "ログイン【F1】",
                        textSize = 18,
                        gradient = Brush.verticalGradient(
                            colors = listOf(Color(0xFF007BFF), Color(0xFF0056B3))
                        ),
                        borderColor = Color(0xFF004494),
                        normalBottomBorder = 6.dp,
                        verticalPadding = 18.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onLogin
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ─── 設定ボタン ──────────────────────────
                LoginGradientButton(
                    text = "設定【F2】",
                    textSize = 17,
                    gradient = Brush.verticalGradient(
                        colors = listOf(Color(0xFF6C757D), Color(0xFF4E555B))
                    ),
                    borderColor = Color(0xFF343A40),
                    normalBottomBorder = 5.dp,
                    verticalPadding = 14.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ─── フッター ────────────────────────────
                Text(
                    text = "$today\n$hostUrl",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF444444),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

// ─── ヘルパーComposable: 入力フィールド ──────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 18.sp,
                color = Color(0xFF999999)
            )
        },
        textStyle = TextStyle(fontSize = 18.sp),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6C757D),
            unfocusedBorderColor = Color(0xFF6C757D),
            disabledBorderColor = Color(0xFF6C757D),
            focusedContainerColor = Color(0xFFF8F9FA),
            unfocusedContainerColor = Color(0xFFF8F9FA),
            disabledContainerColor = Color(0xFFF8F9FA)
        ),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

// ─── ヘルパーComposable: グラデーションボタン ────────────
@Composable
private fun LoginGradientButton(
    text: String,
    textSize: Int,
    gradient: Brush,
    borderColor: Color,
    normalBottomBorder: Dp,
    verticalPadding: Dp,
    cornerRadius: Dp = 10.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        label = "buttonOffsetY"
    )
    val currentBottomBorder by animateDpAsState(
        targetValue = if (isPressed) 2.dp else normalBottomBorder,
        label = "buttonBottomBorder"
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .offset(y = offsetY)
            .clip(shape)
            .background(borderColor)
            .padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = currentBottomBorder)
            .clip(shape)
            .background(brush = gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = textSize.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = Offset(1f, 1f),
                    blurRadius = 2f
                )
            )
        )
    }
}

// ─── プレビュー ──────────────────────────────────────────

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
