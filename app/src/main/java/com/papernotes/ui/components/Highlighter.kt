package com.papernotes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.papernotes.domain.Highlight
import com.papernotes.ui.theme.Lavender
import com.papernotes.ui.theme.SageGreen
import com.papernotes.ui.theme.SoftPeach
import com.papernotes.ui.theme.SunnyYellow

/** Textmarker-Palette (Index = `Highlight.color`). */
val HighlightColors = listOf(SunnyYellow, SageGreen, SoftPeach, Lavender)

/** Marker-Farbe mit moderater Deckkraft, damit die Tinte lesbar bleibt. */
fun highlightColor(index: Int): Color =
    HighlightColors.getOrElse(index) { HighlightColors.first() }.copy(alpha = 0.45f)

/** Baut aus [text] + [ranges] eine AnnotatedString mit farbigem Hintergrund je Markierung. */
fun buildHighlightedString(text: String, ranges: List<Highlight>): AnnotatedString =
    buildAnnotatedString {
        append(text)
        ranges.forEach { h ->
            val start = h.start.coerceIn(0, text.length)
            val end = h.end.coerceIn(0, text.length)
            if (end > start) addStyle(SpanStyle(background = highlightColor(h.color)), start, end)
        }
    }

/**
 * Legt die Marker-Hintergründe über den **unveränderten** Editor-Text – daher
 * [OffsetMapping.Identity] (Cursor/Auswahl bleiben exakt erhalten).
 */
fun highlightTransformation(ranges: List<Highlight>): VisualTransformation =
    VisualTransformation { text ->
        TransformedText(buildHighlightedString(text.text, ranges), OffsetMapping.Identity)
    }

/** TextRange-Selektion robust normalisiert (start ≤ end). */
fun TextRange.ordered(): Pair<Int, Int> = minOf(start, end) to maxOf(start, end)
