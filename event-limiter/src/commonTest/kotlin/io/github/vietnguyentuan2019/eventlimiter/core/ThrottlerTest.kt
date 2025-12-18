package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ThrottlerTest {

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
    // Basic Throttle Behavior
    // ====================

    @Test
    fun `basic throttle - first call executes immediately then subsequent calls blocked`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var callCount = 0

        // First call should execute
        throttler.call { callCount++ }
        assertEquals(1, callCount, "First call should execute immediately")
        assertTrue(throttler.isThrottled(), "Should be throttled after first call")

        // Second call should be blocked
        throttler.call { callCount++ }
        assertEquals(1, callCount, "Second call should be blocked")

        // After duration, should be unthrottled
        advanceTimeBy(100)
        runCurrent() // Execute pending coroutines
        assertFalse(throttler.isThrottled(), "Should be unthrottled after duration")

        // Next call should execute
        throttler.call { callCount++ }
        assertEquals(2, callCount, "Call after throttle period should execute")
    }

    @Test
    fun `multiple rapid calls within throttle period are all blocked`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 200.milliseconds
        )

        var callCount = 0

        throttler.call { callCount++ }
        assertEquals(1, callCount)

        // Try 10 rapid calls
        repeat(10) {
            throttler.call { callCount++ }
        }

        assertEquals(1, callCount, "All rapid calls should be blocked")

        advanceTimeBy(200)
        runCurrent() // Execute pending coroutines

        throttler.call { callCount++ }
        assertEquals(2, callCount, "Call after throttle expires should execute")
    }

    @Test
    fun `throttle with zero duration allows all calls`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 0.milliseconds
        )

        var callCount = 0

        repeat(5) {
            throttler.call { callCount++ }
            advanceTimeBy(1) // Tiny advance
        }

        assertEquals(5, callCount, "Zero duration should allow all calls")
    }

    // ====================
    // Custom Duration
    // ====================

    @Test
    fun `callWithDuration uses custom duration for that call`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds // Default
        )

        var callCount = 0

        // First call with custom 50ms duration
        throttler.callWithDuration({ callCount++ }, 50.milliseconds)
        assertEquals(1, callCount)

        // Should be blocked
        throttler.call { callCount++ }
        assertEquals(1, callCount)

        // After 50ms (custom duration), should be unthrottled
        advanceTimeBy(50)
        runCurrent() // Execute pending coroutines

        throttler.call { callCount++ }
        assertEquals(2, callCount, "Should execute after custom duration")
    }

    @Test
    fun `callWithDuration long custom duration blocks longer than default`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var callCount = 0

        // Use 500ms custom duration
        throttler.callWithDuration({ callCount++ }, 500.milliseconds)
        assertEquals(1, callCount)

        // After default duration (100ms), still throttled
        advanceTimeBy(100)
        throttler.call { callCount++ }
        assertEquals(1, callCount, "Should still be throttled")

        // After custom duration (500ms total), should execute
        advanceTimeBy(400) // Total = 500ms
        runCurrent() // Execute pending coroutines
        throttler.call { callCount++ }
        assertEquals(2, callCount)
    }

    // ====================
    // State Management
    // ====================

    @Test
    fun `reset clears throttle state immediately`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 1.seconds
        )

        var callCount = 0

        throttler.call { callCount++ }
        assertEquals(1, callCount)
        assertTrue(throttler.isThrottled())

        // Reset should clear state
        throttler.reset()
        assertFalse(throttler.isThrottled(), "Reset should clear throttled state")

        // Should be able to call immediately
        throttler.call { callCount++ }
        assertEquals(2, callCount, "Should execute after reset")
    }

    @Test
    fun `cancel stops the throttle timer`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var callCount = 0

        throttler.call { callCount++ }
        assertTrue(throttler.isPending())

        throttler.cancel()
        assertFalse(throttler.isPending(), "Cancel should stop pending timer")

        // Advancing time shouldn't matter
        advanceTimeBy(200)

        // But throttle state isn't reset, so still throttled
        assertTrue(throttler.isThrottled(), "Throttled state remains after cancel")
    }

    @Test
    fun `dispose cleans up all resources`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var callCount = 0

        throttler.call { callCount++ }
        throttler.dispose()

        assertFalse(throttler.isPending())
        assertFalse(throttler.isThrottled())
    }

    // ====================
    // Enabled/Disabled State
    // ====================

    @Test
    fun `disabled throttler executes all calls immediately`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds,
            enabled = false
        )

        var callCount = 0

        repeat(5) {
            throttler.call { callCount++ }
        }

        assertEquals(5, callCount, "Disabled throttler should execute all calls")
        assertFalse(throttler.isThrottled(), "Disabled throttler should never throttle")
    }

    // ====================
    // Metrics Callback
    // ====================

    @Test
    fun `onMetrics callback receives execution info`() = runTest {
        val metrics = mutableListOf<Pair<Duration, Boolean>>()

        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds,
            onMetrics = { duration, executed ->
                metrics.add(duration to executed)
            }
        )

        var callCount = 0

        // First call - should execute
        throttler.call { callCount++ }

        assertEquals(1, metrics.size)
        assertTrue(metrics[0].second, "First call should be marked as executed")

        // Second call - should be blocked
        throttler.call { callCount++ }

        assertEquals(2, metrics.size)
        assertFalse(metrics[1].second, "Blocked call should be marked as not executed")
    }

    // ====================
    // Error Handling
    // ====================

    @Test
    fun `exception in callback propagates correctly`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
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

        assertTrue(caught, "Exception should propagate")
    }

    @Test
    fun `resetOnError true resets throttle after exception`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 1.seconds,
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

        // Should be reset, so next call executes immediately
        throttler.call { callCount++ }
        assertEquals(2, callCount, "Should execute after resetOnError")
    }

    @Test
    fun `resetOnError false - exception prevents throttle from activating`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 1.seconds,
            resetOnError = false
        )

        var callCount = 0

        // First call throws - throttle never activated because exception happens before isThrottled=true
        try {
            throttler.call {
                callCount++
                throw IllegalStateException("Error")
            }
        } catch (e: IllegalStateException) {
            // Expected
        }

        assertEquals(1, callCount)

        // Because exception prevented throttle from activating, next call can execute
        throttler.call { callCount++ }
        assertEquals(2, callCount, "Second call should execute - throttle was never activated")

        // Now throttler is active, third call blocked
        throttler.call { callCount++ }
        assertEquals(2, callCount, "Third call should be blocked")

        // After duration
        advanceTimeBy(1000)
        runCurrent()

        throttler.call { callCount++ }
        assertEquals(3, callCount, "Fourth call executes after cooldown")
    }

    // ====================
    // Wrap Function
    // ====================

    @Test
    fun `wrap creates throttled callback wrapper`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var callCount = 0
        val wrappedCallback = throttler.wrap { callCount++ }

        assertNotNull(wrappedCallback, "Wrap should return non-null")

        // First call executes
        wrappedCallback()
        assertEquals(1, callCount)

        // Second call blocked
        wrappedCallback()
        assertEquals(1, callCount)

        advanceTimeBy(100)
        runCurrent() // Execute pending coroutines

        wrappedCallback()
        assertEquals(2, callCount)
    }

    @Test
    fun `wrap with null callback returns null`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        val wrapped = throttler.wrap(null)
        assertNull(wrapped, "Wrapping null should return null")
    }

    // ====================
    // Concurrent Throttlers
    // ====================

    @Test
    fun `multiple throttlers operate independently`() = runTest {
        val throttler1 = Throttler(backgroundScope, duration = 100.milliseconds, name = "T1")
        val throttler2 = Throttler(backgroundScope, duration = 200.milliseconds, name = "T2")

        var count1 = 0
        var count2 = 0

        throttler1.call { count1++ }
        throttler2.call { count2++ }

        assertEquals(1, count1)
        assertEquals(1, count2)

        // After 100ms, throttler1 unthrottled, throttler2 still throttled
        advanceTimeBy(100)
        runCurrent() // Execute pending coroutines

        throttler1.call { count1++ }
        throttler2.call { count2++ }

        assertEquals(2, count1, "Throttler1 should execute")
        assertEquals(1, count2, "Throttler2 should still be throttled")

        // After 200ms total
        advanceTimeBy(100)
        runCurrent() // Execute pending coroutines

        throttler2.call { count2++ }
        assertEquals(2, count2, "Throttler2 should now execute")
    }

    // ====================
    // isPending and isThrottled States
    // ====================

    @Test
    fun `isPending reflects timer state correctly`() = runTest {
        val throttler = Throttler(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        assertFalse(throttler.isPending(), "Should not be pending initially")

        throttler.call { }

        assertTrue(throttler.isPending(), "Should be pending after call")
        assertTrue(throttler.isThrottled(), "Should be throttled")

        advanceTimeBy(100)
        runCurrent() // Execute pending coroutines

        assertFalse(throttler.isPending(), "Should not be pending after duration")
        assertFalse(throttler.isThrottled(), "Should not be throttled")
    }
}
