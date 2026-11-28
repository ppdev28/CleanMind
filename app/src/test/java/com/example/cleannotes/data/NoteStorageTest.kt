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
class NoteStorageTest {
	@Test
	fun largeBatchDeletionRollsBackEveryChunkIfAnyDeleteFails() = runBlocking {
		val database =
			Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), NoteDatabase::class.java)
				.build()
		try {
			val sql = database.openHelper.writableDatabase
			sql.execSQL("WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<903) INSERT INTO note(id,title,content,timestamp,isReminder) SELECT x, 'Nota', 'Original', x, 0 FROM n")
			sql.execSQL("CREATE TRIGGER prevent_delete BEFORE DELETE ON note WHEN OLD.id=902 BEGIN SELECT RAISE(ABORT, 'Unavailable'); END")
			var failed = false
			try {
				database.noteDao.deleteNotes((1..902).toList())
			} catch (_: Exception) {
				failed = true
			}
			assertTrue(failed)
			assertEquals(903, database.noteDao.observeNotes().first().size)
			sql.execSQL("DROP TRIGGER prevent_delete")
			database.noteDao.deleteNotes((1..902).toList())
			assertEquals(listOf(903), database.noteDao.observeNotes().first().map { it.id })
		} finally {
			database.close()
		}
	}

	@Test
	fun batchDeletionRemovesOnlySelectedNotesAndCancelsTheirAlarms() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val database = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
		try {
			val repository = NoteRepositoryImpl(database.noteDao, context)
			val plain = repository.saveNote(Note(title = "Nota", content = "Texto", timestamp = 1))
			val config = com.example.cleannotes.model.ReminderConfig(System.currentTimeMillis() + 60_000)
			val reminder = repository.saveNote(
				Note(
					title = "Aviso",
					content = com.example.cleannotes.model.NoteDocument(reminder = config).encode(),
					timestamp = 2,
					isReminder = true
				)
			)
			val keep = repository.saveNote(Note(title = "Conservar", content = "Original", timestamp = 3))
			val alarms =
				org.robolectric.Shadows.shadowOf(context.getSystemService(android.app.AlarmManager::class.java))
			assertEquals(1, alarms.scheduledAlarms.size)
			repository.deleteNotes(setOf(plain, reminder))
			assertEquals(listOf(keep), repository.observeNotes().first().map { it.id })
			assertEquals(0, alarms.scheduledAlarms.size)
			assertFalse(
				context.getSharedPreferences("scheduled_reminders", 0).contains("config_$reminder")
			)
		} finally {
			database.close()
		}
	}

	@Test
	fun versionTwoMigrationPreservesIdsTimestampsAndReminderMeaning() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val name = "migration-test.db"
		context.deleteDatabase(name)
		context.openOrCreateDatabase(name, 0, null).use { legacy ->
			legacy.execSQL(
				"CREATE TABLE note (id INTEGER, title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(id))"
			)
			legacy.execSQL("INSERT INTO note VALUES (3, 'Idea', 'Texto conservado', 123)")
			legacy.execSQL("INSERT INTO note VALUES (7, 'Llamar', 'Remember', 456)")
			legacy.version = 2
		}
		val database =
			Room.databaseBuilder(context, NoteDatabase::class.java, name)
				.addMigrations(NoteDatabase.MIGRATION_2_3, NoteDatabase.MIGRATION_3_4)
				.build()
		try {
			val repository =
				NoteRepositoryImpl(
					database.noteDao,
					org.robolectric.RuntimeEnvironment.getApplication(),
				)
			val notes = repository.observeNotes().first()
			assertEquals(listOf(7, 3), notes.map { it.id })
			assertEquals(456L, notes[0].timestamp)
			assertTrue(notes[0].isReminder)
			assertEquals("", notes[0].content)
			assertFalse(notes[1].isReminder)
			assertEquals("Texto conservado", notes[1].content)
			val id = repository.saveNote(notes[0].copy(title = "Actualizado"))
			assertEquals(7, id)
			assertEquals(2, repository.observeNotes().first().size)
		} finally {
			database.close()
			context.deleteDatabase(name)
		}
	}

	@Test
	fun repositoryReturnsStableIdsAndUsesDatabaseOrdering() = runBlocking {
		val database =
			Room.inMemoryDatabaseBuilder(
				RuntimeEnvironment.getApplication(),
				NoteDatabase::class.java,
			)
				.build()
		try {
			val repository =
				NoteRepositoryImpl(
					database.noteDao,
					org.robolectric.RuntimeEnvironment.getApplication(),
				)
			val first =
				repository.saveNote(Note(title = "Primera", content = "Remember", timestamp = 1))
			val second =
				repository.saveNote(
					Note(title = "Segunda", content = "", timestamp = 2, isReminder = true)
				)
			assertNotEquals(first, second)
			assertEquals(listOf(second, first), repository.observeNotes().first().map { it.id })
			assertFalse(repository.getNote(first)!!.isReminder)
			repository.saveNote(repository.getNote(first)!!.copy(content = "Actualizada"))
			assertEquals(2, repository.observeNotes().first().size)
			repository.deleteNote(repository.getNote(second)!!)
			assertNull(repository.getNote(second))
		} finally {
			database.close()
		}
	}

	@Test
	fun creationDateStaysFixedWhenTheNoteIsEditedLater() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val database = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
		try {
			val repository = NoteRepositoryImpl(database.noteDao, context)
			val id =
				repository.saveNote(Note(title = "Agenda", content = "Original", timestamp = 100))
			val first = repository.getNote(id)!!
			assertEquals(
				100L,
				com.example.cleannotes.model.NoteDocument.decode(first.content).createdAt,
			)
			repository.saveNote(
				first.copy(content = "Editada al día siguiente", timestamp = 86_400_100)
			)
			val updated = repository.getNote(id)!!
			assertEquals(
				100L,
				com.example.cleannotes.model.NoteDocument.decode(updated.content).createdAt,
			)
			assertEquals(86_400_100L, updated.timestamp)
			assertEquals(
				"Editada al día siguiente",
				com.example.cleannotes.model.NoteDocument.plainText(updated.content),
			)
		} finally {
			database.close()
		}
	}

	@Test
	fun legacyCreationDateUsesThePreviouslyStoredTimestamp() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val database = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
		try {
			database.noteDao.insertNote(
				com.example.cleannotes.data.source.NoteEntity(
					7,
					"Antigua",
					"Sin metadatos",
					100,
					false,
				)
			)
			val repository = NoteRepositoryImpl(database.noteDao, context)
			repository.saveNote(
				repository.getNote(7)!!.copy(content = "Actualizada", timestamp = 86_400_100)
			)
			assertEquals(
				100L,
				com.example.cleannotes.model.NoteDocument.decode(repository.getNote(7)!!.content)
					.createdAt,
			)
		} finally {
			database.close()
		}
	}
}
