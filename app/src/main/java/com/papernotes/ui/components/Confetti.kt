package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.papernotes.ui.theme.Lavender
import com.papernotes.ui.theme.SageGreen
import com.papernotes.ui.theme.SkyMist
import com.papernotes.ui.theme.SoftPeach
import com.papernotes.ui.theme.SunnyYellow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val angle: Float,      // Abwurfrichtung
    val speed: Float,      // relative Startgeschwindigkeit
    val size: Float,       // Kantenlänge relativ
    val spin: Float,       // Rotationsgeschwindigkeit
    val color: Color,
)

/**
 * Dezenter Papier-Konfetti-Burst (z.B. wenn die letzte Checklisten-Aufgabe abgehakt wird):
 * ~24 kleine Papierschnipsel in Palette-Farben fliegen aus der Mitte, trudeln und verblassen.
 * Rein dekorativ, ~900 ms, danach ruft es [onFinished].
 */
@Composable
fun ConfettiBurst(
    trigger: Any,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    val particles = remember(trigger) {
        val rng = Random(trigger.hashCode())
        val palette = listOf(SunnyYellow, SageGreen, SoftPeach, Lavender, SkyMist)
        List(24) {
            Particle(
                angle = rng.nextFloat() * 2f * Math.PI.toFloat(),
                speed = 0.5f + rng.nextFloat() * 0.9f,
                size = 5f + rng.nextFloat() * 7f,
                spin = (rng.nextFloat() - 0.5f) * 14f,
                color = palette[rng.nextInt(palette.size)],
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(durationMillis = 900, easing = LinearEasing))
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val reach = size.minDimension * 0.55f
        val gravity = size.minDimension * 0.35f
        val alpha = (1f - p).coerceIn(0f, 1f)

        particles.forEach { particle ->
            val dist = reach * particle.speed * p
            val x = cx + cos(particle.angle) * dist
            val y = cy + sin(particle.angle) * dist + gravity * p * p
            rotate(degrees = particle.spin * p * 360f, pivot = Offset(x, y)) {
                drawRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(x - particle.size / 2f, y - particle.size / 2f),
                    size = Size(particle.size, particle.size * 1.6f),
                )
            }
        }
    }
}
