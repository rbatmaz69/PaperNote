package com.papernotes.data.local

import androidx.room.Entity
import com.papernotes.domain.model.NoteLink

/**
 * Verknüpfung zweier Notizen ("roter Faden"). Symmetrisch, daher als normalisiertes
 * Paar gespeichert (`aId < bId`) – siehe [com.papernotes.domain.model.linkOf]. Bewusst
 * ohne Foreign Keys gehalten; verwaiste Links räumt das Repository auf.
 */
@Entity(tableName = "note_links", primaryKeys = ["aId", "bId"])
data class NoteLinkEntity(
    val aId: Long,
    val bId: Long,
)

fun NoteLinkEntity.toDomain(): NoteLink = NoteLink(aId = aId, bId = bId)

fun NoteLink.toEntity(): NoteLinkEntity = NoteLinkEntity(aId = aId, bId = bId)
