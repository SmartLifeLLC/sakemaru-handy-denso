package biz.smt_life.android.feature.login

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent as AndroidKeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    val hostUrl by viewModel.hostUrl.collectAsState(initial = "")
    var staffEditText by remember { mutableStateOf<EditText?>(null) }
    var passwordEditText by remember { mutableStateOf<EditText?>(null) }

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

    LaunchedEffect(staffEditText, state.isLoading) {
        if (!state.isLoading) {
            focusLoginEditText(staffEditText)
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

                NoSoftKeyboardLoginField(
                    value = state.staffCode,
                    onValueChange = viewModel::onStaffCodeChange,
                    label = "スタッフコード",
                    enabled = !state.isLoading,
                    isPassword = false,
                    onEditTextReady = { staffEditText = it },
                    onEnter = { focusLoginEditText(passwordEditText) },
                    onMoveNext = { focusLoginEditText(passwordEditText) },
                    onMovePrevious = {},
                    modifier = Modifier.fillMaxWidth()
                )

//                Spacer(modifier = Modifier.height(4.dp))

                NoSoftKeyboardLoginField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "パスワード",
                    enabled = !state.isLoading,
                    isPassword = true,
                    onEditTextReady = { passwordEditText = it },
                    onEnter = {
                        SoundUtils.playBeep()
                        viewModel.login()
                    },
                    onMoveNext = {},
                    onMovePrevious = { focusLoginEditText(staffEditText) },
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
    var staffEditText by remember { mutableStateOf<EditText?>(null) }
    var passwordEditText by remember { mutableStateOf<EditText?>(null) }

    LaunchedEffect(staffEditText, isLoading) {
        if (!isLoading) {
            focusLoginEditText(staffEditText)
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

                NoSoftKeyboardLoginField(
                    value = staffCode,
                    onValueChange = onStaffCodeChange,
                    label = "スタッフコード",
                    enabled = !isLoading,
                    isPassword = false,
                    onEditTextReady = { staffEditText = it },
                    onEnter = { focusLoginEditText(passwordEditText) },
                    onMoveNext = { focusLoginEditText(passwordEditText) },
                    onMovePrevious = {},
                    modifier = Modifier.fillMaxWidth()
                )

//                Spacer(modifier = Modifier.height(4.dp))

                NoSoftKeyboardLoginField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "パスワード",
                    enabled = !isLoading,
                    isPassword = true,
                    onEditTextReady = { passwordEditText = it },
                    onEnter = {
                        SoundUtils.playBeep()
                        onLogin()
                    },
                    onMoveNext = {},
                    onMovePrevious = { focusLoginEditText(staffEditText) },
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

@Composable
private fun NoSoftKeyboardLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    isPassword: Boolean,
    onEditTextReady: (EditText) -> Unit,
    onEnter: () -> Unit,
    onMoveNext: () -> Unit,
    onMovePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnEditTextReady by rememberUpdatedState(onEditTextReady)
    val currentOnEnter by rememberUpdatedState(onEnter)
    val currentOnMoveNext by rememberUpdatedState(onMoveNext)
    val currentOnMovePrevious by rememberUpdatedState(onMovePrevious)

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            factory = { context ->
                EditText(context).apply {
                    applyLoginInputSettings(isPassword)
                    isEnabled = enabled
                    setText(value)
                    setSelection(text?.length ?: 0)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(s: Editable?) {
                            if (tag == true) return

                            val nextValue = s?.toString().orEmpty()
                            if (nextValue != currentValue) {
                                currentOnValueChange(nextValue)
                            }
                        }
                    })
                    setOnFocusChangeListener { _, hasFocus ->
                        setShowSoftInputOnFocus(false)
                        if (hasFocus) {
                            post {
                                setSelection(text?.length ?: 0)
                                hideLoginSoftwareKeyboard(this)
                            }
                        }
                    }
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_UP && isEnabled) {
                            performClick()
                            setShowSoftInputOnFocus(false)
                            requestFocus()
                            post {
                                setSelection(text?.length ?: 0)
                                hideLoginSoftwareKeyboard(this)
                            }
                        }
                        true
                    }
                    setOnEditorActionListener { _, actionId, event ->
                        val isEnter = event != null &&
                            (event.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                                event.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER)
                        if (actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
                            if (event == null || event.action == AndroidKeyEvent.ACTION_DOWN) {
                                currentOnEnter()
                            }
                            true
                        } else {
                            false
                        }
                    }
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action != AndroidKeyEvent.ACTION_DOWN) {
                            return@setOnKeyListener isLoginHandledKey(keyCode)
                        }
                        when (keyCode) {
                            AndroidKeyEvent.KEYCODE_ENTER,
                            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                            AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                currentOnEnter()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                            AndroidKeyEvent.KEYCODE_NAVIGATE_NEXT,
                            AndroidKeyEvent.KEYCODE_TAB -> {
                                currentOnMoveNext()
                                true
                            }
                            AndroidKeyEvent.KEYCODE_DPAD_UP,
                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                            AndroidKeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                                currentOnMovePrevious()
                                true
                            }
                            else -> false
                        }
                    }
                    currentOnEditTextReady(this)
                }
            },
            update = { editText ->
                editText.setShowSoftInputOnFocus(false)
                editText.isEnabled = enabled
                editText.alpha = if (enabled) 1f else 0.45f
                editText.applyLoginInputSettings(isPassword)
                if (editText.text.toString() != value) {
                    editText.tag = true
                    editText.setText(value)
                    editText.setSelection(value.length)
                    editText.tag = false
                }
                if (editText.hasFocus()) {
                    editText.post { hideLoginSoftwareKeyboard(editText) }
                }
            }
        )
    }
}

private fun EditText.applyLoginInputSettings(isPassword: Boolean) {
    inputType = loginInputType(isPassword)
    transformationMethod = if (isPassword) {
        PasswordTransformationMethod.getInstance()
    } else {
        null
    }
    imeOptions = EditorInfo.IME_ACTION_NONE
    gravity = Gravity.CENTER_VERTICAL
    isSingleLine = true
    setShowSoftInputOnFocus(false)
    isFocusable = true
    isFocusableInTouchMode = true
    isCursorVisible = true
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
    setTextColor(AndroidColor.rgb(23, 32, 51))
    setHintTextColor(AndroidColor.rgb(84, 110, 122))
    setPadding(12, 0, 12, 0)
    background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(AndroidColor.WHITE)
        setStroke(2, AndroidColor.rgb(117, 117, 117))
        cornerRadius = 0f
    }
}

private fun loginInputType(isPassword: Boolean): Int =
    InputType.TYPE_CLASS_TEXT or
        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
        (if (isPassword) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        })

private fun focusLoginEditText(editText: EditText?) {
    editText?.post {
        editText.setShowSoftInputOnFocus(false)
        editText.requestFocus()
        editText.setSelection(editText.text?.length ?: 0)
        hideLoginSoftwareKeyboard(editText)
    }
}

private fun hideLoginSoftwareKeyboard(view: View) {
    val inputMethodManager = view.context
        .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
}

private fun isLoginHandledKey(keyCode: Int): Boolean =
    keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN ||
        keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT ||
        keyCode == AndroidKeyEvent.KEYCODE_NAVIGATE_NEXT ||
        keyCode == AndroidKeyEvent.KEYCODE_TAB ||
        keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
        keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
        keyCode == AndroidKeyEvent.KEYCODE_NAVIGATE_PREVIOUS
