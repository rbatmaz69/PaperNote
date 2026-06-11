package com.papernotes.domain

/** Eine farbige Textmarker-Markierung über dem Zeichenbereich [start, end). [color] = Index in der Palette. */
data class Highlight(val start: Int, val end: Int, val color: Int)

/**
 * Kodiert Textmarker-Markierungen für das `highlights`-Feld einer Notiz als
 * `start-end-color`-Tripel, kommagetrennt – z. B. `"3-9-0,15-20-2"`.
 *
 * Bewusst getrennt vom `body`-Text gehalten, damit Teilen/Kopieren sauberen Text liefert
 * und die Markierungen beim Editieren mitverschoben werden können.
 */
object HighlightCodec {

    fun parse(s: String): List<Highlight> =
        if (s.isBlank()) {
            emptyList()
        } else {
            s.split(',').mapNotNull { token ->
                val parts = token.split('-')
                if (parts.size != 3) return@mapNotNull null
                val start = parts[0].toIntOrNull() ?: return@mapNotNull null
                val end = parts[1].toIntOrNull() ?: return@mapNotNull null
                val color = parts[2].toIntOrNull() ?: return@mapNotNull null
                if (end > start) Highlight(start, end, color) else null
            }
        }

    fun serialize(list: List<Highlight>): String =
        list.filter { it.end > it.start }
            .sortedBy { it.start }
            .joinToString(",") { "${it.start}-${it.end}-${it.color}" }
}
