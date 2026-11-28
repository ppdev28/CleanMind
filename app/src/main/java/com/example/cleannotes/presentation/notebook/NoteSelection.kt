package com.example.cleannotes.presentation.notebook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.DesignIconButton
import com.example.cleannotes.presentation.ui.design.MindIcons

/** The contextual action uses exactly the same filtered collection as the visible cards. */
internal fun visibleNotes(state: NotesUiState, filter: String): List<Note> =
	state.allNotes.filter { note ->
		val document = NoteDocument.decode(note.content)
		when (state.selectedTab) {
			NotesTab.HOME -> !note.isReminder && !document.archived
			NotesTab.REMINDERS -> (note.isReminder || document.reminder != null) && !document.reminderCompleted && !document.archived
			NotesTab.NOTES, NotesTab.SEARCH -> {
				val matchesFilter = when (filter) {
					"Archivadas" -> document.archived
					"Fijadas" -> document.pinned && !document.archived
					"Con imágenes" -> document.images.isNotEmpty() && !document.archived
					else -> !document.archived
				}
				matchesFilter && if (state.selectedTab == NotesTab.NOTES) {
					!note.isReminder && (state.selectedFolderId == null || if (state.selectedFolderId == 0) note.folderId == null else note.folderId == state.selectedFolderId)
				} else {
					state.query.isBlank() || note.title.contains(state.query, true) || NoteDocument.plainText(
						note.content
					).contains(state.query, true)
				}
			}
		}
	}

@Composable
internal fun NoteSelectionBar(
	count: Int,
	enabled: Boolean,
	onClose: () -> Unit,
	onSelectAll: () -> Unit,
	onDelete: () -> Unit
) {
	Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
		Row(
			Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			DesignIconButton(MindIcons.Close, "Cancelar selección", enabled, onClose)
			Text(
				"$count seleccionad${if (count == 1) "o" else "os"}",
				Modifier.weight(1f),
				style = MaterialTheme.typography.titleSmall
			)
			DesignIconButton(MindIcons.CheckBox, "Seleccionar visibles", enabled, onSelectAll)
			DesignIconButton(MindIcons.Delete, "Eliminar seleccionados", enabled, onDelete)
		}
	}
}

@Composable
internal fun BatchDeleteDialog(
	count: Int,
	operation: NoteDeletion,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit
) {
	AlertDialog(
		onDismissRequest = { if (!operation.busy) onDismiss() },
		title = { Text("Eliminar $count ${if (count == 1) "elemento" else "elementos"}") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				Text("Se eliminará la selección de notas y recordatorios. Esta acción no se puede deshacer.")
				operation.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
			}
		},
		confirmButton = {
			TextButton(
				onClick = onConfirm,
				enabled = !operation.busy
			) { Text(if (operation.busy) "Eliminando…" else "Eliminar selección") }
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss,
				enabled = !operation.busy
			) { Text("Cancelar") }
		}
	)
}
