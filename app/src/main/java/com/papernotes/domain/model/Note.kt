package com.papernotes.domain.model

import com.papernotes.domain.ChecklistCodec
import com.papernotes.domain.ChecklistItem
import com.papernotes.domain.StampCodec
import com.papernotes.domain.StampMotif

/** Domänenmodell einer Notiz (UI-/Logik-Schicht, entkoppelt von Room). */
data class Note(
    val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val type: NoteType = NoteType.TEXT,
    val mood: MoodCategory = MoodCategory.PLAIN,
    val dogEarFolded: Boolean = false,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    /** Zeitstempel der Knüll-Löschung; null = nicht im Papierkorb. */
    val deletedAt: Long? = null,
    /** Geplante Erinnerungszeit (Epoch-Millis); null = keine Erinnerung. */
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()

    /** true, wenn eine Erinnerung gesetzt ist (Papier-Reiter als Vorab-Hinweis). */
    val hasReminder: Boolean get() = reminderAt != null

    /** true, wenn die Erinnerung zu [now] fällig ist (löst das Papier-Flattern aus). */
    fun isReminderDue(now: Long): Boolean = reminderAt?.let { it <= now } == true

    val preview: String
        get() = body.trim().ifBlank { "—" }

    /** Checklisten-Einträge (leer für Text-Notizen). */
    val checklist: List<ChecklistItem>
        get() = if (type == NoteType.CHECKLIST) ChecklistCodec.parse(body) else emptyList()

    fun withChecklist(items: List<ChecklistItem>): Note =
        copy(body = ChecklistCodec.serialize(items))

    /** Gestempelte Tage (leer, außer bei Stempelkarten). */
    val stamps: Set<Long>
        get() = if (type == NoteType.STAMPCARD) StampCodec.parse(body) else emptySet()

    /** Gewähltes Stempel-Motiv (nur für Stempelkarten relevant). */
    val stampMotif: StampMotif
        get() = if (type == NoteType.STAMPCARD) StampCodec.motif(body) else StampMotif.CHECK

    fun withStamps(days: Set<Long>): Note =
        copy(body = StampCodec.serialize(days, stampMotif))

    fun withStampMotif(motif: StampMotif): Note =
        copy(body = StampCodec.serialize(stamps, motif))
}
