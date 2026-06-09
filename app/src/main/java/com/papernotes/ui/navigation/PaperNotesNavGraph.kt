package com.papernotes.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.papernotes.domain.model.NoteType
import com.papernotes.ui.editor.EditorScreen
import com.papernotes.ui.notes.NotesScreen

/** Sentinel: eine *neue* Notiz wird mit id 0 geöffnet; null = Übersicht. */
private const val NEW_NOTE_ID = 0L

/**
 * Hält Übersicht und Editor in einem [SharedTransitionLayout], sodass eine Karte beim
 * Antippen via [AnimatedContent] fließend zum Vollbild-Editor morpht (kein harter Wechsel).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaperNotesNavGraph() {
    // null = Grid, sonst die zu bearbeitende Notiz-id (0 = neu, Typ via newNoteType).
    var selectedNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var newNoteType by rememberSaveable { mutableStateOf(NoteType.TEXT.name) }
    // Steigt bei jedem Öffnen → erzwingt frischen Editor-Zustand (siehe EditorViewModel.load).
    var editorSession by rememberSaveable { mutableIntStateOf(0) }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedNoteId,
            label = "paperNav",
            transitionSpec = {
                fadeIn(tween(durationMillis = 240)) togetherWith
                    fadeOut(tween(durationMillis = 180))
            },
        ) { target ->
            if (target == null) {
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
                )
            } else {
                EditorScreen(
                    noteId = target,
                    newType = NoteType.fromName(newNoteType),
                    session = editorSession,
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this@AnimatedContent,
                    onBack = { selectedNoteId = null },
                )
            }
        }
    }
}
