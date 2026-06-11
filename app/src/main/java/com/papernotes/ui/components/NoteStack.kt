package com.papernotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.cardSurface
import com.papernotes.ui.notes.StackItem
import com.papernotes.util.rememberPaperHaptics

private val ClipSteel = Color(0xFF8A8F98)
private val CardShape = RoundedCornerShape(18.dp)

/**
 * Ein Büroklammer-Stapel im Raster. Eingeklappt belegt er **eine** Zelle (oberste Notiz +
 * versetzte Blätter dahinter + Klammer + Zahl); ein Tipp auf die Klammer fächert die
 * Mitglieder untereinander auf, erneutes Tippen klappt wieder zu.
 */
@Composable
fun NoteStack(
    item: StackItem,
    now: Long,
    onOpenNote: (Long) -> Unit,
    onToggleDogEar: (Note) -> Unit,
    onPickMood: (Note) -> Unit,
    onToggleStampDay: (Note, Long) -> Unit,
    onUnclip: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPaperHaptics()
    var expanded by remember(item.clipId) { mutableStateOf(false) }

    if (expanded) {
        ExpandedStack(
            item = item,
            now = now,
            onOpenNote = onOpenNote,
            onToggleDogEar = onToggleDogEar,
            onPickMood = onPickMood,
            onToggleStampDay = onToggleStampDay,
            onUnclip = onUnclip,
            onCollapse = { haptics.tick(); expanded = false },
            modifier = modifier,
        )
    } else {
        CollapsedStack(
            item = item,
            now = now,
            onOpenCover = { onOpenNote(item.cover.note.id) },
            onToggleDogEar = onToggleDogEar,
            onPickMood = onPickMood,
            onToggleStampDay = onToggleStampDay,
            onExpand = { haptics.tick(); expanded = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun CollapsedStack(
    item: StackItem,
    now: Long,
    onOpenCover: () -> Unit,
    onToggleDogEar: (Note) -> Unit,
    onPickMood: (Note) -> Unit,
    onToggleStampDay: (Note, Long) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cover = item.cover.note
    val surface = cover.mood.cardSurface()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 8.dp, bottom = 8.dp),
    ) {
        // Versetzte „Blätter" hinter der Karte (gleiche Größe wie das Cover, leicht gedreht).
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 7.dp)
                .rotate(2.4f)
                .shadow(3.dp, CardShape, clip = false)
                .background(lerp(surface, Color.Black, 0.07f), CardShape),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .rotate(-1.4f)
                .shadow(3.dp, CardShape, clip = false)
                .background(lerp(surface, Color.Black, 0.035f), CardShape),
        )

        // Cover-Karte: bestimmt die Zellengröße, liegt obenauf.
        NoteCard(
            note = cover,
            dimmed = item.dimmed,
            reminderDue = cover.isReminderDue(now),
            now = now,
            onClick = onOpenCover,
            onToggleDogEar = { onToggleDogEar(cover) },
            onPickMood = { onPickMood(cover) },
            onToggleStampDay = { day -> onToggleStampDay(cover, day) },
        )

        // Büroklammer über der Oberkante + Anzahl; Tipp fächert auf.
        ClipBadge(
            count = item.members.size,
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 16.dp, y = (-11).dp),
        )
    }
}

@Composable
private fun ExpandedStack(
    item: StackItem,
    now: Long,
    onOpenNote: (Long) -> Unit,
    onToggleDogEar: (Note) -> Unit,
    onPickMood: (Note) -> Unit,
    onToggleStampDay: (Note, Long) -> Unit,
    onUnclip: (Note) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Kopf: Klammer + „Stapel · N", Tipp klappt zu.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .paperPress(RoundedCornerShape(12.dp)) { onCollapse() }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(modifier = Modifier.size(width = 14.dp, height = 22.dp)) { drawPaperclip() }
            Text(
                text = "Stapel · ${item.members.size}",
                style = MaterialTheme.typography.labelLarge,
                color = ink,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "zuklappen",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        item.members.forEach { gridNote ->
            val note = gridNote.note
            Box(modifier = Modifier.fillMaxWidth()) {
                NoteCard(
                    note = note,
                    dimmed = gridNote.dimmed,
                    reminderDue = note.isReminderDue(now),
                    now = now,
                    onClick = { onOpenNote(note.id) },
                    onToggleDogEar = { onToggleDogEar(note) },
                    onPickMood = { onPickMood(note) },
                    onToggleStampDay = { day -> onToggleStampDay(note, day) },
                )
                // Aus dem Stapel lösen.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .paperPress(CircleShape) { onUnclip(note) }
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Aus Stapel lösen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Gezeichnete Büroklammer mit Anzahl-Badge; eigene Tap-Fläche zum Auffächern. */
@Composable
private fun ClipBadge(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .paperPress(RoundedCornerShape(50)) { onClick() }
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(
            modifier = Modifier
                .width(18.dp)
                .height(30.dp)
                .shadow(0.dp),
        ) { drawPaperclip() }
        Box(
            modifier = Modifier
                .size(18.dp)
                .shadow(2.dp, CircleShape, clip = false)
                .background(ClipSteel, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

/** Stilisierte Draht-Büroklammer: zwei ineinander liegende Schlaufen. */
private fun DrawScope.drawPaperclip() {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.16f, cap = StrokeCap.Round)
    drawRoundRect(
        color = ClipSteel,
        topLeft = Offset(w * 0.16f, 0f),
        size = Size(w * 0.68f, h * 0.86f),
        cornerRadius = CornerRadius(w * 0.42f),
        style = stroke,
    )
    drawRoundRect(
        color = ClipSteel,
        topLeft = Offset(w * 0.34f, h * 0.16f),
        size = Size(w * 0.32f, h * 0.70f),
        cornerRadius = CornerRadius(w * 0.22f),
        style = stroke,
    )
}
