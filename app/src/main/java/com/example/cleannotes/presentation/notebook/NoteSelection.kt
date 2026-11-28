package com.example.cleannotes.presentation.notebook

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cleannotes.R
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.presentation.ui.design.DesignIconButton
import com.example.cleannotes.presentation.ui.design.MindIcons

/** The contextual action uses exactly the same filtered collection as the visible cards. */
internal fun visibleNotes(state: NotesUiState, filter: String): List<Note> {
	val term = state.query.trim()
	return state.allNotes.filter { note ->
		when (state.selectedTab) {
			NotesTab.HOME -> !note.isReminder
			NotesTab.REMINDERS -> note.document.let { (note.isReminder || it.reminder != null) && !it.reminderCompleted }
			NotesTab.NOTES, NotesTab.SEARCH -> {
				val matchesFilter = when (filter) {
					"Fijadas" -> note.document.pinned
					"Con imágenes" -> note.document.images.isNotEmpty()
					else -> true
				}
				matchesFilter && if (state.selectedTab == NotesTab.NOTES) {
					!note.isReminder && (state.selectedFolderId == null || if (state.selectedFolderId == 0) note.folderId == null else note.folderId == state.selectedFolderId)
				} else term.isBlank() || note.title.contains(term, true) || note.searchableText.contains(
					term,
					true
				)
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
	onDelete: () -> Unit,
	onShare: (() -> Unit)? = null,
	onMove: (() -> Unit)? = null,
	onPin: (() -> Unit)? = null,
	pinned: Boolean = false,
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
			if (onShare != null) Box {
				var expanded by remember { mutableStateOf(false) }
				DesignIconButton(MindIcons.MoreVert, "Más acciones de selección", enabled) {
					expanded = true
				}
				DropdownMenu(expanded, { expanded = false }) {
					DropdownMenuItem(
						text = { Text("Compartir nota") },
						onClick = { expanded = false; onShare() })
					onMove?.let { action ->
						DropdownMenuItem(
							text = { Text("Mover a carpeta") },
							onClick = { expanded = false; action() })
					}
					onPin?.let { action ->
						DropdownMenuItem(
							text = { Text(if (pinned) "Desfijar" else "Fijar a mano") },
							onClick = { expanded = false; action() })
					}
				}
			}
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
			) { Text(stringResource(R.string.cancel)) }
		}
	)
}
