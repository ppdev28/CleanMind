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
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun filtersByExplicitTypeAndSearchesContent() = runTest(dispatcher) {
        repository.notes[1] = Note(1, "Viaje", "Remember", 10)
        repository.notes[2] = Note(2, "Llamar", "", 20, true)
        repository.publish()
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(1), viewModel.uiState.value.notes.map { it.id })
        assertEquals(listOf(2), viewModel.uiState.value.reminders.map { it.id })
        viewModel.onQueryChange("  REMEMBER  ")
        runCurrent()
        assertEquals(listOf(1), viewModel.uiState.value.searchResults.map { it.id })
    }

    @Test fun loadingFailureCanBeRetried() = runTest(dispatcher) {
        repository.failReads = true
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        assertEquals(NotesError.LOAD, viewModel.uiState.value.error)
        repository.failReads = false
        viewModel.retry()
        runCurrent()
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test fun failedDeleteKeepsTheNoteAndExposesAnError() = runTest(dispatcher) {
        val note = Note(1, "No borrar", "", 0)
        repository.notes[1] = note
        repository.publish()
        repository.failDeletes = true
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        viewModel.deleteNote(note)
        runCurrent()
        assertEquals(NotesError.DELETE, viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.allNotes.size)
        viewModel.dismissError()
        runCurrent()
        assertNull(viewModel.uiState.value.error)
    }

    @Test fun observationStopsWhenTheScreenHasNoSubscribers() = runTest(dispatcher) {
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        val subscription = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        assertEquals(1, repository.observers)
        subscription.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, repository.observers)
    }

    @Test fun queryAndSelectedTabAreRestored() = runTest(dispatcher) {
        val viewModel = NotesViewModel(repository, SavedStateHandle(mapOf("query" to "viaje", "tab" to NotesTab.SEARCH.name)))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        assertEquals("viaje", viewModel.uiState.value.query)
        assertEquals(NotesTab.SEARCH, viewModel.uiState.value.selectedTab)
    }
    @Test fun simultaneousMetadataChangesUseLatestStoredDocument() = runTest(dispatcher) {
        val note = Note(1, "Paseo", com.example.cleannotes.model.NoteDocument("Texto").encode(), 1)
        repository.notes[1] = note
        repository.writeDelay = 100
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        viewModel.updateNote(note) { it.copy(pinned = true) }
        viewModel.updateNote(note) { it.copy(archived = true) }
        advanceUntilIdle()
        val saved = com.example.cleannotes.model.NoteDocument.decode(repository.notes[1]!!.content)
        assertTrue(saved.pinned)
        assertTrue(saved.archived)
        assertEquals("Texto", saved.text)
        assertEquals(1, repository.notes.size)
    }

    @Test fun failedMetadataChangeKeepsOriginalAndExposesError() = runTest(dispatcher) {
        val note = Note(1, "Paseo", "Texto", 1)
        repository.notes[1] = note
        repository.publish()
        repository.failWrites = true
        val viewModel = NotesViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.updateNote(note) { it.copy(pinned = true) }
        runCurrent()
        assertEquals(NotesError.UPDATE, viewModel.uiState.value.error)
        assertEquals("Texto", repository.notes[1]!!.content)
    }

}
