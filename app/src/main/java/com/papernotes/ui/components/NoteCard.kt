package com.papernotes.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.domain.ChecklistCodec
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent

/**
 * Eine Notiz als schwebendes Stück Papier: weicher, dynamischer Schatten (reagiert auf
 * Druck), sanfte Stimmungsfläche, umknickbares Eselsohr, optional Washi-Tape (gepinnt)
 * und Checklisten-Vorschau. [dimmed] blendet Nicht-Treffer der Suche wie verdünnte Tinte aus.
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onToggleDogEar: () -> Unit,
    onPickMood: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    reminderDue: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Papier-Flattern: zarte, dauerhafte Wackelbewegung, solange die Erinnerung fällig ist.
    // Die Werte werden erst in der graphicsLayer (Draw-Phase) gelesen – nur fällige Karten
    // zeichnen pro Frame neu, der Rest bleibt von der Endlos-Animation unberührt.
    val flutter = rememberInfiniteTransition(label = "flutter")
    val flutterRotation = flutter.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "flutterRotation",
    )
    val flutterShift = flutter.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "flutterShift",
    )

    val elevation by animateDpAsState(
        targetValue = if (pressed) 2.dp else 10.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardElevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale",
    )
    val inkAlpha by animateFloatAsState(
        targetValue = if (dimmed) 0.15f else 1f,
        animationSpec = tween(260),
        label = "cardDim",
    )

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = inkAlpha
                if (reminderDue) {
                    rotationZ = flutterRotation.value
                    translationX = flutterShift.value
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.25f),
                )
                .background(color = note.mood.cardSurface(), shape = shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            Column(
                modifier = Modifier
                    .defaultMinSize(minHeight = 96.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (note.type == NoteType.CHECKLIST) {
                    ChecklistPreview(note)
                } else {
                    Text(
                        text = note.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (note.body.isBlank()) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            DogEar(
                folded = note.dogEarFolded,
                accent = note.mood.earAccent(),
                onToggle = onToggleDogEar,
                onLongPress = onPickMood,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        // Washi-Tape ragt leicht über die Oberkante – wie aufgeklebt
        if (note.pinned) {
            WashiTape(
                color = note.mood.earAccent(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-9).dp),
            )
        }

        // Papier-Reiter am linken Rand: ruhiger Hinweis auf eine gesetzte Erinnerung.
        if (note.hasReminder) {
            ReminderTab(
                color = note.mood.earAccent(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-5).dp),
            )
        }
    }
}

/** Kleiner aufgeklebter Papier-Reiter (wie ein Lesezeichen) für Notizen mit Erinnerung. */
@Composable
private fun ReminderTab(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                clip = false,
                spotColor = Color.Black.copy(alpha = 0.2f),
            )
            .background(
                color = color,
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
            )
            .width(10.dp)
            .height(34.dp),
    )
}

/** Bis zu 5 Mini-Zeilen + Fortschritt „3/5" (read-only; Tap öffnet den Editor). */
@Composable
private fun ChecklistPreview(note: Note) {
    val items = note.checklist
    val (done, total) = ChecklistCodec.progress(items)
    val ink = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.take(5).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PaperCheckbox(
                    checked = item.checked,
                    color = note.mood.earAccent(),
                    inkColor = ink,
                    boxSize = 14.dp,
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration =
                            if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (item.checked) 0.5f else 1f),
                )
            }
        }
        if (total > 0) {
            Text(
                text = "$done/$total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
