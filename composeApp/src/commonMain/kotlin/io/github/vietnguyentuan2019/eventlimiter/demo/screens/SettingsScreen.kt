package io.github.vietnguyentuan2019.eventlimiter.demo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vietnguyentuan2019.eventlimiter.demo.util.viewModel
import io.github.vietnguyentuan2019.eventlimiter.core.Debouncer
import io.github.vietnguyentuan2019.eventlimiter.core.Throttler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds

data class SettingsState(
    val debounceDuration: Int = 300,
    val throttleDuration: Int = 1000,
    val debounceTestValue: String = "",
    val debounceExecutionCount: Int = 0,
    val throttleClickCount: Int = 0,
    val throttleExecutionCount: Int = 0,
    val lastMessage: String = ""
)

class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var debouncer: Debouncer? = null
    private var throttler: Throttler? = null

    init {
        recreateLimiters()
    }

    private fun recreateLimiters() {
        debouncer?.dispose()
        throttler?.dispose()

        debouncer = Debouncer(
            scope = viewModelScope,
            duration = _state.value.debounceDuration.milliseconds,
            name = "SettingsDebouncer"
        )

        throttler = Throttler(
            scope = viewModelScope,
            duration = _state.value.throttleDuration.milliseconds,
            name = "SettingsThrottler"
        )
    }

    fun onDebounceDurationChange(value: Int) {
        _state.update { it.copy(debounceDuration = value) }
        recreateLimiters()
    }

    fun onThrottleDurationChange(value: Int) {
        _state.update { it.copy(throttleDuration = value) }
        recreateLimiters()
    }

    fun onDebounceTestChange(value: String) {
        _state.update { it.copy(debounceTestValue = value) }

        debouncer?.call {
            _state.update {
                it.copy(
                    debounceExecutionCount = it.debounceExecutionCount + 1,
                    lastMessage = "Debounce executed at ${Clock.System.now().toEpochMilliseconds() % 10000}ms"
                )
            }
        }
    }

    fun onThrottleTestClick() {
        _state.update { it.copy(throttleClickCount = it.throttleClickCount + 1) }

        throttler?.call {
            _state.update {
                it.copy(
                    throttleExecutionCount = it.throttleExecutionCount + 1,
                    lastMessage = "Throttle executed at ${Clock.System.now().toEpochMilliseconds() % 10000}ms"
                )
            }
        }
    }

    fun resetCounters() {
        _state.update {
            it.copy(
                debounceExecutionCount = 0,
                throttleClickCount = 0,
                throttleExecutionCount = 0,
                lastMessage = "Counters reset"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        debouncer?.dispose()
        throttler?.dispose()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel() }
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Settings & Playground") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoCard()
            }

            item {
                DebounceConfigCard(
                    duration = state.debounceDuration,
                    onDurationChange = { viewModel.onDebounceDurationChange(it) }
                )
            }

            item {
                DebounceTestCard(
                    value = state.debounceTestValue,
                    executionCount = state.debounceExecutionCount,
                    onValueChange = { viewModel.onDebounceTestChange(it) }
                )
            }

            item {
                ThrottleConfigCard(
                    duration = state.throttleDuration,
                    onDurationChange = { viewModel.onThrottleDurationChange(it) }
                )
            }

            item {
                ThrottleTestCard(
                    clickCount = state.throttleClickCount,
                    executionCount = state.throttleExecutionCount,
                    onTestClick = { viewModel.onThrottleTestClick() }
                )
            }

            if (state.lastMessage.isNotEmpty()) {
                item {
                    MessageCard(message = state.lastMessage)
                }
            }

            item {
                ResetCard(onReset = { viewModel.resetCounters() })
            }

            item {
                LibraryInfoCard()
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🧪 Interactive Playground",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Experiment with different debounce and throttle durations to see how they affect behavior.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DebounceConfigCard(
    duration: Int,
    onDurationChange: (Int) -> Unit
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
                text = "⏳ Debounce Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Duration: ${duration}ms",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Slider(
                value = duration.toFloat(),
                onValueChange = { onDurationChange(it.toInt()) },
                valueRange = 100f..2000f,
                steps = 18
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("100ms", style = MaterialTheme.typography.bodySmall)
                Text("2000ms", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DebounceTestCard(
    value: String,
    executionCount: Int,
    onValueChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Test Debounce",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type rapidly...") },
                placeholder = { Text("Start typing to test debounce") },
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Executions:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$executionCount",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ThrottleConfigCard(
    duration: Int,
    onDurationChange: (Int) -> Unit
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
                text = "⏱️ Throttle Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Duration: ${duration}ms",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Slider(
                value = duration.toFloat(),
                onValueChange = { onDurationChange(it.toInt()) },
                valueRange = 100f..5000f,
                steps = 48
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("100ms", style = MaterialTheme.typography.bodySmall)
                Text("5000ms", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ThrottleTestCard(
    clickCount: Int,
    executionCount: Int,
    onTestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Test Throttle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Click Me Rapidly!", modifier = Modifier.padding(8.dp))
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Clicks:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$clickCount",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        text = "Executions:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$executionCount",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (clickCount > executionCount) {
                Text(
                    text = "${clickCount - executionCount} clicks throttled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = "💬 $message",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ResetCard(onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔄 Reset",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset All Counters")
            }
        }
    }
}

@Composable
private fun LibraryInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📚 KMP Event Limiter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Version: 1.0.0",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "A Kotlin Multiplatform library for event rate limiting with Debouncer, Throttler, and AsyncThrottler.",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Platform Support:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )

            listOf("✓ Android", "✓ iOS", "✓ Desktop (JVM)", "✓ Web (JS)").forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
