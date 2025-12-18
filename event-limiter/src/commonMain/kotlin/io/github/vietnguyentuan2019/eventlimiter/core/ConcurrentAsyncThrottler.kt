package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration

/**
 * Advanced async throttler with concurrency control strategies.
 *
 * **Behavior:** Wraps [AsyncThrottler] with different concurrency modes:
 * - **DROP** (default): Ignore new calls while busy
 * - **ENQUEUE**: Queue calls and execute sequentially (FIFO)
 * - **REPLACE**: Cancel current execution and start new one
 * - **KEEP_LATEST**: Keep latest call and execute after current finishes
 *
 * **Example:**
 * ```kotlin
 * // Enqueue mode - execute all calls sequentially
 * val chatSender = ConcurrentAsyncThrottler(
 *     scope = scope,
 *     mode = ConcurrencyMode.ENQUEUE
 * )
 *
 * // User sends 3 messages rapidly - all execute in order
 * scope.launch { chatSender.call { sendMessage("Hello") } }
 * scope.launch { chatSender.call { sendMessage("World") } }
 * scope.launch { chatSender.call { sendMessage("!") } }
 * ```
 */
class ConcurrentAsyncThrottler(
    private val scope: CoroutineScope,
    val mode: ConcurrencyMode = ConcurrencyMode.DROP,
    maxDuration: Duration? = null,
    debugMode: Boolean = false,
    name: String? = null,
    enabled: Boolean = true,
    resetOnError: Boolean = false,
    onMetrics: ((executionTime: Duration, executed: Boolean) -> Unit)? = null
) {
    private val throttler = AsyncThrottler(
        scope = scope,
        maxDuration = maxDuration,
        debugMode = debugMode,
        name = name,
        enabled = enabled,
        resetOnError = resetOnError,
        onMetrics = onMetrics
    )

    private val queue = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var latestCall: (suspend () -> Unit)? = null
    private var isProcessingQueue = false
    private var latestCallId = 0
    private val mutex = Mutex()

    init {
        if (mode == ConcurrencyMode.ENQUEUE) {
            scope.launch {
                for (action in queue) {
                    throttler.call(action)
                }
            }
        }
    }

    /**
     * Execute async operation with selected concurrency mode.
     */
    suspend fun call(action: suspend () -> Unit) {
        when (mode) {
            ConcurrencyMode.DROP -> throttler.call(action)
            ConcurrencyMode.ENQUEUE -> enqueueCall(action)
            ConcurrencyMode.REPLACE -> replaceCall(action)
            ConcurrencyMode.KEEP_LATEST -> keepLatestCall(action)
        }
    }

    /**
     * Enqueue mode: Queue calls and execute sequentially (FIFO).
     */
    private suspend fun enqueueCall(action: suspend () -> Unit) {
        debugLog("Enqueued call (queue size: ${queue.isEmpty})")
        queue.send(action)
    }

    /**
     * Replace mode: Cancel current execution and start new one.
     *
     * Uses ID-based cancellation - old operations complete but results are ignored.
     */
    private suspend fun replaceCall(action: suspend () -> Unit) {
        val currentCallId = ++latestCallId
        debugLog("Replace mode: new call ID $currentCallId")

        // Reset to allow new call immediately
        throttler.reset()

        // Execute with ID check wrapper
        throttler.call {
            // Check if still valid before executing
            if (currentCallId != latestCallId) {
                debugLog("Replace mode: call $currentCallId cancelled before execution")
                return@call
            }

            action()

            // Check again after execution
            if (currentCallId != latestCallId) {
                debugLog("Replace mode: call $currentCallId result ignored (replaced during execution)")
            }
        }
    }

    /**
     * Keep Latest mode: Keep latest call and execute after current finishes.
     */
    private suspend fun keepLatestCall(action: suspend () -> Unit) {
        mutex.withLock {
            // If throttler is locked (busy), save this as latest call
            if (throttler.isLocked()) {
                latestCall = action
                debugLog("Kept latest call (will execute after current)")
                return
            }
        }

        // Execute immediately if not locked
        throttler.call(action)

        // After execution, check if there's a pending latest call
        mutex.withLock {
            if (latestCall != null) {
                val pendingCall = latestCall!!
                latestCall = null
                debugLog("Executing pending latest call")

                // Execute the pending call (recursive to handle new latest that arrived)
                scope.launch {
                    keepLatestCall(pendingCall)
                }
            }
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
     * Check if throttler is currently locked (busy).
     */
    fun isLocked(): Boolean = throttler.isLocked()

    /**
     * Check if there are pending operations.
     */
    fun hasPendingCalls(): Boolean {
        return when (mode) {
            ConcurrencyMode.ENQUEUE -> !queue.isEmpty || isProcessingQueue
            ConcurrencyMode.KEEP_LATEST -> latestCall != null || throttler.isLocked()
            else -> throttler.isLocked()
        }
    }

    /**
     * Get pending call count (all modes).
     */
    fun pendingCount(): Int {
        return when (mode) {
            ConcurrencyMode.ENQUEUE -> if (isProcessingQueue) 1 else 0
            ConcurrencyMode.KEEP_LATEST -> {
                var count = if (throttler.isLocked()) 1 else 0
                if (latestCall != null) count++
                count
            }
            ConcurrencyMode.DROP, ConcurrencyMode.REPLACE -> {
                if (throttler.isLocked()) 1 else 0
            }
        }
    }

    /**
     * Reset throttler state and clear pending operations.
     */
    fun reset() {
        throttler.reset()
        latestCall = null
        isProcessingQueue = false
        latestCallId++ // Invalidate all pending calls
        debugLog("ConcurrentAsyncThrottler reset (mode: ${mode.name})")
    }

    /**
     * Dispose and clean up resources.
     */
    fun dispose() {
        throttler.dispose()
        queue.close()
        latestCall = null
        isProcessingQueue = false
    }

    private fun debugLog(message: String) {
        if (throttler.debugMode) {
            val prefix = throttler.name?.let { "[$it] " } ?: ""
            println("$prefix$message at ${Clock.System.now().toEpochMilliseconds()}")
        }
    }
}
