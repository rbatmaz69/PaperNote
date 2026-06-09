package com.papernotes.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Handgezeichnet wirkende Checkbox: leicht unregelmäßiger Rahmen, der Haken wird beim
 * Abhaken wie mit einem Stift "gezogen" (Path-Trim mit Spring-Physik).
 */
@Composable
fun PaperCheckbox(
    checked: Boolean,
    color: Color,
    inkColor: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 22.dp,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "checkDraw",
    )

    Canvas(modifier = modifier.size(boxSize)) {
        val w = size.width
        val stroke = w * 0.085f

        // Rahmen – minimal gedreht, wirkt wie von Hand gezeichnet
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke, stroke * 1.4f),
            size = androidx.compose.ui.geometry.Size(w - stroke * 2.2f, w - stroke * 2.4f),
            cornerRadius = CornerRadius(w * 0.22f),
            style = Stroke(width = stroke),
        )

        if (progress > 0.01f) {
            // Haken-Path, der über PathMeasure anteilig "gezogen" wird
            val check = Path().apply {
                moveTo(w * 0.24f, w * 0.52f)
                lineTo(w * 0.43f, w * 0.72f)
                lineTo(w * 0.84f, w * 0.20f)
            }
            val measure = PathMeasure().apply { setPath(check, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * progress.coerceAtMost(1f), partial, true)
            drawPath(
                path = partial,
                color = inkColor,
                style = Stroke(width = stroke * 1.5f, cap = StrokeCap.Round),
            )
        }
    }
}
