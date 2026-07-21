package com.example.minibrowser

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel // ✅ 改用原生
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "browser"
    ) {
        composable(
            route = "browser?url={url}",
            arguments = listOf(
                navArgument("url") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val browserViewModel: BrowserViewModel = viewModel()
            val urlToLoad = backStackEntry.arguments?.getString("url")

            BrowserScreen(
                viewModel = browserViewModel,
                initialUrl = urlToLoad,
                onNavigateToHistory = {
                    navController.navigate("history")
                }
            )
        }

        composable("history") {
            val historyViewModel: HistoryViewModel = viewModel()

            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateToUrl = { url ->
                    navController.navigate("browser?url=$url") {
                        popUpTo("browser") { inclusive = true }
                    }
                }
            )
        }
    }
}
