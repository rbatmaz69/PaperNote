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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.papernotes.util.rememberPaperHaptics

/** Welches Siegel aufgebrochen wird: Position/Größe der Karte (Root-px) + Lack-Farbe. */
data class WaxSealBreakRequest(
    val noteId: Long,
    val bounds: Rect,
    val color: Color,
)

private val SEAL_SIZE = 60.dp

/**
 * Bruch-Gimmick: Das Wachssiegel der angetippten Karte zerspringt in zwei Hälften, die mit
 * leichtem Wobble auseinanderfliegen, kippen und ausfaden – begleitet von einem trockenen
 * Knack ([PaperHaptics.crack]). Danach öffnet sich die Notiz ([onFinished]).
 */
@Composable
fun WaxSealBreakOverlay(
    request: WaxSealBreakRequest,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = rememberPaperHaptics()
    val progress = remember(request.noteId) { Animatable(0f) }

    LaunchedEffect(request.noteId) {
        haptics.crack()
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        )
        onFinished()
    }

    val p = progress.value
    val eased = FastOutSlowInEasing.transform(p)
    val spread = with(density) { 26.dp.toPx() } * eased
    val drop = with(density) { 10.dp.toPx() } * eased
    val alpha = if (p < 0.55f) 1f else (1f - (p - 0.55f) / 0.45f).coerceIn(0f, 1f)
    val center = request.bounds.center

    Box(modifier = Modifier.fillMaxSize()) {
        SealShard(leftHalf = true, center = center, dx = -spread, dy = drop, rotation = -14f * p, alpha = alpha)
        SealShard(leftHalf = false, center = center, dx = spread, dy = drop, rotation = 14f * p, alpha = alpha)
    }
}

@Composable
private fun SealShard(
    leftHalf: Boolean,
    center: Offset,
    dx: Float,
    dy: Float,
    rotation: Float,
    alpha: Float,
) {
    val halfPx = with(LocalDensity.current) { SEAL_SIZE.toPx() / 2f }
    Canvas(
        modifier = Modifier
            .size(SEAL_SIZE)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                translationX = center.x - halfPx + dx
                translationY = center.y - halfPx + dy
                rotationZ = rotation
                this.alpha = alpha
            },
    ) {
        val w = size.width
        val h = size.height
        if (leftHalf) {
            clipRect(left = 0f, top = 0f, right = w / 2f, bottom = h) { drawWaxSeal() }
        } else {
            clipRect(left = w / 2f, top = 0f, right = w, bottom = h) { drawWaxSeal() }
        }
    }
}
