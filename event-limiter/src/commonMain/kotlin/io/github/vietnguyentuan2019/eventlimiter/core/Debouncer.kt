package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Delays execution until user stops calling for [duration] (default 300ms).
 *
 * **Behavior:** Resets timer on each call, executes only after pause.
 *
 * **Use cases:**
 * - Search input (wait for user to stop typing)
 * - Auto-save (save after user stops editing)
 * - Slider changes
 *
 * **Example:**
 * ```kotlin
 * val scope = rememberCoroutineScope()
 * val debouncer = remember { Debouncer(scope) }
 *
 * TextField(
 *     onValueChange = { text ->
 *         debouncer.call { searchApi(text) }
 *     }
 * )
 * ```
 */
class Debouncer(
    private val scope: CoroutineScope,
    val duration: Duration = DEFAULT_DURATION,
    val debugMode: Boolean = false,
    val name: String? = null,
    val enabled: Boolean = true,
    val resetOnError: Boolean = false,
    val onMetrics: ((waitTime: Duration, cancelled: Boolean) -> Unit)? = null
) {
    private var debounceJob: Job? = null
    private var lastCallTime: Long? = null

    /**
     * Execute the callback with debounce control.
     */
    fun call(callback: () -> Unit) {
        callWithDuration(callback, duration)
    }

    /**
     * Execute with custom duration for this specific call.
     */
    fun callWithDuration(callback: () -> Unit, customDuration: Duration) {
        val callTime = System.currentTimeMillis()

        // Skip debounce if disabled
        if (!enabled) {
            debugLog("Debounce bypassed (disabled)")
            executeCallback(callback, callTime, cancelled = false)
            return
        }

        // Cancel previous timer (if any)
        if (lastCallTime != null) {
            val waitTime = (callTime - lastCallTime!!).milliseconds
            debugLog("Debounce cancelled (new call after ${waitTime.inWholeMilliseconds}ms)")
            onMetrics?.invoke(waitTime, true)
        }

        lastCallTime = callTime
        debounceJob?.cancel()

        debounceJob = scope.launch {
            delay(customDuration)
            val totalWaitTime = (System.currentTimeMillis() - callTime).milliseconds
            debugLog("Debounce executed after ${totalWaitTime.inWholeMilliseconds}ms")
            executeCallback(callback, callTime, cancelled = false)
        }
    }

    private fun executeCallback(callback: () -> Unit, callTime: Long, cancelled: Boolean) {
        if (cancelled) return

        try {
            callback()
            val totalTime = (System.currentTimeMillis() - callTime).milliseconds
            onMetrics?.invoke(totalTime, false)
        } catch (e: Exception) {
            if (resetOnError) {
                debugLog("Error occurred, cancelling pending debounce")
                cancel()
                lastCallTime = null
            }
            // Errors in debounced callbacks are swallowed (consistent with Timer behavior)
            debugLog("Debounce callback error (swallowed): $e")
        }
    }

    /**
     * Wraps a callback for use in event handlers.
     */
    fun wrap(callback: (() -> Unit)?): (() -> Unit)? {
        return callback?.let { { call(it) } }
    }

    /**
     * Force immediate execution without waiting for debounce.
     */
    fun flush(callback: () -> Unit) {
        cancel()
        lastCallTime = null
        debugLog("Debounce flushed (immediate execution)")
        callback()
        onMetrics?.invoke(Duration.ZERO, false)
    }

    /**
     * Cancel pending debounce timer.
     */
    fun cancel() {
        debounceJob?.cancel()
        debounceJob = null
    }

    /**
     * Check if there's a pending debounce timer.
     */
    fun isPending(): Boolean = debounceJob?.isActive == true

    /**
     * Dispose and clean up resources.
     */
    fun dispose() {
        cancel()
        lastCallTime = null
    }

    private fun debugLog(message: String) {
        if (debugMode) {
            val prefix = name?.let { "[$it] " } ?: ""
            println("$prefix$message at ${System.currentTimeMillis()}")
        }
    }

    companion object {
        val DEFAULT_DURATION = 300.milliseconds
    }
}
