package com.papernotes.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.delight.DailyDelightProvider
import com.papernotes.data.prefs.DelightPreferences
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.model.DailyDelight
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val delight: DailyDelight,
    val delightAvailable: Boolean = true,
    val statsLine: String = "",
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

    private val filteredNotes = combine(
        repository.observeActiveNotes(),
        searchQuery,
        moodFilter,
    ) { notes, query, mood ->
        val trimmed = query.trim()
        Triple(
            notes.map { note ->
                val matchesQuery = trimmed.isEmpty() ||
                    note.title.contains(trimmed, ignoreCase = true) ||
                    note.body.contains(trimmed, ignoreCase = true)
                val matchesMood = mood == null || note.mood == mood
                GridNote(note = note, dimmed = !(matchesQuery && matchesMood))
            },
            notes.map { it.mood }.distinct().sortedBy { it.ordinal },
            mood,
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
            filteredNotes,
            repository.observeArchivedNotes(),
            repository.observeTrashedNotes(),
            delightPreferences.availableToday,
            dailyStats,
        ) { (gridNotes, presentMoods, activeMood), archived, trashed, available, stats ->
            NotesUiState(
                notes = gridNotes,
                archived = archived,
                trashed = trashed,
                presentMoods = presentMoods,
                activeMoodFilter = activeMood,
                searchQuery = searchQuery.value,
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

    fun setReminder(note: Note, at: Long?) = viewModelScope.launch {
        repository.setReminder(note.id, at)
        if (at != null) {
            reminderScheduler.schedule(note.id, note.title, at)
        } else {
            reminderScheduler.cancel(note.id)
            reminderScheduler.dismissNotification(note.id)
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
