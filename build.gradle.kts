plugins {
    alias(libs.plugins.hilt) apply (false)
    alias(libs.plugins.kotlin.ksp) apply (false)
    alias(libs.plugins.compose.compiler) apply false
}

buildscript {
    val isFullBuild by extra {
        gradle.startParameter.taskNames.none { task -> task.contains("foss", ignoreCase = true) }
    }

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.gradle)
        classpath(kotlin("gradle-plugin", libs.versions.kotlin.get()))
        if (isFullBuild) {
            classpath(libs.google.services)
            classpath(libs.firebase.crashlytics.plugin)
            classpath(libs.firebase.perf.plugin)
        }
    }
}

// =============================================================================
// Ktor version alignment (Round 171 - the startup crash-loop root cause fix)
// =============================================================================
// The kit's translator dependency (com.github.therealbush:translator, jitpack)
// transitively pulls io.ktor:ktor-client-core:3.0.1 and ktor-client-cio:3.0.1.
// Gradle's conflict resolution picks the NEWEST version, so ktor-client-core
// resolved to 3.0.1 while the okhttp engine stayed at muso's pinned 2.3.12.
// Ktor 3.0 REMOVED io.ktor.client.plugins.HttpTimeout (renamed to
// HttpTimeoutConfig/HttpTimeoutCapability), so the 2.3.12 OkHttpEngine could
// not find the class at runtime -> NoClassDefFoundError in App.onCreate (the
// Updater object builds its HttpClient on first class access) -> the app
// crash-looped and never opened (v0.5.182-187 on device).
// Forcing EVERY io.ktor artifact to 2.3.12 keeps the whole graph consistent.
allprojects {
    configurations.all {
        resolutionStrategy {
            force(
                "io.ktor:ktor-client-core:2.3.12",
                "io.ktor:ktor-client-okhttp:2.3.12",
                "io.ktor:ktor-client-cio:2.3.12",
                "io.ktor:ktor-client-websockets:2.3.12",
                "io.ktor:ktor-client-content-negotiation:2.3.12",
                "io.ktor:ktor-client-encoding:2.3.12",
                "io.ktor:ktor-serialization-kotlinx-json:2.3.12",
                "io.ktor:ktor-server-core:2.3.12",
                "io.ktor:ktor-server-cio:2.3.12",
                "io.ktor:ktor-server-websockets:2.3.12",
                "io.ktor:ktor-server-content-negotiation:2.3.12",
                "io.ktor:ktor-http:2.3.12",
                "io.ktor:ktor-http-cio:2.3.12",
                "io.ktor:ktor-utils:2.3.12",
                "io.ktor:ktor-io:2.3.12",
                "io.ktor:ktor-events:2.3.12",
                "io.ktor:ktor-network:2.3.12",
                "io.ktor:ktor-network-tls:2.3.12",
                "io.ktor:ktor-serialization:2.3.12",
                "io.ktor:ktor-websocket-serialization:2.3.12",
                "io.ktor:ktor-websockets:2.3.12",
            )
        }
    }
}

tasks.register<Delete>("Clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        // Task-level compilerOptions: works identically whether a module's
        // Kotlin comes from the KGP plugin (JVM modules) or from AGP 9's
        // built-in Kotlin support (:app), which applies Kotlin for you and
        // rejects the org.jetbrains.kotlin.android plugin id.
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            if (project.name == "app") {
                // Compose-ui is only on :app's classpath; a global opt-in
                // makes every JVM module warn about an unresolved marker.
                freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
            }
            if (project.findProperty("enableComposeCompilerReports") == "true") {
                arrayOf("reports", "metrics").forEach {
                    freeCompilerArgs.add("-P")
                    freeCompilerArgs.add(
                        "plugin:androidx.compose.compiler.plugins.kotlin:${it}Destination=${project.layout.buildDirectory.get().asFile.absolutePath}/compose_metrics"
                    )
                }
            }
        }
    }
}
