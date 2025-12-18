package io.github.vietnguyentuan2019.eventlimiter.demo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vietnguyentuan2019.eventlimiter.demo.util.viewModel
import io.github.vietnguyentuan2019.eventlimiter.core.Debouncer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

data class SearchState(
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val selectedItems: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val searchCount: Int = 0,
    val debounceDuration: Long = 300
)

class SearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val debouncer = Debouncer(
        scope = viewModelScope,
        duration = 300.milliseconds,
        name = "SearchDebouncer"
    )

    private val mockDatabase = listOf(
        "Apple", "Apricot", "Avocado",
        "Banana", "Blueberry", "Blackberry",
        "Cherry", "Coconut", "Cranberry",
        "Date", "Dragonfruit", "Durian",
        "Elderberry", "Fig", "Grape",
        "Guava", "Honeydew", "Kiwi",
        "Lemon", "Lime", "Lychee",
        "Mango", "Melon", "Nectarine",
        "Orange", "Papaya", "Peach",
        "Pear", "Pineapple", "Plum",
        "Pomegranate", "Raspberry", "Strawberry",
        "Tangerine", "Watermelon"
    )

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery, isSearching = true) }

        debouncer.call {
            viewModelScope.launch {
                performSearch(newQuery)
            }
        }
    }

    private suspend fun performSearch(query: String) {
        _state.update { it.copy(searchCount = it.searchCount + 1) }

        // Simulate API delay
        delay(200)

        val results = if (query.isBlank()) {
            emptyList()
        } else {
            mockDatabase.filter { it.contains(query, ignoreCase = true) }.take(8)
        }

        _state.update {
            it.copy(
                suggestions = results,
                isSearching = false
            )
        }
    }

    fun onSuggestionClick(item: String) {
        _state.update {
            it.copy(
                selectedItems = it.selectedItems + item,
                query = "",
                suggestions = emptyList()
            )
        }
    }

    fun onRemoveItem(item: String) {
        _state.update {
            it.copy(selectedItems = it.selectedItems - item)
        }
    }

    fun updateDebounceTime(millis: Long) {
        _state.update { it.copy(debounceDuration = millis) }
        debouncer.dispose()
        // Note: In real app, would recreate debouncer with new duration
    }

    override fun onCleared() {
        super.onCleared()
        debouncer.dispose()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = viewModel { SearchViewModel() }
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔍 Search with Debounce") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
            // Info card
            item {
                InfoCard(searchCount = state.searchCount, debounceMs = state.debounceDuration)
            }

            // Search field
            item {
                SearchField(
                    query = state.query,
                    isSearching = state.isSearching,
                    onQueryChange = { viewModel.onQueryChange(it) }
                )
            }

            // Suggestions
            if (state.suggestions.isNotEmpty()) {
                item {
                    SuggestionsCard(
                        suggestions = state.suggestions,
                        onSuggestionClick = { viewModel.onSuggestionClick(it) }
                    )
                }
            }

            // Selected items
            if (state.selectedItems.isNotEmpty()) {
                item {
                    SelectedItemsCard(
                        items = state.selectedItems,
                        onRemove = { viewModel.onRemoveItem(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(searchCount: Int, debounceMs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Type rapidly in the search field below",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Search executes only ${debounceMs}ms after you stop typing",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• This prevents excessive API calls",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Debounce:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${debounceMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Searches performed:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$searchCount",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search fruits...") },
        placeholder = { Text("Try typing 'apple' or 'berry'") },
        trailingIcon = {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        },
        singleLine = true
    )
}

@Composable
private fun SuggestionsCard(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "💡 Suggestions (${suggestions.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            suggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) },
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = suggestion,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (suggestion != suggestions.last()) {
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun SelectedItemsCard(
    items: List<String>,
    onRemove: (String) -> Unit
) {
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
                text = "✓ Selected (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = { onRemove(item) }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
