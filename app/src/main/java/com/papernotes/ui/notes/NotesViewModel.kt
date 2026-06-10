package com.papernotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.delight.DailyDelightProvider
import com.papernotes.data.prefs.DelightPreferences
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.StampCodec
import com.papernotes.domain.model.DailyDelight
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteLink
import com.papernotes.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

/** Notiz fürs Grid; [dimmed] = Nicht-Treffer von Suche/Stimmungs-Filter ("verdünnte Tinte"). */
data class GridNote(
    val note: Note,
    val dimmed: Boolean,
)

data class NotesUiState(
    val notes: List<GridNote> = emptyList(),
    val archived: List<Note> = emptyList(),
    val trashed: List<Note> = emptyList(),
    val presentMoods: List<MoodCategory> = emptyList(),
    val activeMoodFilter: MoodCategory? = null,
    val searchQuery: String = "",
    val links: List<NoteLink> = emptyList(),
    val delight: DailyDelight,
    val delightAvailable: Boolean = true,
    val statsLine: String = "",
)

/** Zwischenergebnis: Grid-Notizen + Filter-Metadaten + die roten Fäden. */
private data class GridState(
    val notes: List<GridNote>,
    val presentMoods: List<MoodCategory>,
    val activeMood: MoodCategory?,
    val links: List<NoteLink>,
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val delightPreferences: DelightPreferences,
    private val reminderScheduler: ReminderScheduler,
    delightProvider: DailyDelightProvider,
) : ViewModel() {

    private val delight = delightProvider.forToday()

    private val searchQuery = MutableStateFlow("")
    private val moodFilter = MutableStateFlow<MoodCategory?>(null)

    init {
        viewModelScope.launch { repository.purgeOldTrash() }
    }

    private val gridState = combine(
        repository.observeActiveNotes(),
        searchQuery,
        moodFilter,
        repository.observeLinks(),
    ) { notes, query, mood, links ->
        val trimmed = query.trim()
        GridState(
            notes = notes.map { note ->
                // Versiegelte Notizen tauchen nicht in Suchtreffern auf (kein Inhalts-Leak),
                // bleiben aber ohne aktive Suche normal im Grid sichtbar.
                val matchesQuery = trimmed.isEmpty() ||
                    (!note.sealed && (
                        note.title.contains(trimmed, ignoreCase = true) ||
                            note.body.contains(trimmed, ignoreCase = true)
                        ))
                val matchesMood = mood == null || note.mood == mood
                GridNote(note = note, dimmed = !(matchesQuery && matchesMood))
            },
            presentMoods = notes.map { it.mood }.distinct().sortedBy { it.ordinal },
            activeMood = mood,
            links = links,
        )
    }

    private val dailyStats = combine(
        repository.countCreatedSince(startOfToday()),
        delightPreferences.checksToday,
    ) { created, checks ->
        buildString {
            append("Heute · ")
            append(if (created == 1) "1 Notiz" else "$created Notizen")
            if (checks > 0) append(" · $checks Haken")
        }
    }

    val uiState: StateFlow<NotesUiState> =
        combine(
            gridState,
            repository.observeArchivedNotes(),
            repository.observeTrashedNotes(),
            delightPreferences.availableToday,
            dailyStats,
        ) { grid, archived, trashed, available, stats ->
            NotesUiState(
                notes = grid.notes,
                archived = archived,
                trashed = trashed,
                presentMoods = grid.presentMoods,
                activeMoodFilter = grid.activeMood,
                searchQuery = searchQuery.value,
                links = grid.links,
                delight = delight,
                delightAvailable = available,
                statsLine = stats,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState(delight = delight),
        )

    fun onSearchChange(query: String) = searchQuery.update { query }

    fun clearSearch() = searchQuery.update { "" }

    fun toggleMoodFilter(mood: MoodCategory) =
        moodFilter.update { if (it == mood) null else mood }

    fun toggleDogEar(note: Note) = viewModelScope.launch {
        repository.setDogEar(note.id, folded = !note.dogEarFolded, mood = note.mood)
    }

    fun setMood(note: Note, mood: MoodCategory) = viewModelScope.launch {
        repository.setDogEar(note.id, folded = true, mood = mood)
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.setPinned(note.id, !note.pinned)
    }

    fun toggleSeal(note: Note) = viewModelScope.launch {
        repository.setSealed(note.id, !note.sealed)
    }

    fun setReminder(note: Note, at: Long?) = viewModelScope.launch {
        repository.setReminder(note.id, at)
        if (at != null) {
            reminderScheduler.schedule(note.id, note.title, at)
        } else {
            reminderScheduler.cancel(note.id)
            reminderScheduler.dismissNotification(note.id)
        }
    }

    fun linkNotes(a: Long, b: Long) = viewModelScope.launch { repository.linkNotes(a, b) }

    fun unlinkNotes(a: Long, b: Long) = viewModelScope.launch { repository.unlinkNotes(a, b) }

    /** Einmal-Event (Notiz-Id), wenn ein Stempel die Strähne auf eine 7er-Marke hebt. */
    private val _stampMilestone = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val stampMilestone: SharedFlow<Long> = _stampMilestone

    /** Setzt/entfernt den Stempel für [day] einer Stempelkarte direkt aus dem Grid. */
    fun toggleStamp(note: Note, day: Long) = viewModelScope.launch {
        val days = note.stamps.toMutableSet()
        val nowStamped = if (day in days) {
            days.remove(day); false
        } else {
            days.add(day); true
        }
        repository.save(note.withStamps(days))
        if (nowStamped) {
            val streak = StampCodec.streak(days, LocalDate.now().toEpochDay())
            if (streak > 0 && streak % 7L == 0L) _stampMilestone.tryEmit(note.id)
        }
    }

    fun archive(id: Long) = viewModelScope.launch { repository.archive(id) }

    fun restore(id: Long) = viewModelScope.launch { repository.restore(id) }

    fun moveToTrash(id: Long) = viewModelScope.launch { repository.moveToTrash(id) }

    fun markDelightPulled() = viewModelScope.launch { delightPreferences.markPulledToday() }

    private fun startOfToday(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
