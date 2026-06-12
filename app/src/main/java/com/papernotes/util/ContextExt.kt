package com.papernotes.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri

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

/**
 * Öffnet die Teilen-Auswahl für ein gerendertes Karten-Bild (PNG via FileProvider).
 * [text] wird als Begleittext mitgegeben, damit Apps ohne Bild-Support den Inhalt zeigen.
 */
fun Context.shareImage(uri: Uri, text: String = "") {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, "Notiz teilen"))
}
