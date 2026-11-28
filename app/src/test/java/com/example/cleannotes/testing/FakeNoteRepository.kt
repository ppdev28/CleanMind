package com.example.cleannotes.testing

import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.model.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class FakeNoteRepository : NoteRepository {
	val notes = linkedMapOf<Int, Note>()
	val folders = MutableStateFlow<List<com.example.cleannotes.model.NoteFolder>>(emptyList())
	private val updates = MutableStateFlow<List<Note>>(emptyList())
	var writeDelay = 0L
	var failWrites = false
	var failOnWrite: Int? = null
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
		if (failWrites || writes + 1 == failOnWrite) error("Storage unavailable")
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

	override suspend fun deleteNotes(ids: Set<Int>) {
		delay(writeDelay)
		if (failDeletes) error("Delete failed")
		ids.forEach { notes.remove(it) }
		publish()
	}

	override fun observeFolders() = folders

	override suspend fun createFolder(name: String): Int {
		if (failWrites) error("Folder write failed")
		val normalized = com.example.cleannotes.model.normalizedFolderName(name)
		require(
			folders.value.none {
				com.example.cleannotes.model.folderNameKey(it.name) ==
						com.example.cleannotes.model.folderNameKey(normalized)
			}
		)
		val id = (folders.value.maxOfOrNull { it.id } ?: 0) + 1
		folders.value += com.example.cleannotes.model.NoteFolder(id, normalized)
		return id
	}

	override suspend fun renameFolder(id: Int, name: String) {
		if (failWrites) error("Folder write failed")
		folders.value = folders.value.map { if (it.id == id) it.copy(name = name) else it }
	}

	override suspend fun deleteFolder(id: Int) {
		if (failDeletes) error("Delete failed")
		folders.value = folders.value.filterNot { it.id == id }
		notes.replaceAll { _, note ->
			if (note.folderId == id) note.copy(folderId = null) else note
		}
		publish()
	}

	override suspend fun moveNote(id: Int, folderId: Int?) {
		if (failWrites) error("Move failed")
		notes[id] = notes.getValue(id).copy(folderId = folderId)
		publish()
	}
}
