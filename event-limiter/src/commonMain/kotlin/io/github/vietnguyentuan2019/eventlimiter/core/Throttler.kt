package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Prevents spam clicks by blocking calls for [duration] after first execution.
 *
 * **Behavior:** First call executes immediately, subsequent calls blocked for the duration.
 *
 * **Use cases:**
 * - Button clicks (prevent double-clicks)
 * - Form submissions
 * - Refresh actions
 *
 * **Example:**
 * ```kotlin
 * val scope = rememberCoroutineScope()
 * val throttler = remember { Throttler(scope) }
 *
 * Button(onClick = throttler.wrap { submitForm() }) {
 *     Text("Submit")
 * }
 * ```
 */
class Throttler(
    private val scope: CoroutineScope,
    val duration: Duration = DEFAULT_DURATION,
    val debugMode: Boolean = false,
    val name: String? = null,
    val enabled: Boolean = true,
    val resetOnError: Boolean = false,
    val onMetrics: ((executionTime: Duration, executed: Boolean) -> Unit)? = null
) {
    private var isThrottled = false
    private var throttleJob: Job? = null

    /**
     * Execute the callback with throttle control.
     */
    fun call(callback: () -> Unit) {
        callWithDuration(callback, duration)
    }

    /**
     * Execute with custom duration for this specific call.
     */
    fun callWithDuration(callback: () -> Unit, customDuration: Duration) {
        val startTime = Clock.System.now().toEpochMilliseconds()

        // Skip throttle if disabled
        if (!enabled) {
            debugLog("Throttle bypassed (disabled)")
            executeCallback(callback, startTime, executed = true)
            return
        }

        if (isThrottled) {
            debugLog("Throttle blocked")
            onMetrics?.invoke(Duration.ZERO, false)
            return
        }

        debugLog("Throttle executed")
        executeCallback(callback, startTime, executed = true)
        isThrottled = true

        throttleJob = scope.launch {
            delay(customDuration)
            isThrottled = false
            debugLog("Throttle cooldown ended")
        }
    }

    private fun executeCallback(callback: () -> Unit, startTime: Long, executed: Boolean) {
        try {
            callback()
            val executionTime = (Clock.System.now().toEpochMilliseconds() - startTime).milliseconds
            onMetrics?.invoke(executionTime, executed)
        } catch (e: Exception) {
            if (resetOnError) {
                debugLog("Error occurred, resetting throttle state")
                reset()
            }
            throw e
        }
    }

    /**
     * Wraps a callback for use in onClick handlers.
     */
    fun wrap(callback: (() -> Unit)?): (() -> Unit)? {
        return callback?.let { { call(it) } }
    }

    /**
     * Reset throttle state, allowing immediate execution.
     */
    fun reset() {
        cancel()
        isThrottled = false
        debugLog("Throttle reset")
    }

    /**
     * Cancel pending throttle timer.
     */
    fun cancel() {
        throttleJob?.cancel()
        throttleJob = null
    }

    /**
     * Check if currently throttled.
     */
    fun isThrottled(): Boolean = isThrottled

    /**
     * Check if there's a pending timer.
     */
    fun isPending(): Boolean = throttleJob?.isActive == true

    /**
     * Dispose and clean up resources.
     */
    fun dispose() {
        cancel()
        isThrottled = false
    }

    private fun debugLog(message: String) {
        if (debugMode) {
            val prefix = name?.let { "[$it] " } ?: ""
            println("$prefix$message at ${Clock.System.now().toEpochMilliseconds()}")
        }
    }

    companion object {
        val DEFAULT_DURATION = 500.milliseconds
    }
}
