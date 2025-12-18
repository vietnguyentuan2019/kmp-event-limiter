plugins {
    // Kotlin Multiplatform
    kotlin("multiplatform") version "2.2.21" apply false
    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.8.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    // Android
    id("com.android.library") version "8.6.0" apply false
    id("com.android.application") version "8.6.0" apply false
}

allprojects {
    group = "io.github.vietnguyentuan2019"
    version = "1.0.0"
}
