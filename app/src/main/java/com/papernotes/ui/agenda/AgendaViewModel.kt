package com.papernotes.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Art des Zeit-Eintrags einer Notiz in der Agenda. */
enum class AgendaKind { REMINDER, COUNTDOWN, EXPIRY }

/** Ein einzelner Agenda-Eintrag: eine Notiz mit einer ihrer Zeit-Marken. */
data class AgendaEntry(val note: Note, val kind: AgendaKind, val at: Long)

/** Eine nach Zeitfenster gruppierte Sektion (z. B. „Heute"). */
data class AgendaSection(val title: String, val entries: List<AgendaEntry>)

/**
 * Sammelt alle zeitbezogenen Notizen (Erinnerung, Countdown, Ablauf) und gruppiert sie
 * chronologisch nach Tagesfenster. Rein lesend – keine Persistenz-Änderung.
 */
@HiltViewModel
class AgendaViewModel @Inject constructor(
    repository: NoteRepository,
) : ViewModel() {

    val sections: StateFlow<List<AgendaSection>> =
        repository.observeActiveNotes()
            .map { notes -> buildSections(notes, System.currentTimeMillis()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Reihenfolge der Sektionen von oben nach unten. */
private val SECTION_ORDER = listOf("Überfällig", "Heute", "Morgen", "Diese Woche", "Später")

internal fun buildSections(notes: List<Note>, now: Long): List<AgendaSection> {
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()

    val entries = buildList {
        notes.forEach { note ->
            note.reminderAt?.let { add(AgendaEntry(note, AgendaKind.REMINDER, it)) }
            note.countdownAt?.let { add(AgendaEntry(note, AgendaKind.COUNTDOWN, it)) }
            note.expiresAt?.let { add(AgendaEntry(note, AgendaKind.EXPIRY, it)) }
        }
    }

    val grouped = LinkedHashMap<String, MutableList<AgendaEntry>>()
    SECTION_ORDER.forEach { grouped[it] = mutableListOf() }

    entries.forEach { entry ->
        val day = Instant.ofEpochMilli(entry.at).atZone(ZoneId.systemDefault()).toLocalDate()
        val daysOff = ChronoUnit.DAYS.between(today, day)
        val title = when {
            // Vergangene Erinnerungen bleiben relevant ("noch zu erledigen").
            entry.kind == AgendaKind.REMINDER && entry.at < now -> "Überfällig"
            // Vergangene Countdowns/Abläufe sind vorbei → auslassen.
            daysOff < 0L -> return@forEach
            daysOff == 0L -> "Heute"
            daysOff == 1L -> "Morgen"
            daysOff <= 7L -> "Diese Woche"
            else -> "Später"
        }
        grouped.getValue(title).add(entry)
    }

    return grouped.entries
        .filter { it.value.isNotEmpty() }
        .map { (title, list) -> AgendaSection(title, list.sortedBy { it.at }) }
}
