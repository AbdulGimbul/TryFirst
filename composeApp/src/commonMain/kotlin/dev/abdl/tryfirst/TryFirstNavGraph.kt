package dev.abdl.tryfirst

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun TryFirstNavGraph(navController: NavHostController) {

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(navController = navController)
        }
        composable("about") {
            AboutScreen(navController = navController)
        }
    }
}