package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Ein Stück halbtransparentes Washi-Tape, leicht schief "aufgeklebt" — markiert
 * angeheftete Notizen. Gerissene Kanten links/rechts, zarte Querstreifen.
 */
@Composable
fun WashiTape(
    color: Color,
    modifier: Modifier = Modifier,
    rotationDegrees: Float = -4f,
) {
    Canvas(
        modifier = modifier
            .size(width = 64.dp, height = 22.dp)
            .graphicsLayer { rotationZ = rotationDegrees },
    ) {
        val w = size.width
        val h = size.height
        val tearDepth = w * 0.035f

        // Tape-Körper mit gerissenen (gezackten) Schmalseiten
        val tape = Path().apply {
            moveTo(tearDepth, 0f)
            lineTo(w - tearDepth * 0.5f, 0f)
            lineTo(w - tearDepth * 1.6f, h * 0.3f)
            lineTo(w, h * 0.55f)
            lineTo(w - tearDepth, h)
            lineTo(tearDepth * 0.6f, h)
            lineTo(tearDepth * 1.8f, h * 0.65f)
            lineTo(0f, h * 0.35f)
            close()
        }
        drawPath(tape, color = color.copy(alpha = 0.55f))

        // zarte Querstreifen für die Papier-Anmutung
        val stripe = Color.White.copy(alpha = 0.18f)
        var x = w * 0.18f
        while (x < w * 0.95f) {
            drawRect(
                color = stripe,
                topLeft = Offset(x, h * 0.12f),
                size = Size(w * 0.04f, h * 0.76f),
            )
            x += w * 0.16f
        }
    }
}
