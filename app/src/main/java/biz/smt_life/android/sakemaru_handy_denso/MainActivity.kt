package biz.smt_life.android.sakemaru_handy_denso

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import biz.smt_life.android.core.designsystem.theme.HandyTheme
import biz.smt_life.android.core.ui.TokenManager
import biz.smt_life.android.sakemaru_handy_denso.navigation.HandyNavHost
import biz.smt_life.android.sakemaru_handy_denso.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen awake during operations
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            HandyTheme {
                val navController = rememberNavController()
                val startDestination = if (tokenManager.isLoggedIn()) {
                    Routes.Main.route
                } else {
                    Routes.Login.route
                }

                HandyNavHost(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}