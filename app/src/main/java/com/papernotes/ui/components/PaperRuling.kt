package com.papernotes.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.PaperStyle

/**
 * Zeichnet die Papier-Liniierung zart hinter den Inhalt – wie echtes Notizpapier.
 * [BLANK][PaperStyle.BLANK] zeichnet nichts. [color] kommt üblicherweise aus
 * `onSurface`, passt sich also Hell/Dunkel an.
 */
fun Modifier.paperRuling(
    style: PaperStyle,
    color: Color,
    spacing: Dp = 26.dp,
): Modifier {
    if (style == PaperStyle.BLANK) return this
    return drawBehind {
        val gap = spacing.toPx()
        if (gap <= 0f) return@drawBehind
        val line = color.copy(alpha = 0.14f)
        val stroke = 1f

        when (style) {
            PaperStyle.LINED -> {
                var y = gap
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
                    y += gap
                }
            }
            PaperStyle.GRID -> {
                var y = gap
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
                    y += gap
                }
                var x = gap
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), stroke)
                    x += gap
                }
            }
            PaperStyle.DOTTED -> {
                val dot = color.copy(alpha = 0.22f)
                val radius = 1.4f
                var y = gap
                while (y < size.height) {
                    var x = gap
                    while (x < size.width) {
                        drawCircle(dot, radius, Offset(x, y))
                        x += gap
                    }
                    y += gap
                }
            }
            PaperStyle.BLANK -> Unit
        }
    }
}
