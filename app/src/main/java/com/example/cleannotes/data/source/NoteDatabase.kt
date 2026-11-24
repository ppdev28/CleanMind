package com.example.cleannotes.data.source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NoteEntity::class, FolderEntity::class], version = 4, exportSchema = true)
abstract class NoteDatabase : RoomDatabase() {
	abstract val noteDao: NoteDao

	companion object {
		const val DATABASE_NAME = "notes_db"
		val MIGRATION_3_4 =
			object : Migration(3, 4) {
				override fun migrate(db: SupportSQLiteDatabase) {
					db.execSQL(
						"CREATE TABLE IF NOT EXISTS folder (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, nameKey TEXT NOT NULL)"
					)
					db.execSQL(
						"CREATE UNIQUE INDEX IF NOT EXISTS index_folder_nameKey ON folder (nameKey)"
					)
					db.execSQL(
						"CREATE TABLE note_new (id INTEGER, title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, isReminder INTEGER NOT NULL DEFAULT 0, folderId INTEGER, PRIMARY KEY(id), FOREIGN KEY(folderId) REFERENCES folder(id) ON UPDATE NO ACTION ON DELETE SET NULL)"
					)
					db.execSQL(
						"INSERT INTO note_new (id, title, content, timestamp, isReminder) SELECT id, title, content, timestamp, isReminder FROM note"
					)
					db.execSQL("DROP TABLE note")
					db.execSQL("ALTER TABLE note_new RENAME TO note")
					db.execSQL("CREATE INDEX index_note_folderId ON note (folderId)")
				}
			}

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
