package com.papernotes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [NoteEntity::class, NoteLinkEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class PaperNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /** v2: Notiz-Typ (Checkliste), Pinnen (Washi-Tape) und Papierkorb (deletedAt). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN type TEXT NOT NULL DEFAULT 'TEXT'")
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER")
            }
        }

        /** v3: Erinnerungszeit (Papier-Flattern). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
            }
        }

        /** v4: Verknüpfungstabelle ("roter Faden") zwischen Notizen. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_links` " +
                        "(`aId` INTEGER NOT NULL, `bId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`aId`, `bId`))",
                )
            }
        }
    }
}
