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
            freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
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
