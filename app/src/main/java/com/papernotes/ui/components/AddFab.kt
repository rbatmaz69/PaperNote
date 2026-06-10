package com.papernotes.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.NoteType
import com.papernotes.util.rememberPaperHaptics

/**
 * Unauffälliger, runder "+"-Button – das einzige feste UI-Element auf dem Grid.
 * Tap fächert zwei Mini-Buttons auf (Text-Notiz / Checkliste), die mit Spring herausfedern.
 */
@Composable
fun AddFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreate: (NoteType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPaperHaptics()

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabScale",
    )
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fabRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
            exit = fadeOut() + scaleOut(targetScale = 0.6f),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniAction(
                    label = "Checkliste",
                    icon = { tint ->
                        Icon(Icons.Rounded.Checklist, null, tint = tint, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        haptics.tap()
                        onExpandedChange(false)
                        onCreate(NoteType.CHECKLIST)
                    },
                )
                MiniAction(
                    label = "Notiz",
                    icon = { tint ->
                        Icon(Icons.AutoMirrored.Rounded.Notes, null, tint = tint, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        haptics.tap()
                        onExpandedChange(false)
                        onCreate(NoteType.TEXT)
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .shadow(12.dp, CircleShape, clip = false)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(interactionSource = interaction, indication = null) {
                    haptics.tap()
                    onExpandedChange(!expanded)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Neue Notiz",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun MiniAction(
    label: String,
    icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .paperPress(RoundedCornerShape(22.dp), onClick = onClick)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon(tint)
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
        )
    }
}
