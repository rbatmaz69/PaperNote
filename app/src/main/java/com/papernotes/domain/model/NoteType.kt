package com.papernotes.domain.model

/** Art der Notiz: Fließtext oder Checkliste (Items via [com.papernotes.domain.ChecklistCodec]). */
enum class NoteType {
    TEXT,
    CHECKLIST;

    companion object {
        fun fromName(name: String?): NoteType =
            entries.firstOrNull { it.name == name } ?: TEXT
    }
}
