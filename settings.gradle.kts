rootProject.name = "kmp-event-limiter"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":event-limiter")
include(":composeApp")
include(":androidApp")
