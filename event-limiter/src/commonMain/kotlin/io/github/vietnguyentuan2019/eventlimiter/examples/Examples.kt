package io.github.vietnguyentuan2019.eventlimiter.examples

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vietnguyentuan2019.eventlimiter.compose.*
import io.github.vietnguyentuan2019.eventlimiter.core.ConcurrencyMode
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Example 1: Throttle Click - Prevent Double Clicks
 *
 * Demonstrates how to prevent users from clicking a button multiple times rapidly.
 */
@Composable
fun ThrottleClickExample() {
    var clickCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Throttle Click Example", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Click count: $clickCount", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))

        // Using Modifier extension
        Button(
            onClick = {},
            modifier = Modifier.throttleClick(duration = 1000.milliseconds) {
                clickCount++
            }
        ) {
            Text("Click Me (Throttled 1s)")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Try clicking rapidly - only 1 click per second is registered",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Example 2: Async Button - Form Submission with Loading
 *
 * Demonstrates automatic loading state management during async operations.
 */
@Composable
fun AsyncButtonExample() {
    var submitCount by remember { mutableStateOf(0) }
    var lastSubmitTime by remember { mutableStateOf("Never") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Async Button Example", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Submissions: $submitCount", style = MaterialTheme.typography.bodyLarge)
        Text("Last submit: $lastSubmitTime", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        AsyncButton(
            onClick = {
                // Simulate API call
                delay(2000)
                submitCount++
                lastSubmitTime = Clock.System.now().toEpochMilliseconds().toString()
            },
            modifier = Modifier.fillMaxWidth()
        ) { isLoading ->
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Submitting...")
                }
            } else {
                Text("Submit Form")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Button is disabled while loading. Try clicking during loading.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Example 3: Debounced TextField - Search with Debounce
 *
 * Demonstrates search input that waits for user to stop typing before executing.
 */
@Composable
fun DebouncedTextFieldExample() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text("Debounced TextField Example", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Search API calls: $searchCount", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        DebouncedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            onDebouncedChange = { query ->
                // Simulate API call
                delay(500)
                searchCount++
                if (query.isNotBlank()) {
                    searchResults = listOf(
                        "Result 1 for '$query'",
                        "Result 2 for '$query'",
                        "Result 3 for '$query'"
                    )
                } else {
                    searchResults = emptyList()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search") },
            placeholder = { Text("Type to search...") }
        )

        Spacer(Modifier.height(16.dp))

        if (searchResults.isNotEmpty()) {
            Text("Results:", style = MaterialTheme.typography.titleMedium)
            searchResults.forEach { result ->
                Text("• $result", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Type rapidly - API is only called 500ms after you stop typing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Example 4: Async Debounced TextField - Search with Loading State
 *
 * Demonstrates search with loading indicator and error handling.
 */
@Composable
fun AsyncDebouncedTextFieldExample() {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text("Async Debounced TextField Example", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        AsyncDebouncedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            onDebouncedChange = { query ->
                // Simulate API call with random error
                delay(1000)
                if (query.contains("error")) {
                    throw Exception("Search failed!")
                }
                if (query.isNotBlank()) {
                    listOf(
                        "Product 1: $query",
                        "Product 2: $query",
                        "Product 3: $query"
                    )
                } else {
                    emptyList()
                }
            },
            onSuccess = { results ->
                searchResults = results
                searchError = null
            },
            onError = { error ->
                searchError = error.message
                searchResults = emptyList()
            },
            onLoadingChanged = { loading ->
                isSearching = loading
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Products") },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        when {
            searchError != null -> {
                Text(
                    "Error: $searchError",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            searchResults.isNotEmpty() -> {
                Text("Results:", style = MaterialTheme.typography.titleMedium)
                searchResults.forEach { result ->
                    Text("• $result", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Type 'error' to simulate an error. Shows loading indicator during search.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Example 5: Concurrent Async Throttler - Chat Messages (Enqueue)
 *
 * Demonstrates queueing messages to send them in order.
 */
@Composable
fun ConcurrentAsyncThrottlerEnqueueExample() {
    var messageCount by remember { mutableStateOf(0) }
    var sentMessages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Text("Enqueue Mode Example (Chat)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Pending: $pendingCount | Sent: ${sentMessages.size}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))

        ConcurrentAsyncButton(
            onClick = {
                // Simulate sending message
                delay(1000)
                val msg = "Message ${++messageCount}"
                sentMessages = sentMessages + msg
            },
            mode = ConcurrencyMode.ENQUEUE,
            modifier = Modifier.fillMaxWidth()
        ) { isLoading, pending ->
            pendingCount = pending
            Text(
                if (pending > 0) "Sending ($pending)..." else "Send Message"
            )
        }

        Spacer(Modifier.height(16.dp))

        if (sentMessages.isNotEmpty()) {
            Text("Sent Messages:", style = MaterialTheme.typography.titleMedium)
            sentMessages.takeLast(5).forEach { msg ->
                Text("✓ $msg", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Click rapidly - all messages are queued and sent in order",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Complete Example App - Combines all examples
 */
@Composable
fun EventLimiterExamplesApp() {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Throttle") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Async") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Debounce") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Search") }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Queue") }
            )
        }

        when (selectedTab) {
            0 -> ThrottleClickExample()
            1 -> AsyncButtonExample()
            2 -> DebouncedTextFieldExample()
            3 -> AsyncDebouncedTextFieldExample()
            4 -> ConcurrentAsyncThrottlerEnqueueExample()
        }
    }
}
