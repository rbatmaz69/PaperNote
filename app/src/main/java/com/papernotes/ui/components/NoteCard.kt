package com.papernotes.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.papernotes.domain.ChecklistCodec
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import com.papernotes.domain.model.cardSurface
import com.papernotes.domain.model.earAccent
import com.papernotes.util.rememberPaperHaptics
import java.time.LocalDate

/**
 * Eine Notiz als schwebendes Stück Papier: weicher, dynamischer Schatten (reagiert auf
 * Druck), sanfte Stimmungsfläche, umknickbares Eselsohr, optional Washi-Tape (gepinnt)
 * und Checklisten-Vorschau. [dimmed] blendet Nicht-Treffer der Suche wie verdünnte Tinte aus.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onToggleDogEar: () -> Unit,
    onPickMood: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    reminderDue: Boolean = false,
    now: Long = System.currentTimeMillis(),
    onToggleStampDay: ((Long) -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = rememberPaperHaptics()

    // Blatt umdrehen: an Ort und Stelle auf die (nur lesbare) Rückseite blättern.
    var showingBack by remember(note.id) { mutableStateOf(false) }
    val flip by animateFloatAsState(
        targetValue = if (showingBack) 180f else 0f,
        animationSpec = tween(450),
        label = "cardFlip",
    )

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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "cardScale",
    )
    val inkAlpha by animateFloatAsState(
        targetValue = if (dimmed) 0.15f else 1f,
        animationSpec = tween(260),
        label = "cardDim",
    )
    // Stimmungswechsel: Kartenfläche & Akzent weich überblenden.
    val surface by animateColorAsState(note.mood.cardSurface(), tween(320), label = "cardSurface")
    val accent by animateColorAsState(note.mood.earAccent(), tween(320), label = "cardAccent")

    // Vergängliche Notiz: je näher der Ablauf (letzte Stunde), desto stärker vergilbt das
    // Papier und die Ecke rollt sich ein.
    val age = note.expiresAt?.let { exp ->
        (1f - (exp - now).toFloat() / AGING_WINDOW_MS).coerceIn(0f, 1f)
    } ?: 0f
    val agedSurface = lerp(surface, SEPIA, age * 0.6f)

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
                .graphicsLayer {
                    rotationY = flip
                    cameraDistance = 12f * density
                }
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.25f),
                )
                .background(color = agedSurface, shape = shape)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongPress,
                ),
        ) {
          if (flip <= 90f) {
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
                if (note.sealed) {
                    SealedContent()
                } else {
                    when (note.type) {
                        NoteType.CHECKLIST -> ChecklistPreview(note)
                        NoteType.STAMPCARD -> StampCard(
                            stamps = note.stamps,
                            motif = note.stampMotif,
                            today = LocalDate.now().toEpochDay(),
                            accent = accent,
                            onToggleDay = { day -> onToggleStampDay?.invoke(day) },
                            compact = true,
                        )
                        NoteType.SKETCH -> {
                            val strokes = note.sketch
                            if (strokes.isEmpty()) {
                                Text(
                                    text = "leere Skizze",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            } else {
                                SketchView(
                                    strokes = strokes,
                                    inkColor = accent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                )
                            }
                        }
                        NoteType.TEXT -> Text(
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

                // Vergängliche Notiz: Rest-Zeit-Hinweis.
                note.expiresAt?.let { exp ->
                    Text(
                        text = "⌛ " + remainingLabel(exp - now),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Eingerollte, vergilbende Ecke (wächst, je näher der Ablauf).
            if (age > 0f) {
                CurledCorner(
                    age = age,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }

            DogEar(
                folded = note.dogEarFolded,
                accent = note.mood.earAccent(),
                onToggle = onToggleDogEar,
                onLongPress = onPickMood,
                modifier = Modifier.align(Alignment.TopEnd),
            )
          } else {
            BackFace(
                text = note.backText,
                surface = lerp(surface, SEPIA, 0.4f),
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            )
          }
        }

        // Umgeknickte Ecke unten-links: signalisiert eine beschriebene Rückseite und blättert um.
        if (note.hasBack) {
            PageCorner(
                onFlip = {
                    haptics.fold()
                    showingBack = !showingBack
                },
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }

        // Washi-Tape ragt leicht über die Oberkante – wie aufgeklebt
        if (note.pinned) {
            WashiTape(
                color = accent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-9).dp),
            )
        }

        // Papier-Reiter am linken Rand: ruhiger Hinweis auf eine gesetzte Erinnerung.
        if (note.hasReminder) {
            ReminderTab(
                color = accent,
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

/** Zeitfenster (1 h), in dem eine vergängliche Notiz sichtbar altert. */
private const val AGING_WINDOW_MS = 60L * 60L * 1000L
private val SEPIA = Color(0xFFE8D7A0)
private val PAPER_UNDERSIDE = Color(0xFFD9C48F)

/**
 * Eine sich hochrollende, vergilbte Papier-Ecke unten rechts – wächst mit [age] (0…1) und
 * signalisiert, dass die Notiz bald verfällt.
 */
@Composable
private fun CurledCorner(age: Float, modifier: Modifier = Modifier) {
    val curl = (8f + 24f * age).dp
    Canvas(modifier = modifier.size(curl)) {
        val w = size.width
        val h = size.height
        // Unterseite des hochgerollten Eckzipfels (Dreieck unten rechts).
        val triangle = Path().apply {
            moveTo(w, 0f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(triangle, PAPER_UNDERSIDE)
        // Schattenkante entlang der Falz.
        drawLine(
            color = Color.Black.copy(alpha = 0.18f),
            start = Offset(0f, h),
            end = Offset(w, 0f),
            strokeWidth = 2.5f,
        )
        // Heller Glanz auf der gerollten Kante.
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(w * 0.12f, h),
            end = Offset(w, h * 0.12f),
            strokeWidth = 1.5f,
        )
    }
}

/** Kompakter Rest-Zeit-Text: "noch 3 Tg" / "noch 5 Std" / "noch 12 Min" / "läuft ab …". */
private fun remainingLabel(remainingMs: Long): String {
    if (remainingMs <= 60_000L) return "läuft ab …"
    val minutes = remainingMs / 60_000L
    return when {
        minutes < 60 -> "noch $minutes Min"
        minutes < 60 * 24 -> "noch ${minutes / 60} Std"
        else -> "noch ${minutes / (60 * 24)} Tg"
    }
}

/** Versiegelte Notiz: Inhalt verborgen, nur Wachssiegel + zarter Hinweis. */
@Composable
private fun SealedContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WaxSeal(size = 56.dp)
        Text(
            text = "versiegelt",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * Die Rückseite des Blatts im Raster: nur-lesbarer Text auf dem dunkleren „Unterseiten"-Ton.
 * Behält eine Mindesthöhe, damit die Karte beim Umblättern nicht zusammenfällt.
 */
@Composable
private fun BackFace(text: String, surface: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 96.dp)
            .background(surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Rückseite",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = text.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Kleine umgeknickte Ecke unten-links: zeigt an, dass das Blatt eine beschriebene Rückseite
 * hat. Ein Tipp blättert die Karte um (bzw. wieder zurück).
 */
@Composable
private fun PageCorner(onFlip: () -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(26.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onFlip() }) },
    ) {
        val w = size.width
        val h = size.height
        // Hochgerollter Eckzipfel unten links (Dreieck) – die Papier-Unterseite scheint durch.
        val triangle = Path().apply {
            moveTo(0f, h)
            lineTo(0f, 0f)
            lineTo(w, h)
            close()
        }
        drawPath(triangle, PAPER_UNDERSIDE)
        // Schattenkante entlang der Falz.
        drawLine(
            color = Color.Black.copy(alpha = 0.18f),
            start = Offset(0f, 0f),
            end = Offset(w, h),
            strokeWidth = 2.5f,
        )
        // Heller Glanz auf der gerollten Kante.
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, h * 0.88f),
            end = Offset(w * 0.88f, h),
            strokeWidth = 1.5f,
        )
    }
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
