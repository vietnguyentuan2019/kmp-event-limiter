package io.github.vietnguyentuan2019.eventlimiter.demo.screens

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
import io.github.vietnguyentuan2019.eventlimiter.core.AsyncThrottler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

data class Transaction(
    val id: Int,
    val amount: Double,
    val status: String,
    val timestamp: Long
)

data class PaymentState(
    val amount: String = "100.00",
    val transactions: List<Transaction> = emptyList(),
    val isProcessing: Boolean = false,
    val attemptCount: Int = 0,
    val blockedAttempts: Int = 0
)

class PaymentViewModel : ViewModel() {
    private val _state = MutableStateFlow(PaymentState())
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    private val paymentThrottler = AsyncThrottler(
        scope = viewModelScope,
        maxDuration = 10.seconds, // Auto-unlock after 10s if stuck
        name = "PaymentProcessor"
    )

    fun onPayButtonClick() {
        _state.update { it.copy(attemptCount = it.attemptCount + 1) }

        viewModelScope.launch {
            paymentThrottler.call {
                processPayment()
            } ?: run {
                // Payment blocked (another payment in progress)
                _state.update { it.copy(blockedAttempts = it.blockedAttempts + 1) }
            }
        }
    }

    private suspend fun processPayment() {
        val amount = _state.value.amount.toDoubleOrNull() ?: 0.0

        _state.update { it.copy(isProcessing = true) }

        try {
            // Simulate payment processing (network delay)
            delay(2000)

            // Payment successful
            val transaction = Transaction(
                id = _state.value.transactions.size + 1,
                amount = amount,
                status = "✓ Success",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )

            _state.update {
                it.copy(
                    transactions = it.transactions + transaction,
                    isProcessing = false
                )
            }
        } catch (e: Exception) {
            // Payment failed
            _state.update { it.copy(isProcessing = false) }
        }
    }

    fun onAmountChange(newAmount: String) {
        // Only allow numbers and one decimal point
        if (newAmount.isEmpty() || newAmount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _state.update { it.copy(amount = newAmount) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        paymentThrottler.dispose()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = viewModel { PaymentViewModel() }
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💳 Payment Protection") },
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
                AmountCard(
                    amount = state.amount,
                    onAmountChange = { viewModel.onAmountChange(it) }
                )
            }

            item {
                PaymentButton(
                    isProcessing = state.isProcessing,
                    amount = state.amount,
                    onClick = { viewModel.onPayButtonClick() }
                )
            }

            item {
                StatsCard(
                    totalAttempts = state.attemptCount,
                    blockedAttempts = state.blockedAttempts,
                    successfulPayments = state.transactions.size
                )
            }

            item {
                Text(
                    text = "Transaction History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.transactions.reversed()) { transaction ->
                TransactionCard(transaction)
            }

            if (state.transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "No transactions yet. Try clicking Pay multiple times rapidly!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                text = "🔒 Async Lock Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• Click Pay button multiple times rapidly",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Only ONE payment will process at a time",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Subsequent clicks are blocked until payment completes",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "• Prevents double-charging users",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AmountCard(
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Payment Amount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount (USD)") },
                leadingIcon = { Text("$", style = MaterialTheme.typography.titleLarge) },
                singleLine = true
            )
        }
    }
}

@Composable
private fun PaymentButton(
    isProcessing: Boolean,
    amount: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isProcessing && amount.toDoubleOrNull()?.let { it > 0 } == true
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Processing...", modifier = Modifier.padding(8.dp))
        } else {
            Text(
                text = "💳 Pay \$$amount",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (isProcessing) {
        Text(
            text = "Try clicking the button again - it won't work!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StatsCard(
    totalAttempts: Int,
    blockedAttempts: Int,
    successfulPayments: Int
) {
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
            Text(
                text = "📊 Statistics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Button clicks:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$totalAttempts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Blocked (locked):", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$blockedAttempts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Successful payments:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$successfulPayments",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Protection Rate: ${if (totalAttempts > 0) (blockedAttempts * 100 / totalAttempts) else 0}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Transaction #${transaction.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    text = "\$${transaction.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${transaction.timestamp % 100000}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
