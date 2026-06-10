package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.NoteLink
import kotlinx.coroutines.launch
import kotlin.math.sin

/** Tiefes Garn-Rot des Fadens. */
private val ThreadRed = Color(0xFFB3402F)

/**
 * Zeichnet den "roten Faden" zwischen verknüpften Notizkarten – wie Garn auf einer
 * Pinnwand. Jeder Faden hängt sanft durch, trägt an beiden Enden einen kleinen Knoten
 * und „atmet" leicht. Frisch geknüpfte Fäden nähen sich von der einen zur anderen Karte.
 *
 * Speist sich aus den live getrackten Kartenpositionen ([bounds], Root-Koordinaten) –
 * deshalb folgt der Faden beim Scrollen/Zoomen automatisch.
 */
@Composable
fun RedThreadOverlay(
    links: List<NoteLink>,
    bounds: Map<Long, Rect>,
    dimmedIds: Set<Long>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // Einnäh-Fortschritt pro Faden (0 = noch nicht gezogen, 1 = fertig gespannt).
    val stitch = remember { mutableStateMapOf<NoteLink, Animatable<Float, *>>() }

    LaunchedEffect(links) {
        links.forEach { link ->
            if (stitch[link] == null) {
                val anim = Animatable(0f)
                stitch[link] = anim
                scope.launch {
                    anim.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
                }
            }
        }
        stitch.keys.filter { it !in links }.forEach { stitch.remove(it) }
    }

    val breathe = rememberInfiniteTransition(label = "threadBreathe")
    val breath by breathe.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "threadSag",
    )

    Canvas(modifier = modifier) {
        links.forEach { link ->
            val ra = bounds[link.aId] ?: return@forEach
            val rb = bounds[link.bId] ?: return@forEach
            val progress = stitch[link]?.value ?: 1f
            val faded = link.aId in dimmedIds || link.bId in dimmedIds
            drawThread(ra.topCenter, rb.topCenter, link, progress, breath, faded)
        }
    }
}

private fun DrawScope.drawThread(
    start: Offset,
    end: Offset,
    link: NoteLink,
    progress: Float,
    breath: Float,
    faded: Boolean,
) {
    val alpha = if (faded) 0.18f else 0.9f
    val color = ThreadRed.copy(alpha = alpha)
    val strokeWidth = 2.5.dp.toPx()
    val knotRadius = 3.5.dp.toPx()

    // Durchhang nach unten, abhängig von der Distanz, plus ein handgemachter Mini-Versatz.
    val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    val dist = (end - start).getDistance()
    val seed = (link.aId * 31 + link.bId).toFloat()
    val wobbleX = sin(seed) * 7f
    val sag = dist * 0.13f * breath
    val ctrl = Offset(mid.x + wobbleX, mid.y + sag)

    val full = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
    }

    // Faden bis zum aktuellen Einnäh-Fortschritt zeichnen.
    val drawn = if (progress >= 1f) {
        full
    } else {
        val measure = PathMeasure().apply { setPath(full, forceClosed = false) }
        Path().also { measure.getSegment(0f, measure.length * progress, it, true) }
    }
    drawPath(
        path = drawn,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Knoten: am Start sofort, am Ziel erst, wenn der Faden dort ankommt.
    drawCircle(color = color, radius = knotRadius, center = start)
    if (progress >= 0.99f) {
        drawCircle(color = color, radius = knotRadius, center = end)
    }
}
