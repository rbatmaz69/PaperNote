package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.papernotes.domain.SketchPoint

/**
 * Interaktive Zeichenfläche für Tinten-Skizzen. Bereits gespeicherte [strokes] (auf die Breite
 * normiert) werden gezeichnet; der laufende Strich wird live gerendert und beim Loslassen
 * normiert via [onStrokeFinished] gemeldet. [inkColor] ist die Tintenfarbe (i. d. R. Stimmung).
 */
@Composable
fun SketchCanvas(
    strokes: List<List<SketchPoint>>,
    inkColor: Color,
    onStrokeFinished: (List<SketchPoint>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val finished by rememberUpdatedState(onStrokeFinished)
    // Laufender Strich in Roh-Pixeln (für flüssiges Live-Rendering).
    val current = remember { mutableStateListOf<Offset>() }
    var canvasW by remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasW = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    current.clear()
                    current.add(down.position)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        current.add(change.position)
                        change.consume()
                    }
                    val w = canvasW
                    val normalized = current.map { SketchPoint(it.x / w, it.y / w) }
                    if (normalized.isNotEmpty()) finished(normalized)
                    current.clear()
                }
            },
    ) {
        val w = size.width
        strokes.forEach { stroke ->
            drawStroke(stroke.map { Offset(it.x * w, it.y * w) }, inkColor)
        }
        if (current.isNotEmpty()) drawStroke(current.toList(), inkColor)
    }
}

/**
 * Read-only-Miniatur einer Skizze: die normierten [strokes] werden in die Box eingepasst
 * (Bounding-Box zentriert skaliert), bleiben also unverzerrt und vollständig sichtbar.
 */
@Composable
fun SketchView(
    strokes: List<List<SketchPoint>>,
    inkColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val points = strokes.flatten()
        if (points.isEmpty()) return@Canvas

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val boxW = (maxX - minX).coerceAtLeast(1e-3f)
        val boxH = (maxY - minY).coerceAtLeast(1e-3f)

        val pad = size.minDimension * 0.08f
        val availW = (size.width - 2f * pad).coerceAtLeast(1f)
        val availH = (size.height - 2f * pad).coerceAtLeast(1f)
        val scale = minOf(availW / boxW, availH / boxH)
        val offX = (size.width - boxW * scale) / 2f
        val offY = (size.height - boxH * scale) / 2f

        strokes.forEach { stroke ->
            drawStroke(
                stroke.map { Offset(offX + (it.x - minX) * scale, offY + (it.y - minY) * scale) },
                inkColor,
            )
        }
    }
}

/**
 * Zeichnet einen Strich (Pixel-Punkte) Segment für Segment. Die Strichbreite variiert leicht
 * mit dem Tempo: schnellere Züge (größerer Punktabstand) werden dünner – wie ein Federhalter.
 */
private fun DrawScope.drawStroke(pointsPx: List<Offset>, color: Color) {
    if (pointsPx.isEmpty()) return
    if (pointsPx.size == 1) {
        drawCircle(color = color, radius = 2.dp.toPx(), center = pointsPx[0])
        return
    }
    val base = 4.dp.toPx()
    val minW = 2f.dp.toPx()
    val maxW = 5.5f.dp.toPx()
    for (i in 1 until pointsPx.size) {
        val a = pointsPx[i - 1]
        val b = pointsPx[i]
        val speed = (b - a).getDistance()
        val width = (base - speed * 0.05f).coerceIn(minW, maxW)
        drawLine(color = color, start = a, end = b, strokeWidth = width, cap = StrokeCap.Round)
    }
}
