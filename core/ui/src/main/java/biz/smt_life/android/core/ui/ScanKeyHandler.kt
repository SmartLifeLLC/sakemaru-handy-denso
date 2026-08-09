package biz.smt_life.android.core.ui

import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ScanKeyHandler"

@Composable
fun ScanKeyHandler(
    onScan: (String) -> Unit,
    onScanStart: () -> Unit = {},
    minScanLength: Int = 1
) {
    val view = LocalView.current
    val currentOnScan by rememberUpdatedState(onScan)
    val currentOnScanStart by rememberUpdatedState(onScanStart)
    val currentMinScanLength by rememberUpdatedState(minScanLength)
    var scanBuffer by remember { mutableStateOf("") }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    Log.d(TAG, "ScanKeyHandler composed (view=${view.javaClass.simpleName}, minLen=$minScanLength)")

    DisposableEffect(view) {
        val listener = android.view.View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> {
                        Log.d(TAG, "ENTER pressed, buffer='$scanBuffer' (len=${scanBuffer.length}, minLen=$currentMinScanLength)")
                        if (scanBuffer.length >= currentMinScanLength) {
                            currentOnScan(scanBuffer)
                            scanBuffer = ""
                            resetJob?.cancel()
                            true
                        } else {
                            scanBuffer = ""
                            resetJob?.cancel()
                            false
                        }
                    }
                    else -> {
                        val char = event.unicodeChar.toChar()
                        if (char.isLetterOrDigit() || char in setOf('-', '_', ' ')) {
                            if (scanBuffer.isEmpty()) {
                                Log.d(TAG, "Scan START (first char='$char')")
                                currentOnScanStart()
                            }
                            scanBuffer += char
                            Log.d(TAG, "Buffer: '$scanBuffer'")

                            resetJob?.cancel()
                            resetJob = CoroutineScope(Dispatchers.Main).launch {
                                delay(500)
                                if (scanBuffer.isNotEmpty()) {
                                    Log.d(TAG, "Buffer TIMEOUT reset (was '$scanBuffer')")
                                }
                                scanBuffer = ""
                            }
                        }
                        false
                    }
                }
            } else {
                false
            }
        }

        view.setOnKeyListener(listener)
        Log.d(TAG, "OnKeyListener SET on ${view.javaClass.simpleName}")

        onDispose {
            view.setOnKeyListener(null)
            Log.d(TAG, "OnKeyListener CLEARED")
            resetJob?.cancel()
        }
    }
}

fun scanKeyEventModifier(
    onScan: (String) -> Unit,
    onScanStart: () -> Unit = {},
    minScanLength: Int = 1
): Modifier {
    var buffer = StringBuilder()
    var resetJob: Job? = null

    return Modifier.onPreviewKeyEvent { event ->
        val keyEvent = event.nativeKeyEvent
        if (keyEvent.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false

        when (event.key) {
            Key.Enter -> {
                val code = buffer.toString()
                Log.d(TAG, "[Modifier] ENTER, buffer='$code'")
                buffer.clear()
                resetJob?.cancel()
                if (code.length >= minScanLength) {
                    onScan(code)
                    true
                } else {
                    false
                }
            }
            else -> {
                val char = keyEvent.unicodeChar.toChar()
                if (char.isLetterOrDigit() || char in setOf('-', '_', ' ')) {
                    if (buffer.isEmpty()) {
                        Log.d(TAG, "[Modifier] Scan START")
                        onScanStart()
                    }
                    buffer.append(char)
                    resetJob?.cancel()
                    resetJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(500)
                        buffer.clear()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }
}
