package com.papernotes.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/** Findet die umgebende [Activity] eines Compose-[Context] (für Window-Insets-Steuerung). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Öffnet die Android-Teilen-Auswahl für Klartext (z.B. nachdem die Notiz als
 * Papierflieger weggeflogen ist). Wird leerer Text übergeben, passiert nichts.
 */
fun Context.sharePlainText(text: String) {
    if (text.isBlank()) return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, "Notiz teilen"))
}
