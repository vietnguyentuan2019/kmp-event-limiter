package io.github.vietnguyentuan2019.eventlimiter.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import io.github.vietnguyentuan2019.eventlimiter.core.ConcurrencyMode
import io.github.vietnguyentuan2019.eventlimiter.core.ConcurrentAsyncThrottler
import io.github.vietnguyentuan2019.eventlimiter.core.Throttler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Modifier extension to prevent double-clicks with throttle control.
 *
 * **Use case:** Add to any clickable component to prevent spam clicks.
 *
 * **Example:**
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .throttleClick(duration = 500.milliseconds) {
 *             submitForm()
 *         }
 *         .background(Color.Blue)
 *         .padding(16.dp)
 * ) {
 *     Text("Submit")
 * }
 * ```
 */
fun Modifier.throttleClick(
    duration: Duration = 500.milliseconds,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val throttler = remember(scope, duration) {
        Throttler(
            scope = scope,
            duration = duration,
            enabled = enabled
        )
    }

    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        throttler.call(onClick)
    }
}

/**
 * Modifier extension for throttled clicks with ripple effect.
 *
 * **Example:**
 * ```kotlin
 * Text(
 *     text = "Click me",
 *     modifier = Modifier.throttleClickable(
 *         duration = 500.milliseconds,
 *         onClick = { println("Clicked!") }
 *     )
 * )
 * ```
 */
fun Modifier.throttleClickable(
    duration: Duration = 500.milliseconds,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val throttler = remember(scope, duration) {
        Throttler(
            scope = scope,
            duration = duration,
            enabled = enabled
        )
    }

    this.clickable {
        throttler.call(onClick)
    }
}

/**
 * Modifier extension for async operations with throttle lock.
 *
 * **Use case:** Prevent multiple async operations from running simultaneously.
 *
 * **Example:**
 * ```kotlin
 * Button(
 *     onClick = {},
 *     modifier = Modifier.asyncThrottleClick(
 *         maxDuration = 15.seconds
 *     ) {
 *         // This suspend function won't run again until it completes
 *         uploadFile()
 *     }
 * ) {
 *     Text("Upload")
 * }
 * ```
 */
fun Modifier.asyncThrottleClick(
    maxDuration: Duration? = null,
    mode: ConcurrencyMode = ConcurrencyMode.DROP,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val throttler = remember(scope, mode, maxDuration) {
        ConcurrentAsyncThrottler(
            scope = scope,
            mode = mode,
            maxDuration = maxDuration,
            enabled = enabled
        )
    }

    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        scope.let {
            it.launch {
                throttler.call(onClick)
            }
        }
    }
}

/**
 * Modifier extension for async operations with throttle lock and ripple.
 *
 * **Example:**
 * ```kotlin
 * Card(
 *     modifier = Modifier.asyncThrottleClickable(
 *         mode = ConcurrencyMode.DROP
 *     ) {
 *         saveData()
 *     }
 * ) {
 *     Text("Save")
 * }
 * ```
 */
fun Modifier.asyncThrottleClickable(
    maxDuration: Duration? = null,
    mode: ConcurrencyMode = ConcurrencyMode.DROP,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val throttler = remember(scope, mode, maxDuration) {
        ConcurrentAsyncThrottler(
            scope = scope,
            mode = mode,
            maxDuration = maxDuration,
            enabled = enabled
        )
    }

    this.clickable {
        scope.launch {
            throttler.call(onClick)
        }
    }
}
