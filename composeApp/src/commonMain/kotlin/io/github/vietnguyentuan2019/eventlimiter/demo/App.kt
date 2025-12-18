package io.github.vietnguyentuan2019.eventlimiter.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.vietnguyentuan2019.eventlimiter.demo.navigation.AppNavigation
import io.github.vietnguyentuan2019.eventlimiter.demo.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}
