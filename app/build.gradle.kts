@file:Suppress("UnstableApiUsage")

val isFullBuild: Boolean by rootProject.extra

plugins {
    id("com.android.application")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

if (isFullBuild && System.getenv("PULL_REQUEST") == null) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
}

android {
    namespace = "com.muso.music"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.muso.music"
        minSdk = 24
        targetSdk = 35
        versionCode = 198
        versionName = "0.5.191"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("full") {
            dimension = "version"
        }
        create("foss") {
            dimension = "version"
        }
    }

//    splits {
//        abi {
//            isEnable = true
//            reset()
//            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
//            isUniversalApk = false
//        }
//    }
    
    signingConfigs {
        getByName("debug") {
            if (System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD") != null) {
                storeFile = file(System.getenv("MUSIC_DEBUG_KEYSTORE_FILE"))
                storePassword = System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD")
                keyAlias = "debug"
                keyPassword = System.getenv("MUSIC_DEBUG_SIGNING_KEY_PASSWORD")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            // kuromoji-core and kuromoji-ipadic both bundle these
            // (DuplicateRelativeFileException at mergeFossReleaseJavaResource).
            excludes += setOf(
                "META-INF/CONTRIBUTORS.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md",
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
    // avoid DEPENDENCY_INFO_BLOCK for IzzyOnDroid
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    lint {
        lintConfig = file("lint.xml")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":lyrics:kugou"))
    implementation(project(":lyrics:lrclib"))
    implementation(project(":lyrics:simpmusic"))
    implementation(project(":lyrics:paxsenix"))
    implementation(project(":lyrics:betterlyrics"))
    implementation(project(":lyrics:unison"))
    implementation(project(":lyrics:youlyplus"))
    implementation(project(":lastfm"))
    implementation(project(":canvas"))
    implementation(project(":spotifycore"))
    implementation(project(":morideobfuscator"))
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    // ArchiveTune settings kit (Phase 1)
    implementation(libs.translator)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.html)
    implementation(libs.markwon.image)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.simple.ext)
    implementation(libs.accompanist.lyrics.ui)
    implementation(libs.okhttp)
    implementation("androidx.glance:glance:1.1.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation(libs.car.app)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.webkit)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.accompanist.lyrics.core)
    implementation(libs.androidsvg)
    implementation(libs.datastore)
    implementation(libs.work.runtime)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)

    // ===== SimpMusic player suite =====
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.kmpalette.core)
    implementation(libs.materialkolor)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.koin.android)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.paging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.palette)
    implementation(projects.materialColorUtilities)
    implementation(libs.squigglyslider)

    implementation(libs.coil)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.ui.compose)
    implementation(libs.haze)
    implementation(libs.haze.blur)

    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.kizzy)
    implementation(projects.betterlyrics)
    implementation(projects.paxsenixlyrics)
    implementation(projects.simpmusic)
    implementation(projects.youlyplus)
    implementation(projects.unison)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.lifecycle.process)
    implementation("androidx.compose.material3.adaptive:adaptive:1.3.0-rc01")

    coreLibraryDesugaring(libs.desugaring)

    "fullImplementation"(platform(libs.firebase.bom))
    "fullImplementation"(libs.firebase.analytics)
    "fullImplementation"(libs.firebase.crashlytics)
    "fullImplementation"(libs.firebase.config)
    "fullImplementation"(libs.firebase.perf)
    "fullImplementation"(libs.mlkit.language.id)
    "fullImplementation"(libs.mlkit.translate)
    "fullImplementation"(libs.opencc4j)

    implementation(libs.timber)
}

// ArchiveTune kit sources rely on these opt-ins (AT's :app sets the same list).
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        optIn.add("androidx.compose.foundation.layout.ExperimentalLayoutApi")
        optIn.add("androidx.compose.material.ExperimentalMaterialApi")
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
        )
    }
}
