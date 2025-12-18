package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AsyncThrottlerTest {

    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        testScope = TestScope(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
    }

    // ====================
    // Basic Async Lock Behavior
    // ====================

    @Test
    fun `basic async lock - prevents concurrent execution`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds
        )

        var executionCount = 0
        var concurrentExecutions = 0
        var isExecuting = false

        launch {
            throttler.call {
                isExecuting = true
                concurrentExecutions++
                executionCount++
                delay(100)
                concurrentExecutions--
                isExecuting = false
            }
        }

        launch {
            delay(10) // Start slightly after first call
            throttler.call {
                concurrentExecutions++
                executionCount++
                delay(100)
                concurrentExecutions--
            }
        }

        advanceTimeBy(200)

        assertEquals(1, executionCount, "Second call should be blocked")
        assertEquals(0, concurrentExecutions, "No concurrent executions")
    }

    @Test
    fun `async lock releases after execution completes`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 5.seconds
        )

        var callCount = 0

        // First call
        throttler.call {
            callCount++
            delay(100)
        }

        assertTrue(throttler.isLocked(), "Should be locked during execution")

        advanceTimeBy(100)

        assertFalse(throttler.isLocked(), "Should unlock after execution")

        // Second call should execute
        throttler.call {
            callCount++
        }

        advanceTimeBy(10)

        assertEquals(2, callCount, "Second call should execute after first unlocks")
    }

    // ====================
    // Timeout Behavior
    // ====================

    @Test
    fun `auto-unlock after maxDuration timeout`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 200.milliseconds
        )

        var callCount = 0

        // First call with very long delay
        launch {
            throttler.call {
                callCount++
                delay(10.seconds) // Very long
            }
        }

        advanceTimeBy(50)
        assertTrue(throttler.isLocked())

        // After timeout duration
        advanceTimeBy(200)

        assertFalse(throttler.isLocked(), "Should auto-unlock after timeout")

        // New call should execute
        throttler.call { callCount++ }
        advanceTimeBy(10)

        assertEquals(2, callCount, "New call should execute after timeout")
    }

    @Test
    fun `null maxDuration means no auto-unlock`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = null
        )

        var callCount = 0

        launch {
            throttler.call {
                callCount++
                delay(100)
            }
        }

        advanceTimeBy(50)
        assertTrue(throttler.isLocked())

        // Even after long time, stays locked until execution completes
        advanceTimeBy(1000)

        // Still executing the first call
        assertTrue(throttler.isLocked(), "Should stay locked without timeout")

        // Finish first call
        advanceTimeBy(100)

        assertFalse(throttler.isLocked())
    }

    // ====================
    // Multiple Blocked Calls
    // ====================

    @Test
    fun `multiple calls blocked while lock is held`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds
        )

        var callCount = 0

        // First call locks
        launch {
            throttler.call {
                callCount++
                delay(200)
            }
        }

        advanceTimeBy(10)

        // Try multiple calls while locked
        launch {
            throttler.call { callCount++ }
        }

        launch {
            throttler.call { callCount++ }
        }

        launch {
            throttler.call { callCount++ }
        }

        advanceTimeBy(300)

        assertEquals(1, callCount, "All subsequent calls should be blocked")
    }

    // ====================
    // State Management
    // ====================

    @Test
    fun `reset unlocks immediately`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 10.seconds
        )

        var callCount = 0

        launch {
            throttler.call {
                callCount++
                delay(1000)
            }
        }

        advanceTimeBy(10)
        assertTrue(throttler.isLocked())

        throttler.reset()

        assertFalse(throttler.isLocked(), "Reset should unlock immediately")

        throttler.call { callCount++ }
        advanceTimeBy(10)

        assertEquals(2, callCount, "Can execute immediately after reset")
    }

    @Test
    fun `dispose cleans up resources`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds
        )

        var callCount = 0

        launch {
            throttler.call {
                callCount++
                delay(100)
            }
        }

        advanceTimeBy(10)
        throttler.dispose()

        assertFalse(throttler.isLocked())

        advanceTimeBy(1000)

        // Call after dispose should still work (just state is reset)
        throttler.call { callCount++ }
        advanceTimeBy(10)

        assertEquals(2, callCount)
    }

    // ====================
    // Enabled/Disabled State
    // ====================

    @Test
    fun `disabled throttler executes all calls without locking`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds,
            enabled = false
        )

        var callCount = 0

        // Multiple concurrent calls should all execute
        launch {
            throttler.call {
                callCount++
                delay(50)
            }
        }

        launch {
            throttler.call {
                callCount++
                delay(50)
            }
        }

        launch {
            throttler.call {
                callCount++
                delay(50)
            }
        }

        advanceTimeBy(100)

        assertEquals(3, callCount, "All calls should execute when disabled")
        assertFalse(throttler.isLocked())
    }

    // ====================
    // Metrics Callback
    // ====================

    @Test
    fun `onMetrics callback receives execution info`() = runTest {
        val metrics = mutableListOf<Pair<Duration, Boolean>>()

        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds,
            onMetrics = { duration, executed ->
                metrics.add(duration to executed)
            }
        )

        var callCount = 0

        // First call executes
        throttler.call {
            callCount++
            delay(50)
        }

        advanceTimeBy(60)

        assertEquals(1, metrics.size)
        assertTrue(metrics[0].second, "First call should be marked as executed")

        // Second call blocked
        launch {
            throttler.call { callCount++ }
        }

        advanceTimeBy(10)

        assertEquals(2, metrics.size)
        assertFalse(metrics[1].second, "Blocked call should be marked as not executed")
    }

    // ====================
    // Error Handling
    // ====================

    @Test
    fun `exception in action propagates`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds
        )

        var caught = false

        try {
            throttler.call {
                throw IllegalStateException("Test error")
            }
        } catch (e: IllegalStateException) {
            caught = true
            assertEquals("Test error", e.message)
        }

        assertTrue(caught)
    }

    @Test
    fun `resetOnError true unlocks after exception`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 10.seconds,
            resetOnError = true
        )

        var callCount = 0

        // First call throws
        try {
            throttler.call {
                callCount++
                throw IllegalStateException("Error")
            }
        } catch (e: IllegalStateException) {
            // Expected
        }

        assertEquals(1, callCount)
        assertFalse(throttler.isLocked(), "Should unlock after error with resetOnError=true")

        // Next call should execute
        throttler.call { callCount++ }
        advanceTimeBy(10)

        assertEquals(2, callCount)
    }

    @Test
    fun `resetOnError false keeps lock after exception`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 10.seconds,
            resetOnError = false
        )

        var callCount = 0

        // First call throws but takes time
        try {
            launch {
                throttler.call {
                    callCount++
                    delay(100)
                    throw IllegalStateException("Error")
                }
            }

            advanceTimeBy(50)

            // Try second call while first is still running
            throttler.call { callCount++ }
        } catch (e: IllegalStateException) {
            // Expected from first call
        }
        runCurrent()

        assertEquals(1, callCount, "Second call should be blocked")
    }

    // ====================
    // Wrap Function
    // ====================

    @Test
    fun `wrap creates throttled suspend callback wrapper`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds
        )

        var callCount = 0

        val wrapped = throttler.wrap {
            callCount++
            delay(50)
        }

        assertNotNull(wrapped)

        // First call
        launch { wrapped() }
        advanceTimeBy(60)
        assertEquals(1, callCount)

        // Second call blocked
        launch { wrapped() }
        advanceTimeBy(10)
        assertEquals(1, callCount, "Second call should be blocked")
    }

    @Test
    fun `wrap with null callback returns null`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 1.seconds
        )

        val wrapped = throttler.wrap(null)
        assertNull(wrapped)
    }

    // ====================
    // Real-world Scenario Tests
    // ====================

    @Test
    fun `form submission scenario - prevents double submit`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 15.seconds
        )

        var submissionCount = 0

        // User clicks submit button rapidly 5 times
        repeat(5) {
            launch {
                throttler.call {
                    submissionCount++
                    delay(2000) // Simulated API call
                }
            }
        }
        runCurrent()

        advanceTimeBy(100)

        assertEquals(1, submissionCount, "Only one submission should go through")

        // After API call completes
        advanceTimeBy(2000)

        // User can submit again
        throttler.call {
            submissionCount++
        }
        runCurrent()

        advanceTimeBy(10)

        assertEquals(2, submissionCount, "Can submit again after completion")
    }

    @Test
    fun `payment processing scenario - strict single execution`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 30.seconds
        )

        var paymentProcessed = 0
        var totalAmount = 0

        // Simulate user rapidly clicking "Pay $100" button
        repeat(10) {
            launch {
                throttler.call {
                    paymentProcessed++
                    totalAmount += 100
                    delay(1000) // Simulated payment processing
                }
            }
        }
        runCurrent()

        advanceTimeBy(2000)

        assertEquals(1, paymentProcessed, "Only one payment should process")
        assertEquals(100, totalAmount, "Only charged once")
    }

    @Test
    fun `timeout recovery scenario - stuck API call`() = runTest {
        val throttler = AsyncThrottler(
            scope = backgroundScope,
            maxDuration = 3.seconds // 3 second timeout
        )

        var callCount = 0

        // First call gets stuck (network issue)
        launch {
            throttler.call {
                callCount++
                delay(30.seconds) // Stuck!
            }
        }

        advanceTimeBy(1000)

        // User tries again - blocked
        launch {
            throttler.call { callCount++ }
        }

        assertEquals(1, callCount, "Second call blocked")

        // After timeout, auto-unlocks
        advanceTimeBy(2100) // Total 3.1s

        // User can try again
        throttler.call {
            callCount++
            delay(100) // Normal execution
        }

        advanceTimeBy(150)

        assertEquals(2, callCount, "Recovered after timeout")
    }
}
