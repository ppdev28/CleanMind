package com.example.cleannotes.ai

import androidx.lifecycle.SavedStateHandle
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.ai.AiAssistantViewModel
import com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent
import com.example.cleannotes.presentation.ui.edit_note.SaveStatus
import com.example.cleannotes.presentation.ui.edit_note.viewmodel.EditNoteViewModel
import com.example.cleannotes.presentation.ui.settings.AiSettingsViewModel
import com.example.cleannotes.testing.FakeNoteRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiFlowTest {
	private val dispatcher = StandardTestDispatcher()
	private val settings = MemorySettings()
	private val gateway = FakeGateway()

	@Before
	fun setUp() {
		Dispatchers.setMain(dispatcher)
	}

	@After
	fun tearDown() {
		Dispatchers.resetMain()
	}

	@Test
	fun completeSettingsToGenerationToAcceptedAutosavedNoteFlow() =
		runTest(dispatcher) {
			val vm = AiSettingsViewModel(settings, gateway)
			advanceUntilIdle()
			val configuration = AiConfiguration(apiKey = "test-secret")
			vm.edit(configuration)
			vm.testConnection()
			advanceUntilIdle()
			assertTrue(vm.state.value.dirty)
			assertEquals("", settings.config.apiKey)
			assertEquals(1, gateway.tests)
			vm.save()
			advanceUntilIdle()
			assertEquals(configuration, settings.config)
			assertFalse(vm.state.value.dirty)

			val notes = FakeNoteRepository()
			val doc =
				NoteDocument(
					text = "Idea original",
					marks = listOf(TextMark(0, 4, "bold")),
					images = listOf("private-image"),
					subtitle = "Subtítulo",
					checklist = listOf(CheckItem("1", "Llamar")),
					pinned = true,
					createdAt = 1234,
					reminder = ReminderConfig(9999),
				)
			val editor = EditNoteViewModel(notes, SavedStateHandle(), null, false)
			editor.onEvent(EditNoteEvent.EnteredTitle("Mi nota"))
			editor.onEvent(EditNoteEvent.EnteredContent(doc.encode()))
			advanceUntilIdle()
			val writes = notes.writes
			val assistant = AiAssistantViewModel(settings, gateway)
			assistant.open()
			advanceUntilIdle()
			assertEquals(0, gateway.requests.size)
			val snapshot = AiNoteSnapshot(editor.uiState.value.title, editor.uiState.value.content)
			assistant.generate(snapshot)
			assistant.generate(snapshot)
			advanceUntilIdle()
			assertEquals(1, gateway.requests.size)
			assertEquals(writes, notes.writes)
			assertEquals(doc.encode(), editor.uiState.value.content)
			assertTrue(editor.applyAiSuggestion(assistant.state.value.suggestion!!))
			assistant.dismiss()
			advanceUntilIdle()
			assertEquals(1, notes.notes.size)
			val stored = NoteDocument.decode(notes.notes.values.single().content)
			assertEquals(doc.copy(text = "Idea original\n\nTexto generado"), stored)
			assertEquals(SaveStatus.SAVED, editor.uiState.value.saveStatus)
			assertFalse(assistant.state.value.visible)
		}

	@Test
	fun doesNotSendAnythingWithoutConfigurationAndCanReloadAfterSettingsChange() =
		runTest(dispatcher) {
			val vm = AiAssistantViewModel(settings, gateway)
			vm.open()
			advanceUntilIdle()
			vm.generate(AiNoteSnapshot("Nota", "Cuerpo"))
			advanceUntilIdle()
			assertFalse(vm.state.value.configured)
			assertTrue(gateway.requests.isEmpty())
			settings.config = AiConfiguration(apiKey = "test-secret")
			vm.open()
			advanceUntilIdle()
			assertTrue(vm.state.value.configured)
			assertEquals("api.openai.com", vm.state.value.provider)
		}

	@Test
	fun rejectsStaleSuggestionAndDuplicateApplication() =
		runTest(dispatcher) {
			val editor = EditNoteViewModel(FakeNoteRepository(), SavedStateHandle(), null, false)
			editor.onEvent(EditNoteEvent.EnteredContent("Original"))
			val suggestion = AiSuggestion(AiNoteSnapshot("", "Original"), "Propuesta")
			editor.onEvent(EditNoteEvent.EnteredContent("Editado"))
			assertFalse(editor.applyAiSuggestion(suggestion))
			assertEquals("Editado", editor.uiState.value.content)
			val current = AiSuggestion(AiNoteSnapshot("", "Editado"), "Propuesta")
			assertTrue(editor.applyAiSuggestion(current))
			assertFalse(editor.applyAiSuggestion(current))
			advanceUntilIdle()
		}

	@Test
	fun generationFailureAndCancellationLeaveOriginalDraftIntactAndAllowRetry() =
		runTest(dispatcher) {
			settings.config = AiConfiguration(apiKey = "test-secret")
			val vm = AiAssistantViewModel(settings, gateway)
			vm.open()
			advanceUntilIdle()
			gateway.failure = AiException(AiError.RATE_LIMIT)
			val note = AiNoteSnapshot("Mi nota", "Original")
			vm.generate(note)
			advanceUntilIdle()
			assertEquals(AiError.RATE_LIMIT.message, vm.state.value.error)
			assertNull(vm.state.value.suggestion)
			gateway.failure = null
			gateway.waitForever = true
			vm.generate(note)
			runCurrent()
			assertTrue(vm.state.value.generating)
			vm.cancelGeneration()
			advanceUntilIdle()
			assertFalse(vm.state.value.generating)
			assertNull(vm.state.value.error)
			assertNull(vm.state.value.suggestion)
			gateway.waitForever = false
			vm.generate(note)
			advanceUntilIdle()
			assertEquals(note, vm.state.value.suggestion?.note)
		}

	@Test
	fun customInstructionGeneratesFromEmptyNoteAndRejectsOversizedInput() =
		runTest(dispatcher) {
			settings.config = AiConfiguration(apiKey = "test-secret")
			val vm = AiAssistantViewModel(settings, gateway)
			vm.open()
			advanceUntilIdle()
			vm.select(AiAction.CUSTOM)
			vm.instruct("Redacta una invitación")
			vm.generate(AiNoteSnapshot("", ""))
			advanceUntilIdle()
			assertEquals("Redacta una invitación", gateway.requests.single().instruction)
			vm.instruct("x".repeat(2001))
			vm.generate(AiNoteSnapshot("", ""))
			advanceUntilIdle()
			assertEquals(1, gateway.requests.size)
			assertEquals(AiError.INPUT_TOO_LONG.message, vm.state.value.error)
			vm.select(AiAction.SUMMARIZE)
			vm.generate(AiNoteSnapshot("Nota", "Texto de la nota"))
			advanceUntilIdle()
			assertEquals(2, gateway.requests.size)
			assertEquals("", gateway.requests.last().instruction)
			assertNotNull(vm.state.value.suggestion)
		}

	@Test
	fun storageFailuresAreRecoverableAndCredentialsCanBeCleared() =
		runTest(dispatcher) {
			settings.fail = true
			val vm = AiSettingsViewModel(settings, gateway)
			advanceUntilIdle()
			assertEquals(AiError.STORAGE.message, vm.state.value.error)
			assertFalse(vm.state.value.loading)
			settings.fail = false
			vm.reload()
			advanceUntilIdle()
			vm.edit(AiConfiguration(apiKey = "new-secret"))
			settings.fail = true
			vm.save()
			advanceUntilIdle()
			assertTrue(vm.state.value.dirty)
			assertNotNull(vm.state.value.error)
			settings.fail = false
			vm.save()
			advanceUntilIdle()
			vm.clear()
			advanceUntilIdle()
			assertEquals(AiConfiguration(), settings.config)
			assertEquals("", vm.state.value.configuration.apiKey)
		}

	@Test
	fun acceptedSuggestionSurvivesSaveFailureAndRetry() =
		runTest(dispatcher) {
			val notes = FakeNoteRepository()
			val editor = EditNoteViewModel(notes, SavedStateHandle(), null, false)
			editor.onEvent(EditNoteEvent.EnteredContent("Original"))
			advanceUntilIdle()
			notes.failWrites = true
			assertTrue(
				editor.applyAiSuggestion(AiSuggestion(AiNoteSnapshot("", "Original"), "Propuesta"))
			)
			advanceUntilIdle()
			assertEquals(SaveStatus.ERROR, editor.uiState.value.saveStatus)
			assertEquals(
				"Original\n\nPropuesta",
				NoteDocument.decode(editor.uiState.value.content).text,
			)
			notes.failWrites = false
			editor.saveAndClose()
			advanceUntilIdle()
			assertEquals(
				"Original\n\nPropuesta",
				NoteDocument.decode(notes.notes.values.single().content).text,
			)
			assertTrue(editor.uiState.value.exitRequested)
		}

	@Test
	fun immediateCancellationCannotClearTheBusyStateOfANewGeneration() =
		runTest(dispatcher) {
			settings.config = AiConfiguration(apiKey = "test-secret")
			val vm = AiAssistantViewModel(settings, gateway)
			vm.open()
			advanceUntilIdle()
			val note = AiNoteSnapshot("Nota", "Original")
			gateway.waitForever = true
			vm.generate(note)
			vm.cancelGeneration()
			vm.generate(note)
			runCurrent()
			assertTrue(vm.state.value.generating)
			assertEquals(1, gateway.requests.size)
			vm.cancelGeneration()
			gateway.waitForever = false
			vm.generate(note)
			advanceUntilIdle()
			assertFalse(vm.state.value.generating)
			assertNotNull(vm.state.value.suggestion)
		}

	@Test
	fun connectionTestCanBeCancelledAndRetriedWithoutSavingCredentials() =
		runTest(dispatcher) {
			val vm = AiSettingsViewModel(settings, gateway)
			advanceUntilIdle()
			vm.edit(AiConfiguration(apiKey = "test-secret"))
			gateway.waitForever = true
			vm.testConnection()
			runCurrent()
			assertTrue(vm.state.value.busy)
			vm.cancel()
			gateway.waitForever = false
			vm.testConnection()
			advanceUntilIdle()
			assertFalse(vm.state.value.busy)
			assertNull(vm.state.value.error)
			assertTrue(vm.state.value.dirty)
			assertEquals("", settings.config.apiKey)
			assertEquals(2, gateway.tests)
		}

	private class MemorySettings : AiSettingsRepository {
		var config = AiConfiguration()
		var fail = false

		override suspend fun load(): AiConfiguration {
			if (fail) throw AiException(AiError.STORAGE)
			return config
		}

		override suspend fun save(configuration: AiConfiguration) {
			if (fail) throw AiException(AiError.STORAGE)
			config = configuration
		}

		override suspend fun clear() {
			if (fail) throw AiException(AiError.STORAGE)
			config = AiConfiguration()
		}
	}

	private class FakeGateway : AiGateway {
		val requests = mutableListOf<AiRequest>()
		var tests = 0
		var failure: Exception? = null
		var waitForever = false

		override suspend fun complete(configuration: AiConfiguration, request: AiRequest): String {
			requests += request
			failure?.let { throw it }
			if (waitForever) awaitCancellation()
			return "Texto generado"
		}

		override suspend fun testConnection(configuration: AiConfiguration) {
			tests++
			failure?.let { throw it }
			if (waitForever) awaitCancellation()
		}
	}
}
