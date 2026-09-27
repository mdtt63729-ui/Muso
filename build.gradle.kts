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
        // KGP 2.2: the legacy Kotlin compiler DSL is a hard error now —
        // migrated to the task-level compilerOptions DSL.
        compilerOptions {
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
