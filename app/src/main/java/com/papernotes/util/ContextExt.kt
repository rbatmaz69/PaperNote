package com.papernotes.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Findet die umgebende [Activity] eines Compose-[Context] (für Window-Insets-Steuerung). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
