package io.github.vietnguyentuan2019.eventlimiter.demo.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel as desktopViewModel

@Composable
actual inline fun <reified VM : ViewModel> viewModel(
    noinline factory: () -> VM
): VM = desktopViewModel { factory() }
