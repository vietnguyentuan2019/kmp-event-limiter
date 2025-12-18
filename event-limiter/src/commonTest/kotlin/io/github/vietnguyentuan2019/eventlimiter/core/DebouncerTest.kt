package io.github.vietnguyentuan2019.eventlimiter.core

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

class DebouncerTest {

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
    // Basic Debounce Behavior
    // ====================

    @Test
    fun `basic debounce - delays execution until pause`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var callCount = 0

        debouncer.call { callCount++ }

        // Shouldn't execute immediately
        assertEquals(0, callCount, "Should not execute immediately")
        assertTrue(debouncer.isPending(), "Should be pending")

        // After duration
        advanceTimeBy(300)
        runCurrent()

        assertEquals(1, callCount, "Should execute after debounce delay")
        assertFalse(debouncer.isPending())
    }

    @Test
    fun `multiple rapid calls reset the timer - only last executes`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 200.milliseconds
        )

        var callCount = 0
        var lastValue = 0

        debouncer.call { lastValue = 1; callCount++ }
        advanceTimeBy(100) // Not enough time

        runCurrent()
        debouncer.call { lastValue = 2; callCount++ }
        advanceTimeBy(100) // Not enough time

        runCurrent()
        debouncer.call { lastValue = 3; callCount++ }
        advanceTimeBy(100) // Not enough time
        runCurrent()

        // Still no execution
        assertEquals(0, callCount)

        // Now wait full duration from last call
        advanceTimeBy(100) // Total 200ms from last call
        runCurrent()

        assertEquals(1, callCount, "Only final call should execute")
        assertEquals(3, lastValue, "Should execute the last queued value")
    }

    @Test
    fun `cancel stops pending debounce`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var callCount = 0

        debouncer.call { callCount++ }
        assertTrue(debouncer.isPending())

        debouncer.cancel()
        assertFalse(debouncer.isPending(), "Should not be pending after cancel")

        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, callCount, "Cancelled call should not execute")
    }

    // ====================
    // Flush Functionality
    // ====================

    @Test
    fun `flush executes pending call immediately`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 1.seconds
        )

        var callCount = 0

        debouncer.call { callCount++ }
        assertEquals(0, callCount, "Should not execute yet")

        // Flush forces immediate execution
        debouncer.flush { callCount++ }

        assertEquals(2, callCount, "Both pending and flush callback should execute")
        assertFalse(debouncer.isPending())
    }

    @Test
    fun `flush without pending call just executes the callback`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var callCount = 0

        debouncer.flush { callCount++ }

        assertEquals(1, callCount, "Flush callback should execute")
    }

    // ====================
    // Custom Duration
    // ====================

    @Test
    fun `callWithDuration uses custom duration`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var callCount = 0

        // Use custom 50ms duration
        debouncer.callWithDuration({ callCount++ }, 50.milliseconds)

        advanceTimeBy(50)
        runCurrent()

        assertEquals(1, callCount, "Should execute after custom duration")
    }

    @Test
    fun `callWithDuration overrides previous call with different duration`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var lastValue = 0

        debouncer.callWithDuration({ lastValue = 1 }, 500.milliseconds)
        advanceTimeBy(100)

        runCurrent()
        debouncer.callWithDuration({ lastValue = 2 }, 100.milliseconds)

        // First call should be cancelled
        advanceTimeBy(100) // Total 200ms from start, 100ms from second call
        runCurrent()

        assertEquals(2, lastValue, "Second call should execute")
    }

    // ====================
    // Wrap Function
    // ====================

    @Test
    fun `wrap creates debounced callback wrapper`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 200.milliseconds
        )

        var callCount = 0
        val wrappedCallback = debouncer.wrap { callCount++ }

        assertNotNull(wrappedCallback)

        wrappedCallback()
        wrappedCallback()
        wrappedCallback()

        assertEquals(0, callCount, "Should not execute during debounce")

        advanceTimeBy(200)
        runCurrent()

        assertEquals(1, callCount, "Should execute once after debounce")
    }

    @Test
    fun `wrap with null callback returns null`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        val wrapped = debouncer.wrap(null)
        assertNull(wrapped)
    }

    // ====================
    // Enabled/Disabled State
    // ====================

    @Test
    fun `disabled debouncer executes immediately`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds,
            enabled = false
        )

        var callCount = 0

        debouncer.call { callCount++ }

        assertEquals(1, callCount, "Disabled debouncer should execute immediately")
        assertFalse(debouncer.isPending())
    }

    // ====================
    // Metrics Callback
    // ====================

    @Test
    fun `onMetrics callback receives execution info`() = runTest {
        val metrics = mutableListOf<Pair<Duration, Boolean>>()

        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 200.milliseconds,
            onMetrics = { duration, cancelled ->
                metrics.add(duration to cancelled)
            }
        )

        var callCount = 0

        debouncer.call { callCount++ }
        debouncer.call { callCount++ } // Cancels previous

        advanceTimeBy(200)
        runCurrent()

        assertEquals(1, callCount, "Only last call executes")
        assertTrue(metrics.isNotEmpty(), "Metrics should be recorded")
    }

    // ====================
    // Error Handling
    // ====================

    @Test
    fun `exception in callback propagates`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        debouncer.call {
            throw IllegalStateException("Test error")
        }

        var caught = false
        try {
            advanceTimeBy(100)
            runCurrent()
        } catch (e: IllegalStateException) {
            caught = true
            assertEquals("Test error", e.message)
        }

        assertTrue(caught, "Exception should propagate")
    }

    @Test
    fun `resetOnError true cancels pending after exception`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 100.milliseconds,
            resetOnError = true
        )

        var callCount = 0

        debouncer.call {
            callCount++
            throw IllegalStateException("Error")
        }

        try {
            advanceTimeBy(100)
            runCurrent()
        } catch (e: IllegalStateException) {
            // Expected
        }

        assertEquals(1, callCount)

        // New call should work normally
        debouncer.call { callCount++ }
        advanceTimeBy(100)
        runCurrent()

        assertEquals(2, callCount, "Should recover after resetOnError")
    }

    // ====================
    // State Management
    // ====================

    @Test
    fun `dispose cleans up resources`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var callCount = 0

        debouncer.call { callCount++ }
        assertTrue(debouncer.isPending())

        debouncer.dispose()

        assertFalse(debouncer.isPending())

        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, callCount, "Disposed debouncer should not execute")
    }

    @Test
    fun `cancel clears pending state and prevents execution`() = runTest {
        val debouncer = Debouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var callCount = 0

        debouncer.call { callCount++ }
        assertTrue(debouncer.isPending())

        debouncer.cancel()

        assertFalse(debouncer.isPending())

        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, callCount, "Cancel should clear pending call")
    }

    // ====================
    // Multiple Independent Debouncers
    // ====================

    @Test
    fun `multiple debouncers operate independently`() = runTest {
        val debouncer1 = Debouncer(backgroundScope, duration = 100.milliseconds)
        val debouncer2 = Debouncer(backgroundScope, duration = 200.milliseconds)

        var count1 = 0
        var count2 = 0

        debouncer1.call { count1++ }
        debouncer2.call { count2++ }

        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, count1, "Debouncer1 should execute")
        assertEquals(0, count2, "Debouncer2 should still be pending")

        advanceTimeBy(100) // Total 200ms
        runCurrent()

        assertEquals(1, count2, "Debouncer2 should now execute")
    }
}
