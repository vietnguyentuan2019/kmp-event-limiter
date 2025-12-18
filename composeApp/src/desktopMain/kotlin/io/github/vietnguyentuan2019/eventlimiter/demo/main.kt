package io.github.vietnguyentuan2019.eventlimiter.demo

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMP Event Limiter Demo",
    ) {
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }

        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
            App()
        }
    }
}
