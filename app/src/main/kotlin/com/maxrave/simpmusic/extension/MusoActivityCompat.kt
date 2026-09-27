package com.maxrave.simpmusic.extension

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Muso port: unwraps a Compose context down to the hosting Activity. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.getActivityOrNull(): Activity? = findActivity()
