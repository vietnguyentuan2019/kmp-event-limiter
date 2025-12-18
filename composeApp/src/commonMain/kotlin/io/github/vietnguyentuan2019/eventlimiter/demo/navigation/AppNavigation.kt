package io.github.vietnguyentuan2019.eventlimiter.demo.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.vietnguyentuan2019.eventlimiter.demo.screens.*

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onNavigate = { screen -> currentScreen = screen }
        )
        Screen.Search -> SearchScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.Form -> FormScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.Payment -> PaymentScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.Scroll -> InfiniteScrollScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
        Screen.Settings -> SettingsScreen(
            onNavigateBack = { currentScreen = Screen.Home }
        )
    }
}
