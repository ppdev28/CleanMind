package com.example.cleannotes.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.notebook.*
import com.example.cleannotes.testing.FakeNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {
	private val dispatcher = StandardTestDispatcher()
	private val repository = FakeNoteRepository()

	@Before
	fun setUp() {
		Dispatchers.setMain(dispatcher)
	}

	@After
	fun tearDown() {
		Dispatchers.resetMain()
	}

	@Test
	fun removingArchiveKeepsLegacyNotesVisibleWithoutRewritingThem() {
		val note = Note(
			1,
			"Anterior",
			com.example.cleannotes.model.NoteDocument(text = "Original", archived = true).encode(),
			1
		)
		for (tab in listOf(NotesTab.HOME, NotesTab.NOTES, NotesTab.SEARCH)) {
			assertEquals(
				listOf(note),
				visibleNotes(NotesUiState(allNotes = listOf(note), selectedTab = tab), "Todas")
			)
		}
		assertTrue(com.example.cleannotes.model.NoteDocument.decode(note.content).archived)
	}

	@Test
	fun batchDeleteKeepsTheSelectionOnFailureAndCanBeRetried() = runTest(dispatcher) {
		repository.notes[1] = Note(1, "Nota", "Texto", 1)
		repository.notes[2] = Note(2, "Aviso", "", 2, true)
		repository.notes[3] = Note(3, "Conservar", "Original", 3)
		repository.publish()
		val vm = NotesViewModel(repository, SavedStateHandle())
		backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
		runCurrent()
		repository.failDeletes = true
		vm.deleteNotes(setOf(1, 2)); runCurrent()
		assertEquals(3, vm.uiState.value.allNotes.size)
		assertNotNull(vm.uiState.value.deletion.error)
		assertEquals(0, vm.uiState.value.deletion.revision)
		assertFalse(vm.uiState.value.deletion.busy)
		repository.failDeletes = false; repository.writeDelay = 100
		vm.deleteNotes(setOf(1, 2)); runCurrent()
		assertTrue(vm.uiState.value.deletion.busy)
		vm.deleteNotes(setOf(3)) // Ignore a second submission while the first is pending.
		advanceTimeBy(101); runCurrent()
		assertEquals(listOf(3), vm.uiState.value.allNotes.map { it.id })
		assertNull(vm.uiState.value.deletion.error)
		assertEquals(1, vm.uiState.value.deletion.revision)
	}

	@Test
	fun filtersByExplicitTypeAndSearchesContent() =
		runTest(dispatcher) {
			repository.notes[1] = Note(1, "Viaje", "Remember", 10)
			repository.notes[2] = Note(2, "Llamar", "", 20, true)
			repository.publish()
			val viewModel = NotesViewModel(repository, SavedStateHandle())
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiState.collect()
			}
			runCurrent()
			assertFalse(viewModel.uiState.value.isLoading)
			assertEquals(listOf(1), viewModel.uiState.value.notes.map { it.id })
			assertEquals(listOf(2), viewModel.uiState.value.reminders.map { it.id })
			viewModel.onQueryChange("  REMEMBER  ")
			runCurrent()
			assertEquals(listOf(1), viewModel.uiState.value.searchResults.map { it.id })
		}

	@Test
	fun loadingFailureCanBeRetried() =
		runTest(dispatcher) {
			repository.failReads = true
			val viewModel = NotesViewModel(repository, SavedStateHandle())
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiState.collect()
			}
			runCurrent()
			assertEquals(NotesError.LOAD, viewModel.uiState.value.error)
			repository.failReads = false
			viewModel.retry()
			runCurrent()
			assertNull(viewModel.uiState.value.error)
			assertFalse(viewModel.uiState.value.isLoading)
		}

	@Test
	fun failedDeleteKeepsTheNoteAndExposesAnError() =
		runTest(dispatcher) {
			val note = Note(1, "No borrar", "", 0)
			repository.notes[1] = note
			repository.publish()
			repository.failDeletes = true
			val viewModel = NotesViewModel(repository, SavedStateHandle())
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiState.collect()
			}
			runCurrent()
			viewModel.deleteNote(note)
			runCurrent()
			assertEquals(NotesError.DELETE, viewModel.uiState.value.error)
			assertEquals(1, viewModel.uiState.value.allNotes.size)
			viewModel.dismissError()
			runCurrent()
			assertNull(viewModel.uiState.value.error)
		}

	@Test
	fun observationStopsWhenTheScreenHasNoSubscribers() =
		runTest(dispatcher) {
			val viewModel = NotesViewModel(repository, SavedStateHandle())
			val subscription =
				backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
					viewModel.uiState.collect()
				}
			runCurrent()
			assertEquals(1, repository.observers)
			subscription.cancel()
			advanceTimeBy(5_001)
			runCurrent()
			assertEquals(0, repository.observers)
		}

	@Test
	fun queryAndSelectedTabAreRestored() =
		runTest(dispatcher) {
			val viewModel =
				NotesViewModel(
					repository,
					SavedStateHandle(mapOf("query" to "viaje", "tab" to NotesTab.SEARCH.name)),
				)
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiState.collect()
			}
			runCurrent()
			assertEquals("viaje", viewModel.uiState.value.query)
			assertEquals(NotesTab.SEARCH, viewModel.uiState.value.selectedTab)
		}

	@Test
	fun simultaneousMetadataChangesUseLatestStoredDocument() =
		runTest(dispatcher) {
			val note =
				Note(1, "Paseo", com.example.cleannotes.model.NoteDocument("Texto").encode(), 1)
			repository.notes[1] = note
			repository.writeDelay = 100
			val viewModel = NotesViewModel(repository, SavedStateHandle())
			viewModel.updateNote(note) { it.copy(pinned = true) }
			viewModel.updateNote(note) { it.copy(archived = true) }
			advanceUntilIdle()
			val saved =
				com.example.cleannotes.model.NoteDocument.decode(repository.notes[1]!!.content)
			assertTrue(saved.pinned)
			assertTrue(saved.archived)
			assertEquals("Texto", saved.text)
			assertEquals(1, repository.notes.size)
		}

	@Test
	fun failedMetadataChangeKeepsOriginalAndExposesError() =
		runTest(dispatcher) {
			val note = Note(1, "Paseo", "Texto", 1)
			repository.notes[1] = note
			repository.publish()
			repository.failWrites = true
			val viewModel = NotesViewModel(repository, SavedStateHandle())
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
				viewModel.uiState.collect()
			}
			viewModel.updateNote(note) { it.copy(pinned = true) }
			runCurrent()
			assertEquals(NotesError.UPDATE, viewModel.uiState.value.error)
			assertEquals("Texto", repository.notes[1]!!.content)
		}

	@Test
	fun folderSelectionIsRestoredAndCreationSelectsTheNewFolder() =
		runTest(dispatcher) {
			repository.folders.value = listOf(com.example.cleannotes.model.NoteFolder(4, "Trabajo"))
			repository.notes[1] = Note(1, "Dentro", "", 10, folderId = 4)
			repository.notes[2] = Note(2, "Fuera", "", 20)
			repository.publish()
			val vm = NotesViewModel(repository, SavedStateHandle(mapOf("folder" to 4)))
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
			runCurrent()
			assertEquals(listOf(1), vm.uiState.value.notes.map { it.id })
			vm.selectFolder(0)
			runCurrent()
			assertEquals(listOf(2), vm.uiState.value.notes.map { it.id })
			vm.saveFolder(null, "Personal")
			runCurrent()
			assertEquals(
				"Personal",
				vm.uiState.value.folders.first { it.id == vm.uiState.value.selectedFolderId }.name,
			)
			assertEquals(NotesTab.NOTES, vm.uiState.value.selectedTab)
			assertEquals(1, vm.uiState.value.folderOperation.revision)
			vm.deleteFolder(vm.uiState.value.selectedFolderId!!)
			runCurrent()
			assertNull(vm.uiState.value.selectedFolderId)
		}

	@Test
	fun failedFolderMoveKeepsMembershipAndCanBeRetried() =
		runTest(dispatcher) {
			repository.folders.value = listOf(com.example.cleannotes.model.NoteFolder(4, "Trabajo"))
			val note = Note(1, "Idea", "", 10)
			repository.notes[1] = note
			repository.publish()
			val vm = NotesViewModel(repository, SavedStateHandle())
			backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
			repository.failWrites = true
			vm.moveNote(note, 4)
			runCurrent()
			assertNotNull(vm.uiState.value.folderOperation.error)
			assertFalse(vm.uiState.value.folderOperation.busy)
			assertNull(repository.notes[1]!!.folderId)
			assertEquals(0, vm.uiState.value.folderOperation.revision)
			repository.failWrites = false
			vm.moveNote(note, 4)
			runCurrent()
			assertNull(vm.uiState.value.folderOperation.error)
			assertEquals(4, repository.notes[1]!!.folderId)
		}
}
