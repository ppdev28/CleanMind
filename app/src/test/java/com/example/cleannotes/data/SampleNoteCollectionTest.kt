package com.example.cleannotes.data

import com.example.cleannotes.data.samples.SampleNoteCollection
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.NoteFolder
import com.example.cleannotes.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SampleNoteCollectionTest {
	private fun samples() =
		javaClass.getResourceAsStream("/science-notes.json")!!.bufferedReader().use {
			Json.decodeFromString<SampleNoteCollection>(it.readText())
		}

	@Test
	fun sixBriefStanzasAreAddedOnceWithoutChangingExistingNotes() = runTest {
		val collection = samples()
		assertEquals(6, collection.notes.size)
		collection.notes.forEach {
			assertEquals(4, it.text.lines().size)
			assertTrue(it.text.split(Regex("\\s+")).size <= 35)
		}
		val repository = FakeNoteRepository()
		val original = Note(9, "Personal", "Mi texto", 123)
		repository.notes[9] = original
		repository.publish()
		assertEquals(6, collection.addMissingTo(repository, 1000))
		assertEquals(0, collection.addMissingTo(repository, 2000))
		assertEquals(original, repository.getNote(9))
		assertEquals(7, repository.notes.size)
		assertEquals("Ciencia", repository.folders.value.single().name)
		assertTrue(
			repository.notes.values
				.filter { it.id != 9 }
				.all { it.folderId == repository.folders.value.single().id && !it.isReminder }
		)
	}

	@Test
	fun anInterruptedImportCanResumeWithoutDuplicatingCompletedNotes() = runTest {
		val repository = FakeNoteRepository()
		repository.failOnWrite = 3
		var failed = false
		try {
			samples().addMissingTo(repository)
		} catch (_: IllegalStateException) {
			failed = true
		}
		assertTrue(failed)
		assertEquals(2, repository.notes.size)
		repository.failOnWrite = null
		assertEquals(4, samples().addMissingTo(repository))
		assertEquals(6, repository.notes.size)
		assertEquals(1, repository.folders.value.size)
	}

	@Test
	fun matchingNotesKeepTheirFolderAndMetadataAndAnExistingScienceFolderIsReused() = runTest {
		val collection = samples()
		val first = collection.notes.first()
		val original =
			Note(
				1,
				first.title,
				NoteDocument(
					text = first.text,
					subtitle = first.subtitle,
					pinned = true,
					createdAt = 42,
				)
					.encode(),
				123,
				folderId = 8,
			)
		val repository = FakeNoteRepository()
		repository.folders.value = listOf(NoteFolder(7, "CIENCIA"), NoteFolder(8, "Personal"))
		repository.notes[1] = original
		repository.publish()
		assertEquals(5, collection.addMissingTo(repository))
		assertEquals(original, repository.getNote(1))
		assertEquals(2, repository.folders.value.size)
		assertTrue(repository.notes.values.filter { it.id != 1 }.all { it.folderId == 7 })
	}
}
