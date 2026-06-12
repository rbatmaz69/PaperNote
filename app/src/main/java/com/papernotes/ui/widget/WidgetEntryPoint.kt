package com.papernotes.ui.widget

import com.papernotes.data.repository.NoteRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-Zugang für das Glance-Widget: Ein App-Widget ist kein @AndroidEntryPoint, daher holen
 * wir das [NoteRepository] über einen EntryPoint aus dem Application-Graph.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun noteRepository(): NoteRepository
}
