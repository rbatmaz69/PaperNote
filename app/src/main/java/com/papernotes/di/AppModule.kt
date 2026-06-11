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
            .addMigrations(
                PaperNotesDatabase.MIGRATION_1_2,
                PaperNotesDatabase.MIGRATION_2_3,
                PaperNotesDatabase.MIGRATION_3_4,
                PaperNotesDatabase.MIGRATION_4_5,
                PaperNotesDatabase.MIGRATION_5_6,
                PaperNotesDatabase.MIGRATION_6_7,
                PaperNotesDatabase.MIGRATION_7_8,
                PaperNotesDatabase.MIGRATION_8_9,
                PaperNotesDatabase.MIGRATION_9_10,
                PaperNotesDatabase.MIGRATION_10_11,
                PaperNotesDatabase.MIGRATION_11_12,
                PaperNotesDatabase.MIGRATION_12_13,
            )
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
