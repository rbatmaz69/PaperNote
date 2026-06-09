package com.papernotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.papernotes.domain.model.MoodCategory
import com.papernotes.domain.model.Note
import com.papernotes.domain.model.NoteType

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val type: String,
    val mood: String,
    val dogEarFolded: Boolean,
    val pinned: Boolean,
    val archived: Boolean,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    body = body,
    type = NoteType.fromName(type),
    mood = MoodCategory.fromName(mood),
    dogEarFolded = dogEarFolded,
    pinned = pinned,
    archived = archived,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    type = type.name,
    mood = mood.name,
    dogEarFolded = dogEarFolded,
    pinned = pinned,
    archived = archived,
    deletedAt = deletedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
