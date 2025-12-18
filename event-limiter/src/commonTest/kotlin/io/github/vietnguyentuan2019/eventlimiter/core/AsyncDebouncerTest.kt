package io.github.vietnguyentuan2019.eventlimiter.core

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AsyncDebouncerTest {

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
    // Basic Async Debounce Behavior
    // ====================

    @Test
    fun `basic async debounce - delays execution until pause`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var executed = false

        launch {
            val result = debouncer.run {
                delay(50)
                executed = true
                "result"
            }
            assertNotNull(result)
            assertEquals("result", result)
        }

        assertFalse(executed, "Should not execute immediately")

        advanceTimeBy(300)
        runCurrent()
        advanceTimeBy(50) // For the delay inside action

        assertTrue(executed, "Should execute after debounce delay")
    }

    @Test
    fun `multiple rapid calls - only last executes`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 200.milliseconds
        )

        var executionCount = 0
        var lastValue = 0

        // Launch multiple calls rapidly
        val job1 = launch {
            debouncer.run {
                executionCount++
                lastValue = 1
            }
        }

        advanceTimeBy(100)

        val job2 = launch {
            debouncer.run {
                executionCount++
                lastValue = 2
            }
        }

        advanceTimeBy(100)

        val job3 = launch {
            debouncer.run {
                executionCount++
                lastValue = 3
            }
        }

        // Wait for final execution
        advanceTimeBy(200)
        runCurrent()

        assertEquals(1, executionCount, "Only last call should execute")
        assertEquals(3, lastValue)
    }

    @Test
    fun `cancelled calls return null`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 200.milliseconds
        )

        var result1: String? = "not-null"
        var result2: String? = "not-null"

        launch {
            result1 = debouncer.run {
                "first"
            }
        }

        advanceTimeBy(100)

        // Second call cancels first
        launch {
            result2 = debouncer.run {
                "second"
            }
        }

        advanceTimeBy(200)
        runCurrent()

        assertNull(result1, "First call should return null (cancelled)")
        assertEquals("second", result2)
    }

    // ====================
    // Cancel Functionality
    // ====================

    @Test
    fun `cancel stops all pending operations`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var executed = false

        launch {
            val result = debouncer.run {
                executed = true
            }
            assertNull(result, "Cancelled operation should return null")
        }

        assertTrue(debouncer.isPending())

        debouncer.cancel()

        assertFalse(debouncer.isPending())

        advanceTimeBy(500)
        runCurrent()

        assertFalse(executed, "Cancelled operation should not execute")
    }

    // ====================
    // Enabled/Disabled State
    // ====================

    @Test
    fun `disabled debouncer executes immediately`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 300.milliseconds,
            enabled = false
        )

        var executed = false

        launch {
            val result = debouncer.run {
                executed = true
                "immediate"
            }
            assertEquals("immediate", result)
        }

        runCurrent()

        assertTrue(executed, "Disabled debouncer should execute immediately")
        assertFalse(debouncer.isPending())
    }

    // ====================
    // Metrics Callback
    // ====================

    @Test
    fun `onMetrics callback receives execution info`() = runTest {
        val metrics = mutableListOf<Pair<Duration, Boolean>>()

        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 200.milliseconds,
            onMetrics = { duration, cancelled ->
                metrics.add(duration to cancelled)
            }
        )

        launch {
            debouncer.run { "first" }
        }

        advanceTimeBy(100)

        // Second call cancels first
        launch {
            debouncer.run { "second" }
        }

        advanceTimeBy(200)
        runCurrent()

        assertTrue(metrics.size >= 1, "Should have metrics recorded")
    }

    // ====================
    // Error Handling
    // ====================

    @Test
    fun `exception in action propagates`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var caught = false

        launch {
            try {
                debouncer.run {
                    delay(10)
                    throw IllegalStateException("Test error")
                }
            } catch (e: IllegalStateException) {
                caught = true
                assertEquals("Test error", e.message)
            }
        }

        advanceTimeBy(100)
        runCurrent()
        advanceTimeBy(10)
        runCurrent()

        assertTrue(caught, "Exception should propagate")
    }

    @Test
    fun `resetOnError true cancels pending after exception`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 100.milliseconds,
            resetOnError = true
        )

        var executionCount = 0

        launch {
            try {
                debouncer.run {
                    executionCount++
                    delay(10)
                    throw IllegalStateException("Error")
                }
            } catch (e: IllegalStateException) {
                // Expected
            }
        }

        advanceTimeBy(100)
        runCurrent()
        advanceTimeBy(10)
        runCurrent()

        assertEquals(1, executionCount)

        // New call should work normally
        launch {
            debouncer.run {
                executionCount++
            }
        }

        advanceTimeBy(100)
        runCurrent()

        assertEquals(2, executionCount, "Should recover after resetOnError")
    }

    // ====================
    // State Management
    // ====================

    @Test
    fun `dispose cleans up resources`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var executed = false

        launch {
            debouncer.run { executed = true }
        }

        assertTrue(debouncer.isPending())

        debouncer.dispose()

        assertFalse(debouncer.isPending())

        advanceTimeBy(500)
        runCurrent()

        assertFalse(executed, "Disposed debouncer should not execute")
    }

    @Test
    fun `isPending reflects state correctly`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 200.milliseconds
        )

        assertFalse(debouncer.isPending())

        launch {
            debouncer.run { }
        }

        runCurrent()
        assertTrue(debouncer.isPending(), "Should be pending during delay")

        advanceTimeBy(200)
        runCurrent()

        assertFalse(debouncer.isPending(), "Should not be pending after execution")
    }

    // ====================
    // Concurrent Operations
    // ====================

    @Test
    fun `concurrent calls - latest wins`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        val results = mutableListOf<String?>()

        launch {
            val r = debouncer.run { "A" }
            results.add(r)
        }

        advanceTimeBy(50)

        launch {
            val r = debouncer.run { "B" }
            results.add(r)
        }

        advanceTimeBy(50)

        launch {
            val r = debouncer.run { "C" }
            results.add(r)
        }

        advanceTimeBy(100)
        runCurrent()

        assertEquals(3, results.size)
        assertTrue(results.count { it == null } == 2, "Two calls should be cancelled")
        assertTrue(results.contains("C"), "Latest call should execute")
    }

    @Test
    fun `multiple independent debouncers operate separately`() = runTest {
        val debouncer1 = AsyncDebouncer(backgroundScope, duration = 100.milliseconds)
        val debouncer2 = AsyncDebouncer(backgroundScope, duration = 200.milliseconds)

        var count1 = 0
        var count2 = 0

        launch { debouncer1.run { count1++ } }
        launch { debouncer2.run { count2++ } }

        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, count1, "Debouncer1 should execute")
        assertEquals(0, count2, "Debouncer2 should still be pending")

        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, count2, "Debouncer2 should now execute")
    }

    // ====================
    // Generic Return Type
    // ====================

    @Test
    fun `generic return type works correctly`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 100.milliseconds
        )

        var intResult: Int? = null
        var stringResult: String? = null
        var dataResult: TestData? = null

        launch {
            intResult = debouncer.run { 42 }
        }

        advanceTimeBy(100)
        runCurrent()

        assertEquals(42, intResult)

        launch {
            stringResult = debouncer.run { "test" }
        }

        advanceTimeBy(100)
        runCurrent()

        assertEquals("test", stringResult)

        launch {
            dataResult = debouncer.run { TestData("foo", 123) }
        }

        advanceTimeBy(100)
        runCurrent()

        assertNotNull(dataResult)
        assertEquals("foo", dataResult?.name)
        assertEquals(123, dataResult?.value)
    }

    // ====================
    // Real-world Scenarios
    // ====================

    @Test
    fun `search API scenario - cancels stale requests`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 300.milliseconds
        )

        var apiCallCount = 0
        val searchResults = mutableListOf<String?>()

        // User types: "a"
        launch {
            val result = debouncer.run {
                apiCallCount++
                delay(100) // Simulated API call
                "results for a"
            }
            searchResults.add(result)
        }

        advanceTimeBy(100)

        // User types: "ab"
        launch {
            val result = debouncer.run {
                apiCallCount++
                delay(100)
                "results for ab"
            }
            searchResults.add(result)
        }

        advanceTimeBy(100)

        // User types: "abc"
        launch {
            val result = debouncer.run {
                apiCallCount++
                delay(100)
                "results for abc"
            }
            searchResults.add(result)
        }

        advanceTimeBy(300)
        runCurrent()
        advanceTimeBy(100) // For API call delay
        runCurrent()

        assertEquals(1, apiCallCount, "Only final search should execute")
        assertTrue(searchResults.contains("results for abc"))
        assertEquals(2, searchResults.count { it == null }, "Earlier searches cancelled")
    }

    @Test
    fun `autocomplete scenario - handles rapid input changes`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 500.milliseconds
        )

        var fetchCount = 0

        // Simulate 10 rapid keystrokes
        repeat(10) { index ->
            launch {
                debouncer.run {
                    fetchCount++
                    delay(50)
                    "suggestion $index"
                }
            }
            advanceTimeBy(50) // 50ms between keystrokes
        }

        // Wait for debounce
        advanceTimeBy(500)
        runCurrent()
        advanceTimeBy(50) // For the fetch delay
        runCurrent()

        assertEquals(1, fetchCount, "Only one fetch should execute despite 10 keystrokes")
    }

    @Test
    fun `validation scenario - debounced server check`() = runTest {
        val debouncer = AsyncDebouncer(
            scope = backgroundScope,
            duration = 400.milliseconds
        )

        var validationCalls = 0
        var isValid = false

        // User types username
        launch {
            debouncer.run {
                validationCalls++
                delay(200) // Server validation delay
                true // Valid
            }
        }

        advanceTimeBy(200)

        // User continues typing
        launch {
            val result = debouncer.run {
                validationCalls++
                delay(200)
                false // Invalid
            }
            isValid = result ?: false
        }

        advanceTimeBy(400)
        runCurrent()
        advanceTimeBy(200)
        runCurrent()

        assertEquals(1, validationCalls, "Only final validation should run")
        assertFalse(isValid)
    }

    // Test data class
    private data class TestData(val name: String, val value: Int)
}
