plugins {
    // Kotlin Multiplatform
    kotlin("multiplatform") version "2.1.0" apply false
    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    // Android
    id("com.android.library") version "8.7.3" apply false
    // Publishing
    id("maven-publish") apply false
}

allprojects {
    group = "io.github.vietnguyentuan2019"
    version = "1.0.0"
}
