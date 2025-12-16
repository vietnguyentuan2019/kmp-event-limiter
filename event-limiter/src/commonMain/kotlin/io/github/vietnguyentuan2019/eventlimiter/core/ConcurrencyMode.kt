package io.github.vietnguyentuan2019.eventlimiter.core

/**
 * Execution strategy for concurrent async operations.
 *
 * Determines how [ConcurrentAsyncThrottler] handles multiple async calls
 * when the throttler is busy processing a previous operation.
 */
enum class ConcurrencyMode {
    /**
     * **Drop Mode** (Default)
     *
     * Ignores new calls while a previous call is executing.
     *
     * **Use cases:**
     * - Button clicks where only first action matters
     * - Operations that shouldn't be repeated
     * - Payment submission button
     */
    DROP,

    /**
     * **Enqueue Mode**
     *
     * Queues new calls and executes them sequentially (FIFO).
     *
     * **Use cases:**
     * - Chat message sending (preserve order)
     * - Sequential API calls with dependencies
     * - Analytics events (no data loss)
     */
    ENQUEUE,

    /**
     * **Replace Mode**
     *
     * Cancels current execution and starts new one.
     *
     * **Use cases:**
     * - Search queries (only latest query matters)
     * - Real-time preview updates
     * - Filter operations
     */
    REPLACE,

    /**
     * **Keep Latest Mode**
     *
     * Keeps track of latest call and executes it after current finishes.
     *
     * **Use cases:**
     * - Form auto-save (save latest after current save completes)
     * - Settings updates
     * - Document sync
     */
    KEEP_LATEST;

    val displayName: String
        get() = when (this) {
            DROP -> "Drop"
            ENQUEUE -> "Enqueue"
            REPLACE -> "Replace"
            KEEP_LATEST -> "Keep Latest"
        }

    val description: String
        get() = when (this) {
            DROP -> "Ignore new calls while busy"
            ENQUEUE -> "Queue calls and execute sequentially"
            REPLACE -> "Cancel current and start new"
            KEEP_LATEST -> "Keep latest call and execute after current"
        }

    val requiresQueue: Boolean
        get() = this == ENQUEUE

    val supportsPending: Boolean
        get() = this == ENQUEUE || this == KEEP_LATEST
}
