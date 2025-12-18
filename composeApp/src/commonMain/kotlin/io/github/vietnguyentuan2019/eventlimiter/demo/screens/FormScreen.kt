package io.github.vietnguyentuan2019.eventlimiter.demo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

data class FormState(
    val username: String = "",
    val usernameError: String? = null,
    val isValidatingUsername: Boolean = false,

    val email: String = "",
    val emailError: String? = null,
    val isValidatingEmail: Boolean = false,

    val validationCount: Int = 0
)

class FormViewModel : ViewModel() {
    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    private val usernameDebouncer = Debouncer(
        scope = viewModelScope,
        duration = 500.milliseconds,
        name = "UsernameValidator"
    )

    private val emailDebouncer = Debouncer(
        scope = viewModelScope,
        duration = 500.milliseconds,
        name = "EmailValidator"
    )

    private val takenUsernames = listOf("admin", "user", "test", "demo", "root")

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, usernameError = null, isValidatingUsername = true) }

        usernameDebouncer.call {
            viewModelScope.launch {
                validateUsername(value)
            }
        }
    }

    private suspend fun validateUsername(username: String) {
        _state.update { it.copy(validationCount = it.validationCount + 1) }

        delay(300) // Simulate API call

        val error = when {
            username.isBlank() -> null
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 20 -> "Username must be at most 20 characters"
            !username.all { it.isLetterOrDigit() } -> "Username can only contain letters and numbers"
            takenUsernames.contains(username.lowercase()) -> "Username '$username' is already taken"
            else -> null
        }

        _state.update {
            it.copy(
                usernameError = error,
                isValidatingUsername = false
            )
        }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailError = null, isValidatingEmail = true) }

        emailDebouncer.call {
            viewModelScope.launch {
                validateEmail(value)
            }
        }
    }

    private suspend fun validateEmail(email: String) {
        _state.update { it.copy(validationCount = it.validationCount + 1) }

        delay(300) // Simulate API call

        val error = when {
            email.isBlank() -> null
            !email.contains("@") -> "Email must contain @"
            !email.contains(".") -> "Email must contain a domain"
            email.count { it == '@' } > 1 -> "Email can only have one @"
            else -> null
        }

        _state.update {
            it.copy(
                emailError = error,
                isValidatingEmail = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        usernameDebouncer.dispose()
        emailDebouncer.dispose()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onNavigateBack: () -> Unit,
    viewModel: FormViewModel = viewModel { FormViewModel() }
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 Form Validation") },
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
                InfoCard(validationCount = state.validationCount)
            }

            item {
                UsernameField(
                    value = state.username,
                    error = state.usernameError,
                    isValidating = state.isValidatingUsername,
                    onValueChange = { viewModel.onUsernameChange(it) }
                )
            }

            item {
                EmailField(
                    value = state.email,
                    error = state.emailError,
                    isValidating = state.isValidatingEmail,
                    onValueChange = { viewModel.onEmailChange(it) }
                )
            }

            item {
                SubmitSection(
                    canSubmit = state.usernameError == null && state.emailError == null &&
                            state.username.isNotBlank() && state.email.isNotBlank() &&
                            !state.isValidatingUsername && !state.isValidatingEmail
                )
            }
        }
    }
}

@Composable
private fun InfoCard(validationCount: Int) {
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
                text = "Real-time Validation with Debounce",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Type rapidly - validation waits for you to pause",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Prevents excessive API calls during typing",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• 500ms delay after last keystroke",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Server validations: $validationCount",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun UsernameField(
    value: String,
    error: String?,
    isValidating: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            placeholder = { Text("Enter username (min 3 chars)") },
            trailingIcon = {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (value.isNotBlank() && error == null) {
                    Text("✓", color = Color.Green, style = MaterialTheme.typography.titleLarge)
                }
            },
            isError = error != null,
            singleLine = true
        )

        if (error != null) {
            Text(
                text = "❌ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (value.isNotBlank() && error == null && !isValidating) {
            Text(
                text = "✓ Username available!",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun EmailField(
    value: String,
    error: String?,
    isValidating: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            placeholder = { Text("your.email@example.com") },
            trailingIcon = {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (value.isNotBlank() && error == null) {
                    Text("✓", color = Color.Green, style = MaterialTheme.typography.titleLarge)
                }
            },
            isError = error != null,
            singleLine = true
        )

        if (error != null) {
            Text(
                text = "❌ $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (value.isNotBlank() && error == null && !isValidating) {
            Text(
                text = "✓ Email format valid!",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun SubmitSection(canSubmit: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canSubmit)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Submit */ },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (canSubmit) "✓ Submit Form" else "Fill all fields correctly",
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (!canSubmit) {
                Text(
                    text = "Complete the form above with valid inputs to submit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
