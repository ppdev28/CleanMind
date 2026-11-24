package com.example.cleannotes.presentation.notebook

import com.example.cleannotes.model.Note

enum class NotesTab {
	HOME,
	NOTES,
	REMINDERS,
	SEARCH,
}

enum class NotesError {
	LOAD,
	DELETE,
	UPDATE,
}

data class NotesUiState(
	val allNotes: List<Note> = emptyList(),
	val notes: List<Note> = emptyList(),
	val reminders: List<Note> = emptyList(),
	val searchResults: List<Note> = emptyList(),
	val query: String = "",
	val selectedTab: NotesTab = NotesTab.HOME,
	val isLoading: Boolean = true,
	val error: NotesError? = null,
)
