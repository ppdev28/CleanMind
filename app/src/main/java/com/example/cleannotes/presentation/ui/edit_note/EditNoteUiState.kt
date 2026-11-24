package com.example.cleannotes.presentation.ui.edit_note

enum class SaveStatus {
	IDLE,
	SAVING,
	SAVED,
	ERROR,
}

data class EditNoteUiState(
	val title: String = "",
	val content: String = "",
	val isReminder: Boolean = false,
	val saveStatus: SaveStatus = SaveStatus.IDLE,
	val isLoading: Boolean = false,
	val isClosing: Boolean = false,
	val loadFailed: Boolean = false,
	val hasChanges: Boolean = false,
	val exitRequested: Boolean = false,
)
