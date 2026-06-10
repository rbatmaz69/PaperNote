package com.papernotes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import com.papernotes.ui.theme.LocalPaperDark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Virtuelles "Eselsohr" an der oberen rechten Ecke einer Notizkarte.
 *
 * - Tippen knickt die Ecke um/auf (physikbasierte Spring-Animation des Fold-Fortschritts).
 * - Die [accent]-Farbe signalisiert die Stimmung/Kategorie der Notiz.
 * - Langes Drücken öffnet die Stimmungsauswahl ([onLongPress]).
 */
@Composable
fun DogEar(
    folded: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    earSize: Dp = 34.dp,
    paperBack: Color =
        if (LocalPaperDark.current) Color(0xFF3B342C) else Color(0xFFEFE7D8),
) {
    val progress by animateFloatAsState(
        targetValue = if (folded) 1f else 0.18f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "dogEarFold",
    )
    // Stimmungswechsel: Akzentfarbe weich überblenden statt hart umspringen.
    val animatedAccent by animateColorAsState(accent, tween(320), label = "dogEarAccent")

    Canvas(
        modifier = modifier
            .size(earSize)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggle() },
                    onLongPress = { onLongPress() },
                )
            },
    ) {
        val w = size.width
        val leg = w * progress

        // Schatten unter dem umgeknickten Eck (weich, leicht versetzt)
        val shadow = Path().apply {
            moveTo(w - leg, 0f)
            lineTo(w, 0f)
            lineTo(w, leg)
            close()
        }
        drawPath(shadow, color = Color.Black.copy(alpha = 0.10f * progress))

        // Rückseite des Papiers (heller Creme-Ton) – wirkt wie umgeklappt
        val back = Path().apply {
            moveTo(w - leg, 0f)
            lineTo(w, 0f)
            lineTo(w, leg)
            close()
        }
        drawPath(back, color = paperBack)

        // Stimmungsfarbe als zarter, dreieckiger Akzent auf dem Knick
        val tint = Path().apply {
            moveTo(w - leg, 0f)
            lineTo(w, leg)
            lineTo(w - leg * 0.45f, leg * 0.45f)
            close()
        }
        drawPath(tint, color = animatedAccent.copy(alpha = 0.85f))

        // feine Faltlinie
        drawLine(
            color = Color.Black.copy(alpha = 0.12f),
            start = Offset(w - leg, 0f),
            end = Offset(w, leg),
            strokeWidth = 1f,
        )
    }
}
