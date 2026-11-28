package com.example.cleannotes.data.repository

import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteFolder
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
	fun observeNotes(): Flow<List<Note>>

	suspend fun getNote(id: Int): Note?

	/** Inserts or updates, returning the stable database ID. A title is required. */
	suspend fun saveNote(note: Note): Int

	/** Deletes the selection atomically and cancels its scheduled notifications. */
	suspend fun deleteNotes(ids: Set<Int>)

	fun observeFolders(): Flow<List<NoteFolder>>

	suspend fun createFolder(name: String): Int

	suspend fun renameFolder(id: Int, name: String)

	/** Removes the folder, keeping every note without a folder. */
	suspend fun deleteFolder(id: Int)

	suspend fun moveNote(id: Int, folderId: Int?)
}
