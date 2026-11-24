package com.example.cleannotes.presentation.ui.edit_note

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.TextMark
import com.example.cleannotes.presentation.ui.theme.CleanNotesTheme

@Preview(
	name = "Editor · vacío",
	widthDp = 393,
	heightDp = 852,
	locale = "es",
	showBackground = true,
)
@Composable
private fun EmptyEditorPreview() {
	CleanNotesTheme { EditNoteScreen(EditNoteUiState(), {}, {}, {}) }
}

@Preview(
	name = "Editor · claro",
	widthDp = 393,
	heightDp = 852,
	locale = "es",
	showBackground = true,
)
@Composable
private fun LightEditorPreview() = EditorPreview(false)

@Preview(
	name = "Editor · oscuro",
	widthDp = 393,
	heightDp = 852,
	locale = "es",
	showBackground = true,
)
@Composable
private fun DarkEditorPreview() = EditorPreview(true)

@Preview(
	name = "Editor · texto grande",
	widthDp = 320,
	heightDp = 740,
	fontScale = 1.5f,
	locale = "es",
	showBackground = true,
)
@Composable
private fun LargeTextEditorPreview() = EditorPreview(false)

@Composable
private fun EditorPreview(dark: Boolean) {
	CleanNotesTheme(darkTheme = dark) {
		EditNoteScreen(
			EditNoteUiState(
				title = "Ideas para el fin de semana",
				content =
					NoteDocument(
						text =
							"Salir sin prisa\nPasear por el parque y descubrir una cafetería nueva.\n\nLlevar un libro y dejar el móvil en silencio.",
						marks = listOf(TextMark(0, 15, "bold")),
						extraLineSpacing = 0,
					)
						.encode(),
				saveStatus = SaveStatus.SAVED,
			),
			{},
			{},
			{},
		)
	}
}
