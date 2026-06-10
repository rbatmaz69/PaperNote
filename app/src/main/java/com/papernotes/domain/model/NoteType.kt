package com.papernotes.domain.model

/**
 * Art der Notiz: Fließtext, Checkliste (Items via [com.papernotes.domain.ChecklistCodec]),
 * Stempelkarte (gestempelte Tage via [com.papernotes.domain.StampCodec]) oder Tinten-Skizze
 * (Striche via [com.papernotes.domain.SketchCodec]).
 */
enum class NoteType {
    TEXT,
    CHECKLIST,
    STAMPCARD,
    SKETCH;

    companion object {
        fun fromName(name: String?): NoteType =
            entries.firstOrNull { it.name == name } ?: TEXT
    }
}
