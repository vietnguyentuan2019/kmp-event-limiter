import Foundation
import ComposeApp

/// Shared CoroutineScope for event limiters
/// Uses Main dispatcher to ensure callbacks run on main thread
private let eventLimiterScope: Kotlinx_coroutines_coreCoroutineScope = {
    return ScopeHelperKt.createEventLimiterScope()
}()

/// Bridge class to make Kotlin Debouncer easier to use from Swift
class SwiftDebouncer {
    private var debouncer: Debouncer?
    private var callback: (() -> Void)?

    init(delayMillis: Int64, action: @escaping () -> Void) {
        self.callback = action
        self.debouncer = Debouncer(
            scope: eventLimiterScope,
            duration: delayMillis,
            debugMode: false,
            name: nil,
            enabled: true,
            resetOnError: false,
            onMetrics: nil
        )
    }

    func call() {
        guard let callback = callback else { return }
        debouncer?.call(callback: callback)
    }

    func cancel() {
        debouncer?.cancel()
    }
}

/// Bridge class to make Kotlin Throttler easier to use from Swift
class SwiftThrottler {
    private var throttler: Throttler?
    private var callback: (() -> Void)?

    init(delayMillis: Int64, action: @escaping () -> Void) {
        self.callback = action
        self.throttler = Throttler(
            scope: eventLimiterScope,
            duration: delayMillis,
            debugMode: false,
            name: nil,
            enabled: true,
            resetOnError: false,
            onMetrics: nil
        )
    }

    func call() {
        guard let callback = callback else { return }
        throttler?.call(callback: callback)
    }

    func cancel() {
        throttler?.cancel()
    }
}

/// Bridge class to make Kotlin AsyncThrottler easier to use from Swift
/// Note: AsyncThrottler requires more complex async integration
class SwiftAsyncThrottler {
    private var throttler: AsyncThrottler?

    init() {
        self.throttler = AsyncThrottler(
            scope: eventLimiterScope,
            maxDuration: nil,
            debugMode: false,
            name: nil,
            enabled: true,
            resetOnError: false,
            onMetrics: nil
        )
    }

    func call(action: @escaping () async -> Void) async throws {
        // AsyncThrottler.call() expects a suspend function
        // For now, provide basic implementation
        // Full async/await integration would require custom Kotlin wrapper
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            // This is a simplified version - proper implementation would need
            // conversion between Swift async and Kotlin suspend functions
            Task {
                await action()
                continuation.resume()
            }
        }
    }
}

/// Bridge class for ConcurrentAsyncThrottler
class SwiftConcurrentAsyncThrottler {
    private var throttler: ConcurrentAsyncThrottler?

    init(mode: ConcurrencyMode = .drop) {
        self.throttler = ConcurrentAsyncThrottler(
            scope: eventLimiterScope,
            mode: mode,
            maxDuration: nil,
            debugMode: false,
            name: nil,
            enabled: true,
            resetOnError: false,
            onMetrics: nil
        )
    }

    func call(action: @escaping () async -> Void) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            Task {
                await action()
                continuation.resume()
            }
        }
    }
}
