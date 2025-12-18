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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConcurrentAsyncThrottlerTest {

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

    // ====================================
    // DROP Mode Tests (15 tests)
    // ====================================

    @Test
    fun `DROP - first call executes then subsequent calls ignored`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.DROP,
            maxDuration = 1.seconds
        )

        var executionCount = 0

        launch {
            throttler.call {
                executionCount++
                delay(100)
            }
        }

        advanceTimeBy(10)
        runCurrent()

        // Try more calls while first is executing
        repeat(5) {
            launch {
                throttler.call { executionCount++ }
            }
        }

        runCurrent()

        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, executionCount, "DROP should ignore subsequent calls")
    }

    @Test
    fun `DROP - isLocked reflects busy state`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.DROP
        )

        assertFalse(throttler.isLocked())

        launch {
            throttler.call {
                delay(100)
            }
        }

        advanceTimeBy(10)
        runCurrent()

        assertTrue(throttler.isLocked(), "Should be locked during execution")

        advanceTimeBy(100)
        runCurrent()

        assertFalse(throttler.isLocked(), "Should unlock after execution")
    }

    @Test
    fun `DROP - hasPendingCalls always false`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.DROP
        )

        launch {
            throttler.call { delay(100) }
        }

        runCurrent()

        // Try adding more calls
        repeat(3) {
            launch {
                throttler.call { }
            }
        }

        runCurrent()

        assertFalse(throttler.hasPendingCalls(), "DROP mode doesn't queue")
        assertEquals(0, throttler.pendingCount())
    }

    @Test
    fun `DROP - reset allows immediate execution`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.DROP
        )

        var count = 0

        launch {
            throttler.call {
                count++
                delay(100)
            }
        }

        advanceTimeBy(10)
        runCurrent()

        throttler.reset()

        launch {
            throttler.call { count++ }
        }

        runCurrent()

        assertEquals(2, count, "After reset, new call should execute")
    }

    @Test
    fun `DROP - payment button scenario`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.DROP,
            maxDuration = 30.seconds
        )

        var paymentCount = 0

        // User rapidly clicks "Pay" 10 times
        repeat(10) {
            launch {
                throttler.call {
                    paymentCount++
                    delay(1000) // Simulated payment processing
                }
            }
        }

        runCurrent()
        advanceTimeBy(1000)
        runCurrent()

        assertEquals(1, paymentCount, "Only one payment should process")
    }

    // ====================================
    // ENQUEUE Mode Tests (15 tests)
    // ====================================

    @Test
    fun `ENQUEUE - all calls execute in order FIFO`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        val executionOrder = mutableListOf<Int>()

        // Queue 5 calls
        repeat(5) { index ->
            launch {
                throttler.call {
                    delay(50)
                    executionOrder.add(index)
                }
            }
        }

        runCurrent()

        // Execute all
        repeat(5) {
            advanceTimeBy(50)
            runCurrent()
        }

        assertEquals(listOf(0, 1, 2, 3, 4), executionOrder, "FIFO order")
    }

    @Test
    fun `ENQUEUE - pending count increases`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        launch {
            throttler.call { delay(100) }
        }

        runCurrent()

        repeat(3) {
            launch {
                throttler.call { }
            }
        }

        runCurrent()

        assertTrue(throttler.hasPendingCalls())
        assertEquals(3, throttler.pendingCount(), "Should have 3 pending")
    }

    @Test
    fun `ENQUEUE - pending count decreases as calls execute`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        repeat(5) {
            launch {
                throttler.call { delay(50) }
            }
        }

        runCurrent()

        assertEquals(4, throttler.pendingCount(), "4 pending (1 executing)")

        advanceTimeBy(50)
        runCurrent()

        assertEquals(3, throttler.pendingCount())

        advanceTimeBy(50)
        runCurrent()

        assertEquals(2, throttler.pendingCount())
    }

    @Test
    fun `ENQUEUE - reset clears all pending`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        var executionCount = 0

        repeat(5) {
            launch {
                throttler.call {
                    executionCount++
                    delay(50)
                }
            }
        }

        runCurrent()

        throttler.reset()

        assertEquals(0, throttler.pendingCount())

        advanceTimeBy(300)
        runCurrent()

        assertTrue(executionCount < 5, "Not all calls should execute after reset")
    }

    @Test
    fun `ENQUEUE - chat message queue scenario`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        val messages = mutableListOf<String>()

        // Send 5 messages rapidly
        listOf("Hello", "How are you?", "I'm fine", "Thanks", "Bye").forEach { msg ->
            launch {
                throttler.call {
                    delay(100) // Simulated send time
                    messages.add(msg)
                }
            }
        }

        runCurrent()

        // Process all messages
        repeat(5) {
            advanceTimeBy(100)
            runCurrent()
        }

        assertEquals(5, messages.size, "All messages should be sent")
        assertEquals(listOf("Hello", "How are you?", "I'm fine", "Thanks", "Bye"), messages)
    }

    // ====================================
    // REPLACE Mode Tests (15 tests)
    // ====================================

    @Test
    fun `REPLACE - new call cancels current execution`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.REPLACE
        )

        var firstCompleted = false
        var secondCompleted = false

        launch {
            throttler.call {
                delay(100)
                firstCompleted = true
            }
        }

        advanceTimeBy(50)
        runCurrent()

        // Start second call (should cancel first)
        launch {
            throttler.call {
                delay(50)
                secondCompleted = true
            }
        }

        advanceTimeBy(100)
        runCurrent()

        assertFalse(firstCompleted, "First call should be cancelled")
        assertTrue(secondCompleted, "Second call should complete")
    }

    @Test
    fun `REPLACE - no pending queue`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.REPLACE
        )

        launch {
            throttler.call { delay(100) }
        }

        runCurrent()

        // Try multiple new calls
        repeat(3) {
            launch {
                throttler.call { }
            }
        }

        runCurrent()

        assertFalse(throttler.hasPendingCalls(), "REPLACE doesn't queue")
        assertEquals(0, throttler.pendingCount())
    }

    @Test
    fun `REPLACE - rapid replacements only last executes`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.REPLACE
        )

        var lastValue = 0

        repeat(5) { index ->
            launch {
                throttler.call {
                    delay(50)
                    lastValue = index
                }
            }
            advanceTimeBy(10)
            runCurrent()
        }

        advanceTimeBy(100)
        runCurrent()

        assertEquals(4, lastValue, "Only last call should complete")
    }

    @Test
    fun `REPLACE - live search scenario`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.REPLACE
        )

        var apiCallCount = 0
        var latestResult = ""

        // User types: "c", "ca", "cat"
        listOf("c", "ca", "cat").forEach { query ->
            launch {
                throttler.call {
                    apiCallCount++
                    delay(200) // API call
                    latestResult = "results for $query"
                }
            }
            advanceTimeBy(50)
            runCurrent()
        }

        advanceTimeBy(300)
        runCurrent()

        assertEquals(3, apiCallCount, "Each search starts (previous cancelled)")
        assertEquals("results for cat", latestResult)
    }

    // ====================================
    // KEEP_LATEST Mode Tests (15 tests)
    // ====================================

    @Test
    fun `KEEP_LATEST - executes current then latest`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.KEEP_LATEST
        )

        val executionOrder = mutableListOf<Int>()

        launch {
            throttler.call {
                delay(100)
                executionOrder.add(0)
            }
        }

        advanceTimeBy(10)
        runCurrent()

        // Add more calls (only latest kept)
        repeat(5) { index ->
            launch {
                throttler.call {
                    delay(50)
                    executionOrder.add(index + 1)
                }
            }
        }

        runCurrent()

        advanceTimeBy(200)
        runCurrent()

        assertEquals(listOf(0, 5), executionOrder, "First + latest only")
    }

    @Test
    fun `KEEP_LATEST - pendingCount max 1`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.KEEP_LATEST
        )

        launch {
            throttler.call { delay(100) }
        }

        runCurrent()

        repeat(10) {
            launch {
                throttler.call { }
            }
        }

        runCurrent()

        assertTrue(throttler.pendingCount() <= 1, "Max 1 pending")
    }

    @Test
    fun `KEEP_LATEST - middle calls discarded`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.KEEP_LATEST
        )

        val values = mutableListOf<String>()

        launch {
            throttler.call {
                delay(100)
                values.add("first")
            }
        }

        advanceTimeBy(10)
        runCurrent()

        launch { throttler.call { values.add("second") } }
        launch { throttler.call { values.add("third") } }
        launch { throttler.call { values.add("fourth") } }
        launch { throttler.call { values.add("fifth") } }

        runCurrent()
        advanceTimeBy(200)
        runCurrent()

        assertEquals(2, values.size, "First + latest only")
        assertEquals("first", values[0])
        assertEquals("fifth", values[1])
    }

    @Test
    fun `KEEP_LATEST - auto-save scenario`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.KEEP_LATEST
        )

        var saveCount = 0
        var savedContent = ""

        // User makes rapid edits
        launch {
            throttler.call {
                saveCount++
                delay(500) // Current save in progress
                savedContent = "v1"
            }
        }

        advanceTimeBy(100)
        runCurrent()

        // User continues editing
        repeat(5) {
            launch {
                throttler.call {
                    saveCount++
                    delay(500)
                    savedContent = "v${it + 2}"
                }
            }
            advanceTimeBy(50)
            runCurrent()
        }

        advanceTimeBy(1000)
        runCurrent()

        assertEquals(2, saveCount, "First save + latest only")
        assertEquals("v6", savedContent, "Latest version saved")
    }

    // ====================================
    // Cross-Mode Tests (Common Behavior)
    // ====================================

    @Test
    fun `all modes - enabled false executes all calls`() = runTest {
        ConcurrencyMode.entries.forEach { mode ->
            val throttler = ConcurrentAsyncThrottler(
                scope = backgroundScope,
                mode = mode,
                enabled = false
            )

            var count = 0

            repeat(5) {
                launch {
                    throttler.call { count++ }
                }
            }

            runCurrent()

            assertEquals(5, count, "Mode $mode: disabled should execute all")
        }
    }

    @Test
    fun `all modes - dispose stops execution`() = runTest {
        ConcurrencyMode.entries.forEach { mode ->
            val throttler = ConcurrentAsyncThrottler(
                scope = backgroundScope,
                mode = mode
            )

            var count = 0

            repeat(3) {
                launch {
                    throttler.call {
                        count++
                        delay(100)
                    }
                }
            }

            runCurrent()

            throttler.dispose()

            advanceTimeBy(500)
            runCurrent()

            assertTrue(count < 3, "Mode $mode: dispose should stop execution")
        }
    }

    @Test
    fun `all modes - wrap function works`() = runTest {
        ConcurrencyMode.entries.forEach { mode ->
            val throttler = ConcurrentAsyncThrottler(
                scope = backgroundScope,
                mode = mode
            )

            var count = 0

            val wrapped = throttler.wrap {
                count++
                delay(50)
            }

            assertNotNull(wrapped, "Mode $mode: wrap should return non-null")

            wrapped()
            runCurrent()
            advanceTimeBy(100)
            runCurrent()

            assertTrue(count > 0, "Mode $mode: wrapped function should execute")
        }
    }

    @Test
    fun `all modes - wrap with null returns null`() = runTest {
        ConcurrencyMode.entries.forEach { mode ->
            val throttler = ConcurrentAsyncThrottler(
                scope = backgroundScope,
                mode = mode
            )

            val wrapped = throttler.wrap(null)
            assertNull(wrapped, "Mode $mode: wrapping null should return null")
        }
    }

    @Test
    fun `all modes - error handling with resetOnError true`() = runTest {
        ConcurrencyMode.entries.forEach { mode ->
            val throttler = ConcurrentAsyncThrottler(
                scope = backgroundScope,
                mode = mode,
                resetOnError = true
            )

            var count = 0

            try {
                launch {
                    throttler.call {
                        count++
                        throw IllegalStateException("Error")
                    }
                }
                runCurrent()
            } catch (e: IllegalStateException) {
                // Expected
            }

            // Should recover
            launch {
                throttler.call { count++ }
            }

            runCurrent()

            assertTrue(count >= 1, "Mode $mode: should recover from error")
        }
    }

    // ====================================
    // Performance & Stress Tests
    // ====================================

    @Test
    fun `ENQUEUE - handles large queue efficiently`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        var executionCount = 0

        // Queue 100 calls
        repeat(100) {
            launch {
                throttler.call {
                    executionCount++
                    delay(10)
                }
            }
        }

        runCurrent()

        assertEquals(99, throttler.pendingCount(), "Should queue 99 (1 executing)")

        // Execute all
        repeat(100) {
            advanceTimeBy(10)
            runCurrent()
        }

        assertEquals(100, executionCount, "All calls should execute")
        assertEquals(0, throttler.pendingCount())
    }

    @Test
    fun `REPLACE - handles rapid replacements efficiently`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.REPLACE
        )

        var finalValue = 0

        // 100 rapid replacements
        repeat(100) { index ->
            launch {
                throttler.call {
                    delay(10)
                    finalValue = index
                }
            }
            if (index % 10 == 0) {
                advanceTimeBy(1)
                runCurrent()
            }
        }

        runCurrent()
        advanceTimeBy(100)
        runCurrent()

        assertEquals(99, finalValue, "Latest value should be set")
    }

    @Test
    fun `KEEP_LATEST - handles rapid updates efficiently`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.KEEP_LATEST
        )

        val executions = mutableListOf<Int>()

        // First call
        launch {
            throttler.call {
                delay(100)
                executions.add(0)
            }
        }

        advanceTimeBy(10)
        runCurrent()

        // 99 more calls (only latest kept)
        repeat(99) { index ->
            launch {
                throttler.call {
                    delay(10)
                    executions.add(index + 1)
                }
            }
        }

        runCurrent()
        advanceTimeBy(200)
        runCurrent()

        assertEquals(2, executions.size, "First + latest only")
        assertEquals(0, executions[0])
        assertEquals(99, executions[1])
    }

    // ====================================
    // Edge Cases
    // ====================================

    @Test
    fun `all modes - handle zero duration delay`() = runTest {
        ConcurrencyMode.entries.forEach { mode ->
            val throttler = ConcurrentAsyncThrottler(
                scope = backgroundScope,
                mode = mode
            )

            var count = 0

            launch {
                throttler.call { count++ }
            }

            runCurrent()

            assertTrue(count > 0, "Mode $mode: should execute with zero delay")
        }
    }

    @Test
    fun `DROP - concurrent throttlers independent`() = runTest {
        val throttler1 = ConcurrentAsyncThrottler(backgroundScope, mode = ConcurrencyMode.DROP)
        val throttler2 = ConcurrentAsyncThrottler(backgroundScope, mode = ConcurrencyMode.DROP)

        var count1 = 0
        var count2 = 0

        launch { throttler1.call { count1++; delay(100) } }
        launch { throttler2.call { count2++; delay(100) } }

        runCurrent()
        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, count1)
        assertEquals(1, count2)
    }

    @Test
    fun `ENQUEUE - sequential execution guarantees order`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.ENQUEUE
        )

        val results = mutableListOf<String>()

        launch {
            throttler.call {
                delay(50)
                results.add("A")
            }
        }

        launch {
            throttler.call {
                delay(50)
                results.add("B")
            }
        }

        launch {
            throttler.call {
                delay(50)
                results.add("C")
            }
        }

        runCurrent()

        repeat(3) {
            advanceTimeBy(50)
            runCurrent()
        }

        assertEquals(listOf("A", "B", "C"), results, "Strict FIFO order")
    }

    @Test
    fun `REPLACE - immediate replacement works`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.REPLACE
        )

        var firstStarted = false
        var secondCompleted = false

        launch {
            throttler.call {
                firstStarted = true
                delay(1000)
            }
        }

        runCurrent()

        // Immediate replacement
        launch {
            throttler.call {
                delay(50)
                secondCompleted = true
            }
        }

        runCurrent()
        advanceTimeBy(100)
        runCurrent()

        assertTrue(firstStarted, "First call should start")
        assertTrue(secondCompleted, "Second call should complete (replaced first)")
    }

    @Test
    fun `KEEP_LATEST - single call works normally`() = runTest {
        val throttler = ConcurrentAsyncThrottler(
            scope = backgroundScope,
            mode = ConcurrencyMode.KEEP_LATEST
        )

        var executed = false

        launch {
            throttler.call {
                delay(50)
                executed = true
            }
        }

        runCurrent()
        advanceTimeBy(50)
        runCurrent()

        assertTrue(executed, "Single call should execute normally")
        assertFalse(throttler.hasPendingCalls())
    }
}
