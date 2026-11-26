package com.example.cleannotes.presentation.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.theme.CleanMindTheme

@Preview(
	name = "Inicio · claro",
	widthDp = 390,
	heightDp = 844,
	locale = "es",
	showBackground = true,
)
@Composable
private fun HomeLightPreview() = HomePreview(dark = false)

@Preview(
	name = "Inicio · oscuro",
	widthDp = 390,
	heightDp = 844,
	locale = "es",
	showBackground = true,
)
@Composable
private fun HomeDarkPreview() = HomePreview(dark = true)

@Preview(
	name = "Inicio vacío · texto grande",
	widthDp = 360,
	heightDp = 800,
	fontScale = 1.5f,
	locale = "es",
	showBackground = true,
)
@Composable
private fun HomeEmptyPreview() = HomePreview(dark = false, empty = true)

@Composable
private fun HomePreview(dark: Boolean, empty: Boolean = false) {
	val now = System.currentTimeMillis()
	val notes =
		if (empty) emptyList()
		else
			listOf(
				Note(
					1,
					"Ideas que merecen quedarse",
					NoteDocument(
						text =
							"Un paseo sin prisa, un libro pendiente y un nuevo proyecto que empieza a tomar forma.",
						subtitle = "Inspiración para esta semana",
					)
						.encode(),
					now,
				),
				Note(2, "Llamar a Marta", "", now - 60_000, isReminder = true),
				Note(
					3,
					"Mi próximo viaje",
					"Perderse por las calles, descubrir una cafetería y escribir un poco cada día.",
					now - 86_400_000,
				),
			)
	CleanMindTheme(darkTheme = dark) {
		com.example.cleannotes.presentation.notebook.NotebookScreen(
			state =
				com.example.cleannotes.presentation.notebook.NotesUiState(
					allNotes = notes,
					notes = notes.filterNot { it.isReminder },
					reminders = notes.filter { it.isReminder },
					isLoading = false,
				),
			onTabSelected = {},
			onQueryChange = {},
			onDelete = {},
			onRetry = {},
			onDismissError = {},
			onOpenNote = {},
			onCreate = {},
			onSettings = {},
		)
	}
}
