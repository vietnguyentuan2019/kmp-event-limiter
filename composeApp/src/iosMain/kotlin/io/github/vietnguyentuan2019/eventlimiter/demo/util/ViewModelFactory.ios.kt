package io.github.vietnguyentuan2019.eventlimiter.demo.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel

@Composable
actual inline fun <reified VM : ViewModel> viewModel(
    noinline factory: () -> VM
): VM = remember { factory() }
