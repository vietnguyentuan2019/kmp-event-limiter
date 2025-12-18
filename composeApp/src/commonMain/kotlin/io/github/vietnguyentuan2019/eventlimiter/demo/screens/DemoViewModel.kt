package io.github.vietnguyentuan2019.eventlimiter.demo.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vietnguyentuan2019.eventlimiter.core.Debouncer
import io.github.vietnguyentuan2019.eventlimiter.core.Throttler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class DemoState(
    val throttleClickCount: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<String> = emptyList(),
    val eventLog: List<String> = emptyList()
)

class DemoViewModel : ViewModel() {

    private val _state = MutableStateFlow(DemoState())
    val state: StateFlow<DemoState> = _state.asStateFlow()

    private val throttler = Throttler(
        scope = viewModelScope,
        duration = 1.seconds,
        name = "ButtonThrottler"
    )

    private val debouncer = Debouncer(
        scope = viewModelScope,
        duration = 500.milliseconds,
        name = "SearchDebouncer"
    )

    fun onThrottleClick() {
        throttler.call {
            val newCount = _state.value.throttleClickCount + 1
            val timestamp = Clock.System.now()

            _state.update {
                it.copy(
                    throttleClickCount = newCount,
                    eventLog = it.eventLog + "[${timestamp.toEpochMilliseconds() % 100000}] Throttled click executed (#$newCount)"
                )
            }
        }

        // Log attempt (will show even when throttled)
        val timestamp = Clock.System.now()
        _state.update {
            it.copy(
                eventLog = it.eventLog + "[${timestamp.toEpochMilliseconds() % 100000}] Click attempted (throttled: ${throttler.isThrottled()})"
            )
        }
    }

    fun onSearchChange(query: String) {
        _state.update { it.copy(searchQuery = query) }

        debouncer.call {
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        val timestamp = Clock.System.now()

        if (query.isBlank()) {
            _state.update {
                it.copy(
                    searchResults = emptyList(),
                    eventLog = it.eventLog + "[${timestamp.toEpochMilliseconds() % 100000}] Search cleared"
                )
            }
            return
        }

        // Simulate search
        val results = listOf(
            "Result for '$query' #1",
            "Result for '$query' #2",
            "Result for '$query' #3"
        )

        _state.update {
            it.copy(
                searchResults = results,
                eventLog = it.eventLog + "[${timestamp.toEpochMilliseconds() % 100000}] Search executed for: '$query'"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        throttler.dispose()
        debouncer.dispose()
    }
}
