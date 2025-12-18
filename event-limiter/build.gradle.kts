plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "io.github.vietnguyentuan2019.eventlimiter"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "EventLimiter"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
                implementation("app.cash.turbine:turbine:1.1.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.15.0")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            withType<MavenPublication> {
                groupId = "io.github.vietnguyentuan2019"
                artifactId = "kmp-event-limiter"
                version = "1.0.0"

                pom {
                    name.set("KMP Event Limiter")
                    description.set("Production-ready throttle and debounce for Kotlin Multiplatform & Compose Multiplatform")
                    url.set("https://github.com/vietnguyentuan2019/kmp-event-limiter")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("vietnguyentuan2019")
                            name.set("Nguyễn Tuấn Việt")
                            email.set("vietnguyentuan@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/vietnguyentuan2019/kmp-event-limiter.git")
                        developerConnection.set("scm:git:ssh://github.com/vietnguyentuan2019/kmp-event-limiter.git")
                        url.set("https://github.com/vietnguyentuan2019/kmp-event-limiter")
                    }
                }
            }
        }
    }
}
