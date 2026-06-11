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
import com.papernotes.domain.model.PaperStyle
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

/**
 * Eine Grid-Zelle: entweder eine einzelne Notiz ([SoloItem]) oder ein Büroklammer-Stapel
 * ([StackItem]) aus mehreren zusammengeklammerten Notizen.
 */
sealed interface GridItem {
    val key: String
    val dimmed: Boolean
}

data class SoloItem(val gridNote: GridNote) : GridItem {
    override val key: String get() = "note-${gridNote.note.id}"
    override val dimmed: Boolean get() = gridNote.dimmed
}

/** Stapel: [cover] liegt oben, [members] enthält alle (inkl. Cover) in Sortier-Reihenfolge. */
data class StackItem(
    val clipId: Long,
    val cover: GridNote,
    val members: List<GridNote>,
) : GridItem {
    override val key: String get() = "clip-$clipId"
    override val dimmed: Boolean get() = members.all { it.dimmed }
}

data class NotesUiState(
    val notes: List<GridNote> = emptyList(),
    val items: List<GridItem> = emptyList(),
    val archived: List<Note> = emptyList(),
    val trashed: List<Note> = emptyList(),
    val presentMoods: List<MoodCategory> = emptyList(),
    val activeMoodFilter: MoodCategory? = null,
    val presentTags: List<String> = emptyList(),
    val activeTagFilter: String? = null,
    val searchQuery: String = "",
    val links: List<NoteLink> = emptyList(),
    val delight: DailyDelight,
    /** false, solange Room noch nicht das erste Mal emittiert hat (für Splash & Leerzustand). */
    val loaded: Boolean = false,
    val delightAvailable: Boolean = true,
    val statsLine: String = "",
)

/** Zwischenergebnis: Grid-Notizen + (zu Stapeln gruppierte) Grid-Items + Filter-Metadaten + Fäden. */
private data class GridState(
    val notes: List<GridNote>,
    val items: List<GridItem>,
    val presentMoods: List<MoodCategory>,
    val activeMood: MoodCategory?,
    val presentTags: List<String>,
    val activeTag: String?,
    val links: List<NoteLink>,
)

/**
 * Fasst die (bereits sortierten) Grid-Notizen zu Grid-Items zusammen: Notizen mit gleichem
 * `clipId` werden – sofern ≥2 aktiv sichtbar – an der Position der obersten zu einem [StackItem]
 * gebündelt; alle anderen bleiben [SoloItem]. Die ursprüngliche Reihenfolge bleibt erhalten.
 */
private fun groupIntoItems(notes: List<GridNote>): List<GridItem> {
    val byClip = notes.filter { it.note.clipId != null }.groupBy { it.note.clipId!! }
    val emitted = mutableSetOf<Long>()
    val items = mutableListOf<GridItem>()
    for (gridNote in notes) {
        val clipId = gridNote.note.clipId
        val group = clipId?.let { byClip[it] }
        if (clipId != null && group != null && group.size >= 2) {
            if (emitted.add(clipId)) {
                items += StackItem(clipId = clipId, cover = group.first(), members = group)
            }
        } else {
            items += SoloItem(gridNote)
        }
    }
    return items
}

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
    private val tagFilter = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { repository.purgeOldTrash() }
        // Abgelaufene Notizen (während die App zu war) beim Start in den Papierkorb räumen.
        viewModelScope.launch { repository.purgeExpired() }
    }

    private val gridState = combine(
        repository.observeActiveNotes(),
        searchQuery,
        moodFilter,
        tagFilter,
        repository.observeLinks(),
    ) { notes, query, mood, tag, links ->
        val trimmed = query.trim()
        val gridNotes = notes.map { note ->
            // Versiegelte Notizen tauchen nicht in Suchtreffern auf (kein Inhalts-Leak),
            // bleiben aber ohne aktive Suche normal im Grid sichtbar.
            val matchesQuery = trimmed.isEmpty() ||
                (!note.sealed && (
                    note.title.contains(trimmed, ignoreCase = true) ||
                        note.body.contains(trimmed, ignoreCase = true)
                    ))
            val matchesMood = mood == null || note.mood == mood
            val matchesTag = tag == null || tag in note.tagList
            GridNote(note = note, dimmed = !(matchesQuery && matchesMood && matchesTag))
        }
        GridState(
            notes = gridNotes,
            items = groupIntoItems(gridNotes),
            presentMoods = notes.map { it.mood }.distinct().sortedBy { it.ordinal },
            activeMood = mood,
            presentTags = notes.flatMap { it.tagList }.distinct().sorted(),
            activeTag = tag,
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
                items = grid.items,
                archived = archived,
                trashed = trashed,
                presentMoods = grid.presentMoods,
                activeMoodFilter = grid.activeMood,
                presentTags = grid.presentTags,
                activeTagFilter = grid.activeTag,
                searchQuery = searchQuery.value,
                links = grid.links,
                delight = delight,
                loaded = true,
                delightAvailable = available,
                statsLine = stats,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NotesUiState(delight = delight),
        )

    fun onSearchChange(query: String) = searchQuery.update { query }

    fun clearSearch() = searchQuery.update { "" }

    fun toggleMoodFilter(mood: MoodCategory) =
        moodFilter.update { if (it == mood) null else mood }

    fun toggleTagFilter(tag: String) =
        tagFilter.update { if (it == tag) null else tag }

    fun toggleTag(note: Note, tag: String) = viewModelScope.launch {
        val current = note.tagList
        val next = if (tag in current) current - tag else current + tag
        repository.setTags(note.id, note.withTags(next).tags)
    }

    fun addTag(note: Note, tag: String) = viewModelScope.launch {
        repository.setTags(note.id, note.withTags(note.tagList + tag).tags)
    }

    /** Exportiert alle Notizen (inkl. Fotos & Verknüpfungen) als ZIP nach [uri]. */
    fun exportBackup(context: android.content.Context, uri: android.net.Uri, onResult: (Int) -> Unit) =
        viewModelScope.launch {
            onResult(
                runCatching {
                    com.papernotes.data.backup.BackupManager.export(context, repository, uri)
                }.getOrDefault(-1),
            )
        }

    fun importBackup(context: android.content.Context, uri: android.net.Uri, onResult: (Int) -> Unit) =
        viewModelScope.launch {
            onResult(
                runCatching {
                    com.papernotes.data.backup.BackupManager.import(context, repository, uri)
                }.getOrDefault(-1),
            )
        }

    /** Speichert die per Pinnwand gezogene Reihenfolge (Stapel-Mitglieder bleiben zusammenhängend). */
    fun applyOrder(items: List<GridItem>) = viewModelScope.launch {
        val ids = items.flatMap { item ->
            when (item) {
                is SoloItem -> listOf(item.gridNote.note.id)
                is StackItem -> item.members.map { it.note.id }
            }
        }
        repository.applyOrder(ids)
    }

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

    fun toggleInvisibleInk(note: Note) = viewModelScope.launch {
        repository.setInvisibleInk(note.id, !note.invisibleInk)
    }

    fun toggleDone(note: Note) = viewModelScope.launch {
        repository.setDone(note.id, !note.done)
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

    fun setExpiry(note: Note, at: Long?) = viewModelScope.launch {
        repository.setExpiry(note.id, at)
    }

    fun setCountdown(note: Note, at: Long?) = viewModelScope.launch {
        repository.setCountdown(note.id, at)
    }

    fun setPhoto(note: Note, path: String?) = viewModelScope.launch {
        repository.setPhoto(note.id, path)
    }

    fun setPaper(note: Note, style: PaperStyle) = viewModelScope.launch {
        repository.setPaper(note.id, style)
    }

    fun linkNotes(a: Long, b: Long) = viewModelScope.launch { repository.linkNotes(a, b) }

    fun unlinkNotes(a: Long, b: Long) = viewModelScope.launch { repository.unlinkNotes(a, b) }

    /**
     * Klammert [otherId] an den Stapel von [target] (bzw. löst sie wieder). Hat [target] noch
     * keinen Stapel, wird einer mit der eigenen Id als clipId eröffnet.
     */
    fun toggleClip(target: Note, otherId: Long) = viewModelScope.launch {
        val groupId = target.clipId ?: target.id
        if (target.clipId == null) repository.setClip(target.id, groupId)
        if (isInGroup(otherId, groupId)) {
            repository.setClip(otherId, null)
        } else {
            repository.setClip(otherId, groupId)
        }
    }

    /** Löst eine einzelne Notiz aus ihrem Stapel. */
    fun unclip(note: Note) = viewModelScope.launch { repository.setClip(note.id, null) }

    private fun isInGroup(id: Long, groupId: Long): Boolean =
        uiState.value.notes.any { it.note.id == id && it.note.clipId == groupId }

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
