package com.papernotes.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch

/**
 * "Papier-Druck" – das einheitliche Druck-Feedback der App. Ersetzt den Material-Ripple:
 * Beim Tippen drückt sich das Element sanft ein (leichtes Skalieren) und legt einen
 * weichen Tinten-Schleier darüber, beides auf die Form geclippt (kein „schwarzes Quadrat").
 *
 * Bewusst als [IndicationNodeFactory]/[DrawModifierNode] umgesetzt: Die Press-Animation
 * läuft komplett in der Draw-/Node-Schicht – ohne Recomposition, daher günstig und snappy.
 */
private data class PaperPressIndication(val tint: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PaperPressNode(interactionSource, tint)
}

private class PaperPressNode(
    private val interactionSource: InteractionSource,
    private val tint: Color,
) : Modifier.Node(), DrawModifierNode {

    /** 0 = losgelassen … 1 = voll eingedrückt. */
    private val press = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                val target = when (interaction) {
                    is PressInteraction.Press -> 1f
                    is PressInteraction.Release, is PressInteraction.Cancel -> 0f
                    else -> return@collect
                }
                launch {
                    press.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val p = press.value
        val s = 1f - 0.07f * p
        scale(s, s, pivot = center) {
            this@draw.drawContent()
        }
        if (p > 0f) {
            drawRect(color = tint.copy(alpha = tint.alpha * p))
        }
    }
}

/** Merkt sich die Indication mit dem aktuellen Tinten-Ton (folgt dem Theme). */
@Composable
private fun rememberPaperPress(): PaperPressIndication {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
    return remember(tint) { PaperPressIndication(tint) }
}

/**
 * Anklickbar mit Papier-Druck statt Ripple. [shape] wird geclippt – dadurch entsteht bei
 * runden/abgerundeten Flächen kein rechteckiger Press-Schatten mehr.
 */
fun Modifier.paperPress(
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val indication = rememberPaperPress()
    val source = remember { MutableInteractionSource() }
    clip(shape).clickable(
        interactionSource = source,
        indication = indication,
        enabled = enabled,
        onClick = onClick,
    )
}

/** Wie [paperPress], zusätzlich mit Lang-Druck (z. B. Stimmung über Karte). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.paperPress(
    shape: Shape = RectangleShape,
    enabled: Boolean = true,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier = composed {
    val indication = rememberPaperPress()
    val source = remember { MutableInteractionSource() }
    clip(shape).combinedClickable(
        interactionSource = source,
        indication = indication,
        enabled = enabled,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}
