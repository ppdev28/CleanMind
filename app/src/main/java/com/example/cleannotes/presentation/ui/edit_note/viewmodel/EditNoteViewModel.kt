package com.example.cleannotes.presentation.ui.edit_note.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent
import com.example.cleannotes.presentation.ui.edit_note.EditNoteUiState
import com.example.cleannotes.presentation.ui.edit_note.SaveStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel(assistedFactory = EditNoteViewModel.Factory::class)
class EditNoteViewModel
@AssistedInject
constructor(
	private val repository: NoteRepository,
	private val savedStateHandle: SavedStateHandle,
	@Assisted private val noteId: Int?,
	@Assisted private val reminder: Boolean,
) : ViewModel() {
	@AssistedFactory
	interface Factory {
		fun create(noteId: Int?, reminder: Boolean): EditNoteViewModel
	}

	private val _uiState =
		MutableStateFlow(
			EditNoteUiState(
				title = savedStateHandle["draftTitle"] ?: "",
				content = savedStateHandle["draftContent"] ?: "",
				isReminder = savedStateHandle["draftReminder"] ?: reminder,
				hasChanges = savedStateHandle["hasChanges"] ?: false,
			)
		)
	val uiState = _uiState.asStateFlow()
	private var currentNoteId: Int? = savedStateHandle["savedNoteId"] ?: noteId
	private var savedTitle = ""
	private var savedContent = ""
	private var debounceJob: Job? = null
	private val saveMutex = Mutex()

	init {
		currentNoteId?.let { id ->
			_uiState.update { it.copy(isLoading = true) }
			viewModelScope.launch {
				try {
					val note = repository.getNote(id) ?: error("Note not found")
					savedTitle = note.title
					savedContent = note.content
					val restoredDraft = savedStateHandle.contains("draftTitle")
					_uiState.update { state ->
						val title = if (restoredDraft) state.title else note.title
						val content = if (restoredDraft) state.content else note.content
						val changed = title != savedTitle || content != savedContent
						state.copy(
							title = title,
							content = content,
							isReminder = note.isReminder,
							hasChanges = changed,
							saveStatus = if (changed) SaveStatus.IDLE else SaveStatus.SAVED,
						)
					}
				} catch (e: CancellationException) {
					throw e
				} catch (_: Exception) {
					_uiState.update { it.copy(loadFailed = true, saveStatus = SaveStatus.ERROR) }
				} finally {
					_uiState.update { it.copy(isLoading = false) }
				}
			}
		}
	}

	fun setInitialFolder(folderId: Int?) {
		if (currentNoteId == null && !savedStateHandle.contains("initialFolder")) {
			savedStateHandle["initialFolder"] = folderId ?: 0
		}
	}

	fun onEvent(event: EditNoteEvent) {
		val state = uiState.value
		if (state.isLoading || state.loadFailed || state.isClosing || state.exitRequested) return
		val edited =
			when (event) {
				is EditNoteEvent.EnteredTitle -> state.copy(title = event.value)
				is EditNoteEvent.EnteredContent -> state.copy(content = event.value)
			}
		val changed = edited.title != savedTitle || edited.content != savedContent
		_uiState.value =
			edited.copy(
				hasChanges = changed,
				saveStatus =
					if (!changed && currentNoteId != null) SaveStatus.SAVED else SaveStatus.IDLE,
			)
		rememberDraft()
		debounceJob?.cancel()
		if (edited.isReminder) return
		debounceJob = viewModelScope.launch {
			delay(600)
			// Do not cancel a database write when the user types again.
			viewModelScope.launch { persist() }
		}
	}

	fun saveAndClose() {
		if (uiState.value.isClosing || uiState.value.exitRequested) return
		_uiState.update { it.copy(isClosing = true) }
		debounceJob?.cancel()
		viewModelScope.launch {
			try {
				// Always join an in-flight write, even if the user reverted the draft.
				if (!persist()) return@launch
				_uiState.update { it.copy(exitRequested = true) }
			} finally {
				_uiState.update { it.copy(isClosing = false) }
			}
		}
	}

	private suspend fun persist(): Boolean = saveMutex.withLock {
		val state = uiState.value
		if (state.isLoading || state.loadFailed) return@withLock false
		if (!state.hasChanges) return@withLock true
		val document = com.example.cleannotes.model.NoteDocument.decode(state.content)
		val fallback =
			listOf(
				document.subtitle,
				document.text.lineSequence().firstOrNull().orEmpty(),
				document.checklist.firstOrNull()?.text.orEmpty(),
			)
				.firstOrNull { it.isNotBlank() }
				?.take(80) ?: "Sin título"
		if (
			state.title.isBlank() &&
			document.text.isBlank() &&
			document.subtitle.isBlank() &&
			document.images.isEmpty() &&
			document.checklist.isEmpty()
		)
			return@withLock true
		_uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
		try {
			currentNoteId =
				repository.saveNote(
					Note(
						currentNoteId,
						state.title.ifBlank { fallback },
						state.content,
						System.currentTimeMillis(),
						state.isReminder,
						folderId = savedStateHandle.get<Int>("initialFolder")?.takeIf { it > 0 },
					)
				)
			savedStateHandle["savedNoteId"] = currentNoteId
			savedTitle = state.title
			savedContent = state.content
			_uiState.update {
				val changed = it.title != savedTitle || it.content != savedContent
				it.copy(
					hasChanges = changed,
					saveStatus = if (changed) SaveStatus.IDLE else SaveStatus.SAVED,
				)
			}
			rememberDraft()
			true
		} catch (e: CancellationException) {
			throw e
		} catch (_: Exception) {
			_uiState.update { it.copy(saveStatus = SaveStatus.ERROR) }
			false
		}
	}

	private fun rememberDraft() {
		val state = uiState.value
		savedStateHandle["draftTitle"] = state.title
		savedStateHandle["draftContent"] = state.content
		savedStateHandle["draftReminder"] = state.isReminder
		savedStateHandle["hasChanges"] = state.hasChanges
	}
}
