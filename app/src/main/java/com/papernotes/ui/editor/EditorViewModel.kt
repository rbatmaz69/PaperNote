package com.papernotes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papernotes.data.prefs.DelightPreferences
import com.papernotes.data.repository.NoteRepository
import com.papernotes.domain.ChecklistCodec
import com.papernotes.domain.ChecklistItem
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Checklisten-Zeile mit stabiler UI-Id (für Move-/Sink-Animationen beim Sortieren). */
data class EditableChecklistItem(
    val uiId: Long,
    val text: String,
    val checked: Boolean,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val delightPreferences: DelightPreferences,
) : ViewModel() {

    private val _note = MutableStateFlow(Note())
    val note: StateFlow<Note> = _note.asStateFlow()

    private val _items = MutableStateFlow<List<EditableChecklistItem>>(emptyList())
    val items: StateFlow<List<EditableChecklistItem>> = _items.asStateFlow()

    /** Zähler; jede Erhöhung = ein Konfetti-Burst (letzter offener Eintrag abgehakt). */
    private val _celebration = MutableStateFlow(0)
    val celebration: StateFlow<Int> = _celebration.asStateFlow()

    /** Zeile, die nach dem Anlegen den Fokus bekommen soll. */
    private val _focusRequest = MutableStateFlow<Long?>(null)
    val focusRequest: StateFlow<Long?> = _focusRequest.asStateFlow()

    private var loadedSession = -1
    private var saveJob: Job? = null
    private var nextUiId = 1L

    /**
     * Lädt eine bestehende Notiz (id > 0) oder startet eine leere. [session] kommt vom NavGraph
     * und steigt bei *jedem* Öffnen — so wird der (Activity-weit geteilte) ViewModel-Zustand bei
     * jedem Editor-Aufruf frisch zurückgesetzt, auch beim wiederholten Anlegen neuer Notizen.
     */
    fun load(id: Long, newType: NoteType, session: Int) {
        if (session == loadedSession) return
        loadedSession = session

        // Frischer Start: alten Zustand vollständig zurücksetzen.
        saveJob?.cancel()
        saveJob = null
        nextUiId = 1L
        _celebration.value = 0
        _focusRequest.value = null
        _items.value = emptyList()

        if (id <= 0L) {
            _note.value = Note(type = newType)
            if (newType == NoteType.CHECKLIST) addItem()
            return
        }
        _note.value = Note(id = id)
        viewModelScope.launch {
            repository.getNote(id)?.let { note ->
                if (loadedSession != session) return@launch
                _note.value = note
                _items.value = note.checklist.map { it.toEditable() }
            }
        }
    }

    fun onTitleChange(value: String) {
        _note.update { it.copy(title = value) }
        scheduleSave()
    }

    fun onBodyChange(value: String) {
        _note.update { it.copy(body = value) }
        scheduleSave()
    }

    fun setMood(mood: MoodCategory) {
        _note.update { it.copy(mood = mood, dogEarFolded = true) }
        scheduleSave()
    }

    fun toggleDogEar() {
        _note.update { it.copy(dogEarFolded = !it.dogEarFolded) }
        scheduleSave()
    }

    fun togglePin() {
        _note.update { it.copy(pinned = !it.pinned) }
        scheduleSave()
    }

    // --- Checkliste ---

    fun setItemText(uiId: Long, text: String) {
        _items.update { list -> list.map { if (it.uiId == uiId) it.copy(text = text) else it } }
        syncChecklistBody()
    }

    fun toggleItem(uiId: Long) {
        val before = _items.value
        val target = before.firstOrNull { it.uiId == uiId } ?: return
        val nowChecked = !target.checked

        // Erledigte sinken ans Ende des offenen Blocks bzw. ganz nach unten.
        val rest = before.filter { it.uiId != uiId }
        val updated = target.copy(checked = nowChecked)
        _items.value = if (nowChecked) {
            rest + updated
        } else {
            rest.filter { !it.checked } + updated + rest.filter { it.checked }
        }
        syncChecklistBody()

        if (nowChecked) {
            viewModelScope.launch { delightPreferences.incrementChecksToday() }
            if (_items.value.all { it.checked }) _celebration.update { it + 1 }
        }
    }

    /** Fügt eine leere Zeile hinter [afterUiId] (oder ans Ende des offenen Blocks) ein. */
    fun addItem(afterUiId: Long? = null) {
        val newItem = EditableChecklistItem(uiId = nextUiId++, text = "", checked = false)
        _items.update { list ->
            val index = list.indexOfFirst { it.uiId == afterUiId }
            if (index >= 0) {
                list.toMutableList().apply { add(index + 1, newItem) }
            } else {
                val firstChecked = list.indexOfFirst { it.checked }
                if (firstChecked < 0) {
                    list + newItem
                } else {
                    list.toMutableList().apply { add(firstChecked, newItem) }
                }
            }
        }
        _focusRequest.value = newItem.uiId
        syncChecklistBody()
    }

    fun removeItem(uiId: Long) {
        _items.update { list -> list.filter { it.uiId != uiId } }
        syncChecklistBody()
    }

    fun consumeFocusRequest() {
        _focusRequest.value = null
    }

    // --- Persistenz ---

    /** Sofort speichern – beim Verlassen des Editors. Notiz wird sofort gefangen (race-sicher). */
    fun flush() {
        saveJob?.cancel()
        val snapshot = _note.value
        viewModelScope.launch { persist(snapshot) }
    }

    fun moveToTrash(onDone: () -> Unit) {
        saveJob?.cancel()
        val id = _note.value.id
        viewModelScope.launch {
            if (id != 0L) repository.moveToTrash(id)
            onDone()
        }
    }

    private fun ChecklistItem.toEditable() =
        EditableChecklistItem(uiId = nextUiId++, text = text, checked = checked)

    private fun syncChecklistBody() {
        _note.update { note ->
            note.copy(
                body = ChecklistCodec.serialize(
                    _items.value
                        .filter { it.text.isNotBlank() }
                        .map { ChecklistItem(it.text, it.checked) },
                ),
            )
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            persist(_note.value)
        }
    }

    private suspend fun persist(note: Note) {
        if (note.isBlank && note.id == 0L) return
        val id = repository.save(note)
        if (note.id == 0L) {
            // Nur die id nachtragen, wenn der Editor noch dieselbe (frisch angelegte) Notiz zeigt.
            _note.update { if (it.id == 0L) it.copy(id = id) else it }
        }
    }
}
