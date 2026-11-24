package com.example.cleannotes.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent
import com.example.cleannotes.presentation.ui.edit_note.SaveStatus
import com.example.cleannotes.presentation.ui.edit_note.viewmodel.EditNoteViewModel
import com.example.cleannotes.testing.FakeNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditNoteViewModelTest {
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

	private fun editor(
		state: SavedStateHandle = SavedStateHandle(),
		noteId: Int? = null,
		reminder: Boolean = false,
	) = EditNoteViewModel(repository, state, noteId, reminder)

	@Test
	fun repeatedAutosavesUpdateTheInsertedNote() =
		runTest(dispatcher) {
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("Una idea"))
			advanceUntilIdle()
			editor.onEvent(EditNoteEvent.EnteredContent("Primer párrafo"))
			advanceUntilIdle()
			editor.onEvent(EditNoteEvent.EnteredContent("Segundo párrafo"))
			advanceUntilIdle()
			assertEquals(1, repository.notes.size)
			assertEquals("Segundo párrafo", repository.notes.values.single().content)
			assertEquals(SaveStatus.SAVED, editor.uiState.value.saveStatus)
		}

	@Test
	fun closingImmediatelyFlushesThePendingEdit() =
		runTest(dispatcher) {
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("Antes de salir"))
			editor.saveAndClose()
			advanceUntilIdle()
			assertTrue(editor.uiState.value.exitRequested)
			assertEquals("Antes de salir", repository.notes.values.single().title)
		}

	@Test
	fun typingDuringASlowInsertDoesNotCreateDuplicates() =
		runTest(dispatcher) {
			repository.writeDelay = 1_000
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("Borrador"))
			advanceTimeBy(601)
			runCurrent()
			assertEquals(SaveStatus.SAVING, editor.uiState.value.saveStatus)
			editor.onEvent(EditNoteEvent.EnteredTitle("Idea terminada"))
			advanceUntilIdle()
			assertEquals(1, repository.notes.size)
			assertEquals("Idea terminada", repository.notes.values.single().title)
			assertFalse(editor.uiState.value.hasChanges)
		}

	@Test
	fun failedSaveKeepsTheDraftAndDoesNotClose() =
		runTest(dispatcher) {
			repository.failWrites = true
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("No perder"))
			editor.saveAndClose()
			advanceUntilIdle()
			assertFalse(editor.uiState.value.exitRequested)
			assertTrue(editor.uiState.value.hasChanges)
			assertEquals(SaveStatus.ERROR, editor.uiState.value.saveStatus)
			repository.failWrites = false
			editor.saveAndClose()
			advanceUntilIdle()
			assertTrue(editor.uiState.value.exitRequested)
			assertEquals("No perder", repository.notes.values.single().title)
		}

	@Test
	fun editingAReminderPreservesItsTypeWithoutDisplayingTheMarker() =
		runTest(dispatcher) {
			repository.notes[7] = Note(7, "Comprar pan", "", 0, isReminder = true)
			val editor = editor(noteId = 7)
			advanceUntilIdle()
			assertTrue(editor.uiState.value.isReminder)
			assertEquals("", editor.uiState.value.content)
			editor.onEvent(EditNoteEvent.EnteredTitle("Comprar leche"))
			advanceUntilIdle()
			assertEquals("Comprar pan", repository.notes[7]?.title)
			editor.saveAndClose()
			advanceUntilIdle()
			assertEquals(1, repository.notes.size)
			assertTrue(repository.notes[7]?.isReminder == true)
			assertEquals("Comprar leche", repository.notes[7]?.title)
		}

	@Test
	fun repeatedBackPressesCloseOnlyOnce() =
		runTest(dispatcher) {
			repository.writeDelay = 1_000
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("Una sola salida"))
			editor.saveAndClose()
			editor.saveAndClose()
			advanceUntilIdle()
			assertTrue(editor.uiState.value.exitRequested)
			assertEquals(1, repository.writes)
			assertEquals(1, repository.notes.size)
		}

	@Test
	fun revertingToSavedTextShowsSavedStatus() =
		runTest(dispatcher) {
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("Original"))
			advanceUntilIdle()
			editor.onEvent(EditNoteEvent.EnteredTitle("Cambio temporal"))
			editor.onEvent(EditNoteEvent.EnteredTitle("Original"))
			assertFalse(editor.uiState.value.hasChanges)
			assertEquals(SaveStatus.SAVED, editor.uiState.value.saveStatus)
			advanceUntilIdle()
			assertEquals(1, repository.notes.size)
		}

	@Test
	fun optionalTitleUsesBodyAsFallbackWithoutLosingContent() =
		runTest(dispatcher) {
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredContent("Una idea sin título"))
			editor.saveAndClose()
			advanceUntilIdle()
			assertTrue(editor.uiState.value.exitRequested)
			assertFalse(editor.uiState.value.hasChanges)
			assertEquals("Una idea sin título", repository.notes.values.single().title)
			assertEquals("Una idea sin título", repository.notes.values.single().content)
		}

	@Test
	fun restoredDraftRetainsItsTextAndUpdatesTheSameNote() =
		runTest(dispatcher) {
			repository.notes[7] = Note(7, "Original", "Contenido", 0)
			val state =
				SavedStateHandle(
					mapOf(
						"savedNoteId" to 7,
						"draftTitle" to "Borrador restaurado",
						"draftContent" to "Texto nuevo",
						"hasChanges" to true,
					)
				)
			val editor = editor(state)
			advanceUntilIdle()
			assertEquals("Borrador restaurado", editor.uiState.value.title)
			assertTrue(editor.uiState.value.hasChanges)
			editor.saveAndClose()
			advanceUntilIdle()
			assertEquals(1, repository.notes.size)
			assertEquals("Texto nuevo", repository.notes[7]?.content)
		}

	@Test
	fun literalRememberTextIsAnOrdinaryNote() =
		runTest(dispatcher) {
			val editor = editor()
			editor.onEvent(EditNoteEvent.EnteredTitle("Una palabra"))
			editor.onEvent(EditNoteEvent.EnteredContent("Remember"))
			advanceUntilIdle()
			assertFalse(repository.notes.values.single().isReminder)
			assertEquals("Remember", repository.notes.values.single().content)
		}

	@Test
	fun failedLoadCannotOverwriteAnExistingNote() =
		runTest(dispatcher) {
			repository.failReads = true
			val editor = editor(noteId = 7)
			advanceUntilIdle()
			editor.onEvent(EditNoteEvent.EnteredTitle("No sobrescribir"))
			editor.saveAndClose()
			advanceUntilIdle()
			assertTrue(editor.uiState.value.loadFailed)
			assertFalse(editor.uiState.value.exitRequested)
			assertEquals(0, repository.writes)
		}

	@Test
	fun newNoteRetainsTheSelectedFolderAcrossAutosaves() =
		runTest(dispatcher) {
			val editor = editor()
			editor.setInitialFolder(4)
			editor.setInitialFolder(7)
			editor.onEvent(EditNoteEvent.EnteredTitle("Idea del trabajo"))
			advanceUntilIdle()
			assertEquals(4, repository.notes.values.single().folderId)
			editor.onEvent(EditNoteEvent.EnteredContent("Más información"))
			advanceUntilIdle()
			assertEquals(4, repository.notes.values.single().folderId)
			assertEquals(1, repository.notes.size)
		}
}
