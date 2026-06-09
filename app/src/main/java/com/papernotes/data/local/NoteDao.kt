package com.papernotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query(
        "SELECT * FROM notes WHERE archived = 0 AND deletedAt IS NULL " +
            "ORDER BY pinned DESC, updatedAt DESC",
    )
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE archived = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes WHERE createdAt >= :since AND deletedAt IS NULL")
    fun countCreatedSince(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Query("UPDATE notes SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET dogEarFolded = :folded, mood = :mood, updatedAt = :now WHERE id = :id")
    suspend fun setDogEar(id: Long, folded: Boolean, mood: String, now: Long)

    @Query("UPDATE notes SET deletedAt = :now WHERE id = :id")
    suspend fun moveToTrash(id: Long, now: Long)

    @Query("UPDATE notes SET deletedAt = NULL, archived = 0, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrash(cutoff: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}
