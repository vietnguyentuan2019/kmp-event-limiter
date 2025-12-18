import SwiftUI

class SettingsDemoViewModel: ObservableObject {
    @Published var debounceDuration: Double = 300 {
        didSet {
            // Recreate debouncer with new duration
            recreateDebouncer()
        }
    }
    @Published var throttleDuration: Double = 1000 {
        didSet {
            // Recreate throttler with new duration
            recreateThrottler()
        }
    }
    @Published var debounceCount = 0
    @Published var throttleCount = 0

    private var debouncer: SwiftDebouncer?
    private var throttler: SwiftThrottler?

    init() {
        recreateDebouncer()
        recreateThrottler()
    }

    private func recreateDebouncer() {
        debouncer?.cancel()
        debouncer = SwiftDebouncer(delayMillis: Int64(debounceDuration)) { [weak self] in
            self?.performDebounceAction()
        }
    }

    private func recreateThrottler() {
        throttler?.cancel()
        throttler = SwiftThrottler(delayMillis: Int64(throttleDuration)) { [weak self] in
            self?.performThrottleAction()
        }
    }

    func testDebounce() {
        // Call debouncer - will only execute after delay
        debouncer?.call()
    }

    private func performDebounceAction() {
        debounceCount += 1
    }

    func testThrottle() {
        // Call throttler - executes immediately, then blocks for duration
        throttler?.call()
    }

    private func performThrottleAction() {
        throttleCount += 1
    }

    func reset() {
        debounceCount = 0
        throttleCount = 0
    }
}

struct SettingsDemoView: View {
    @StateObject private var viewModel = SettingsDemoViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Header
                VStack(alignment: .leading, spacing: 8) {
                    Text("Interactive Playground")
                        .font(.title2)
                        .bold()

                    Text("Adjust the durations and test both debounce and throttle patterns.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .cornerRadius(12)

                // Debounce Section
                VStack(alignment: .leading, spacing: 16) {
                    Text("Debounce Pattern")
                        .font(.headline)

                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Duration:")
                            Spacer()
                            Text("\(Int(viewModel.debounceDuration))ms")
                                .bold()
                                .foregroundColor(.blue)
                        }

                        Slider(value: $viewModel.debounceDuration, in: 100...2000, step: 100)
                    }

                    Text("Click rapidly - only the last click after delay will execute")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Button(action: { viewModel.testDebounce() }) {
                        Text("Test Debounce")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }

                    HStack {
                        Text("Debounce Count:")
                        Spacer()
                        Text("\(viewModel.debounceCount)")
                            .bold()
                            .foregroundColor(.blue)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                // Throttle Section
                VStack(alignment: .leading, spacing: 16) {
                    Text("Throttle Pattern")
                        .font(.headline)

                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("Duration:")
                            Spacer()
                            Text("\(Int(viewModel.throttleDuration))ms")
                                .bold()
                                .foregroundColor(.green)
                        }

                        Slider(value: $viewModel.throttleDuration, in: 100...5000, step: 100)
                    }

                    Text("Click rapidly - executes immediately, then blocks for duration")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    Button(action: { viewModel.testThrottle() }) {
                        Text("Test Throttle")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }

                    HStack {
                        Text("Throttle Count:")
                        Spacer()
                        Text("\(viewModel.throttleCount)")
                            .bold()
                            .foregroundColor(.green)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                // Reset Button
                Button(action: { viewModel.reset() }) {
                    Text("Reset Counters")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.red.opacity(0.1))
                        .foregroundColor(.red)
                        .cornerRadius(12)
                }
                .padding(.horizontal)

                // Library Info
                VStack(alignment: .leading, spacing: 12) {
                    Text("About KMP Event Limiter")
                        .font(.headline)

                    VStack(alignment: .leading, spacing: 8) {
                        infoRow(label: "Version", value: "1.0.0")
                        infoRow(label: "Platform", value: "Kotlin Multiplatform")
                        infoRow(label: "Targets", value: "Android, iOS, Desktop, Web")
                        infoRow(label: "License", value: "MIT")
                    }

                    Link("View on GitHub", destination: URL(string: "https://github.com/yourusername/kmp-event-limiter")!)
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)

                Spacer()
            }
            .padding()
        }
        .background(Color(.systemGroupedBackground))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .bold()
        }
        .font(.caption)
    }
}

struct SettingsDemoView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            SettingsDemoView()
        }
    }
}
