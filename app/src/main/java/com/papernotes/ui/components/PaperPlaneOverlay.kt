package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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

/** Was geteilt werden soll: Position/Größe der Karte (Root-Koordinaten in px) + Optik. */
data class PaperPlaneRequest(
    val noteId: Long,
    val bounds: Rect,
    val color: Color,
)

private const val FOLD_END = 0.45f

/**
 * "Papierflieger"-Gimmick: Die Karte faltet sich wie ein Blatt Papier zu einem
 * Origami-Dart (Rechteck → Pfeil-Silhouette mit Mittelfalz), startet dann auf einer
 * Bogenbahn nach oben aus dem Bild – begleitet von einem haptischen "Wurf". Danach
 * öffnet der Aufrufer die Android-Teilen-Auswahl.
 *
 * Eng modelliert nach [CrumpleOverlay] (gleiches graphicsLayer-/Animatable-Muster).
 */
@Composable
fun PaperPlaneOverlay(
    request: PaperPlaneRequest,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val haptics = rememberPaperHaptics()

    val progress = remember(request.noteId) { Animatable(0f) }

    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    // Ziel: oberhalb des Bildschirms, mittig – der Flieger verschwindet "in die Ferne".
    val target = Offset(screenWidthPx / 2f, -request.bounds.height)

    LaunchedEffect(request.noteId) {
        haptics.tick() // erste Knickfalte
        // Phase A: Falten
        progress.animateTo(
            targetValue = FOLD_END,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        )
        haptics.fold() // der Wurf
        // Phase B: Start / Flug
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 460, easing = LinearEasing),
        )
        onFinished()
    }

    val p = progress.value
    val foldP = (p / FOLD_END).coerceIn(0f, 1f)
    val flightP = ((p - FOLD_END) / (1f - FOLD_END)).coerceIn(0f, 1f)
    // Flug beschleunigt (Papier nimmt Fahrt auf) – quadratisches Easing.
    val accel = flightP * flightP

    val widthDp = with(density) { request.bounds.width.toDp() }
    val heightDp = with(density) { request.bounds.height.toDp() }

    val start = request.bounds.center
    val flightCenter = lerp(start, target, accel)
    // Kurzes "Anheben", bevor er fortfliegt.
    val liftPx = with(density) { 14.dp.toPx() } * foldP * (1f - flightP)
    val center = Offset(flightCenter.x, flightCenter.y - liftPx)

    val foldScale = lerp(1f, 0.86f, foldP)
    val scale = lerp(foldScale, 0.22f, accel)
    // Leichtes Banking: kippt beim Falten an, wiegt sich sanft im Flug.
    val rotation = lerp(0f, -10f, foldP) * (1f - flightP) + sin(flightP * 3.5f) * 6f * flightP
    val alpha = if (p < 0.8f) 1f else lerp(1f, 0f, (p - 0.8f) / 0.2f)

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
            drawFoldingPlane(request.color, foldP)
        }
    }
}

/**
 * Zeichnet den faltenden Flieger: fp=0 glattes Rechteck … fp=1 Origami-Dart (nach
 * oben zeigend) mit Mittelfalz und einer abgeschatteten Flügelhälfte für Tiefe.
 */
internal fun DrawScope.drawFoldingPlane(color: Color, fp: Float) {
    val w = size.width
    val h = size.height
    val cx = w / 2f

    if (fp < 0.04f) {
        drawRoundRect(color = color, size = Size(w, h))
        return
    }

    // Vier Stützpunkte interpolieren Rechteck → Dart (Pfeil nach oben).
    val nose = lerp(Offset(0f, 0f), Offset(cx, 0f), fp)               // Spitze (oben Mitte)
    val rightWing = lerp(Offset(w, 0f), Offset(w, h), fp)             // rechte Flügelspitze
    val tail = lerp(Offset(w, h), Offset(cx, h * 0.78f), fp)         // Heckkerbe (Mitte)
    val leftWing = lerp(Offset(0f, h), Offset(0f, h), fp)             // linke Flügelspitze

    val body = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(rightWing.x, rightWing.y)
        lineTo(tail.x, tail.y)
        lineTo(leftWing.x, leftWing.y)
        close()
    }
    drawPath(body, color = color)

    // Rechte Flügelhälfte leicht abgeschattet → Falt-Tiefe.
    val shade = Color.Black.copy(alpha = 0.10f * fp)
    val rightHalf = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(rightWing.x, rightWing.y)
        lineTo(tail.x, tail.y)
        close()
    }
    drawPath(rightHalf, color = shade)

    // Mittelfalz vom Bug zum Heck.
    drawLine(
        color = Color.Black.copy(alpha = 0.18f * fp),
        start = nose,
        end = tail,
        strokeWidth = 1.5f,
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(lerp(a.x, b.x, t), lerp(a.y, b.y, t))
