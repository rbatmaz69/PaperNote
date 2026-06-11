package com.papernotes.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Verwaltet die an Notizen angehängten Fotos im App-internen Speicher
 * (`filesDir/photos/`). Bilder werden beim Speichern heruntergerechnet und als JPEG abgelegt –
 * die Notiz merkt sich nur den Dateinamen. Keine externe Bibliothek, keine Berechtigung.
 */
object PhotoStore {

    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 85

    private fun dir(context: Context): File =
        File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun delete(context: Context, name: String) {
        runCatching { file(context, name).delete() }
    }

    /**
     * Kopiert das Bild aus [uri] herunterskaliert in den App-Speicher und gibt den neuen
     * Dateinamen zurück (oder null bei Fehler). Läuft auf [Dispatchers.IO].
     */
    suspend fun save(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 1) Maße ermitteln, um den Verkleinerungsfaktor zu bestimmen.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sample = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)

            // 2) Verkleinert dekodieren.
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@runCatching null

            // 3) Als JPEG ablegen.
            val name = "${UUID.randomUUID()}.jpg"
            file(context, name).outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            bitmap.recycle()
            name
        }.getOrNull()
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxEdge && h / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }
}
