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
    @Test fun versionTwoMigrationPreservesIdsTimestampsAndReminderMeaning() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, 0, null).use { legacy ->
            legacy.execSQL("CREATE TABLE note (id INTEGER, title TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(id))")
            legacy.execSQL("INSERT INTO note VALUES (3, 'Idea', 'Texto conservado', 123)")
            legacy.execSQL("INSERT INTO note VALUES (7, 'Llamar', 'Remember', 456)")
            legacy.version = 2
        }
        val database = Room.databaseBuilder(context, NoteDatabase::class.java, name).addMigrations(NoteDatabase.MIGRATION_2_3).build()
        try {
            val repository = NoteRepositoryImpl(database.noteDao, org.robolectric.RuntimeEnvironment.getApplication())
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
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun repositoryReturnsStableIdsAndUsesDatabaseOrdering() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), NoteDatabase::class.java).build()
        try {
            val repository = NoteRepositoryImpl(database.noteDao, org.robolectric.RuntimeEnvironment.getApplication())
            val first = repository.saveNote(Note(title = "Primera", content = "Remember", timestamp = 1))
            val second = repository.saveNote(Note(title = "Segunda", content = "", timestamp = 2, isReminder = true))
            assertNotEquals(first, second)
            assertEquals(listOf(second, first), repository.observeNotes().first().map { it.id })
            assertFalse(repository.getNote(first)!!.isReminder)
            repository.saveNote(repository.getNote(first)!!.copy(content = "Actualizada"))
            assertEquals(2, repository.observeNotes().first().size)
            repository.deleteNote(repository.getNote(second)!!)
            assertNull(repository.getNote(second))
        } finally { database.close() }
    }
}
