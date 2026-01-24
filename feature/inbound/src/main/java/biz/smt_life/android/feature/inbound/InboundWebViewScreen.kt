package biz.smt_life.android.feature.inbound

import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Full-screen WebView for Inbound Processing (入庫処理).
 * Loads: https://wms.lw-hana.net/handy/incoming?auth_key=...&warehouse_id=...
 *
 * ESC key and back button close the WebView and return to main menu.
 */
@Composable
fun InboundWebViewScreen(
    authKey: String,
    warehouseId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle back button press
    BackHandler {
        onNavigateBack()
    }

    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                // Handle ESC key
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    onNavigateBack()
                    true
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(false)
                    }

                    // Build URL with query parameters
                    val url = buildString {
                        append("https://wms.lw-hana.net/handy/incoming")
                        append("?auth_key=").append(authKey)
                        append("&warehouse_id=").append(warehouseId)
                    }

                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
