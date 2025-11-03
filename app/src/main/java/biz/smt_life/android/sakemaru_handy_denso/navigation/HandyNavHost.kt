package biz.smt_life.android.sakemaru_handy_denso.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import biz.smt_life.android.feature.inbound.InboundScreen
import biz.smt_life.android.feature.login.LoginScreen
import biz.smt_life.android.feature.main.MainRoute
import biz.smt_life.android.feature.outbound.OutboundEntryScreen
import biz.smt_life.android.feature.outbound.OutboundSelectScreen
import biz.smt_life.android.feature.outbound.PickingListScreen
import biz.smt_life.android.feature.outbound.history.OutboundHistoryScreen

@Composable
fun HandyNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Main.route) {
            MainRoute(
                onNavigateToWarehouseSettings = {
                    navController.navigate(Routes.WarehouseSettings.route)
                },
                onNavigateToInbound = {
                    navController.navigate(Routes.Inbound.route)
                },
                onNavigateToOutbound = {
                    navController.navigate(Routes.OutboundSelect.route)
                },
                onNavigateToMove = {
                    navController.navigate(Routes.Move.route)
                },
                onNavigateToInventory = {
                    navController.navigate(Routes.Inventory.route)
                },
                onNavigateToLocationSearch = {
                    navController.navigate(Routes.LocationSearch.route)
                },
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Main.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Inbound.route) {
            InboundScreen()
        }

        // Outbound routes (2.5.1 - 2.5.4 spec flow)
        composable(Routes.OutboundSelect.route) {
            OutboundSelectScreen(
                onNavigateToPickingList = {
                    navController.navigate(Routes.PickingList.route)
                },
                onNavigateToSlipEntry = {
                    navController.navigate(Routes.SlipEntry.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.PickingList.route) {
            PickingListScreen(
                onSelectCourse = { courseId ->
                    navController.navigate(Routes.OutboundEntry.createRoute(courseId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.OutboundEntry.route
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: return@composable
            OutboundEntryScreen(
                courseId = courseId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHistory = { courseId ->
                    navController.navigate(Routes.OutboundHistory.createRoute(courseId))
                }
            )
        }

        composable(
            route = Routes.OutboundHistory.route,
            arguments = listOf(
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            OutboundHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEntry = { courseId, itemId ->
                    navController.navigate(Routes.OutboundEntry.createRoute(courseId)) {
                        popUpTo(Routes.OutboundHistory.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SlipEntry.route) {
            // TODO: Implement SlipEntryScreen (stub for now)
        }
    }
}
