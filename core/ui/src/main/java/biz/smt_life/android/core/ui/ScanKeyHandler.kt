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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ScanKeyHandler"

object HardwareKeyDispatcher {
    private data class Entry(
        val id: Long,
        val onKeyEvent: (KeyEvent) -> Boolean
    )

    private val handlers = mutableListOf<Entry>()
    private var nextId = 0L

    fun register(onKeyEvent: (KeyEvent) -> Boolean): () -> Unit {
        val entry = Entry(++nextId, onKeyEvent)
        handlers += entry

        return {
            handlers.remove(entry)
        }
    }

    fun dispatch(event: KeyEvent): Boolean {
        for (entry in handlers.asReversed()) {
            if (entry.onKeyEvent(event)) {
                return true
            }
        }

        return false
    }
}

@Composable
fun ScanKeyHandler(
    onScan: (String) -> Unit,
    onScanStart: () -> Unit = {},
    minScanLength: Int = 1,
    onKeyDown: (keyCode: Int, event: KeyEvent) -> Boolean = { _, _ -> false }
) {
    val currentOnScan by rememberUpdatedState(onScan)
    val currentOnScanStart by rememberUpdatedState(onScanStart)
    val currentMinScanLength by rememberUpdatedState(minScanLength)
    val currentOnKeyDown by rememberUpdatedState(onKeyDown)
    var scanBuffer by remember { mutableStateOf("") }
    var resetJob by remember { mutableStateOf<Job?>(null) }

    Log.d(TAG, "ScanKeyHandler composed (minLen=$minScanLength)")

    DisposableEffect(Unit) {
        val unregister = HardwareKeyDispatcher.register { event ->
            val keyCode = event.keyCode
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode in setOf(KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    Log.d(TAG, "ENTER pressed, buffer='$scanBuffer' (len=${scanBuffer.length}, minLen=$currentMinScanLength)")
                    if (scanBuffer.length >= currentMinScanLength) {
                        currentOnScan(scanBuffer)
                        scanBuffer = ""
                        resetJob?.cancel()
                        return@register true
                    }

                    scanBuffer = ""
                    resetJob?.cancel()
                    return@register currentOnKeyDown(keyCode, event)
                }

                if (currentOnKeyDown(keyCode, event)) {
                    return@register true
                }

                when (keyCode) {
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

        Log.d(TAG, "ScanKeyHandler registered")

        onDispose {
            unregister()
            Log.d(TAG, "ScanKeyHandler unregistered")
            resetJob?.cancel()
        }
    }
}

@Composable
fun HardwareKeyHandler(
    onKeyDown: (keyCode: Int, event: KeyEvent) -> Boolean
) {
    val currentOnKeyDown by rememberUpdatedState(onKeyDown)

    DisposableEffect(Unit) {
        val unregister = HardwareKeyDispatcher.register { event ->
            event.action == KeyEvent.ACTION_DOWN && currentOnKeyDown(event.keyCode, event)
        }

        Log.d(TAG, "HardwareKeyHandler registered")

        onDispose {
            unregister()
            Log.d(TAG, "HardwareKeyHandler unregistered")
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
