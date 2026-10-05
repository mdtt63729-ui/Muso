@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "Muso"
include(":app")
include(":betterlyrics")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":material-color-utilities")
include(":kizzy")
include(":paxsenixlyrics")
include(":simpmusic")
include(":youlyplus")
include(":unison")

include(":core")
include(":lyrics:kugou")
include(":lyrics:lrclib")
include(":lyrics:simpmusic")
include(":lyrics:paxsenix")
include(":lyrics:betterlyrics")
include(":lyrics:unison")
include(":lyrics:youlyplus")
include(":lastfm")
include(":canvas")
include(":spotifycore")
include(":morideobfuscator")
