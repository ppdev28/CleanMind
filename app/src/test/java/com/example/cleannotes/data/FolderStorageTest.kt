package com.example.cleannotes.data

import android.app.Application
import androidx.room.Room
import com.example.cleannotes.data.repository.NoteRepositoryImpl
import com.example.cleannotes.data.source.NoteDatabase
import com.example.cleannotes.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class FolderStorageTest {
	@Test
	fun migrationFromVersionThreePreservesEveryNoteField() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val name = "folder-migration.db"
		context.deleteDatabase(name)
		context.openOrCreateDatabase(name, 0, null).use {
			it.execSQL(
				"CREATE TABLE note (id INTEGER, title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, isReminder INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))"
			)
			it.execSQL("INSERT INTO note VALUES (11, 'Idea', 'Texto con acentos: café', 123, 0)")
			it.execSQL("INSERT INTO note VALUES (12, 'Tarea', 'Aviso', 456, 1)")
			it.version = 3
		}
		val db =
			Room.databaseBuilder(context, NoteDatabase::class.java, name)
				.addMigrations(NoteDatabase.MIGRATION_3_4)
				.build()
		try {
			assertEquals(
				Note(11, "Idea", "Texto con acentos: café", 123),
				db.noteDao.getNoteById(11)!!.toNote(),
			)
			assertEquals(
				Note(12, "Tarea", "Aviso", 456, true),
				db.noteDao.getNoteById(12)!!.toNote(),
			)
			val repository = NoteRepositoryImpl(db.noteDao, context)
			val folder = repository.createFolder("Trabajo")
			repository.moveNote(11, folder)
			assertEquals(folder, repository.getNote(11)!!.folderId)
		} finally {
			db.close()
			context.deleteDatabase(name)
		}
	}

	@Test
	fun folderLifecycleKeepsNotesAndStaleSavesCannotUndoMoves() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
		try {
			val repo = NoteRepositoryImpl(db.noteDao, context)
			val folder = repo.createFolder("  Trabajo  ")
			val other = repo.createFolder("Personal")
			val id =
				repo.saveNote(
					Note(title = "Nota", content = "Cuerpo", timestamp = 123, folderId = folder)
				)
			val stale = repo.getNote(id)!!
			repo.moveNote(id, other)
			repo.saveNote(stale.copy(title = "Editada"))
			assertEquals(other, repo.getNote(id)!!.folderId)
			repo.renameFolder(other, "Ideas")
			assertEquals("Ideas", repo.observeFolders().first().first { it.id == other }.name)
			repo.deleteFolder(other)
			assertNull(repo.getNote(id)!!.folderId)
			assertEquals("Editada", repo.getNote(id)!!.title)
			repo.saveNote(stale.copy(title = "Tras eliminar"))
			assertNull(repo.getNote(id)!!.folderId)
			assertEquals(
				"Cuerpo",
				com.example.cleannotes.model.NoteDocument.plainText(repo.getNote(id)!!.content),
			)
		} finally {
			db.close()
		}
	}

	@Test
	fun duplicateAccentedNamesAreRejectedAndMissingFoldersFallBackSafely() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
		try {
			val repo = NoteRepositoryImpl(db.noteDao, context)
			repo.createFolder("Árbol")
			var rejected = false
			try {
				repo.createFolder("  ÁRBOL  ")
			} catch (_: android.database.sqlite.SQLiteConstraintException) {
				rejected = true
			}
			assertTrue(rejected)
			val id =
				repo.saveNote(
					Note(title = "Nueva", content = "Texto", timestamp = 1, folderId = 999)
				)
			assertNull(repo.getNote(id)!!.folderId)
			val reminder =
				repo.saveNote(
					Note(
						title = "Aviso",
						content = "",
						timestamp = 2,
						isReminder = true,
						folderId = repo.observeFolders().first().first().id,
					)
				)
			assertNull(repo.getNote(reminder)!!.folderId)
		} finally {
			db.close()
		}
	}
}
