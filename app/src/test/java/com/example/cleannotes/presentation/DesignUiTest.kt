package com.example.cleannotes.presentation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.ReminderConfig
import com.example.cleannotes.presentation.notebook.*
import com.example.cleannotes.presentation.ui.edit_note.EditNoteScreen
import com.example.cleannotes.presentation.ui.edit_note.EditNoteUiState
import com.example.cleannotes.presentation.ui.edit_note.SaveStatus
import com.example.cleannotes.presentation.ui.theme.CleanMindTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "es-w390dp-h844dp-mdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignUiTest {
	@get:Rule
	val compose = createAndroidComposeRule<ComponentActivity>()
	private val sample =
		listOf(
			Note(
				1,
				"Para cuando me bloquee",
				NoteDocument(
					text = "CONTENIDO PRIVADO",
					subtitle = "Elegir solo el siguiente paso.\nLo demás puede esperar.",
					pinned = true,
				)
					.encode(),
				System.currentTimeMillis(),
			),
			Note(
				2,
				"Paseo del sábado",
				NoteDocument(
					text = "OTRO CONTENIDO PRIVADO",
					subtitle = "Cámara y ruta junto al río.",
				)
					.encode(),
				System.currentTimeMillis(),
			),
			Note(
				3,
				"Idea para el salón",
				NoteDocument(subtitle = "Lámpara junto a la ventana.").encode(),
				System.currentTimeMillis(),
			),
			Note(
				4,
				"Llamar a la clínica",
				NoteDocument(
					subtitle = "Preguntar por la cita de revisión.",
					reminder =
						ReminderConfig(
							java.util.Calendar.getInstance()
								.apply { set(java.util.Calendar.HOUR_OF_DAY, 17) }
								.timeInMillis
						),
				)
					.encode(),
				System.currentTimeMillis(),
				isReminder = true,
			),
		)

	private fun screen(
		dark: Boolean = true,
		initial: NotesTab = NotesTab.HOME,
		onCreate: (Boolean) -> Unit = {},
	) {
		compose.setContent {
			var tab by remember { mutableStateOf(initial) }
			var query by remember { mutableStateOf("") }
			CleanMindTheme(darkTheme = dark) {
				NotebookScreen(
					NotesUiState(
						allNotes = sample,
						selectedTab = tab,
						query = query,
						isLoading = false,
					),
					onTabSelected = { tab = it },
					onQueryChange = { query = it },
					onRetry = {},
					onDismissError = {},
					onOpenNote = {},
					onCreate = onCreate,
					onSettings = {},
				)
			}
		}
	}

	private fun screenshot(name: String, dialog: Boolean = false) {
		compose.waitForIdle()
		compose.runOnIdle {
			val view =
				if (dialog)
					requireNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog())
						.window!!
						.decorView
				else compose.activity.window.decorView
			val bitmap =
				android.graphics.Bitmap.createBitmap(
					view.width,
					view.height,
					android.graphics.Bitmap.Config.ARGB_8888,
				)

			// A software Canvas must replay Compose's cached layers as well as native children.
			fun invalidateLayers(node: android.view.View) {
				node.javaClass.methods.firstOrNull { it.name == "invalidateDescendants" && it.parameterCount == 0 }
					?.invoke(node)
				if (node is android.view.ViewGroup) for (i in 0 until node.childCount) invalidateLayers(
					node.getChildAt(
						i
					)
				)
			}
			invalidateLayers(view)
			view.draw(android.graphics.Canvas(bitmap))
			java.io.File("/tmp/cleanmind-ui-$name.png").outputStream().use {
				bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
			}
		}
	}

	@Test
	fun homeCanSelectSeveralNotesAndDeleteOnlyAfterConfirmation() {
		var deleted = emptySet<Int>()
		compose.setContent {
			var state by remember { mutableStateOf(NotesUiState(allNotes = sample, isLoading = false)) }
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(state, {}, {}, {}, {}, {}, {}, {}, onDeleteSelection = { ids ->
					deleted = ids
					state = state.copy(
						allNotes = state.allNotes.filterNot { it.id in ids },
						deletion = NoteDeletion(revision = 1)
					)
				})
			}
		}
		compose.onNodeWithText(sample[0].title).performTouchInput { longClick() }
		compose.onNodeWithText("1 seleccionado").assertIsDisplayed()
		compose.onAllNodes(isDialog()).assertCountEquals(0)
		compose.onNodeWithText(sample[1].title).performClick()
		screenshot("selection-home-dark")
		compose.onNodeWithText("2 seleccionados").assertIsDisplayed()
		compose.onNodeWithContentDescription("Eliminar seleccionados").performClick()
		compose.onNodeWithText("Eliminar 2 elementos").assertIsDisplayed()
		compose.onNodeWithText("Cancelar").performClick()
		assertTrue(deleted.isEmpty())
		compose.onNodeWithContentDescription("Eliminar seleccionados").performClick()
		compose.onNodeWithText("Eliminar selección").performClick()
		assertEquals(setOf(1, 2), deleted)
		compose.onNodeWithText(sample[0].title).assertDoesNotExist()
		compose.onNodeWithText(sample[2].title).assertExists()
		compose.onNodeWithContentDescription("Cancelar selección").assertDoesNotExist()
	}

	@Test
	fun reminderSelectionDoesNotCompleteOrOpenTheReminders() {
		var completed = false
		var opened = false
		var deleted = emptySet<Int>()
		val reminders = listOf(sample.last(), sample.last().copy(id = 5, title = "Segundo aviso"))
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(
					NotesUiState(
						allNotes = reminders,
						selectedTab = NotesTab.REMINDERS,
						isLoading = false
					),
					{}, {}, {}, {}, { opened = true }, {}, {},
					onUpdateNote = { _, _ -> completed = true }, onDeleteSelection = { deleted = it })
			}
		}
		compose.onNodeWithText(reminders.first().title).performTouchInput { longClick() }
		compose.onNodeWithText("1 seleccionado").assertIsDisplayed()
		compose.onAllNodes(isDialog()).assertCountEquals(0)
		compose.onNodeWithContentDescription("Seleccionar Segundo aviso").performClick()
		compose.onNodeWithText("2 seleccionados").assertIsDisplayed()
		assertFalse(completed); assertFalse(opened)
		compose.onNodeWithContentDescription("Eliminar seleccionados").performClick()
		compose.onNodeWithText("Eliminar selección").performClick()
		assertEquals(setOf(4, 5), deleted)
	}

	@Test
	fun homeHidesBodyAndLongPressOffersSharing() {
		screen()
		compose.onNodeWithText("CONTENIDO PRIVADO").assertDoesNotExist()
		compose
			.onNodeWithText("Elegir solo el siguiente paso.\nLo demás puede esperar.")
			.assertExists()
		compose.onNodeWithText("Tu agenda").assertDoesNotExist()
		screenshot("home-dark")
		compose.onNodeWithText("Para cuando me bloquee").performTouchInput { longClick() }
		compose.onNodeWithText("1 seleccionado").assertIsDisplayed()
		compose.onAllNodes(isDialog()).assertCountEquals(0)
		compose.onNodeWithContentDescription("Más acciones de selección").performClick()
		compose.onNodeWithText("Compartir nota").assertExists()
		compose.onNodeWithText("Archivar").assertDoesNotExist()
	}

	@Test
	@Config(qualifiers = "es-w360dp-h800dp-mdpi")
	fun homeGridCanReturnToTheAgendaWithoutRevealingContent() {
		screen()
		val first = compose.onNodeWithText("Idea para el salón").fetchSemanticsNode().boundsInRoot
		val second = compose.onNodeWithText("Paseo del sábado").fetchSemanticsNode().boundsInRoot
		assertEquals(first.top, second.top, 1f)
		assertTrue(first.right < second.left)
		val toggle =
			compose.onNodeWithContentDescription("Ver en agenda").fetchSemanticsNode().boundsInRoot
		val settings = compose.onNodeWithContentDescription("Ajustes").fetchSemanticsNode().boundsInRoot
		assertEquals(settings.top, toggle.top, 1f)
		assertTrue(toggle.right <= settings.left)
		compose.onNodeWithContentDescription("Ver en agenda").performClick()
		val agendaFirst = compose.onNodeWithText("Idea para el salón").fetchSemanticsNode().boundsInRoot
		val agendaSecond = compose.onNodeWithText("Paseo del sábado").fetchSemanticsNode().boundsInRoot
		assertTrue(agendaSecond.top > agendaFirst.bottom)
		compose.onNodeWithText("CONTENIDO PRIVADO").assertDoesNotExist()
		screenshot("home-agenda-dark")
		compose.onNodeWithContentDescription("Ver en cuadrícula").performClick()
		compose.onNodeWithContentDescription("Ver en agenda").assertIsDisplayed()
	}

	@Test
	fun searchCanFilterAndSwitchToGrid() {
		screen(initial = NotesTab.SEARCH)
		compose.onNodeWithContentDescription("Cambiar vista").performClick()
		screenshot("search-grid-dark")
		compose.onNode(hasSetTextAction()).performTextInput("Lámpara")
		compose.onNodeWithText("Idea para el salón").assertExists()
		compose.onNodeWithText("Paseo del sábado").assertDoesNotExist()
	}

	@Test
	fun bottomSearchOpensTheFieldReadyToType() {
		screen()
		compose.onNodeWithContentDescription("Buscar").performClick()
		compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("Lámpara")
		compose.onNodeWithText("Idea para el salón").assertExists()
		compose.onNodeWithText("Paseo del sábado").assertDoesNotExist()
	}

	@Test
	fun lightHomeUsesTheSameLayout() {
		screen(dark = false)
		screenshot("home-light")
	}

	@Test
	fun headerReturnsOnlyAtTheTopAndKeepsContentOutsideTheBars() {
		var tab by mutableStateOf(NotesTab.HOME)
		val verticalScroll =
			hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(
					NotesUiState(
						selectedTab = tab,
						allNotes = List(18) { index ->
							sample.first().copy(id = index + 1, title = "Idea $index")
						},
						isLoading = false,
					),
					{ tab = it }, {}, {}, {}, {}, {}, {},
				)
			}
		}
		compose.onNodeWithText("Clean Mind").assertIsDisplayed()
		fun assertViewportBetweenBars(title: String = "Clean Mind") {
			val header = compose.onAllNodesWithText(title).fetchSemanticsNodes()
				.minBy { it.boundsInRoot.top }.boundsInRoot
			val viewport = compose.onNode(verticalScroll).fetchSemanticsNode().boundsInRoot

			assertTrue("The list must start below the header", viewport.top >= header.bottom)
			val bottomBar =
				compose.onNodeWithContentDescription("Inicio").fetchSemanticsNode().boundsInRoot
			assertTrue("The list must end above navigation", viewport.bottom <= bottomBar.top)

		}
		assertViewportBetweenBars()
		fun scrollPosition() = compose.onNode(verticalScroll).fetchSemanticsNode()
			.config[SemanticsProperties.VerticalScrollAxisRange].value()
		repeat(3) {
			compose.onNode(verticalScroll).performTouchInput {
				swipe(Offset(centerX, height * 0.65f), Offset(centerX, height * 0.25f), 500)
			}
		}
		screenshot("agenda-scroll-dark")
		compose.onNodeWithText("Clean Mind").assertIsNotDisplayed()
		assertTrue(scrollPosition() > 0f)
		compose.onNodeWithContentDescription("Nueva nota").assertIsDisplayed()
		compose.onNode(verticalScroll).performTouchInput {
			swipe(Offset(centerX, height * 0.4f), Offset(centerX, height * 0.5f), 500)
		}
		assertTrue("A short upward return must stay away from the start", scrollPosition() > 0f)
		compose.onNodeWithText("Clean Mind").assertIsNotDisplayed()
		repeat(8) {
			compose.onNode(verticalScroll).performTouchInput {
				swipe(Offset(centerX, height * 0.25f), Offset(centerX, height * 0.75f), 500)
			}
		}
		assertEquals(0f, scrollPosition(), 0.001f)
		compose.onNodeWithText("Clean Mind").assertIsDisplayed()
		assertViewportBetweenBars()
		compose.onNodeWithContentDescription("Notas").performClick()
		assertViewportBetweenBars("Notas")
		compose.onNode(verticalScroll).performTouchInput {
			swipe(Offset(centerX, height * 0.65f), Offset(centerX, height * 0.25f))
		}
		compose.onAllNodesWithText("Notas").onFirst().assertIsNotDisplayed()
		compose.onNodeWithContentDescription("Nueva nota").assertIsDisplayed()
	}

	@Test
	fun editorKeepsOnlyCaptureAndFormattingTools() {
		var document = NoteDocument(text = "Salir sin prisa.", subtitle = "Cámara y río")
		compose.setContent {
			var state by remember {
				mutableStateOf(
					EditNoteUiState(
						title = "Paseo del sábado",
						content = document.encode()
					)
				)
			}
			CleanMindTheme(darkTheme = true) {
				EditNoteScreen(state, { event ->
					if (event is com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent.EnteredContent) {
						document = NoteDocument.decode(event.value); state = state.copy(content = event.value)
					}
				}, {}, {})
			}
		}
		screenshot("editor-dark")
		compose.onNodeWithText("Concentrarme").assertDoesNotExist()
		compose.onNodeWithContentDescription("Archivar nota").assertDoesNotExist()
		compose.onNodeWithContentDescription("Casilla en el texto").assertDoesNotExist()
		val mic = compose.onNodeWithContentDescription("Dictar por voz").assertIsDisplayed()
			.fetchSemanticsNode().boundsInRoot
		val list = compose.onNodeWithText("Lista").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
		assertTrue(mic.top <= list.top && mic.bottom >= list.bottom)
		compose.onNodeWithText("Lista").performClick()
		assertEquals(1, document.checklist.size)
		compose.onNodeWithText("Lista").performClick()
		assertEquals(2, document.checklist.size)
		assertEquals("Salir sin prisa.", document.text)
		compose.onNodeWithContentDescription("Más opciones").assertDoesNotExist()
		compose.onNodeWithContentDescription("Compartir nota").assertIsDisplayed()
		compose.onNodeWithContentDescription("Deshacer").assertExists()
		compose.onNodeWithContentDescription("Rehacer").assertExists()
		compose.onNodeWithContentDescription("Más formato y espaciado").performClick()
		compose.onNodeWithText("Casilla").assertDoesNotExist()
		compose.onNodeWithText("H6").assertExists()
	}

	@Test
	fun normalEditorShowsRichTextWithoutMarkdownControls() {
		val original = NoteDocument(
			text = "Ciencia",
			marks = listOf(com.example.cleannotes.model.TextMark(0, 7, "bold"))
		)
		var changes = 0
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				EditNoteScreen(
					EditNoteUiState(title = "Nota", content = original.encode()),
					{ changes++ },
					{},
					{})
			}
		}
		compose.onNodeWithContentDescription("Cuerpo de la nota").assertIsDisplayed()
		compose.onNodeWithText("Markdown").assertDoesNotExist()
		compose.onNodeWithText("Vista previa").assertDoesNotExist()
		compose.onNodeWithContentDescription("Negrita").assertIsDisplayed()
		compose.onNodeWithContentDescription("Más formato y espaciado").performClick()
		compose.onNodeWithText("Formato de texto").assertIsDisplayed()
		compose.onNodeWithText("H6").assertExists()
		screenshot("normal-format-panel-dark", dialog = true)
		assertEquals(0, changes)
	}

	@Test
	fun checklistRowsCanBeDeletedAndRestoredWithoutChangingTheBody() {
		val original = NoteDocument(
			text = "Cuerpo", subtitle = "Subtítulo", pinned = true, createdAt = 123L,
			marks = listOf(com.example.cleannotes.model.TextMark(0, 6, "bold")),
			checklist = listOf(
				com.example.cleannotes.model.CheckItem("a", "Pendiente"),
				com.example.cleannotes.model.CheckItem("b", "Hecha", true),
				com.example.cleannotes.model.CheckItem("c")
			)
		)
		var saved = original
		compose.setContent {
			var state by remember {
				mutableStateOf(
					EditNoteUiState(
						title = "Lista",
						content = original.encode()
					)
				)
			}
			CleanMindTheme(darkTheme = true) {
				EditNoteScreen(state, { event ->
					if (event is com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent.EnteredContent) {
						saved = NoteDocument.decode(event.value)
						state = state.copy(content = event.value)
					}
				}, {}, {})
			}
		}
		compose.onNodeWithContentDescription("Eliminar casilla 1").performClick()
		assertEquals(listOf("b", "c"), saved.checklist.map { it.id })
		assertEquals(original.copy(checklist = saved.checklist), saved)
		compose.onNodeWithText("Casilla eliminada · Deshacer").performClick()
		assertEquals(original, saved)
		compose.onNodeWithContentDescription("Eliminar casilla 2").performClick()
		compose.onNodeWithContentDescription("Eliminar casilla 2").performClick()
		assertEquals(listOf(original.checklist.first()), saved.checklist)
		assertEquals(original.copy(checklist = saved.checklist), saved)
	}

	@Test
	fun editorToolsRemainReadableWithLargeTextOnANarrowScreen() {
		compose.setContent {
			val density = LocalDensity.current
			CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f)) {
				CleanMindTheme(darkTheme = true) {
					Box(Modifier.requiredWidth(320.dp)) {
						EditNoteScreen(EditNoteUiState(title = "Nota"), {}, {}, {})
					}
				}
			}
		}
		compose.onNodeWithContentDescription("Negrita").assertIsDisplayed()
		compose.onNodeWithContentDescription("Más formato y espaciado").assertIsDisplayed()
		listOf("Dictar", "Lista", "Imagen", "Recordar").forEach { label ->
			val layouts = mutableListOf<TextLayoutResult>()
			compose.onNodeWithText(label).assertIsDisplayed().performSemanticsAction(
				SemanticsActions.GetTextLayoutResult
			) {
				it(layouts)
			}
			assertEquals(
				"$label must fit without splitting the word",
				1,
				layouts.single().lineCount,
			)
			val layout = layouts.single()
			assertFalse("$label must not be truncated", layout.isLineEllipsized(0))
			assertEquals(label.length, layout.getLineEnd(0, visibleEnd = true))
		}
	}

	@Test
	fun reminderCanBeCompletedWithoutDeletingItsNote() {
		var persisted = sample.last()
		compose.setContent {
			var notes by remember { mutableStateOf(listOf(persisted)) }
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(
					NotesUiState(
						allNotes = notes,
						selectedTab = NotesTab.REMINDERS,
						isLoading = false,
					),
					{},
					{},
					{},
					{},
					{},
					{},
					{},
					onUpdateNote = { note, transform ->
						persisted =
							note.copy(
								content = transform(NoteDocument.decode(note.content)).encode()
							)
						notes = listOf(persisted)
					},
				)
			}
		}
		screenshot("reminders-dark")
		compose.onNodeWithContentDescription("Completar Llamar a la clínica").performClick()
		compose.onNodeWithText("Todo al día").assertExists()
		assertEquals("Llamar a la clínica", persisted.title)
		assertTrue(NoteDocument.decode(persisted.content).reminderCompleted)
	}

	@Test
	fun settingsCanSelectAndApplyAnAccentPalette() {
		var appearance = com.example.cleannotes.model.Appearance()
		compose.setContent {
			var value by remember { mutableStateOf(appearance) }
			CleanMindTheme(darkTheme = true, appearance = value) {
				com.example.cleannotes.presentation.ui.settings.SettingsScreen(
					com.example.cleannotes.presentation.ui.settings.SettingsUiState(
						isLoading = false,
						appearance = value
					), {}, {}, {}, storageContent = {},
					onPaletteSelected = { id -> value = value.copy(selectedId = id); appearance = value })
			}
		}
		compose.onNodeWithText("Azul acero").performScrollTo().performClick()
		compose.onNodeWithText("Azul acero").assertIsSelected()
		assertEquals("steel", appearance.selectedId)
	}

	@Test
	fun settingsShowAppearanceAndStorageDirectly() {
		val storage =
			com.example.cleannotes.presentation.ui.settings.StorageSettingsViewModel(
				com.example.cleannotes.testing.FakeNoteRepository(),
				org.robolectric.RuntimeEnvironment.getApplication(),
			)
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				com.example.cleannotes.presentation.ui.settings.SettingsScreen(
					com.example.cleannotes.presentation.ui.settings.SettingsUiState(
						isLoading = false
					),
					{},
					{},
					{},
					storageContent = {
						com.example.cleannotes.presentation.ui.settings.StorageSettings(storage)
					},
				)
			}
		}
		compose.onNodeWithText("Apariencia").assertExists()
		compose.onNodeWithText("Carpeta de las notas").assertExists()
		screenshot("settings-dark")
	}

	@Test
	fun reminderSheetShowsQuickChoices() {
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				Surface(
					Modifier.fillMaxSize(),
					color = androidx.compose.material3.MaterialTheme.colorScheme.background,
				) {
					com.example.cleannotes.presentation.ui.edit_note.ReminderSheet(
						"Paseo del sábado",
						NoteDocument(),
						{},
						{},
					)
				}
			}
		}
		compose.onNodeWithText("Mañana").assertExists()
		screenshot("reminder-sheet-dark", dialog = true)
		// A modal sheet has its own window; interaction assertions cover the real composed sheet.
		compose.onNodeWithText("Elegir fecha y hora").performClick()
		compose.onNodeWithText("Hora local · Formato de 24 horas").assertExists()
		screenshot("reminder-date-dark", dialog = true)
	}

	@Test
	fun agendaGroupsByCreationDayRatherThanLatestEdit() {
		val now = System.currentTimeMillis()
		val yesterday =
			java.util.Calendar.getInstance()
				.apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
				.timeInMillis
		val notes =
			listOf(
				Note(101, "Primera de hoy", NoteDocument(createdAt = now).encode(), now),
				Note(102, "Segunda de hoy", NoteDocument(createdAt = now).encode(), now),
				Note(
					103,
					"Creada ayer y editada hoy",
					NoteDocument(createdAt = yesterday).encode(),
					now,
				),
			)
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(
					NotesUiState(allNotes = notes, isLoading = false),
					{},
					{},
					{},
					{},
					{},
					{},
					{},
				)
			}
		}
		compose.onNodeWithText("Hoy", substring = false).assertDoesNotExist()
		compose.onNodeWithText("Ayer", substring = false).assertDoesNotExist()
		compose
			.onAllNodes(
				SemanticsMatcher.keyIsDefined(
					androidx.compose.ui.semantics.SemanticsProperties.Heading
				)
			)
			.assertCountEquals(2)
		compose.onNodeWithText("Creada ayer y editada hoy").assertExists()
		screenshot("agenda-days-dark")
	}

	@Test
	@Config(qualifiers = "es-w320dp-h740dp-mdpi")
	fun bottomNavigationFitsFiveActionsWithoutOverlapping() {
		compose.setContent {
			val density = LocalDensity.current
			CompositionLocalProvider(LocalDensity provides Density(density.density, 1.35f)) {
				CleanMindTheme(darkTheme = true) {
					com.example.cleannotes.presentation.ui.components.NotebookBottomBar(
						NotesTab.HOME,
						{},
						{})
				}
			}
		}
		val labels = listOf("Inicio", "Notas", "Nueva nota", "Recordar", "Buscar")
		val bounds = labels.map {
			compose.onNodeWithContentDescription(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
		}
		bounds.forEachIndexed { index, rect ->
			assertTrue(rect.width >= 48f && rect.height >= 48f)
			bounds.drop(index + 1)
				.forEach { assertFalse("Navigation hit targets must not overlap", rect.overlaps(it)) }
		}
		labels.forEach { label ->
			val layouts = mutableListOf<TextLayoutResult>()
			compose.onNodeWithText(label)
				.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
			assertEquals(
				label.length,
				layouts.single().getLineEnd(layouts.single().lineCount - 1, visibleEnd = true)
			)
		}
		compose.onAllNodes(isDialog()).assertCountEquals(0)
		screenshot("bottom-bar-compact-dark")
	}

	@Test
	fun bottomNavigationOpensNotesAndContextualReminderCreation() {
		var reminder: Boolean? = null
		screen(onCreate = { reminder = it })
		compose.onNodeWithContentDescription("Nueva nota").performClick()
		compose.onNodeWithText("Crear").assertIsDisplayed()
		compose.onNodeWithText("Nuevo recordatorio").assertIsDisplayed()
		screenshot("creation-sheet-dark", dialog = true)
		compose.onNode(hasText("Nueva nota") and hasAnyAncestor(isDialog())).performClick()
		assertEquals(false, reminder)
		compose.onNodeWithContentDescription("Nueva nota").performClick()
		compose.onNodeWithText("Nuevo recordatorio").performClick()
		assertEquals(true, reminder)
		compose.onNodeWithContentDescription("Recordar").performClick()
		compose.onNodeWithContentDescription("Nuevo recordatorio").performClick()
		assertEquals(true, reminder)
		compose.onNodeWithContentDescription("Inicio").performClick()
		compose.onNode(hasSetTextAction()).assertDoesNotExist()
		compose.onNodeWithText("Buscar en tus notas").assertDoesNotExist()
		screenshot("bottom-navigation-dark")
	}

	@Test
	fun reminderEditorIsTaskFocusedAndAsksBeforeDiscarding() {
		var back = false
		compose.setContent {
			var state by remember { mutableStateOf(EditNoteUiState(isReminder = true)) }
			CleanMindTheme(darkTheme = true) {
				com.example.cleannotes.presentation.ui.edit_note.ReminderEditorScreen(
					state,
					{ event ->
						state =
							when (event) {
								is com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent.EnteredTitle ->
									state.copy(title = event.value, hasChanges = true)

								is com.example.cleannotes.presentation.ui.edit_note.EditNoteEvent.EnteredContent ->
									state.copy(content = event.value, hasChanges = true)
							}
					},
					{},
					{ back = true },
				)
			}
		}
		compose.onNodeWithText("¿Qué quieres recordar?").performTextInput("Llamar mañana")
		compose.onNodeWithText("Guardar recordatorio").assertExists()
		compose.onNodeWithText("Concentrarme").assertDoesNotExist()
		compose.onNodeWithText("Imagen").assertDoesNotExist()
		screenshot("reminder-editor-dark")
		compose.onNodeWithContentDescription("Volver").performClick()
		assertFalse(back)
		compose.onNodeWithText("Descartar").performClick()
		assertTrue(back)
	}

	@Test
	fun foldersFilterNotesAndMoveOnlyAfterPersistence() {
		val inside = Note(301, "Reunión del equipo", "Cuerpo privado", 100, folderId = 4)
		val outside = Note(302, "Comprar café", "Otro cuerpo", 90)
		compose.setContent {
			var state by remember {
				mutableStateOf(
					NotesUiState(
						allNotes = listOf(inside, outside),
						folders = listOf(com.example.cleannotes.model.NoteFolder(4, "Trabajo")),
						selectedTab = NotesTab.NOTES,
						isLoading = false,
					)
				)
			}
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(
					state,
					{},
					{},
					{},
					{},
					{},
					{},
					{},
					onFolderSelected = { state = state.copy(selectedFolderId = it) },
					onMoveNote = { note, folder ->
						state =
							state.copy(
								allNotes =
									state.allNotes.map {
										if (it.id == note.id) it.copy(folderId = folder) else it
									},
								folderOperation =
									state.folderOperation.copy(
										revision = state.folderOperation.revision + 1
									),
							)
					},
				)
			}
		}
		compose.onNodeWithContentDescription("Abrir carpeta Trabajo").performClick()
		compose.onNode(hasScrollToIndexAction()).performScrollToKey(301)
		compose.onNodeWithText("Reunión del equipo").assertExists()
		compose.onNodeWithText("Comprar café").assertDoesNotExist()
		screenshot("folders-dark")
		compose.onNodeWithText("Reunión del equipo").performTouchInput { longClick() }
		compose.onNodeWithContentDescription("Más acciones de selección").performClick()
		compose.onNodeWithText("Mover a carpeta").performClick()
		compose.onNodeWithContentDescription("Mover a Sin carpeta").performClick()
		compose.onNodeWithText("Reunión del equipo").assertDoesNotExist()
		compose.onNode(hasScrollToIndexAction()).performScrollToKey("folder-unfiled")
		compose.onNodeWithContentDescription("Abrir carpeta Sin carpeta").performClick()
		compose.onNode(hasScrollToIndexAction()).performScrollToKey(302)
		compose.onNodeWithText("Comprar café").assertExists()
		compose.onNode(hasScrollToIndexAction()).performScrollToKey("folder-header")

		compose.onNodeWithContentDescription("Gestionar carpetas").performClick()
		compose.onNodeWithText("Tus carpetas").assertExists()
		screenshot("folder-manager-dark", dialog = true)
	}

	@Test
	fun aHiddenNotebookDoesNotInterceptBackFromAnotherDestination() {
		var fallbackHandled = false
		var tabChanged = false
		compose.setContent {
			androidx.activity.compose.BackHandler { fallbackHandled = true }
			CleanMindTheme(darkTheme = true) {
				NotebookScreen(
					NotesUiState(selectedTab = NotesTab.NOTES, isLoading = false),
					{ tabChanged = true },
					{},
					{},
					{},
					{},
					{},
					{},
					active = false,
				)
			}
		}
		compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
		assertTrue(fallbackHandled)
		assertFalse(tabChanged)
	}

	@Test
	fun notesShowsAFolderGridWithoutASearchField() {
		screen(initial = NotesTab.NOTES)
		compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
		val all =
			compose
				.onNodeWithContentDescription("Abrir carpeta Todas las notas")
				.fetchSemanticsNode()
				.boundsInRoot
		val unfiled =
			compose
				.onNodeWithContentDescription("Abrir carpeta Sin carpeta")
				.fetchSemanticsNode()
				.boundsInRoot
		assertTrue(kotlin.math.abs(all.top - unfiled.top) < 1f)
		assertTrue(all.right <= unfiled.left)
		compose.onNodeWithContentDescription("Cambiar vista").performClick()
		screenshot("notes-grid-dark")
	}
}
