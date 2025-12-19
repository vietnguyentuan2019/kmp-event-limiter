# iOS Demo App for KMP Event Limiter

This directory contains a SwiftUI demo app that showcases the KMP Event Limiter library on iOS.

## Project Structure

```
iosApp/
├── iosApp/
│   ├── EventLimiterDemoApp.swift    # Main app entry point
│   ├── ContentView.swift             # Home screen with navigation
│   ├── SearchDemoView.swift          # Autocomplete search demo
│   ├── FormDemoView.swift            # Form validation demo
│   ├── PaymentDemoView.swift         # Payment protection demo
│   ├── InfiniteScrollDemoView.swift  # Infinite scroll demo
│   ├── SettingsDemoView.swift        # Settings & playground
│   └── Info.plist                    # App configuration
└── README.md                          # This file
```

## Setup Instructions

### Step 1: Build the Kotlin Framework

First, build the iOS framework from the Kotlin Multiplatform module:

```bash
cd /Users/vietnguyen/DATA/TECHX/kmp-event-limiter
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

The framework will be built at:
```
composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework
```

### Step 2: Create Xcode Project

1. Open Xcode
2. Create a new project: **File → New → Project**
3. Select **iOS → App**
4. Configure the project:
   - **Product Name:** `EventLimiterDemo`
   - **Team:** Select your team
   - **Organization Identifier:** `io.github.vietnguyentuan2019`
   - **Bundle Identifier:** `io.github.vietnguyentuan2019.EventLimiterDemo`
   - **Interface:** SwiftUI
   - **Language:** Swift
   - **Use Core Data:** Unchecked
5. Save the project in the `iosApp` directory

### Step 3: Add Swift Files

1. Delete the default `ContentView.swift` and `EventLimiterDemoApp.swift` files that Xcode created
2. Add the existing Swift files from the `iosApp/iosApp` directory:
   - Right-click on the project in Xcode
   - Select **Add Files to "EventLimiterDemo"...**
   - Navigate to `iosApp/iosApp`
   - Select all `.swift` files
   - Make sure **Copy items if needed** is UNCHECKED (files are already in the right place)
   - Click **Add**

### Step 4: Link the Kotlin Framework

#### Option A: Manual Linking (Recommended for Development)

1. In Xcode, select your project in the navigator
2. Select the app target
3. Go to **General** tab
4. Scroll to **Frameworks, Libraries, and Embedded Content**
5. Click the **+** button
6. Click **Add Other... → Add Files...**
7. Navigate to:
   ```
   composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework
   ```
8. Click **Open**
9. Set **Embed** to **Embed & Sign**

#### Option B: Build Phase Script (Automatic)

Add a build phase to automatically copy the framework:

1. Select your target → **Build Phases**
2. Click **+** → **New Run Script Phase**
3. Add this script:

```bash
FRAMEWORK_PATH="${PROJECT_DIR}/../composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework"

if [ -d "$FRAMEWORK_PATH" ]; then
    echo "Copying framework from $FRAMEWORK_PATH"
    rm -rf "${BUILT_PRODUCTS_DIR}/${FRAMEWORKS_FOLDER_PATH}/ComposeApp.framework"
    cp -R "$FRAMEWORK_PATH" "${BUILT_PRODUCTS_DIR}/${FRAMEWORKS_FOLDER_PATH}/"
else
    echo "Error: Framework not found at $FRAMEWORK_PATH"
    echo "Please run: ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64"
    exit 1
fi
```

4. Drag this phase BEFORE **Embed Frameworks**

### Step 5: Configure Build Settings

1. Select your target → **Build Settings**
2. Search for **Framework Search Paths**
3. Add:
   ```
   $(PROJECT_DIR)/../composeApp/build/bin/iosSimulatorArm64/debugFramework
   ```

### Step 6: Import the Framework in Swift

In your Swift files, import the framework:

```swift
import ComposeApp
```

## Demo Screens

### 1. Search Demo (SearchDemoView.swift)
- Autocomplete fruit search
- Debounce (300ms) to reduce API calls
- Selected items tracking
- Search count statistics

### 2. Form Validation Demo (FormDemoView.swift)
- Real-time username validation
- Real-time email validation
- Debounce (500ms) to avoid server spam
- Visual feedback (checkmarks, errors)

### 3. Payment Protection Demo (PaymentDemoView.swift)
- Payment button with AsyncThrottler
- Prevents double-submission
- Transaction history
- Protection rate statistics

### 4. Infinite Scroll Demo (InfiniteScrollDemoView.swift)
- 100 posts across 10 pages
- Throttle (2s) prevents excessive loads
- Load attempt tracking
- End-of-list indicator

### 5. Settings & Playground (SettingsDemoView.swift)
- Adjustable debounce/throttle durations
- Interactive testing
- Statistics display
- Library information

## Integrating the Kotlin Event Limiter

The Swift views currently have placeholder implementations. To integrate the actual Kotlin event limiters:

### Example: Debouncer Integration

**Recommended: Use the SwiftDebouncer bridge class** for cleaner code:

```swift
class SearchDemoViewModel: ObservableObject {
    private var debouncer: SwiftDebouncer?

    init() {
        // Create debouncer with 300ms delay
        debouncer = SwiftDebouncer(delayMillis: 300) {
            self.performSearch()
        }
    }

    func search(query: String) {
        // Call debouncer - will only execute after 300ms of inactivity
        debouncer?.call()
    }

    private func performSearch() {
        // Actual search implementation
    }
}
```

**Direct Kotlin API usage** (if needed):

```swift
import ComposeApp

class SearchDemoViewModel: ObservableObject {
    private var debouncer: Debouncer?
    private let scope = ComposeAppKotlinx_coroutines_coreCoroutineScopeKt.MainScope()

    init() {
        debouncer = Debouncer(
            scope: scope,
            duration: 300,
            debugMode: false,
            name: nil,
            enabled: true,
            resetOnError: false,
            onMetrics: nil
        )
    }

    func search(query: String) {
        debouncer?.call {
            self.performSearch()
        }
    }
}
```

### Example: Throttler Integration

**Recommended: Use SwiftThrottler:**

```swift
class InfiniteScrollViewModel: ObservableObject {
    private var throttler: SwiftThrottler?

    init() {
        // Create throttler with 2000ms delay
        throttler = SwiftThrottler(delayMillis: 2000) {
            self.performLoad()
        }
    }

    func loadMore() {
        // Call throttler - executes immediately, then blocks for 2s
        throttler?.call()
    }

    private func performLoad() {
        // Actual API call
    }
}
```

### Important Notes

1. **CoroutineScope**: The bridge classes use a shared `MainScope()` internally
2. **Thread Safety**: All callbacks execute on the main thread via Kotlin's MainScope
3. **Memory Management**: Bridge classes properly manage Kotlin object lifecycle
4. **Callback Storage**: Bridge classes store Swift closures and pass them to Kotlin on each call

### API Changes from Direct Kotlin

The new Kotlin/Native API requires:
- A `CoroutineScope` parameter in constructors
- Callbacks passed to `call()` method instead of constructor
- Multiple optional parameters (debugMode, name, enabled, etc.)

The SwiftDebouncer/SwiftThrottler bridge classes hide this complexity and provide a cleaner Swift API.

## Building for Device

To build for a real device:

1. Build the framework for the appropriate architecture:
   ```bash
   # For iOS device (arm64)
   ./gradlew :composeApp:linkDebugFrameworkIosArm64
   ```

2. Update the framework search path in Xcode to point to:
   ```
   composeApp/build/bin/iosArm64/debugFramework
   ```

3. Select your device in Xcode and run

## Troubleshooting

### Framework Not Found
- Make sure you've built the framework: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- Check the framework path in Build Settings → Framework Search Paths

### Module 'ComposeApp' not found
- Verify the framework is embedded: Target → General → Frameworks, Libraries, and Embedded Content
- Make sure **Embed & Sign** is selected

### Architecture Mismatch
- For simulator: Use `iosSimulatorArm64` (Apple Silicon) or `iosX64` (Intel)
- For device: Use `iosArm64`

## Next Steps

1. **Integrate Kotlin Event Limiters**: Replace placeholder implementations with actual Kotlin calls
2. **Add Tests**: Write unit tests for ViewModels
3. **Improve UI**: Add animations, better error handling
4. **Add More Examples**: Showcase additional use cases
5. **Performance**: Profile and optimize

## Resources

- [KMP Event Limiter Documentation](../../README.md)
- [Kotlin Multiplatform iOS Integration](https://kotlinlang.org/docs/multiplatform-mobile-integrate-in-existing-app.html)
- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui/)
