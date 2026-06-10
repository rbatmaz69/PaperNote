package com.papernotes.domain

/**
 * Ein Punkt einer Tinten-Skizze, auf die Zeichen-**Breite** normiert: `x` liegt in 0..1,
 * `y` im selben Maßstab (also 0..Höhe/Breite) – dadurch bleibt das Seitenverhältnis erhalten,
 * egal wie breit die Zeichen-/Vorschaufläche ist.
 */
data class SketchPoint(val x: Float, val y: Float)

/**
 * Kodiert eine Skizze (Liste von Strichen, jeder Strich eine Punktfolge) im `body`-Feld einer
 * Notiz: Striche per `;`, Punkte per Leerzeichen, Koordinaten als `x,y`.
 *
 * Wie [ChecklistCodec] / [StampCodec] braucht es dadurch keine zweite Room-Tabelle; Suche,
 * Vorschau, Archiv und Papierkorb funktionieren für Skizzen identisch zu anderen Notizen.
 */
object SketchCodec {

    fun parse(body: String): List<List<SketchPoint>> =
        body.split(';')
            .mapNotNull { stroke ->
                val points = stroke.trim().split(' ')
                    .mapNotNull { token ->
                        val parts = token.split(',')
                        if (parts.size != 2) return@mapNotNull null
                        val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
                        val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
                        SketchPoint(x, y)
                    }
                points.ifEmpty { null }
            }

    fun serialize(strokes: List<List<SketchPoint>>): String =
        strokes
            .filter { it.isNotEmpty() }
            .joinToString(";") { stroke ->
                stroke.joinToString(" ") { p -> "${fmt(p.x)},${fmt(p.y)}" }
            }

    /** Kompakt runden (3 Nachkommastellen genügen für die normierte Auflösung). */
    private fun fmt(v: Float): String = ((v * 1000f).toInt() / 1000f).toString()
}
