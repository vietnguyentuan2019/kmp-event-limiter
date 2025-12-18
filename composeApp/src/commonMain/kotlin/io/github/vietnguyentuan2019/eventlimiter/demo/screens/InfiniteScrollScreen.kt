package io.github.vietnguyentuan2019.eventlimiter.demo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vietnguyentuan2019.eventlimiter.demo.util.viewModel
import io.github.vietnguyentuan2019.eventlimiter.core.Throttler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

data class Post(
    val id: Int,
    val title: String,
    val content: String,
    val author: String,
    val timestamp: Long
)

data class ScrollState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val currentPage: Int = 0,
    val hasMorePages: Boolean = true,
    val totalLoadAttempts: Int = 0,
    val throttledAttempts: Int = 0,
    val successfulLoads: Int = 0
)

class InfiniteScrollViewModel : ViewModel() {
    private val _state = MutableStateFlow(ScrollState())
    val state: StateFlow<ScrollState> = _state.asStateFlow()

    private val loadMoreThrottler = Throttler(
        scope = viewModelScope,
        duration = 2.seconds,
        name = "InfiniteScrollThrottler"
    )

    init {
        // Load initial posts
        viewModelScope.launch {
            loadMorePosts()
        }
    }

    fun onScrollNearEnd() {
        _state.update { it.copy(totalLoadAttempts = it.totalLoadAttempts + 1) }

        loadMoreThrottler.call {
            viewModelScope.launch {
                loadMorePosts()
            }
        } ?: run {
            // Throttled - too soon after last load
            _state.update { it.copy(throttledAttempts = it.throttledAttempts + 1) }
        }
    }

    private suspend fun loadMorePosts() {
        val currentState = _state.value
        if (!currentState.hasMorePages || currentState.isLoading) return

        _state.update { it.copy(isLoading = true) }

        // Simulate network delay
        delay(1000)

        val nextPage = currentState.currentPage + 1
        val newPosts = generatePosts(nextPage)

        _state.update {
            it.copy(
                posts = it.posts + newPosts,
                currentPage = nextPage,
                hasMorePages = nextPage < 10, // Max 10 pages
                isLoading = false,
                successfulLoads = it.successfulLoads + 1
            )
        }
    }

    private fun generatePosts(page: Int): List<Post> {
        val startId = (page - 1) * 10 + 1
        return List(10) { index ->
            val id = startId + index
            Post(
                id = id,
                title = "Post #$id - ${getTitleForPost(id)}",
                content = "This is the content for post $id. " +
                        "It demonstrates infinite scroll with throttling to prevent excessive API calls. " +
                        "Try scrolling rapidly to the bottom!",
                author = getAuthorForPost(id),
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private fun getTitleForPost(id: Int): String {
        val titles = listOf(
            "Amazing Discovery", "Breaking News", "Tech Update",
            "Life Hacks", "Travel Tips", "Food Review",
            "Movie Review", "Book Recommendation", "Music Release",
            "Gaming News", "Sports Highlights", "Science Breakthrough"
        )
        return titles[id % titles.size]
    }

    private fun getAuthorForPost(id: Int): String {
        val authors = listOf(
            "Alice", "Bob", "Charlie", "Diana", "Eve",
            "Frank", "Grace", "Henry", "Ivy", "Jack"
        )
        return authors[id % authors.size]
    }

    override fun onCleared() {
        super.onCleared()
        loadMoreThrottler.dispose()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfiniteScrollScreen(
    onNavigateBack: () -> Unit,
    viewModel: InfiniteScrollViewModel = viewModel { InfiniteScrollViewModel() }
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Detect when user scrolls near the end
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            // Trigger when within 3 items of the end
            lastVisibleItem >= totalItems - 3 && totalItems > 0
        }.collect { nearEnd ->
            if (nearEnd && !state.isLoading && state.hasMorePages) {
                viewModel.onScrollNearEnd()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 Infinite Scroll") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.headlineMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Stats header
            StatsHeader(
                totalAttempts = state.totalLoadAttempts,
                throttledAttempts = state.throttledAttempts,
                successfulLoads = state.successfulLoads
            )

            // Post list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info card at top
                item {
                    InfoCard()
                }

                // Posts
                items(state.posts) { post ->
                    PostCard(post)
                }

                // Loading indicator
                if (state.isLoading) {
                    item {
                        LoadingCard()
                    }
                }

                // End of list
                if (!state.hasMorePages && state.posts.isNotEmpty()) {
                    item {
                        EndOfListCard(totalPosts = state.posts.size)
                    }
                }
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
                text = "🔄 Throttled Infinite Scroll",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Scroll down rapidly to see throttling in action",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• New page loads only every 2 seconds max",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Prevents loading same page multiple times",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Reduces server load and data usage",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun StatsHeader(
    totalAttempts: Int,
    throttledAttempts: Int,
    successfulLoads: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Load Attempts",
                value = totalAttempts.toString(),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            StatItem(
                label = "Throttled",
                value = throttledAttempts.toString(),
                color = MaterialTheme.colorScheme.error
            )

            StatItem(
                label = "Loaded",
                value = successfulLoads.toString(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun PostCard(post: Post) {
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
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "#${post.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "by ${post.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Loading more posts...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EndOfListCard(totalPosts: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = "You've reached the end!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total posts loaded: $totalPosts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
