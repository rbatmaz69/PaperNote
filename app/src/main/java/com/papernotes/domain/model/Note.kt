package com.papernotes.domain.model

import com.papernotes.domain.ChecklistCodec
import com.papernotes.domain.ChecklistItem
import com.papernotes.domain.Highlight
import com.papernotes.domain.HighlightCodec
import com.papernotes.domain.SketchCodec
import com.papernotes.domain.SketchStroke
import com.papernotes.domain.StampCodec
import com.papernotes.domain.StampMotif
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    /** Mit Wachs versiegelt: Inhalt im Grid verborgen, bis das Siegel aufgebrochen wird. */
    val sealed: Boolean = false,
    /** Geheimtinte: Text im Grid verborgen und nur beim Gedrückthalten der Karte lesbar. */
    val invisibleInk: Boolean = false,
    /** Zeitstempel der Knüll-Löschung; null = nicht im Papierkorb. */
    val deletedAt: Long? = null,
    /** Geplante Erinnerungszeit (Epoch-Millis); null = keine Erinnerung. */
    val reminderAt: Long? = null,
    /** Wiederholungs-Regel der Erinnerung (täglich/werktags/wöchentlich); NONE = einmalig. */
    val reminderRule: ReminderRule = ReminderRule.NONE,
    /** Ablaufzeit (Epoch-Millis); null = beständig. Zur Zeit zerknüllt sich die Notiz selbst. */
    val expiresAt: Long? = null,
    /** Zeitkapsel: Öffnungsdatum (Epoch-Millis). Bis dahin bleibt die versiegelte Notiz zu. */
    val capsuleAt: Long? = null,
    /** Text auf der Rückseite des Blatts (für jeden Notiz-Typ; leer = unbeschriebene Rückseite). */
    val backText: String = "",
    /** Büroklammer-Stapel: Notizen mit gleichem clipId bilden ein Bündel. null = nicht geklammert. */
    val clipId: Long? = null,
    /** Abreißkalender: Zieldatum (Epoch-Millis am Tagesbeginn); null = kein Countdown. */
    val countdownAt: Long? = null,
    /** Foto-Polaroid: interner Dateiname im App-Speicher; null = kein Foto. */
    val photoPath: String? = null,
    /** Papier-Liniierung: blanko, liniert, kariert oder gepunktet. */
    val paper: PaperStyle = PaperStyle.BLANK,
    /** Karteireiter: Tag-Labels, `\n`-getrennt; leer = keine Reiter. */
    val tags: String = "",
    /** Manuelle Reihenfolge (Pinnwand): kleiner = weiter oben. 0 = noch nie angeordnet. */
    val position: Long = 0L,
    /** Textmarker-Markierungen (kodiert via HighlightCodec); leer = keine. */
    val highlights: String = "",
    /** Als erledigt abgestempelt (Notiz bleibt erhalten, trägt einen Gummistempel). */
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isBlank: Boolean get() = title.isBlank() && body.isBlank() && backText.isBlank()

    /** true, wenn die Rückseite beschrieben ist (Karte zeigt dann eine umgeknickte Ecke). */
    val hasBack: Boolean get() = backText.isNotBlank()

    /** true, wenn eine Erinnerung gesetzt ist (Papier-Reiter als Vorab-Hinweis). */
    val hasReminder: Boolean get() = reminderAt != null

    /** true, wenn die Erinnerung zu [now] fällig ist (löst das Papier-Flattern aus). */
    fun isReminderDue(now: Long): Boolean = reminderAt?.let { it <= now } == true

    /** true, wenn die Erinnerung sich wiederholt (Reiter trägt ein „↻"). */
    val isRecurring: Boolean get() = reminderRule != ReminderRule.NONE

    /** true, wenn eine Ablaufzeit gesetzt ist (vergängliche Notiz). */
    val hasExpiry: Boolean get() = expiresAt != null

    /** true, wenn die Notiz eine noch nicht fällige Zeitkapsel ist (verschlossen bis [capsuleAt]). */
    fun isCapsuleLocked(now: Long): Boolean =
        sealed && capsuleAt?.let { it > now } == true

    /** true, wenn die Notiz zu [now] abgelaufen ist (zerknüllt sich dann selbst). */
    fun isExpired(now: Long): Boolean = expiresAt?.let { it <= now } == true

    /** true, wenn ein Abreißkalender-Zieldatum gesetzt ist. */
    val hasCountdown: Boolean get() = countdownAt != null

    /** true, wenn ein Foto angehängt ist (Polaroid). */
    val hasPhoto: Boolean get() = photoPath != null

    /** Karteireiter als Liste (getrimmt, ohne Leereinträge). */
    val tagList: List<String>
        get() = tags.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    /** true, wenn mindestens ein Karteireiter gesetzt ist. */
    val hasTags: Boolean get() = tagList.isNotEmpty()

    /** Setzt die Reiter neu (getrimmt, dedupliziert, ohne Leereinträge). */
    fun withTags(list: List<String>): Note =
        copy(tags = list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n"))

    /** Textmarker-Markierungen als Liste. */
    val highlightRanges: List<Highlight> get() = HighlightCodec.parse(highlights)

    fun withHighlights(list: List<Highlight>): Note =
        copy(highlights = HighlightCodec.serialize(list))

    /** Verbleibende Tage bis zum Zieltag (0 = heute, negativ = vorbei); null = kein Countdown. */
    fun daysUntil(now: Long): Long? = countdownAt?.let {
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val target = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        ChronoUnit.DAYS.between(today, target)
    }

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

    /** Tinten-Striche (leer, außer bei Skizzen). */
    val sketch: List<SketchStroke>
        get() = if (type == NoteType.SKETCH) SketchCodec.parse(body) else emptyList()

    fun withSketch(strokes: List<SketchStroke>): Note =
        copy(body = SketchCodec.serialize(strokes))
}
