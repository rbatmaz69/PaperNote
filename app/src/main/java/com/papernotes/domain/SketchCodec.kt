package com.papernotes.domain

/**
 * Ein Punkt einer Tinten-Skizze, auf die Zeichen-**Breite** normiert: `x` liegt in 0..1,
 * `y` im selben Maßstab (also 0..Höhe/Breite) – dadurch bleibt das Seitenverhältnis erhalten,
 * egal wie breit die Zeichen-/Vorschaufläche ist.
 */
data class SketchPoint(val x: Float, val y: Float)

/**
 * Ein einzelner Strich der Skizze: Punktfolge plus optionale [color] (RGB-Int, `null` = Legacy →
 * Stimmungsfarbe) und [width] (Basisbreite in dp, `null` = Standardbreite).
 */
data class SketchStroke(
    val points: List<SketchPoint>,
    val color: Int? = null,
    val width: Float? = null,
)

/**
 * Kodiert eine Skizze (Liste von Strichen) im `body`-Feld einer Notiz. Pro Strich optional ein
 * Kopf `meta|punkte`: `meta = "rrggbb"` (+ optional `,width`), Punkte als `x,y` per Leerzeichen,
 * Striche per `;`. Fehlt der `|`-Kopf, ist es ein **Legacy-Strich** (ohne Farbe/Breite).
 *
 * Wie [ChecklistCodec] / [StampCodec] braucht es dadurch keine zweite Room-Tabelle; Suche,
 * Vorschau, Archiv und Papierkorb funktionieren für Skizzen identisch zu anderen Notizen.
 */
object SketchCodec {

    fun parse(body: String): List<SketchStroke> =
        body.split(';')
            .mapNotNull { raw ->
                val stroke = raw.trim()
                if (stroke.isEmpty()) return@mapNotNull null

                val sep = stroke.indexOf('|')
                val meta = if (sep >= 0) stroke.substring(0, sep) else ""
                val pointsPart = if (sep >= 0) stroke.substring(sep + 1) else stroke

                var color: Int? = null
                var width: Float? = null
                if (meta.isNotEmpty()) {
                    val fields = meta.split(',')
                    color = fields.getOrNull(0)?.toIntOrNull(16)
                    width = fields.getOrNull(1)?.toFloatOrNull()
                }

                val points = pointsPart.trim().split(' ')
                    .mapNotNull { token ->
                        val parts = token.split(',')
                        if (parts.size != 2) return@mapNotNull null
                        val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
                        val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
                        SketchPoint(x, y)
                    }
                if (points.isEmpty()) null else SketchStroke(points, color, width)
            }

    fun serialize(strokes: List<SketchStroke>): String =
        strokes
            .filter { it.points.isNotEmpty() }
            .joinToString(";") { stroke ->
                val pts = stroke.points.joinToString(" ") { p -> "${fmt(p.x)},${fmt(p.y)}" }
                if (stroke.color == null && stroke.width == null) {
                    pts // Legacy-kompatibel, wenn nichts zu speichern ist
                } else {
                    val hex = "%06x".format((stroke.color ?: 0) and 0xFFFFFF)
                    val meta = if (stroke.width != null) "$hex,${fmt(stroke.width)}" else hex
                    "$meta|$pts"
                }
            }

    /** Kompakt runden (3 Nachkommastellen genügen für die normierte Auflösung). */
    private fun fmt(v: Float): String = ((v * 1000f).toInt() / 1000f).toString()
}
