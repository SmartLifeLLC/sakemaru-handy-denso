package biz.smt_life.android.sakemaru_handy_denso.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import biz.smt_life.android.feature.inbound.InboundScreen
import biz.smt_life.android.feature.inbound.InboundWebViewScreen
import biz.smt_life.android.feature.inbound.incoming.HistoryScreen
import biz.smt_life.android.feature.inbound.incoming.IncomingInputScreen
import biz.smt_life.android.feature.inbound.incoming.IncomingViewModel
import biz.smt_life.android.feature.inbound.incoming.ProductListScreen
import biz.smt_life.android.feature.inbound.incoming.ScheduleListScreen
import biz.smt_life.android.feature.inbound.incoming.WarehouseSelectionScreen
import biz.smt_life.android.feature.login.LoginScreen
import biz.smt_life.android.feature.main.MainRoute
import biz.smt_life.android.feature.outbound.tasks.PickingTasksScreen
import biz.smt_life.android.feature.outbound.tasks.PickingTasksViewModel
import biz.smt_life.android.feature.outbound.picking.OutboundPickingScreen
import biz.smt_life.android.feature.outbound.picking.PickingHistoryScreen
import biz.smt_life.android.feature.settings.SettingsScreen

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
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route)
                },
                appVersion = biz.smt_life.android.sakemaru_handy_denso.BuildConfig.VERSION_NAME
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.Main.route) { backStackEntry ->
            MainRoute(
                onNavigateToWarehouseSettings = {
                    navController.navigate(Routes.Settings.route)
                },
                onNavigateToInbound = {
                    navController.navigate(Routes.Inbound.route)
                },
                onNavigateToInboundWebView = { authKey, warehouseId ->
                    // Navigate to native incoming warehouse selection instead of WebView
                    navController.navigate(Routes.IncomingWarehouseSelection.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToOutbound = {
                    // Navigate directly to PickingList (Course Selection)
                    navController.navigate(Routes.PickingList.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
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
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Inbound.route) {
            InboundScreen()
        }

        // Native Incoming routes (入庫処理)
        composable(Routes.IncomingWarehouseSelection.route) { backStackEntry ->
            // Scoped ViewModel for all incoming screens
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.IncomingWarehouseSelection.route)
            }
            val incomingViewModel: IncomingViewModel = hiltViewModel(parentEntry)

            WarehouseSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWarehouseSelected = {
                    navController.navigate(Routes.IncomingProductList.route)
                },
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = incomingViewModel
            )
        }

        composable(Routes.IncomingProductList.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.IncomingWarehouseSelection.route)
            }
            val incomingViewModel: IncomingViewModel = hiltViewModel(parentEntry)

            ProductListScreen(
                onNavigateBack = {
                    incomingViewModel.resetToWarehouseSelection()
                    navController.popBackStack()
                },
                onProductSelected = {
                    navController.navigate(Routes.IncomingScheduleList.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.IncomingHistory.route)
                },
                viewModel = incomingViewModel
            )
        }

        composable(Routes.IncomingScheduleList.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.IncomingWarehouseSelection.route)
            }
            val incomingViewModel: IncomingViewModel = hiltViewModel(parentEntry)

            ScheduleListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onScheduleSelected = {
                    navController.navigate(Routes.IncomingInput.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.IncomingHistory.route)
                },
                viewModel = incomingViewModel
            )
        }

        composable(Routes.IncomingInput.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.IncomingWarehouseSelection.route)
            }
            val incomingViewModel: IncomingViewModel = hiltViewModel(parentEntry)

            IncomingInputScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSubmitSuccess = {
                    // After successful submission, go back to schedule list
                    // The schedule list will show updated data
                    navController.popBackStack(Routes.IncomingScheduleList.route, inclusive = false)
                },
                viewModel = incomingViewModel
            )
        }

        composable(Routes.IncomingHistory.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.IncomingWarehouseSelection.route)
            }
            val incomingViewModel: IncomingViewModel = hiltViewModel(parentEntry)

            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProductList = {
                    navController.popBackStack(Routes.IncomingProductList.route, inclusive = false)
                },
                onEditWorkItem = {
                    navController.navigate(Routes.IncomingInput.route)
                },
                viewModel = incomingViewModel
            )
        }

        composable(Routes.InboundWebView.route) { backStackEntry ->
            // Retrieve auth_key and warehouse_id from savedStateHandle
            val savedStateHandle = backStackEntry.savedStateHandle
            val authKey = savedStateHandle.get<String>("auth_key") ?: ""
            val warehouseId = savedStateHandle.get<String>("warehouse_id") ?: ""

            InboundWebViewScreen(
                authKey = authKey,
                warehouseId = warehouseId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Outbound routes (2.5.1 - 2.5.4 spec flow)
        composable(Routes.PickingList.route) { backStackEntry ->
            // Scoped ViewModel for PickingTasks and OutboundPicking screens
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.PickingList.route)
            }
            val pickingTasksViewModel: PickingTasksViewModel = hiltViewModel(parentEntry)

            PickingTasksScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDataInput = { taskId ->
                    // Navigate to OutboundPicking (Data Input) screen
                    navController.navigate(Routes.OutboundPicking.createRoute(taskId))
                },
                onNavigateToHistory = { taskId ->
                    // Navigate to PickingHistory screen
                    navController.navigate(Routes.PickingHistory.createRoute(taskId))
                },
                viewModel = pickingTasksViewModel
            )
        }

        composable(
            route = Routes.OutboundPicking.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable

            // Get the shared ViewModel from the parent navigation entry
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.PickingList.route)
            }
            val pickingTasksViewModel: PickingTasksViewModel = hiltViewModel(parentEntry)
            val state by pickingTasksViewModel.state.collectAsStateWithLifecycle()

            // Retrieve the selected task from the shared ViewModel
            val task = state.selectedTask

            if (task == null || task.taskId != taskId) {
                // Task not found or mismatch - show error and navigate back
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("タスクが見つかりません")
                        Button(onClick = { navController.popBackStack() }) {
                            Text("戻る")
                        }
                    }
                }
            } else {
                OutboundPickingScreen(
                    task = task,
                    onNavigateBack = {
                        pickingTasksViewModel.clearSelectedTask()
                        navController.popBackStack()
                    },
                    onNavigateToHistory = {
                        navController.navigate(Routes.PickingHistory.createRoute(taskId))
                    },
                    onTaskCompleted = {
                        pickingTasksViewModel.clearSelectedTask()
                        pickingTasksViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }
        }

        // Picking History screen (2.5.3 - 出庫処理＞履歴)
        composable(
            route = Routes.PickingHistory.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable

            // Get the shared ViewModel from the parent navigation entry
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.PickingList.route)
            }
            val pickingTasksViewModel: PickingTasksViewModel = hiltViewModel(parentEntry)

            // Pass taskId directly - PickingHistoryViewModel will observe repository flow
            PickingHistoryScreen(
                taskId = taskId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onHistoryConfirmed = {
                    // All items confirmed - navigate back to course list and refresh
                    pickingTasksViewModel.clearSelectedTask()
                    pickingTasksViewModel.refresh()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SlipEntry.route) {
            // TODO: Implement SlipEntryScreen (stub for now)
        }
    }
}
