package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Debounce with auto-cancel for async operations (search API, autocomplete).
 *
 * **Behavior:** Waits for pause before execution, cancels previous pending calls.
 *
 * **Use cases:**
 * - Search API (user types "abc" → only last call executes)
 * - Autocomplete (cancels stale API responses)
 * - Real-time validation with async server check
 *
 * **Example:**
 * ```kotlin
 * val scope = rememberCoroutineScope()
 * val debouncer = remember { AsyncDebouncer(scope) }
 *
 * TextField(
 *     onValueChange = { text ->
 *         scope.launch {
 *             val results = debouncer.run { searchApi(text) }
 *             if (results != null) {
 *                 // Update UI with results
 *             }
 *         }
 *     }
 * )
 * ```
 */
class AsyncDebouncer(
    private val scope: CoroutineScope,
    val duration: Duration = DEFAULT_DURATION,
    val debugMode: Boolean = false,
    val name: String? = null,
    val enabled: Boolean = true,
    val resetOnError: Boolean = false,
    val onMetrics: ((executionTime: Duration, cancelled: Boolean) -> Unit)? = null
) {
    private var debounceJob: Job? = null
    private var latestCallId = 0
    private var activeCallId: Int? = null

    /**
     * Executes async action after debounce delay, auto-cancels previous calls.
     *
     * Returns `null` if the call was cancelled by a newer call.
     */
    suspend fun <T> run(action: suspend () -> T): T? {
        val startTime = Clock.System.now().toEpochMilliseconds()

        // Skip debounce if disabled
        if (!enabled) {
            debugLog("AsyncDebounce bypassed (disabled)")
            return try {
                val result = action()
                val executionTime = (Clock.System.now().toEpochMilliseconds() - startTime).milliseconds
                onMetrics?.invoke(executionTime, false)
                result
            } catch (e: Exception) {
                if (resetOnError) {
                    debugLog("Error occurred, state reset")
                }
                throw e
            }
        }

        // Cancel old debounce timer
        debounceJob?.cancel()

        // Cancel old call if still running
        if (activeCallId != null) {
            debugLog("AsyncDebounce cancelled previous call")
            val cancelTime = (Clock.System.now().toEpochMilliseconds() - startTime).milliseconds
            onMetrics?.invoke(cancelTime, true)
        }

        val currentCallId = ++latestCallId

        // Wait for debounce delay
        debounceJob = scope.launch {
            delay(duration)
        }

        try {
            debounceJob?.join()
        } catch (e: Exception) {
            // Cancelled during delay
            return null
        }

        // Check if this is still the latest call after delay
        if (currentCallId != latestCallId) {
            debugLog("AsyncDebounce cancelled during wait")
            return null
        }

        activeCallId = currentCallId
        debugLog("AsyncDebounce executing async action")

        return try {
            val result = action()

            // Double-check after execution (another call might have started)
            if (currentCallId == latestCallId) {
                val executionTime = (Clock.System.now().toEpochMilliseconds() - startTime).milliseconds
                debugLog("AsyncDebounce completed in ${executionTime.inWholeMilliseconds}ms")
                onMetrics?.invoke(executionTime, false)
                activeCallId = null
                result
            } else {
                debugLog("AsyncDebounce cancelled after execution")
                activeCallId = null
                null
            }
        } catch (e: Exception) {
            debugLog("AsyncDebounce error: $e")
            if (resetOnError) {
                debugLog("Resetting AsyncDebouncer state due to error")
                cancel()
            }
            activeCallId = null
            throw e
        }
    }

    /**
     * Cancels all pending and in-flight operations.
     */
    fun cancel() {
        debounceJob?.cancel()
        debounceJob = null
        latestCallId++ // Invalidate all pending calls
        activeCallId = null
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
    }

    private fun debugLog(message: String) {
        if (debugMode) {
            val prefix = name?.let { "[$it] " } ?: ""
            println("$prefix$message at ${Clock.System.now().toEpochMilliseconds()}")
        }
    }

    companion object {
        val DEFAULT_DURATION = 300.milliseconds
    }
}
