package com.papernotes.di

import android.content.Context
import androidx.room.Room
import com.papernotes.data.local.NoteDao
import com.papernotes.data.local.PaperNotesDatabase
import com.papernotes.data.repository.NoteRepository
import com.papernotes.data.repository.NoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PaperNotesDatabase =
        Room.databaseBuilder(context, PaperNotesDatabase::class.java, "paper_notes.db")
            .addMigrations(PaperNotesDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideNoteDao(db: PaperNotesDatabase): NoteDao = db.noteDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
}
