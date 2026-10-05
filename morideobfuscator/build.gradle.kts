plugins {
    id("com.android.library")
}

android {
    namespace = "moe.rukamori.archivetune.morideobfuscator"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.okhttp)
    implementation(libs.quickjs.kt)
}
