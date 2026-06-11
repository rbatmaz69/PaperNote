package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.util.rememberPaperHaptics
import kotlinx.coroutines.launch

/** Warmer Tinten-Braunton, in dem die Geheimtinte beim Erwärmen hervortritt. */
private val InkBrown = Color(0xFF7A4A2B)

/**
 * Geheimtinte: Der [text] ist im Grid verborgen und wird nur sichtbar, solange der Finger auf der
 * Karte ruht – die Buchstaben „erwärmen" sich sepia-braun und verblassen beim Loslassen wieder.
 * Ein kurzer Tipp (statt Halten) öffnet die Notiz über [onOpen].
 */
@Composable
fun InvisibleInkText(
    text: String,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val haptics = rememberPaperHaptics()
    val scope = rememberCoroutineScope()
    val reveal = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .pointerInput(text) {
                val touchSlop = viewConfiguration.touchSlop
                val longPress = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val downAt = System.currentTimeMillis()
                    var moved = false

                    // Aufwärmen: Text tritt über ~600 ms hervor; bei voller Enthüllung ein Tick.
                    val warming = scope.launch {
                        reveal.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                        haptics.tick()
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.first()
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        if ((change.position - down.position).getDistance() > touchSlop) {
                            moved = true
                        }
                        change.consume()
                    }

                    warming.cancel()
                    val heldFor = System.currentTimeMillis() - downAt
                    if (!moved && heldFor < longPress) onOpen()

                    // Wieder verblassen lassen.
                    scope.launch {
                        reveal.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Hinweis im verborgenen Zustand – verblasst, während sich die Tinte zeigt.
        Text(
            text = "🕯 halten zum Lesen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.alpha(1f - reveal.value),
        )

        // Der eigentliche Text: erwärmt sich aus dem Nichts ins Sepia-Braun.
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = lerp(Color.Transparent, InkBrown, reveal.value),
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = reveal.value },
        )
    }
}
