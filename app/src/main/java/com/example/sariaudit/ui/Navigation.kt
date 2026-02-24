package com.example.sariaudit.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sariaudit.viewmodel.MainViewModel
import com.example.sariaudit.viewmodel.UserRole

// --- EXPLICIT IMPORTS TO FIX UNRESOLVED REFERENCES ---
import com.example.sariaudit.ui.screens.LoginScreen
import com.example.sariaudit.ui.screens.DashboardScreen
import com.example.sariaudit.ui.screens.InventoryScreen
import com.example.sariaudit.ui.screens.QuickSaleScreen
import com.example.sariaudit.ui.screens.UtangScreen
import com.example.sariaudit.ui.screens.AnalyticsScreen
import com.example.sariaudit.ui.screens.AboutScreen
import com.example.sariaudit.ui.screens.StoreSelectionScreen
import androidx.compose.runtime.collectAsState

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val userRole by viewModel.userRole.collectAsState()
    val currentStore by viewModel.currentStore.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val startDest = if (currentUser != null) {
        if (currentStore != null) "dashboard" else "store_select"
    } else "login"

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
                // Check if user has a store, if not go to selection
                navController.navigate("store_select") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("store_select") {
            StoreSelectionScreen(navController, viewModel)
        }
        composable("dashboard") {
            DashboardScreen(navController, viewModel)
        }
        composable("inventory") { InventoryScreen(navController, viewModel) }
        composable("quicksale") { QuickSaleScreen(navController, viewModel) }
        composable("utang") { UtangScreen(navController, viewModel) }
        composable("analytics") {
            if (userRole == UserRole.ADMIN || userRole == UserRole.OWNER) {
                AnalyticsScreen(navController, viewModel)
            } else {
                DashboardScreen(navController, viewModel)
            }
        }
        composable("about") { AboutScreen(navController) }
    }
}