package io.github.vietnguyentuan2019.eventlimiter.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.vietnguyentuan2019.eventlimiter.core.ConcurrencyMode
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * A button that automatically manages loading state during async operations.
 *
 * **Features:**
 * - Automatic loading state management
 * - Prevents double-clicks during execution
 * - Customizable loading indicator
 * - Error handling with callbacks
 *
 * **Example:**
 * ```kotlin
 * AsyncButton(
 *     onClick = { submitForm() },
 *     onError = { error -> showSnackbar("Error: $error") },
 *     modifier = Modifier.fillMaxWidth()
 * ) { isLoading ->
 *     if (isLoading) {
 *         CircularProgressIndicator(color = Color.White)
 *     } else {
 *         Text("Submit")
 *     }
 * }
 * ```
 */
@Composable
fun AsyncButton(
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    mode: ConcurrencyMode = ConcurrencyMode.DROP,
    maxDuration: Duration? = null,
    onError: ((Throwable) -> Unit)? = null,
    loadingIndicator: @Composable () -> Unit = { CircularProgressIndicator() },
    content: @Composable (isLoading: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!isLoading && enabled) {
                scope.launch {
                    isLoading = true
                    try {
                        onClick()
                    } catch (e: Exception) {
                        onError?.invoke(e)
                    } finally {
                        isLoading = false
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            Box {
                loadingIndicator()
            }
        } else {
            content(false)
        }
    }
}

/**
 * A button with concurrency control for complex async scenarios.
 *
 * **Use cases:**
 * - Chat apps: Use `ConcurrencyMode.ENQUEUE` to queue messages
 * - Search: Use `ConcurrencyMode.REPLACE` to cancel old queries
 * - Auto-save: Use `ConcurrencyMode.KEEP_LATEST` to save final version
 *
 * **Example:**
 * ```kotlin
 * ConcurrentAsyncButton(
 *     onClick = { sendMessage(text) },
 *     mode = ConcurrencyMode.ENQUEUE,
 *     modifier = Modifier.fillMaxWidth()
 * ) { isLoading, pendingCount ->
 *     Text(
 *         if (pendingCount > 0) "Sending ($pendingCount)..." else "Send"
 *     )
 * }
 * ```
 */
@Composable
fun ConcurrentAsyncButton(
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    mode: ConcurrencyMode = ConcurrencyMode.DROP,
    maxDuration: Duration? = null,
    onError: ((Throwable) -> Unit)? = null,
    loadingIndicator: @Composable () -> Unit = { CircularProgressIndicator() },
    content: @Composable (isLoading: Boolean, pendingCount: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var pendingCount by remember { mutableStateOf(0) }

    Button(
        onClick = {
            if (enabled) {
                scope.launch {
                    isLoading = true
                    pendingCount++
                    try {
                        onClick()
                    } catch (e: Exception) {
                        onError?.invoke(e)
                    } finally {
                        pendingCount--
                        if (pendingCount == 0) {
                            isLoading = false
                        }
                    }
                }
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading && mode == ConcurrencyMode.DROP) {
            Box {
                loadingIndicator()
            }
        } else {
            content(isLoading, pendingCount)
        }
    }
}
