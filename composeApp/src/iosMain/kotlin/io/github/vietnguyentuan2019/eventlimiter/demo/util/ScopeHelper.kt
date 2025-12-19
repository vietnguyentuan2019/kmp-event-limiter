package io.github.vietnguyentuan2019.eventlimiter.demo.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Creates a CoroutineScope for iOS event limiters.
 * Uses Main dispatcher to ensure callbacks run on main thread.
 */
fun createEventLimiterScope(): CoroutineScope {
    return CoroutineScope(Dispatchers.Main + SupervisorJob())
}
