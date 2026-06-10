package com.papernotes.domain.model

/**
 * Art der Notiz: Fließtext, Checkliste (Items via [com.papernotes.domain.ChecklistCodec])
 * oder Stempelkarte (gestempelte Tage via [com.papernotes.domain.StampCodec]).
 */
enum class NoteType {
    TEXT,
    CHECKLIST,
    STAMPCARD;

    companion object {
        fun fromName(name: String?): NoteType =
            entries.firstOrNull { it.name == name } ?: TEXT
    }
}
