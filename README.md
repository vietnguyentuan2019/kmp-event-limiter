# KMP Event Limiter 🛡️

[![Maven Central](https://img.shields.io/maven-central/v/io.github.vietnguyentuan2019/kmp-event-limiter?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.vietnguyentuan2019/kmp-event-limiter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-android%20|%20ios%20|%20desktop%20|%20web-lightgrey)](https://kotlinlang.org/docs/multiplatform.html)

**Production-ready throttle and debounce for Kotlin Multiplatform & Compose Multiplatform.**

Stop wrestling with coroutine boilerplate, race conditions, and state management. Handle button spam, search debouncing, and async operations with **Kotlin-first** design leveraging Coroutines and Compose.

Inspired by [flutter_event_limiter](https://pub.dev/packages/flutter_event_limiter) but redesigned for the Kotlin/Compose ecosystem with idiomatic patterns.

---

## ⚡ Quick Start

### Problem: Manual Throttling in Compose (Verbose & Error-Prone)

```kotlin
// Traditional approach - lots of boilerplate
val scope = rememberCoroutineScope()
var isLoading by remember { mutableStateOf(false) }

Button(
    onClick = {
        if (!isLoading) {
            scope.launch {
                isLoading = true
                try {
                    submitForm()
                } finally {
                    isLoading = false
                }
            }
        }
    },
    enabled = !isLoading
) {
    if (isLoading) CircularProgressIndicator() else Text("Submit")
}
```

### Solution: KMP Event Limiter (Clean & Concise)

```kotlin
// With modifier - 1 line
Button(
    onClick = {},
    modifier = Modifier.asyncThrottleClick { submitForm() }
) {
    Text("Submit")
}

// With AsyncButton - automatic loading state
AsyncButton(
    onClick = { submitForm() }
) { isLoading ->
    if (isLoading) CircularProgressIndicator() else Text("Submit")
}
```

**Result:** 80% less code. Auto-dispose. Auto-cancellation. Type-safe.

---

## ✨ Why This Library?

**Kotlin-First Design:**
- ✅ **Coroutines Native** - Built on `suspend` functions, not callbacks
- ✅ **Compose Modifiers** - Idiomatic extension functions like `.throttleClick()`
- ✅ **Type Safety** - Full type inference, no `Any` or reflection

**Production Ready:**
- ✅ **Multiplatform** - Android, iOS, Desktop, Web (Compose Multiplatform)
- ✅ **Zero Dependencies** - Only Kotlin stdlib + Compose
- ✅ **Memory Safe** - Auto-disposal via Compose lifecycle

**Developer Experience:**
- ✅ **Concise** - 90% less code than manual implementation
- ✅ **Flexible** - Modifiers, Builders, or Direct Controllers
- ✅ **Debuggable** - Built-in debug mode with logging

---

## 🚀 Features

### Core Capabilities

| Feature | Description | Use Case |
|---------|-------------|----------|
| **Throttle** | Execute immediately, block duplicates | Button clicks, Refresh |
| **Debounce** | Wait for pause, then execute | Search input, Auto-save |
| **AsyncThrottler** | Lock during async execution | Form submit, File upload |
| **Concurrency Control** | 4 modes: Drop, Enqueue, Replace, Keep Latest | Chat, Search, Sync |

### Concurrency Modes (NEW)

| Mode | Behavior | Perfect For |
|------|----------|-------------|
| **Drop** | Ignore new calls while busy | Payment buttons |
| **Enqueue** | Queue and execute sequentially | Chat messages |
| **Replace** | Cancel old, start new | Search queries |
| **Keep Latest** | Run current + latest only | Auto-save drafts |

---

## 📦 Installation

### Gradle (Kotlin DSL)

```kotlin
commonMain.dependencies {
    implementation("io.github.vietnguyentuan2019:kmp-event-limiter:1.0.0")
}
```

### Version Catalog

```toml
[versions]
kmpEventLimiter = "1.0.0"

[libraries]
kmp-event-limiter = { module = "io.github.vietnguyentuan2019:kmp-event-limiter", version.ref = "kmpEventLimiter" }
```

Then in your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kmp.event.limiter)
        }
    }
}
```

---

## 🎯 Usage Examples

### 1. Prevent Button Double-Clicks (Throttle)

**Using Modifier (Recommended):**

```kotlin
Button(
    onClick = {},
    modifier = Modifier.throttleClick(duration = 500.milliseconds) {
        submitOrder()
    }
) {
    Text("Submit Order")
}
```

**Using Direct Controller:**

```kotlin
val scope = rememberCoroutineScope()
val throttler = remember { Throttler(scope) }

Button(onClick = throttler.wrap { submitOrder() }) {
    Text("Submit Order")
}
```

---

### 2. Smart Search Bar (Debounce + Async)

```kotlin
var searchQuery by remember { mutableStateOf("") }
var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
var isSearching by remember { mutableStateOf(false) }

AsyncDebouncedTextField(
    value = searchQuery,
    onValueChange = { searchQuery = it },
    onDebouncedChange = { query ->
        // This is a suspend function called after 500ms pause
        searchApi(query)
    },
    onSuccess = { results ->
        searchResults = results
    },
    onLoadingChanged = { loading ->
        isSearching = loading
    },
    debounceTime = 500.milliseconds,
    trailingIcon = {
        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
        else Icon(Icons.Default.Search, "Search")
    }
)
```

---

### 3. Form Submission with Loading State

```kotlin
AsyncButton(
    onClick = {
        // Suspend function automatically manages loading state
        uploadFile()
    },
    onError = { error ->
        showSnackbar("Upload failed: ${error.message}")
    }
) { isLoading ->
    if (isLoading) {
        Row {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Uploading...")
        }
    } else {
        Text("Upload File")
    }
}
```

---

### 4. Advanced Concurrency Control

#### Chat App: Enqueue Messages (Preserve Order)

```kotlin
val scope = rememberCoroutineScope()
val messageSender = remember {
    ConcurrentAsyncThrottler(
        scope = scope,
        mode = ConcurrencyMode.ENQUEUE
    )
}

ConcurrentAsyncButton(
    onClick = {
        messageSender.call {
            sendMessage(messageText)
        }
    },
    mode = ConcurrencyMode.ENQUEUE
) { isLoading, pendingCount ->
    Text(
        if (pendingCount > 0) "Sending ($pendingCount)..." else "Send"
    )
}
```

#### Search: Replace Old Queries (Only Latest Matters)

```kotlin
val scope = rememberCoroutineScope()
val searchController = remember {
    ConcurrentAsyncThrottler(
        scope = scope,
        mode = ConcurrencyMode.REPLACE
    )
}

TextField(
    value = searchQuery,
    onValueChange = { query ->
        searchQuery = query
        scope.launch {
            searchController.call {
                // Old search calls are cancelled
                val results = searchApi(query)
                searchResults = results
            }
        }
    }
)
```

#### Auto-Save: Keep Latest Version (No Redundant Saves)

```kotlin
val scope = rememberCoroutineScope()
val autoSaver = remember {
    ConcurrentAsyncThrottler(
        scope = scope,
        mode = ConcurrencyMode.KEEP_LATEST
    )
}

TextField(
    value = documentText,
    onValueChange = { text ->
        documentText = text
        scope.launch {
            autoSaver.call {
                saveDraft(text) // Only saves current + final version
            }
        }
    }
)
```

---

## 🎨 API Reference

### Core Controllers

#### Throttler

```kotlin
class Throttler(
    scope: CoroutineScope,
    duration: Duration = 500.milliseconds,
    debugMode: Boolean = false,
    name: String? = null,
    enabled: Boolean = true,
    resetOnError: Boolean = false,
    onMetrics: ((Duration, Boolean) -> Unit)? = null
)
```

**Methods:**
- `call(callback: () -> Unit)` - Execute with throttle
- `wrap(callback: (() -> Unit)?)` - Wrap for onClick handlers
- `reset()` - Reset throttle state
- `dispose()` - Clean up resources

#### AsyncThrottler

```kotlin
class AsyncThrottler(
    scope: CoroutineScope,
    maxDuration: Duration? = 15.seconds,
    // ... same params as Throttler
)
```

**Methods:**
- `suspend fun call(action: suspend () -> Unit)` - Execute with async lock
- `fun isLocked(): Boolean` - Check if currently locked
- `fun dispose()` - Clean up

#### ConcurrentAsyncThrottler

```kotlin
class ConcurrentAsyncThrottler(
    scope: CoroutineScope,
    mode: ConcurrencyMode = ConcurrencyMode.DROP,
    maxDuration: Duration? = null,
    // ...
)
```

**Methods:**
- `suspend fun call(action: suspend () -> Unit)` - Execute with concurrency mode
- `fun pendingCount(): Int` - Get pending operation count
- `fun dispose()` - Clean up

---

### Compose Modifiers

#### throttleClick / throttleClickable

```kotlin
fun Modifier.throttleClick(
    duration: Duration = 500.milliseconds,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier

fun Modifier.throttleClickable(
    duration: Duration = 500.milliseconds,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier // With ripple effect
```

#### asyncThrottleClick / asyncThrottleClickable

```kotlin
fun Modifier.asyncThrottleClick(
    maxDuration: Duration? = null,
    mode: ConcurrencyMode = ConcurrencyMode.DROP,
    enabled: Boolean = true,
    onClick: suspend () -> Unit
): Modifier
```

---

### Compose Components

#### AsyncButton

```kotlin
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
)
```

#### DebouncedTextField

```kotlin
@Composable
fun DebouncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDebouncedChange: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    debounceTime: Duration = 500.milliseconds,
    // ... standard TextField params
)
```

#### AsyncDebouncedTextField

```kotlin
@Composable
fun <T> AsyncDebouncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDebouncedChange: suspend (String) -> T?,
    modifier: Modifier = Modifier,
    onSuccess: ((T) -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
    onLoadingChanged: ((Boolean) -> Unit)? = null,
    // ...
)
```

---

## 📊 Throttle vs Debounce

### Throttle (Anti-Spam)

Fires **immediately**, then blocks for duration.

```
User clicks: ▼     ▼   ▼▼▼       ▼
Executes:    ✓     X   X X       ✓
             |<-500ms->|         |<-500ms->|
```

**Use for:** Button clicks, refresh actions, preventing spam

### Debounce (Wait for Pause)

Waits for **pause** in events, then fires.

```
User types:  a  b  c  d ... (pause) ... e  f  g
Executes:                   ✓                   ✓
             |<--300ms wait-->|     |<--300ms wait-->|
```

**Use for:** Search input, auto-save, slider changes

---

## 🧪 Platform Support

| Platform | Status | Notes |
|----------|--------|-------|
| **Android** | ✅ Full | Min SDK 21 |
| **iOS** | ✅ Full | Xcode 15+ |
| **Desktop** | ✅ Full | Windows, macOS, Linux |
| **Web** | ✅ Full | Kotlin/Wasm |

---

## 🔄 Migration from Flutter

If you're familiar with `flutter_event_limiter`, here's the mapping:

| Flutter | KMP Event Limiter |
|---------|-------------------|
| `ThrottledInkWell` | `Modifier.throttleClick()` |
| `AsyncThrottledCallbackBuilder` | `AsyncButton` |
| `AsyncDebouncedTextController` | `AsyncDebouncedTextField` |
| `ThrottledBuilder` | `ThrottledBuilder` (similar API) |
| `ConcurrentAsyncThrottler` | `ConcurrentAsyncThrottler` (same concept) |

**Key Differences:**
- **Kotlin Coroutines** instead of Dart Futures/Timers
- **Compose Modifiers** instead of Widget wrappers
- **Type-safe suspend functions** instead of callbacks
- **Auto-disposal** via Compose lifecycle

---

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new features
4. Submit a pull request

---

## 📄 License

```
Copyright 2025 Nguyễn Tuấn Việt (vietnguyentuan2019)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 💬 Support

- **Questions:** [GitHub Discussions](https://github.com/vietnguyentuan2019/kmp-event-limiter/discussions)
- **Bugs:** [GitHub Issues](https://github.com/vietnguyentuan2019/kmp-event-limiter/issues)
- **⭐ Star this repo** if you find it useful!

---

<p align="center">
Built with ❤️ for the Kotlin Multiplatform community<br/>
Inspired by <a href="https://pub.dev/packages/flutter_event_limiter">flutter_event_limiter</a>
</p>
