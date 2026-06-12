package com.papernotes.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.papernotes.domain.model.NoteType
import com.papernotes.ui.agenda.AgendaScreen
import com.papernotes.ui.editor.EditorScreen
import com.papernotes.ui.notes.NotesScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Sentinel: eine *neue* Notiz wird mit id 0 geöffnet; null = Übersicht. */
private const val NEW_NOTE_ID = 0L

/** Inhalt für einen direkt beim Start zu öffnenden neuen Zettel (geteilter Text / Shortcut). */
data class QuickCapture(val title: String, val body: String)

/**
 * Hält Übersicht und Editor in einem [SharedTransitionLayout], sodass eine Karte beim
 * Antippen via [AnimatedContent] fließend zum Vollbild-Editor morpht (kein harter Wechsel).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaperNotesNavGraph(
    openNote: Flow<Long> = emptyFlow(),
    quickCapture: QuickCapture? = null,
) {
    // null = Grid, sonst die zu bearbeitende Notiz-id (0 = neu, Typ via newNoteType).
    // Per „Einkleben"/Shortcut: direkt einen neuen Zettel (id 0) öffnen.
    var selectedNoteId by rememberSaveable {
        mutableStateOf<Long?>(if (quickCapture != null) NEW_NOTE_ID else null)
    }
    var newNoteType by rememberSaveable { mutableStateOf(NoteType.TEXT.name) }
    // Steigt bei jedem Öffnen → erzwingt frischen Editor-Zustand (siehe EditorViewModel.load).
    var editorSession by rememberSaveable { mutableIntStateOf(0) }
    // Im Grid: true = Schreibtisch-Agenda statt Notiz-Raster.
    var agendaVisible by rememberSaveable { mutableStateOf(false) }

    // Widget-/Notification-Tap: betreffende Notiz öffnen – auch wenn die App schon läuft.
    LaunchedEffect(Unit) {
        openNote.collect { id ->
            editorSession++
            agendaVisible = false
            selectedNoteId = id
        }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedNoteId,
            label = "paperNav",
            transitionSpec = {
                fadeIn(tween(durationMillis = 160)) togetherWith
                    fadeOut(tween(durationMillis = 120))
            },
        ) { target ->
            if (target == null) {
                Crossfade(targetState = agendaVisible, label = "agendaSwap") { showAgenda ->
                    if (showAgenda) {
                        AgendaScreen(
                            onOpenNote = { id ->
                                editorSession++
                                agendaVisible = false
                                selectedNoteId = id
                            },
                            onBack = { agendaVisible = false },
                        )
                    } else {
                        NotesScreen(
                            sharedScope = this@SharedTransitionLayout,
                            animatedScope = this@AnimatedContent,
                            onOpenNote = {
                                editorSession++
                                selectedNoteId = it
                            },
                            onCreateNote = { type ->
                                newNoteType = type.name
                                editorSession++
                                selectedNoteId = NEW_NOTE_ID
                            },
                            onOpenAgenda = { agendaVisible = true },
                        )
                    }
                }
            } else {
                // QuickCapture-Inhalt nur für den initial geöffneten Zettel (session 0) vorbefüllen.
                val capture = if (editorSession == 0) quickCapture else null
                EditorScreen(
                    noteId = target,
                    newType = NoteType.fromName(newNoteType),
                    session = editorSession,
                    initialTitle = capture?.title.orEmpty(),
                    initialBody = capture?.body.orEmpty(),
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                    onBack = { selectedNoteId = null },
                    onOpenLinkedNote = { id ->
                        editorSession++
                        selectedNoteId = id
                    },
                )
            }
        }
    }
}
