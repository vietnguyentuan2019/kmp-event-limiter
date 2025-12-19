import Foundation
import ComposeApp

/// Bridge class to make Kotlin Debouncer easier to use from Swift
class SwiftDebouncer {
    private var debouncer: ComposeAppDebouncer?

    init(delayMillis: Int64, action: @escaping () -> Void) {
        self.debouncer = ComposeAppDebouncer(delayMillis: delayMillis) {
            action()
        }
    }

    func call() {
        debouncer?.call()
    }

    func cancel() {
        debouncer?.cancel()
    }
}

/// Bridge class to make Kotlin Throttler easier to use from Swift
class SwiftThrottler {
    private var throttler: ComposeAppThrottler?

    init(delayMillis: Int64, action: @escaping () -> Void) {
        self.throttler = ComposeAppThrottler(delayMillis: delayMillis) {
            action()
        }
    }

    func call() {
        throttler?.call()
    }

    func cancel() {
        throttler?.cancel()
    }
}

/// Bridge class to make Kotlin AsyncThrottler easier to use from Swift
class SwiftAsyncThrottler {
    private var throttler: ComposeAppAsyncThrottler?

    init() {
        self.throttler = ComposeAppAsyncThrottler()
    }

    func call(action: @escaping () async -> Void) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            throttler?.call { error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    Task {
                        await action()
                        continuation.resume()
                    }
                }
            }
        }
    }
}

/// Bridge class for ConcurrentAsyncThrottler
class SwiftConcurrentAsyncThrottler {
    private var throttler: ComposeAppConcurrentAsyncThrottler?

    init() {
        self.throttler = ComposeAppConcurrentAsyncThrottler()
    }

    func call(action: @escaping () async -> Void) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            throttler?.call { error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    Task {
                        await action()
                        continuation.resume()
                    }
                }
            }
        }
    }
}
