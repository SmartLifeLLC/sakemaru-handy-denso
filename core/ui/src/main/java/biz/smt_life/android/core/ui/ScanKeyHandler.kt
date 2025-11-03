package biz.smt_life.android.core.ui

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles keyboard wedge barcode scanner input.
 * Accumulates characters and triggers onScan when Enter/Return is pressed.
 *
 * Usage:
 * ```
 * ScanKeyHandler(onScan = { barcode ->
 *     // Handle scanned barcode
 * })
 * ```
 */
@Composable
fun ScanKeyHandler(onScan: (String) -> Unit) {
    val view = LocalView.current
    var scanBuffer by remember { mutableStateOf("") }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(view) {
        val listener = android.view.View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ENTER -> {
                        if (scanBuffer.isNotEmpty()) {
                            onScan(scanBuffer)
                            scanBuffer = ""
                            resetJob?.cancel()
                        }
                        true
                    }
                    else -> {
                        val char = event.unicodeChar.toChar()
                        if (char.isLetterOrDigit() || char in setOf('-', '_', ' ')) {
                            scanBuffer += char

                            // Reset buffer after 500ms of inactivity
                            resetJob?.cancel()
                            resetJob = CoroutineScope(Dispatchers.Main).launch {
                                delay(500)
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

        onDispose {
            view.setOnKeyListener(null)
            resetJob?.cancel()
        }
    }
}
