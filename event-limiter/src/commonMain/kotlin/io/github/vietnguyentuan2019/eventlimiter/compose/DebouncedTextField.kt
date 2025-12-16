package io.github.vietnguyentuan2019.eventlimiter.compose

import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.vietnguyentuan2019.eventlimiter.core.AsyncDebouncer
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A TextField that debounces user input before triggering the callback.
 *
 * **Features:**
 * - Automatic debouncing of text changes
 * - Cancels previous pending calls when user continues typing
 * - Smooth UI updates (no lag)
 *
 * **Example:**
 * ```kotlin
 * var searchQuery by remember { mutableStateOf("") }
 *
 * DebouncedTextField(
 *     value = searchQuery,
 *     onValueChange = { searchQuery = it },
 *     onDebouncedChange = { query ->
 *         // This is called 500ms after user stops typing
 *         searchResults = searchApi(query)
 *     },
 *     debounceTime = 500.milliseconds,
 *     label = { Text("Search") }
 * )
 * ```
 */
@Composable
fun DebouncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDebouncedChange: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    debounceTime: Duration = 500.milliseconds,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val debouncer = remember(scope, debounceTime) {
        AsyncDebouncer(
            scope = scope,
            duration = debounceTime,
            enabled = enabled
        )
    }

    TextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue) // Update UI immediately
            scope.launch {
                debouncer.run {
                    onDebouncedChange(newValue) // Call after debounce delay
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}

/**
 * A TextField with loading state management for async operations.
 *
 * **Features:**
 * - Shows loading indicator while async operation is in progress
 * - Error handling with callback
 * - Automatic cancellation of stale requests
 *
 * **Example:**
 * ```kotlin
 * var searchQuery by remember { mutableStateOf("") }
 * var isSearching by remember { mutableStateOf(false) }
 *
 * AsyncDebouncedTextField(
 *     value = searchQuery,
 *     onValueChange = { searchQuery = it },
 *     onDebouncedChange = { query ->
 *         searchApi(query) // Suspend function
 *     },
 *     onSuccess = { results ->
 *         searchResults = results
 *     },
 *     onLoadingChanged = { loading ->
 *         isSearching = loading
 *     },
 *     trailingIcon = {
 *         if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
 *         else Icon(Icons.Default.Search, contentDescription = "Search")
 *     }
 * )
 * ```
 */
@Composable
fun <T> AsyncDebouncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDebouncedChange: suspend (String) -> T?,
    modifier: Modifier = Modifier,
    debounceTime: Duration = 500.milliseconds,
    enabled: Boolean = true,
    onSuccess: ((T) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    onLoadingChanged: ((Boolean) -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val debouncer = remember(scope, debounceTime) {
        AsyncDebouncer(
            scope = scope,
            duration = debounceTime,
            enabled = enabled
        )
    }

    TextField(
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue) // Update UI immediately
            scope.launch {
                onLoadingChanged?.invoke(true)
                try {
                    val result = debouncer.run {
                        onDebouncedChange(newValue)
                    }
                    if (result != null) {
                        onSuccess?.invoke(result)
                    }
                } catch (e: Exception) {
                    onError?.invoke(e)
                } finally {
                    onLoadingChanged?.invoke(false)
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}
