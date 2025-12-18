package io.github.vietnguyentuan2019.eventlimiter.demo.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel

@Composable
expect inline fun <reified VM : ViewModel> viewModel(
    noinline factory: () -> VM
): VM
