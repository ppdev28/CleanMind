package com.example.cleannotes.presentation.notebook

import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteFolder

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
	val folders: List<NoteFolder> = emptyList(),
	val selectedFolderId: Int? = null,
	val folderOperation: FolderOperation = FolderOperation(),
	val deletion: NoteDeletion = NoteDeletion(),
	val notes: List<Note> = emptyList(),
	val reminders: List<Note> = emptyList(),
	val searchResults: List<Note> = emptyList(),
	val query: String = "",
	val selectedTab: NotesTab = NotesTab.HOME,
	val isLoading: Boolean = true,
	val error: NotesError? = null,
)

/** Each successful mutation advances revision, allowing sheets to close only after persistence. */
data class FolderOperation(
	val busy: Boolean = false,
	val error: String? = null,
	val revision: Int = 0,
)

/** Selection is cleared only after a confirmed successful batch deletion. */
data class NoteDeletion(val busy: Boolean = false, val error: String? = null, val revision: Int = 0)
