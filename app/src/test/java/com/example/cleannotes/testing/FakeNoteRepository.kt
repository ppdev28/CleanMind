package com.example.cleannotes.testing

import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.model.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class FakeNoteRepository : NoteRepository {
	val notes = linkedMapOf<Int, Note>()
	private val updates = MutableStateFlow<List<Note>>(emptyList())
	var writeDelay = 0L
	var failWrites = false
	var failReads = false
	var failDeletes = false
	var writes = 0
	var observers = 0

	fun publish() {
		updates.value =
			notes.values.sortedWith(
				compareByDescending<Note> { it.timestamp }.thenByDescending { it.id }
			)
	}

	override fun observeNotes(): Flow<List<Note>> = flow {
		observers++
		try {
			if (failReads) error("Read failed")
			emitAll(updates)
		} finally {
			observers--
		}
	}

	override suspend fun getNote(id: Int): Note? {
		if (failReads) error("Read failed")
		return notes[id]
	}

	override suspend fun saveNote(note: Note): Int {
		delay(writeDelay)
		if (failWrites) error("Storage unavailable")
		require(note.title.isNotBlank())
		val id = note.id ?: ((notes.keys.maxOrNull() ?: 0) + 1)
		notes[id] = note.copy(id = id)
		writes++
		publish()
		return id
	}

	override suspend fun deleteNote(note: Note) {
		if (failDeletes) error("Delete failed")
		notes.remove(note.id)
		publish()
	}
}
