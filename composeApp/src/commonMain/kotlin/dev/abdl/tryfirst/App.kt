package dev.abdl.tryfirst

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import dev.abdl.tryfirst.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
internal fun App() = AppTheme {

    val navController = rememberNavController()
    TryFirstNavGraph(navController)
}
