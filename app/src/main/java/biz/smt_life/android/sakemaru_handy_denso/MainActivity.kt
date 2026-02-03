package biz.smt_life.android.sakemaru_handy_denso

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.domain.repository.AuthRepository
import biz.smt_life.android.core.ui.TokenManager
import biz.smt_life.android.sakemaru_handy_denso.navigation.HandyNavHost
import biz.smt_life.android.sakemaru_handy_denso.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity with session validation per Task 4.
 * Validates stored token with server on app start and resume.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var authRepository: AuthRepository

    private var isSessionValidated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide navigation bar (immersive mode)
        hideNavigationBar()

        // Keep screen awake during operations
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Adjust resize for keyboard without showing navigation bar
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setContent {
            HandyTheme {
                var isValidating by remember { mutableStateOf(true) }
                var startDestination by remember { mutableStateOf(Routes.Login.route) }

                LaunchedEffect(Unit) {
                    startDestination = validateSession()
                    isValidating = false
                    isSessionValidated = true
                }

                if (isValidating) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val navController = rememberNavController()
                    HandyNavHost(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    override fun onResume() {
        super.onResume()

        // Keep navigation bar hidden
        hideNavigationBar()

        // Validate session on resume if already validated once
        if (isSessionValidated && tokenManager.isLoggedIn()) {
            lifecycleScope.launch {
                authRepository.validateSession()
                    .onFailure {
                        // Session expired, clear token and restart activity
                        tokenManager.clearAuth()
                        recreate()
                    }
            }
        }
    }

    /**
     * Hide navigation bar using immersive sticky mode.
     */
    private fun hideNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Listen for insets changes and keep navigation bar hidden
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, insets ->
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            insets
        }

        // Also use legacy flags for better compatibility with keyboard
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        // Re-hide navigation bar when system UI visibility changes (e.g., keyboard shown)
        @Suppress("DEPRECATION")
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                // Navigation bar became visible, hide it again after a short delay
                window.decorView.postDelayed({
                    hideNavigationBarFlags()
                }, 100)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun hideNavigationBarFlags() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    /**
     * Validates session on app start.
     * Returns the appropriate start destination.
     */
    private suspend fun validateSession(): String {
        return if (tokenManager.isLoggedIn()) {
            // Token exists, validate with server
            authRepository.validateSession()
                .onSuccess {
                    // Session valid, go to Main
                    return@validateSession Routes.Main.route
                }
                .onFailure {
                    // Session invalid, clear token
                    tokenManager.clearAuth()
                }
            // If validation failed, go to Login
            Routes.Login.route
        } else {
            // No token, go to Login
            Routes.Login.route
        }
    }
}