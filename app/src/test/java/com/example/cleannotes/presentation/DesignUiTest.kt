package com.example.cleannotes.presentation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
					onDelete = {},
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
			view.draw(android.graphics.Canvas(bitmap))
			java.io.File("/tmp/cleanmind-ui-$name.png").outputStream().use {
				bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
			}
		}
	}

	@Test
	fun homeHidesBodyAndLongPressOffersSharing() {
		screen()
		compose.onNodeWithText("CONTENIDO PRIVADO").assertDoesNotExist()
		compose
			.onNodeWithText("Elegir solo el siguiente paso.\nLo demás puede esperar.")
			.assertExists()
		screenshot("home-dark")
		compose.onNodeWithText("Para cuando me bloquee").performTouchInput { longClick() }
		compose.onNodeWithText("Compartir nota").assertExists()
		compose.onNodeWithText("Archivar").assertExists()
	}

	@Test
	fun notesCanSearchAndSwitchToGrid() {
		screen(initial = NotesTab.NOTES)
		compose.onNodeWithContentDescription("Cambiar vista").performClick()
		screenshot("notes-grid-dark")
		compose.onNode(hasSetTextAction()).performTextInput("Lámpara")
		compose.onNodeWithText("Idea para el salón").assertExists()
		compose.onNodeWithText("Paseo del sábado").assertDoesNotExist()
	}

	@Test
	fun lightHomeUsesTheSameLayout() {
		screen(dark = false)
		screenshot("home-light")
	}

	@Test
	fun editorCanEnterAndLeaveFocusWithoutNavigatingAway() {
		var back = false
		compose.setContent {
			CleanMindTheme(darkTheme = true) {
				EditNoteScreen(
					EditNoteUiState(
						title = "Paseo del sábado",
						content =
							NoteDocument(text = "Salir sin prisa.", subtitle = "Cámara y río")
								.encode(),
						saveStatus = SaveStatus.SAVED,
					),
					{},
					{},
					{ back = true },
				)
			}
		}
		screenshot("editor-dark")
		compose.onNodeWithText("Concentrarme").performClick()
		compose.onNodeWithText("Recordar").assertDoesNotExist()
		screenshot("focus-dark")
		compose.onNodeWithContentDescription("Salir de concentración").performClick()
		compose.onNodeWithText("Concentrarme").assertExists()
		assertFalse(back)
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
		compose.onNodeWithText("Completar").performClick()
		compose.onNodeWithText("Todo al día").assertExists()
		assertEquals("Llamar a la clínica", persisted.title)
		assertTrue(NoteDocument.decode(persisted.content).reminderCompleted)
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
	fun restoredBottomBarOffersBothCreationActions() {
		var reminder: Boolean? = null
		screen(onCreate = { reminder = it })
		compose.onNodeWithContentDescription("Crear").performClick()
		screenshot("create-sheet-dark", dialog = true)
		compose.onNodeWithText("Nuevo recordatorio").performClick()
		assertEquals(true, reminder)
		compose.onNodeWithContentDescription("Crear").performClick()
		compose.onNodeWithText("Nueva nota").performClick()
		assertEquals(false, reminder)
	}
}
