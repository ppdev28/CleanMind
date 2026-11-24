package com.example.cleannotes.presentation.notebook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleannotes.data.repository.NoteRepository
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel
@Inject
constructor(
	private val repository: NoteRepository,
	private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
	private val selectedFolder = savedStateHandle.getStateFlow<Int?>("folder", null)
	private val folderOperation = MutableStateFlow(FolderOperation())
	private val query = savedStateHandle.getStateFlow("query", "")
	private val tab = savedStateHandle.getStateFlow("tab", NotesTab.HOME.name)
	private val reload = MutableStateFlow(0)
	private val actionError = MutableStateFlow<NotesError?>(null)
	private val data = reload.flatMapLatest {
		combine(repository.observeNotes(), repository.observeFolders()) { notes, folders ->
			NotesUiState(allNotes = notes, folders = folders, isLoading = false)
		}
			.onStart { emit(NotesUiState()) }
			.catch { error ->
				if (error is CancellationException) throw error
				emit(NotesUiState(isLoading = false, error = NotesError.LOAD))
			}
	}
	val uiState: StateFlow<NotesUiState> =
		combine(data, query, tab, selectedFolder, actionError) { state, query, tab, folderId, error
			->
			val selected = folderId?.takeIf {
				it == 0 || state.folders.any { folder -> folder.id == it }
			}
			val term = query.trim()
			state.copy(
				notes =
					state.allNotes
						.filterNot { it.isReminder }
						.filter {
							selected == null ||
									(if (selected == 0) it.folderId == null
									else it.folderId == selected)
						},
				selectedFolderId = selected,
				reminders =
					state.allNotes.filter { note ->
						NoteDocument.decode(note.content).let {
							(note.isReminder || it.reminder != null) && !it.reminderCompleted
						}
					},
				searchResults =
					if (term.isBlank()) emptyList()
					else
						state.allNotes.filter {
							it.title.contains(term, ignoreCase = true) ||
									NoteDocument.plainText(it.content)
										.contains(term, ignoreCase = true)
						},
				query = query,
				selectedTab = NotesTab.entries.firstOrNull { it.name == tab } ?: NotesTab.HOME,
				error = state.error ?: error,
			)
		}
			.combine(folderOperation) { state, operation ->
				state.copy(folderOperation = operation)
			}
			.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

	private val edits = Mutex()

	fun updateNote(note: Note, transform: (NoteDocument) -> NoteDocument) {
		viewModelScope.launch {
			try {
				edits.withLock {
					val latest = note.id?.let { repository.getNote(it) } ?: return@withLock
					val updated = transform(NoteDocument.decode(latest.content))
					repository.saveNote(latest.copy(content = updated.encode()))
				}
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				actionError.value = NotesError.UPDATE
			}
		}
	}

	fun selectFolder(id: Int?) {
		savedStateHandle["folder"] = id
	}

	fun clearFolderError() {
		folderOperation.update { it.copy(error = null) }
	}

	private fun folderAction(action: suspend () -> Unit) {
		if (folderOperation.value.busy) return
		folderOperation.update { it.copy(busy = true, error = null) }
		viewModelScope.launch {
			try {
				action()
				folderOperation.update { it.copy(busy = false, revision = it.revision + 1) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				val message =
					when (e) {
						is android.database.sqlite.SQLiteConstraintException ->
							"Ya existe una carpeta con ese nombre o la carpeta ya no está disponible."

						is IllegalArgumentException ->
							e.message ?: "Revisa el nombre de la carpeta."

						else -> "No se pudo realizar el cambio. Vuelve a intentarlo."
					}
				folderOperation.update { it.copy(busy = false, error = message) }
			}
		}
	}

	fun saveFolder(id: Int?, name: String) = folderAction {
		if (id == null) {
			val created = repository.createFolder(name)
			selectFolder(created)
			onTabSelected(NotesTab.NOTES)
		} else repository.renameFolder(id, name)
	}

	fun deleteFolder(id: Int) = folderAction {
		repository.deleteFolder(id)
		if (selectedFolder.value == id) selectFolder(null)
	}

	fun moveNote(note: Note, folderId: Int?) = folderAction {
		repository.moveNote(requireNotNull(note.id), folderId)
	}

	fun onQueryChange(value: String) {
		savedStateHandle["query"] = value
	}

	fun onTabSelected(value: NotesTab) {
		savedStateHandle["tab"] = value.name
	}

	fun retry() {
		actionError.value = null
		reload.update { it + 1 }
	}

	fun dismissError() {
		actionError.value = null
	}

	fun deleteNote(note: Note) {
		viewModelScope.launch {
			try {
				repository.deleteNote(note)
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				actionError.value = NotesError.DELETE
			}
		}
	}
}
