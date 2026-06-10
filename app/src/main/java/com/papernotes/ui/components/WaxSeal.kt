package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Tiefes Siegellack-Rot – Farbe des Wachssiegels (und der zerspringenden Scherben). */
val WaxRed = Color(0xFF9E2B25)
private val WaxRim = Color(0xFF7A1E1A)
private val WaxEmboss = Color(0xFF85221D)

/**
 * Ein aufgedrücktes Wachssiegel: eine unregelmäßige Lack-Scheibe in tiefem Rot mit weichem
 * Schatten und einem eingeprägten Blüten-Motiv. Markiert versiegelte (private) Notizen.
 */
@Composable
fun WaxSeal(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
) {
    Canvas(modifier = modifier.size(size)) { drawWaxSeal() }
}

/** Zeichnet ein intaktes Wachssiegel über die gesamte Zeichenfläche. */
internal fun DrawScope.drawWaxSeal(seed: Int = 7) {
    val c = center
    val r = size.minDimension / 2f
    val rng = Random(seed)

    // Weicher Schlagschatten leicht nach unten versetzt.
    drawCircle(Color.Black.copy(alpha = 0.18f), radius = r * 0.9f, center = c + Offset(0f, r * 0.10f))

    // Unregelmäßiger Wachs-Klecks (handgedrückt).
    val pts = 14
    val path = Path()
    for (i in 0 until pts) {
        val a = (i / pts.toFloat()) * 2f * PI.toFloat()
        val jitter = 0.86f + rng.nextFloat() * 0.16f
        val rad = r * 0.82f * jitter
        val x = c.x + rad * cos(a)
        val y = c.y + rad * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, WaxRed)

    // Dunklerer Innenrand – wirkt wie eingedrücktes Lack.
    drawCircle(WaxRim.copy(alpha = 0.5f), radius = r * 0.78f, style = Stroke(width = r * 0.10f))

    // Eingeprägtes Blüten-Motiv (5 Blütenblätter + Zentrum).
    val petal = r * 0.18f
    val ring = r * 0.30f
    for (i in 0 until 5) {
        val a = -PI.toFloat() / 2f + i * (2f * PI.toFloat() / 5f)
        drawCircle(WaxEmboss, radius = petal, center = Offset(c.x + ring * cos(a), c.y + ring * sin(a)))
    }
    drawCircle(WaxEmboss, radius = r * 0.20f, center = c)
}
