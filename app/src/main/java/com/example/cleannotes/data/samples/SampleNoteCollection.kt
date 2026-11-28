package com.example.cleannotes.data.samples

import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.folderNameKey
import com.example.cleannotes.model.normalizedFolderName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
data class SampleNote(val title: String, val subtitle: String = "", val text: String)

private val sampleImportMutex = Mutex()

/** Additive import: existing notes and their metadata are never rewritten or moved. */
@Serializable
data class SampleNoteCollection(val folder: String, val notes: List<SampleNote>) {
	suspend fun addMissingTo(
		repository: NoteRepository,
		now: Long = System.currentTimeMillis(),
	): Int = sampleImportMutex.withLock {
		val name = normalizedFolderName(folder)
		require(notes.all { it.title.isNotBlank() && it.text.isNotBlank() })
		val existing = repository.observeNotes().first().filterNot { it.isReminder }
		val missing =
			notes.distinct().filterNot { sample ->
				existing.any { note ->
					val document = note.document
					note.title == sample.title &&
							document.text == sample.text &&
							document.subtitle == sample.subtitle
				}
			}
		if (missing.isEmpty()) return@withLock 0
		suspend fun existingFolder() =
			repository
				.observeFolders()
				.first()
				.firstOrNull { folderNameKey(it.name) == folderNameKey(name) }
				?.id

		val folderId =
			existingFolder()
				?: try {
					repository.createFolder(name)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					existingFolder() ?: throw e
				}
		missing.forEachIndexed { index, sample ->
			repository.saveNote(
				Note(
					title = sample.title,
					content = NoteDocument(text = sample.text, subtitle = sample.subtitle).encode(),
					timestamp = now + index,
					folderId = folderId,
				)
			)
		}
		missing.size
	}
}
