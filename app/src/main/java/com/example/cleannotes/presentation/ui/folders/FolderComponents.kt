package com.example.cleannotes.presentation.ui.folders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteFolder
import com.example.cleannotes.presentation.notebook.FolderOperation
import com.example.cleannotes.presentation.ui.design.GlassBottomSheet
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.design.glassMaterial

@Composable
fun FolderGridHeader(onManage: () -> Unit) {
	Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
		Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Text("Carpetas", style = MaterialTheme.typography.titleLarge)
			Text(
				"Un espacio para cada idea",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
		IconButton(onClick = onManage) { Icon(MindIcons.CreateNewFolder, "Gestionar carpetas") }
	}
}

@Composable
fun FolderTile(
	name: String,
	count: Int,
	selected: Boolean,
	onSelect: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Surface(
		onClick = onSelect,
		modifier =
			modifier
				.fillMaxWidth()
				.heightIn(min = 140.dp)
				.semantics {
					contentDescription = "Abrir carpeta $name"
					this.selected = selected
				}
				.glassMaterial(RoundedCornerShape(24.dp), selected = selected),
		shape = RoundedCornerShape(24.dp),
		color = Color.Transparent,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				Icon(
					MindIcons.FolderOpen,
					null,
					Modifier.size(28.dp),
					tint = MaterialTheme.colorScheme.tertiary,
				)
				Spacer(Modifier.weight(1f))
				if (selected)
					Icon(
						MindIcons.Check,
						"Carpeta seleccionada",
						Modifier.size(18.dp),
						tint = MaterialTheme.colorScheme.tertiary,
					)
			}
			Spacer(Modifier.height(12.dp))
			Text(
				name,
				style = MaterialTheme.typography.titleSmall,
				maxLines = 2,
				overflow = TextOverflow.Ellipsis,
			)
			Text(
				"$count ${if (count == 1) "nota" else "notas"}",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
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
	var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
	val editing = folders.firstOrNull { it.id == editingId }
	var deletingId by rememberSaveable { mutableStateOf<Int?>(null) }
	val deleting = folders.firstOrNull { it.id == deletingId }
	var name by rememberSaveable { mutableStateOf("") }
	var lastRevision by rememberSaveable { mutableIntStateOf(operation.revision) }
	LaunchedEffect(operation.revision) {
		if (operation.revision != lastRevision) {
			creating = false
			editingId = null
			deletingId = null
			lastRevision = operation.revision
		}
	}
	if (creating || editing != null) {
		AlertDialog(
			onDismissRequest = {
				if (!operation.busy) {
					creating = false
					editingId = null
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
						editingId = null
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
					deletingId = null
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
						deletingId = null
						onClearError()
					},
					enabled = !operation.busy,
				) {
					Text("Cancelar")
				}
			},
		)
	}
	GlassBottomSheet(
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
						modifier = Modifier.animateItem(),
						headlineContent = {
							Text(folder.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
						},
						leadingContent = { Icon(MindIcons.FolderOpen, null) },
						trailingContent = {
							Row {
								IconButton(
									onClick = {
										editingId = folder.id
										name = folder.name
										onClearError()
									},
									enabled = !operation.busy,
								) {
									Icon(MindIcons.EditNote, "Renombrar ${folder.name}")
								}
								IconButton(
									onClick = {
										deletingId = folder.id
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
	GlassBottomSheet(
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
		modifier =
			Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
				contentDescription = "Mover a $name"
			},
	) {
		Icon(MindIcons.FolderOpen, null)
		Text(name, Modifier.weight(1f).padding(horizontal = 12.dp))
		if (selected) Icon(MindIcons.Check, "Carpeta actual")
	}
}
