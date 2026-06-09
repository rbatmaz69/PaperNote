package com.papernotes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.DailyDelight
import com.papernotes.ui.theme.LocalPaperDark
import com.papernotes.ui.theme.SunnyYellow
import com.papernotes.ui.theme.SunnyYellowNight
import com.papernotes.util.rememberPaperHaptics

/**
 * Der "Glücks-Teebeutel": eine dezente Schnur mit Label am oberen Rand. Per Wischgeste
 * nach unten zieht man das Gimmick des Tages heraus. Über der Schwelle rastet eine kleine
 * Karte mit Spring-Overshoot ein (haptisch bestätigt); erneutes Ziehen/Tippen tuckt sie zurück.
 *
 * Die Zugbewegung wird über [draggable] synchron in ein Float-State geführt (flüssig, keine
 * Coroutine pro Event); nur das Zurückschnellen läuft über eine [Animatable]-Spring.
 *
 * [stats] (optional) zeigt eine dezente Tageszeile, z.B. "Heute · 3 Notizen · 5 Haken".
 */
@Composable
fun TeabagPull(
    delight: DailyDelight,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    stats: String? = null,
    onPulled: () -> Unit = {},
) {
    val density = LocalDensity.current
    val haptics = rememberPaperHaptics()
    val dark = LocalPaperDark.current
    val ink = MaterialTheme.colorScheme.onBackground

    val maxPull = with(density) { 120.dp.toPx() }
    val threshold = maxPull * 0.55f

    var dragOffset by remember { mutableFloatStateOf(0f) }
    var animating by remember { mutableStateOf(false) }
    val springBack = remember { Animatable(0f) }
    var revealed by remember { mutableStateOf(false) }
    var crossed by remember { mutableStateOf(false) }

    val translation = if (animating) springBack.value else dragOffset

    val dragState = rememberDraggableState { delta ->
        dragOffset = (dragOffset + delta).coerceIn(0f, maxPull)
        if (!crossed && dragOffset >= threshold) {
            crossed = true
            haptics.tick()
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Schnur – Länge folgt der Zugbewegung
            val stringLenDp = with(density) { (18f + translation * 0.5f).toDp() }
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = stringLenDp)
                    .background(ink.copy(alpha = 0.35f)),
            )

            // Label / Teebeutel-Tag
            val tagColor = when {
                highlighted && dark -> SunnyYellowNight
                highlighted -> SunnyYellow
                dark -> Color(0xFF3B342C)
                else -> Color(0xFFEFE7D8)
            }
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = translation }
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(tagColor, RoundedCornerShape(8.dp))
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = {
                            if (dragOffset >= threshold) {
                                revealed = true
                                haptics.confirm()
                                onPulled()
                            }
                            crossed = false
                            // weich zurückschnellen
                            animating = true
                            springBack.snapTo(dragOffset)
                            dragOffset = 0f
                            springBack.animateTo(
                                0f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                            animating = false
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = if (highlighted) "✦ heute" else "✦",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlighted && !dark) Color(0xFF3A322E) else ink,
                )
            }

            // Herausgezogene Karte mit dem Glücksmoment
            AnimatedVisibility(
                visible = revealed,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
            ) {
                DelightCard(
                    delight = delight,
                    stats = stats,
                    onDismiss = { revealed = false },
                )
            }
        }
    }
}

@Composable
private fun DelightCard(
    delight: DailyDelight,
    stats: String?,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 10.dp)
            .width(260.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .pointerInput(Unit) { detectVerticalDragGestures { _, _ -> onDismiss() } }
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = delight.emoji, style = MaterialTheme.typography.displaySmall)
        Text(
            text = delight.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (stats != null) {
            Text(
                text = stats,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Text(
            text = "↑ zurückschieben",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
