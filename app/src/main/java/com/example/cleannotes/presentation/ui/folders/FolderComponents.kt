package com.example.cleannotes.presentation.ui.folders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteFolder
import com.example.cleannotes.presentation.notebook.FolderOperation
import com.example.cleannotes.presentation.ui.design.MindIcons

@Composable
fun FolderStrip(
	folders: List<NoteFolder>,
	notes: List<Note>,
	selected: Int?,
	onSelect: (Int?) -> Unit,
	onManage: () -> Unit,
) {
	Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
		Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
			Text("Carpetas", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
			TextButton(
				onClick = onManage,
				modifier = Modifier.semantics { contentDescription = "Gestionar carpetas" },
			) {
				Icon(MindIcons.CreateNewFolder, null, Modifier.size(20.dp))
				Spacer(Modifier.width(8.dp))
				Text("Gestionar")
			}
		}
		Row(
			Modifier.horizontalScroll(rememberScrollState()),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			FilterChip(selected == null, { onSelect(null) }, label = { Text("Todas las notas") })
			FilterChip(selected == 0, { onSelect(0) }, label = { Text("Sin carpeta") })
			folders.forEach { folder ->
				val count = notes.count { it.folderId == folder.id }
				FilterChip(
					selected == folder.id,
					{ onSelect(folder.id) },
					label = { Text("${folder.name} · $count") },
					leadingIcon = { Icon(MindIcons.FolderOpen, null, Modifier.size(18.dp)) },
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderManager(
	folders: List<NoteFolder>,
	operation: FolderOperation,
	onSave: (Int?, String) -> Unit,
	onDelete: (Int) -> Unit,
	onClearError: () -> Unit,
	onDismiss: () -> Unit,
) {
	var creating by rememberSaveable { mutableStateOf(false) }
	var editing by remember { mutableStateOf<NoteFolder?>(null) }
	var deleting by remember { mutableStateOf<NoteFolder?>(null) }
	var name by rememberSaveable { mutableStateOf("") }
	LaunchedEffect(operation.revision) {
		creating = false
		editing = null
		deleting = null
	}
	if (creating || editing != null) {
		AlertDialog(
			onDismissRequest = {
				if (!operation.busy) {
					creating = false
					editing = null
					onClearError()
				}
			},
			title = { Text(if (creating) "Nueva carpeta" else "Renombrar carpeta") },
			text = {
				Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
					OutlinedTextField(
						name,
						{
							name = it
							onClearError()
						},
						label = { Text("Nombre de la carpeta") },
						singleLine = true,
						enabled = !operation.busy,
						isError = operation.error != null,
						supportingText = { Text("${name.length}/40") },
					)
					operation.error?.let {
						Text(
							it,
							color = MaterialTheme.colorScheme.error,
							style = MaterialTheme.typography.bodySmall,
						)
					}
				}
			},
			confirmButton = {
				TextButton(
					onClick = { onSave(editing?.id, name) },
					enabled = !operation.busy && name.isNotBlank() && name.length <= 40,
				) {
					Text(if (operation.busy) "Guardando…" else "Guardar")
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						creating = false
						editing = null
						onClearError()
					},
					enabled = !operation.busy,
				) {
					Text("Cancelar")
				}
			},
		)
	}
	deleting?.let { folder ->
		AlertDialog(
			onDismissRequest = {
				if (!operation.busy) {
					deleting = null
					onClearError()
				}
			},
			title = { Text("¿Eliminar ${folder.name}?") },
			text = {
				Column {
					Text("Tus notas se conservarán en Sin carpeta.")
					operation.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
				}
			},
			confirmButton = {
				TextButton(onClick = { onDelete(folder.id) }, enabled = !operation.busy) {
					Text("Eliminar carpeta", color = MaterialTheme.colorScheme.error)
				}
			},
			dismissButton = {
				TextButton(
					onClick = {
						deleting = null
						onClearError()
					},
					enabled = !operation.busy,
				) {
					Text("Cancelar")
				}
			},
		)
	}
	ModalBottomSheet(
		onDismissRequest = { if (!operation.busy) onDismiss() },
		sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
	) {
		Column(
			Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text("Tus carpetas", style = MaterialTheme.typography.titleLarge)
			Text(
				"Un lugar para cada tema.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			OutlinedButton(
				onClick = {
					name = ""
					creating = true
					onClearError()
				},
				enabled = !operation.busy,
				modifier = Modifier.fillMaxWidth(),
			) {
				Icon(MindIcons.CreateNewFolder, null)
				Spacer(Modifier.width(8.dp))
				Text("Nueva carpeta")
			}
			if (folders.isEmpty())
				Text(
					"Crea tu primera carpeta y agrupa las notas que quieras.",
					style = MaterialTheme.typography.bodyMedium,
				)
			LazyColumn(Modifier.heightIn(max = 400.dp)) {
				items(folders, key = { it.id }) { folder ->
					ListItem(
						headlineContent = {
							Text(folder.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
						},
						leadingContent = { Icon(MindIcons.FolderOpen, null) },
						trailingContent = {
							Row {
								IconButton(
									onClick = {
										editing = folder
										name = folder.name
										onClearError()
									},
									enabled = !operation.busy,
								) {
									Icon(MindIcons.EditNote, "Renombrar ${folder.name}")
								}
								IconButton(
									onClick = {
										deleting = folder
										onClearError()
									},
									enabled = !operation.busy,
								) {
									Icon(MindIcons.Delete, "Eliminar ${folder.name}")
								}
							}
						},
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveNoteSheet(
	note: Note,
	folders: List<NoteFolder>,
	operation: FolderOperation,
	onMove: (Int?) -> Unit,
	onManage: () -> Unit,
	onDismiss: () -> Unit,
) {
	val initialRevision = remember { operation.revision }
	LaunchedEffect(operation.revision) { if (operation.revision != initialRevision) onDismiss() }
	ModalBottomSheet(
		onDismissRequest = { if (!operation.busy) onDismiss() },
		sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
	) {
		Column(
			Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text("Mover a carpeta", style = MaterialTheme.typography.titleLarge)
			Text(
				note.title,
				style = MaterialTheme.typography.bodyMedium,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			operation.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
			LazyColumn(Modifier.heightIn(max = 400.dp)) {
				item {
					FolderDestination("Sin carpeta", note.folderId == null, !operation.busy) {
						if (note.folderId == null) onDismiss() else onMove(null)
					}
				}
				items(folders, key = { it.id }) { folder ->
					FolderDestination(folder.name, note.folderId == folder.id, !operation.busy) {
						if (note.folderId == folder.id) onDismiss() else onMove(folder.id)
					}
				}
			}
			TextButton(onClick = onManage, enabled = !operation.busy) {
				Icon(MindIcons.CreateNewFolder, null)
				Spacer(Modifier.width(8.dp))
				Text("Gestionar carpetas")
			}
		}
	}
}

@Composable
private fun FolderDestination(
	name: String,
	selected: Boolean,
	enabled: Boolean,
	onClick: () -> Unit,
) {
	TextButton(
		onClick = onClick,
		enabled = enabled,
		modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
	) {
		Icon(MindIcons.FolderOpen, null)
		Text(name, Modifier.weight(1f).padding(horizontal = 12.dp))
		if (selected) Icon(MindIcons.Check, "Carpeta actual")
	}
}
