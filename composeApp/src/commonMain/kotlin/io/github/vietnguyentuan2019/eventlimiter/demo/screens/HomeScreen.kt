package io.github.vietnguyentuan2019.eventlimiter.demo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.vietnguyentuan2019.eventlimiter.demo.util.viewModel
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DemoViewModel = viewModel { DemoViewModel() },
    onNavigate: (io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KMP Event Limiter Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Throttler Demo
            item {
                ThrottlerDemoSection(
                    throttleCount = state.throttleClickCount,
                    onThrottleClick = { viewModel.onThrottleClick() },
                    eventLog = state.eventLog
                )
            }

            // Debouncer Demo
            item {
                DebouncerDemoSection(
                    searchQuery = state.searchQuery,
                    searchResults = state.searchResults,
                    onSearchChange = { viewModel.onSearchChange(it) }
                )
            }

            // Event Log
            item {
                EventLogSection(events = state.eventLog)
            }

            // Navigation to other demos
            item {
                NavigationSection(onNavigate = onNavigate)
            }
        }
    }
}

@Composable
private fun NavigationSection(onNavigate: (io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🚀 More Demos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            val demos = listOf(
                io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen.Search,
                io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen.Form,
                io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen.Payment,
                io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen.Scroll,
                io.github.vietnguyentuan2019.eventlimiter.demo.navigation.Screen.Settings
            )

            demos.forEach { screen ->
                OutlinedButton(
                    onClick = { onNavigate(screen) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = screen.icon, style = MaterialTheme.typography.titleMedium)
                        Text(text = screen.title, modifier = Modifier.weight(1f))
                        Text(text = "→", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThrottlerDemoSection(
    throttleCount: Int,
    onThrottleClick: () -> Unit,
    eventLog: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⏱️ Throttler Demo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Click rapidly - only first click executes, subsequent clicks blocked for 1 second",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onThrottleClick
                ) {
                    Text("Throttled Button (1s)")
                }

                Text(
                    text = "Executed: $throttleCount times",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "Try clicking multiple times rapidly!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun DebouncerDemoSection(
    searchQuery: String,
    searchResults: List<String>,
    onSearchChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⏳ Debouncer Demo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Type rapidly - search executes only after 500ms pause",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search (500ms debounce)") },
                placeholder = { Text("Type something...") },
                singleLine = true
            )

            if (searchResults.isNotEmpty()) {
                Text(
                    text = "Search Results:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                searchResults.forEach { result ->
                    Text(
                        text = "• $result",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventLogSection(events: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📝 Event Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${events.size} events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }

            Divider()

            if (events.isEmpty()) {
                Text(
                    text = "No events yet - try interacting above!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    events.takeLast(10).reversed().forEach { event ->
                        Text(
                            text = event,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    if (events.size > 10) {
                        Text(
                            text = "... and ${events.size - 10} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
