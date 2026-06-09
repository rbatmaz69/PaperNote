package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.papernotes.util.rememberPaperHaptics
import kotlin.math.sin
import kotlin.random.Random

/** Was zerknüllt werden soll: Position/Größe der Karte (Root-Koordinaten in px) + Optik. */
data class CrumpleRequest(
    val noteId: Long,
    val bounds: Rect,
    val color: Color,
)

/**
 * "Knuddel"-Gimmick: Die Karte knüllt sich wie echtes Papier zusammen (Skalieren +
 * Rotations-Wobble + wachsende Knickfalten) und fliegt in den Mini-Papierkorb am unteren
 * Rand – begleitet von einem haptischen Knister-Feedback.
 */
@Composable
fun CrumpleOverlay(
    request: CrumpleRequest,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val haptics = rememberPaperHaptics()

    val progress = remember(request.noteId) { Animatable(0f) }

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val target = Offset(screenWidthPx / 2f, screenHeightPx - with(density) { 48.dp.toPx() })

    LaunchedEffect(request.noteId) {
        haptics.crumple()
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    val p = progress.value
    val widthDp = with(density) { request.bounds.width.toDp() }
    val heightDp = with(density) { request.bounds.height.toDp() }

    val startCenter = request.bounds.center
    val center = lerp(startCenter, target, FastOutSlowInEasing.transform(p))
    val scale = lerp(1f, 0.08f, p)
    val rotation = sin(p * 18f) * 22f * p
    val alpha = if (p < 0.82f) 1f else lerp(1f, 0f, (p - 0.82f) / 0.18f)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .size(widthDp, heightDp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    translationX = center.x - request.bounds.width / 2f
                    translationY = center.y - request.bounds.height / 2f
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                    this.alpha = alpha
                },
        ) {
            drawCrumpledPaper(request.color, p)
        }

        // Mini-Papierkorb erscheint, während die Kugel ankommt
        if (p > 0.4f) {
            val binAlpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        translationX = target.x - with(density) { 20.dp.toPx() }
                        translationY = target.y - with(density) { 12.dp.toPx() }
                        this.alpha = binAlpha
                    },
            ) {
                drawTrashBin()
            }
        }
    }
}

/** Zeichnet das Knüll-Polygon; p=0 glattes Blatt … p=1 zerknüllte Kugel. (Reuse: Papierkorb) */
internal fun DrawScope.drawCrumpledPaper(color: Color, p: Float) {
    val w = size.width
    val h = size.height
    val rng = Random(42)

    if (p < 0.05f) {
        drawRoundRect(color = color, size = Size(w, h))
        return
    }

    // Unregelmäßiges Polygon, das mit steigendem p stärker "eingedrückt" wird
    val pts = 10
    val cx = w / 2f
    val cy = h / 2f
    val path = Path()
    for (i in 0 until pts) {
        val a = (i / pts.toFloat()) * 2f * Math.PI.toFloat()
        val jitter = lerp(1f, 0.55f + rng.nextFloat() * 0.5f, p)
        val rx = (w / 2f) * jitter
        val ry = (h / 2f) * jitter
        val x = cx + rx * kotlin.math.cos(a)
        val y = cy + ry * kotlin.math.sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)

    // Knickfalten – Linien, die mit p deutlicher werden
    val creaseAlpha = 0.18f * p
    repeat(6) {
        val sx = rng.nextFloat() * w
        val sy = rng.nextFloat() * h
        val ex = rng.nextFloat() * w
        val ey = rng.nextFloat() * h
        drawLine(
            color = Color.Black.copy(alpha = creaseAlpha),
            start = Offset(sx, sy),
            end = Offset(ex, ey),
            strokeWidth = 1.5f,
        )
    }
}

private fun DrawScope.drawTrashBin() {
    val w = size.width
    val h = size.height
    val body = Color(0xFF8A817A)
    // Korpus
    drawRoundRect(
        color = body,
        topLeft = Offset(w * 0.18f, h * 0.28f),
        size = Size(w * 0.64f, h * 0.62f),
    )
    // Deckel
    drawRoundRect(
        color = body,
        topLeft = Offset(w * 0.1f, h * 0.16f),
        size = Size(w * 0.8f, h * 0.12f),
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
