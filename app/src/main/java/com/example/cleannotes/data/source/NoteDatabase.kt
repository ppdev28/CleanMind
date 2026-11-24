package com.example.cleannotes.data.source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class], version = 3, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
	abstract val noteDao: NoteDao

	companion object {
		const val DATABASE_NAME = "notes_db"
		val MIGRATION_2_3 =
			object : Migration(2, 3) {
				override fun migrate(db: SupportSQLiteDatabase) {
					db.execSQL("ALTER TABLE note ADD COLUMN isReminder INTEGER NOT NULL DEFAULT 0")
					// Preserve the meaning of reminders created by the previous app version.
					db.execSQL(
						"UPDATE note SET isReminder = 1, content = '' WHERE content = 'Remember'"
					)
				}
			}
	}
}
