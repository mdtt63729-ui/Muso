package com.maxrave.simpmusic

/**
 * Android build of the reference's expect/actual platform declaration: Muso
 * runs only on Android, so the expect collapses to a plain function.
 */
sealed class Platform {
    object Android : Platform()
    object iOS : Platform()
    object Desktop : Platform()

    fun osName(): String = when (this) {
        Android -> "android"
        iOS -> "iOS"
        Desktop -> System.getProperty("os.name") ?: "jvm"
    }
}

fun getPlatform(): Platform = Platform.Android
