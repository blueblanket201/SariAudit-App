package com.example.sariaudit.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sariaudit.viewmodel.MainViewModel
import com.example.sariaudit.viewmodel.UserRole
import com.example.sariaudit.ui.screens.*

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val userRole by viewModel.userRole.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "login",
        // DISABLE ALL ANIMATIONS
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable("login") {
            LoginScreen(viewModel) {
                navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("dashboard") {
            DashboardScreen(navController, viewModel)
        }
        composable("inventory") { InventoryScreen(navController, viewModel) }
        composable("quicksale") { QuickSaleScreen(navController, viewModel) }
        composable("utang") { UtangScreen(navController, viewModel) }
        composable("analytics") {
            if (userRole == UserRole.ADMIN) AnalyticsScreen(navController, viewModel)
            else DashboardScreen(navController, viewModel) // Fallback
        }
        composable("about") { AboutScreen(navController) }
    }
}