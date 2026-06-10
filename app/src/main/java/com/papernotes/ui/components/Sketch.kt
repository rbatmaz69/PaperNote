package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.papernotes.domain.SketchPoint
import com.papernotes.domain.SketchStroke

/** Feste Tinten-Palette (Papier-Töne) für die Skizze. */
val INK_PALETTE = listOf(
    Color(0xFF2B2B33), // Tinten-Schwarz
    Color(0xFF6B4A2B), // Sepia
    Color(0xFFB3402F), // Rot (roter Faden)
    Color(0xFF2E5E8C), // Blau
    Color(0xFF4F7A3A), // Grün
    Color(0xFF6A4C93), // Violett
)

const val PEN_THIN = 2.5f
const val PEN_MEDIUM = 4f
const val PEN_THICK = 6.5f
private const val DEFAULT_PEN = PEN_MEDIUM

/**
 * Interaktive Zeichenfläche für Tinten-Skizzen. Bereits gespeicherte [strokes] (auf die Breite
 * normiert, je mit eigener Farbe/Breite) werden gezeichnet; im Zeichen-Modus wird der laufende
 * Strich live gerendert und beim Loslassen via [onStrokeFinished] gemeldet. Im [eraseMode]
 * löscht Drüberwischen die berührten ganzen Striche ([onErase] mit den Indizes).
 */
@Composable
fun SketchCanvas(
    strokes: List<SketchStroke>,
    penColor: Color,
    penWidthDp: Float,
    eraseMode: Boolean,
    defaultColor: Color,
    onStrokeFinished: (SketchStroke) -> Unit,
    onErase: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val finished by rememberUpdatedState(onStrokeFinished)
    val erase by rememberUpdatedState(onErase)
    val strokesState by rememberUpdatedState(strokes)
    val current = remember { mutableStateListOf<Offset>() }
    val liveErased = remember { mutableStateListOf<Int>() }
    var canvasW by remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasW = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(eraseMode) {
                if (!eraseMode) {
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
                        if (normalized.isNotEmpty()) {
                            finished(SketchStroke(normalized, penColor.toRgbInt(), penWidthDp))
                        }
                        current.clear()
                    }
                } else {
                    val threshold = 14.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        liveErased.clear()
                        hitErase(strokesState, down.position, canvasW, threshold, liveErased)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) break
                            hitErase(strokesState, change.position, canvasW, threshold, liveErased)
                            change.consume()
                        }
                        if (liveErased.isNotEmpty()) erase(liveErased.toSet())
                        liveErased.clear()
                    }
                }
            },
    ) {
        val w = size.width
        strokes.forEachIndexed { i, stroke ->
            if (i in liveErased) return@forEachIndexed
            val color = stroke.color?.let { rgbColor(it) } ?: defaultColor
            val base = (stroke.width ?: DEFAULT_PEN).dp.toPx()
            drawStroke(stroke.points.map { Offset(it.x * w, it.y * w) }, color, base)
        }
        if (current.isNotEmpty()) {
            drawStroke(current.toList(), penColor, penWidthDp.dp.toPx())
        }
    }
}

/**
 * Read-only-Miniatur einer Skizze: die normierten [strokes] werden in die Box eingepasst
 * (Bounding-Box zentriert skaliert), je mit eigener Farbe (Fallback [inkColor] = Stimmung).
 */
@Composable
fun SketchView(
    strokes: List<SketchStroke>,
    inkColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val points = strokes.flatMap { it.points }
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
        val thumbWidth = 2.5.dp.toPx()

        strokes.forEach { stroke ->
            val color = stroke.color?.let { rgbColor(it) } ?: inkColor
            drawStroke(
                stroke.points.map { Offset(offX + (it.x - minX) * scale, offY + (it.y - minY) * scale) },
                color,
                thumbWidth,
            )
        }
    }
}

/**
 * Zeichnet einen Strich (Pixel-Punkte) Segment für Segment in [color]. Die Strichbreite variiert
 * leicht um [baseWidthPx]: schnellere Züge (größerer Punktabstand) werden dünner – wie ein Federhalter.
 */
private fun DrawScope.drawStroke(pointsPx: List<Offset>, color: Color, baseWidthPx: Float) {
    if (pointsPx.isEmpty()) return
    if (pointsPx.size == 1) {
        drawCircle(color = color, radius = baseWidthPx / 2f, center = pointsPx[0])
        return
    }
    val minW = baseWidthPx * 0.6f
    val maxW = baseWidthPx * 1.3f
    for (i in 1 until pointsPx.size) {
        val a = pointsPx[i - 1]
        val b = pointsPx[i]
        val speed = (b - a).getDistance()
        val width = (baseWidthPx - speed * 0.04f).coerceIn(minW, maxW)
        drawLine(color = color, start = a, end = b, strokeWidth = width, cap = StrokeCap.Round)
    }
}

/** Markiert alle Striche, die [pos] (px) berühren (Distanz < [threshold]), in [out]. */
private fun hitErase(
    strokes: List<SketchStroke>,
    pos: Offset,
    width: Float,
    threshold: Float,
    out: MutableList<Int>,
) {
    strokes.forEachIndexed { i, stroke ->
        if (i in out) return@forEachIndexed
        val pts = stroke.points
        if (pts.size == 1) {
            if ((pos - Offset(pts[0].x * width, pts[0].y * width)).getDistance() < threshold) out.add(i)
            return@forEachIndexed
        }
        for (s in 1 until pts.size) {
            val a = Offset(pts[s - 1].x * width, pts[s - 1].y * width)
            val b = Offset(pts[s].x * width, pts[s].y * width)
            if (distanceToSegment(pos, a, b) < threshold) {
                out.add(i)
                break
            }
        }
    }
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val len2 = abx * abx + aby * aby
    if (len2 == 0f) return (p - a).getDistance()
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / len2).coerceIn(0f, 1f)
    val proj = Offset(a.x + abx * t, a.y + aby * t)
    return (p - proj).getDistance()
}

private fun rgbColor(rgb: Int): Color = Color(0xFF000000.toInt() or (rgb and 0xFFFFFF))

private fun Color.toRgbInt(): Int = toArgb() and 0xFFFFFF

/**
 * Werkzeugleiste des Skizzen-Editors: Rückgängig, Leeren, Radierer-Umschalter, drei Stiftstärken
 * und die Farb-Palette (+ „＋" für einen eigenen Farbwähler). Verwaltet den Farbwähler-Dialog selbst.
 */
@Composable
fun SketchToolbar(
    penColor: Color,
    penWidth: Float,
    eraseMode: Boolean,
    onPenColor: (Color) -> Unit,
    onPenWidth: (Float) -> Unit,
    onToggleErase: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Aktionen + Stiftstärken
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionChip(label = "Rückgängig", icon = Icons.AutoMirrored.Rounded.Undo, onClick = onUndo)
            ActionChip(label = "Leeren", icon = Icons.Rounded.Delete, onClick = onClear)
            ActionChip(label = "Radierer", active = eraseMode, onClick = onToggleErase)
            Spacer(Modifier.width(4.dp))
            listOf(PEN_THIN, PEN_MEDIUM, PEN_THICK).forEach { size ->
                WidthDot(
                    width = size,
                    selected = !eraseMode && penWidth == size,
                    onClick = { onPenWidth(size) },
                )
            }
        }
        // Farb-Palette + eigener Farbwähler
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            INK_PALETTE.forEach { c ->
                Swatch(color = c, selected = !eraseMode && c == penColor, onClick = { onPenColor(c) })
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .paperPress(CircleShape) { showPicker = true }
                    .border(1.5.dp, ink.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Eigene Farbe", tint = ink, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showPicker) {
        InkColorPickerDialog(
            initial = penColor,
            onPick = {
                onPenColor(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val bg = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier
            .paperPress(RoundedCornerShape(50)) { onClick() }
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = label, tint = ink, modifier = Modifier.size(18.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

@Composable
private fun WidthDot(width: Float, selected: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .size(30.dp)
            .paperPress(CircleShape) { onClick() }
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((width * 2.2f).dp)
                .background(ink, CircleShape),
        )
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .size(30.dp)
            .paperPress(CircleShape) { onClick() }
            .background(color, CircleShape)
            .then(
                if (selected) Modifier.border(2.5.dp, ink, CircleShape)
                else Modifier.border(1.dp, ink.copy(alpha = 0.15f), CircleShape),
            ),
    )
}

/** Einfacher RGB-Farbwähler (drei Schieber + Vorschau). */
@Composable
private fun InkColorPickerDialog(
    initial: Color,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var r by remember { mutableFloatStateOf(initial.red) }
    var g by remember { mutableFloatStateOf(initial.green) }
    var b by remember { mutableFloatStateOf(initial.blue) }
    val preview = Color(r, g, b)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(preview) }) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        title = { Text("Eigene Farbe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 28.dp)
                        .background(preview, RoundedCornerShape(8.dp)),
                )
                Slider(value = r, onValueChange = { r = it })
                Slider(value = g, onValueChange = { g = it })
                Slider(value = b, onValueChange = { b = it })
            }
        },
    )
}
