package com.papernotes.domain.model

/**
 * Eine symmetrische Verknüpfung ("roter Faden") zwischen zwei Notizen. Stets normalisiert
 * gehalten ([aId] < [bId]), damit ein Paar nur einmal existiert – erzeugt über [linkOf].
 */
data class NoteLink(
    val aId: Long,
    val bId: Long,
) {
    /** Die mit [id] verbundene Gegenseite, oder null, wenn [id] nicht Teil des Paars ist. */
    fun otherEnd(id: Long): Long? = when (id) {
        aId -> bId
        bId -> aId
        else -> null
    }

    fun involves(id: Long): Boolean = id == aId || id == bId
}

/** Baut eine normalisierte [NoteLink] (kleinere Id zuerst), egal in welcher Reihenfolge. */
fun linkOf(x: Long, y: Long): NoteLink =
    if (x <= y) NoteLink(x, y) else NoteLink(y, x)
