package com.papernotes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.papernotes.domain.model.Note
import com.papernotes.domain.toShareBody
import java.io.File

/**
 * Rendert eine Notiz als hübsche „Papierkarte" (PNG) zum Teilen – unabhängig von Compose,
 * deterministisch über einen Android-[Canvas]. Gibt eine FileProvider-[Uri] auf die Datei
 * in `cacheDir/shared/` zurück (oder null bei Fehler).
 */
object ShareCardRenderer {

    private const val WIDTH = 1080
    private const val MARGIN = 48f          // Rand des Bildes
    private const val PAD = 64f             // Innenabstand der Karte
    private const val RADIUS = 44f

    fun render(
        context: Context,
        note: Note,
        surfaceArgb: Int,
        inkArgb: Int,
        accentArgb: Int,
    ): Uri? = runCatching {
        val contentWidth = (WIDTH - 2 * MARGIN - 2 * PAD).toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkArgb
            textSize = 58f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(inkArgb, 0.86f)
            textSize = 42f
            typeface = Typeface.SANS_SERIF
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(inkArgb, 0.45f)
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        val title = note.title.trim()
        val body = note.toShareBody().trim()

        val titleLayout = title.takeIf { it.isNotEmpty() }
            ?.let { staticLayout(it, titlePaint, contentWidth) }
        val bodyLayout = body.takeIf { it.isNotEmpty() }
            ?.let { staticLayout(it, bodyPaint, contentWidth) }
        val footerLayout = staticLayout("✿ PaperNotes", footerPaint, contentWidth)

        val gap = 28f
        val cardTop = MARGIN
        var y = cardTop + PAD
        val titleH = titleLayout?.height ?: 0
        val bodyH = bodyLayout?.height ?: 0
        val footerH = footerLayout.height
        val cardContentH = titleH +
            (if (titleLayout != null && bodyLayout != null) gap.toInt() else 0) +
            bodyH + (gap * 1.4f).toInt() + footerH
        val cardHeight = PAD * 2 + cardContentH
        val totalHeight = (cardTop + cardHeight + MARGIN).toInt()

        val bitmap = Bitmap.createBitmap(WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Hintergrund: gedämpfte Variante der Kartenfarbe, damit die Karte „aufliegt".
        canvas.drawColor(darken(surfaceArgb, 0.90f))

        // Karte mit weichem Schatten.
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = surfaceArgb
            setShadowLayer(26f, 0f, 12f, withAlpha(0xFF000000.toInt(), 0.22f))
        }
        val cardLeft = MARGIN
        val cardRight = WIDTH - MARGIN
        val cardBottom = cardTop + cardHeight
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, RADIUS, RADIUS, cardPaint)

        // Eselsohr oben rechts in Akzentfarbe.
        val ear = 70f
        val earPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb }
        val earPath = Path().apply {
            moveTo(cardRight - ear, cardTop)
            lineTo(cardRight, cardTop)
            lineTo(cardRight, cardTop + ear)
            close()
        }
        canvas.drawPath(earPath, earPaint)

        val x = cardLeft + PAD
        titleLayout?.let {
            canvas.save(); canvas.translate(x, y); it.draw(canvas); canvas.restore()
            y += it.height + gap
        }
        bodyLayout?.let {
            canvas.save(); canvas.translate(x, y); it.draw(canvas); canvas.restore()
            y += it.height
        }
        // Footer unten in der Karte.
        canvas.save()
        canvas.translate(x, cardBottom - PAD - footerH)
        footerLayout.draw(canvas)
        canvas.restore()

        writeToCache(context, bitmap, note.id)
    }.getOrNull()

    private fun staticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1f)
            .setIncludePad(false)
            .build()

    private fun writeToCache(context: Context, bitmap: Bitmap, noteId: Long): Uri {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "note_$noteId.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun withAlpha(argb: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (a shl 24) or (argb and 0x00FFFFFF)
    }

    private fun darken(argb: Int, factor: Float): Int {
        val r = ((argb shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((argb shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((argb and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
