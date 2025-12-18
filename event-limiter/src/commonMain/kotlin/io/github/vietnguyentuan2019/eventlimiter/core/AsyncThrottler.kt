package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * Prevents duplicate async operations by locking until operation completes.
 *
 * **Behavior:** Locks during async execution, auto-unlocks after timeout.
 *
 * **Use cases:**
 * - Form submission (prevent double-submit)
 * - File upload (lock during upload)
 * - Payment processing
 *
 * **Example:**
 * ```kotlin
 * val scope = rememberCoroutineScope()
 * val throttler = remember { AsyncThrottler(scope) }
 *
 * Button(
 *     onClick = {
 *         scope.launch {
 *             throttler.call {
 *                 submitForm()
 *             }
 *         }
 *     }
 * ) {
 *     Text("Submit")
 * }
 * ```
 */
class AsyncThrottler(
    private val scope: CoroutineScope,
    val maxDuration: Duration? = DEFAULT_MAX_DURATION,
    val debugMode: Boolean = false,
    val name: String? = null,
    val enabled: Boolean = true,
    val resetOnError: Boolean = false,
    val onMetrics: ((executionTime: Duration, executed: Boolean) -> Unit)? = null
) {
    private val mutex = Mutex()
    private var isLocked = false
    private var timeoutJob: Job? = null

    /**
     * Execute async operation with throttle lock.
     */
    suspend fun call(action: suspend () -> Unit) {
        val startTime = Clock.System.now().toEpochMilliseconds()
        // Skip throttle if disabled
        if (!enabled) {
            debugLog("AsyncThrottle bypassed (disabled)")
            try {
                action()
                val executionTime = (Clock.System.now().toEpochMilliseconds() - startTime).milliseconds
                onMetrics?.invoke(executionTime, true)
            } catch (e: Exception) {
                if (resetOnError) {
                    debugLog("Error occurred, resetting lock state")
                }
                throw e
            }
            return
        }

        mutex.withLock {
            if (isLocked) {
                debugLog("AsyncThrottle blocked (locked)")
                onMetrics?.invoke(Duration.ZERO, false)
                return
            }

            isLocked = true
            debugLog("AsyncThrottle locked")
        }

        // Start timeout timer if configured
        if (maxDuration != null) {
            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(maxDuration)
                debugLog("AsyncThrottle timeout reached, auto-unlocking")
                mutex.withLock {
                    isLocked = false
                }
                timeoutJob = null
            }
        }

        try {
            action()
            val executionTime = (Clock.System.now().toEpochMilliseconds() - startTime).milliseconds
            debugLog("AsyncThrottle completed in ${executionTime.inWholeMilliseconds}ms")
            onMetrics?.invoke(executionTime, true)
        } catch (e: Exception) {
            debugLog("AsyncThrottle error: $e")
            if (resetOnError) {
                debugLog("Resetting AsyncThrottler state due to error")
                reset()
            }
            throw e
        } finally {
            // Only unlock if timeout hasn't already unlocked
            timeoutJob?.cancel()
            if (timeoutJob != null) {
                mutex.withLock {
                    isLocked = false
                }
                debugLog("AsyncThrottle unlocked")
            }
            timeoutJob = null
        }
    }

    /**
     * Wraps a suspend callback for use in event handlers.
     */
    fun wrap(callback: (suspend () -> Unit)?): (() -> Unit)? {
        return callback?.let {
            {
                scope.launch {
                    call(it)
                }
            }
        }
    }

    /**
     * Check if currently locked (busy).
     */
    fun isLocked(): Boolean = isLocked

    /**
     * Reset lock state, allowing immediate execution.
     */
    fun reset() {
        timeoutJob?.cancel()
        timeoutJob = null
        isLocked = false
        debugLog("AsyncThrottle reset")
    }

    /**
     * Dispose and clean up resources.
     */
    fun dispose() {
        timeoutJob?.cancel()
        timeoutJob = null
        isLocked = false
    }

    private fun debugLog(message: String) {
        if (debugMode) {
            val prefix = name?.let { "[$it] " } ?: ""
            println("$prefix$message at ${Clock.System.now().toEpochMilliseconds()}")
        }
    }

    companion object {
        val DEFAULT_MAX_DURATION = 15.seconds
    }
}
