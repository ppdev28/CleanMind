package com.example.cleannotes.data.repository

import com.example.cleannotes.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
	fun observeNotes(): Flow<List<Note>>

	suspend fun getNote(id: Int): Note?

	/** Inserts or updates, returning the stable database ID. A title is required. */
	suspend fun saveNote(note: Note): Int

	suspend fun deleteNote(note: Note)
}
