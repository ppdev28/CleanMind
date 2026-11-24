package com.example.cleannotes.presentation.notebook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.creationTimestamp
import com.example.cleannotes.presentation.ui.components.AgendaDayGroup
import com.example.cleannotes.presentation.ui.components.BottomNavigationBar
import com.example.cleannotes.presentation.ui.components.CustomTopBar
import com.example.cleannotes.presentation.ui.components.DeleteNoteDialog
import com.example.cleannotes.presentation.ui.design.*
import com.example.cleannotes.presentation.ui.edit_note.ExportMenu
import com.example.cleannotes.presentation.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.launch

typealias NoteChange = (Note, (NoteDocument) -> NoteDocument) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
	state: NotesUiState,
	onTabSelected: (NotesTab) -> Unit,
	onQueryChange: (String) -> Unit,
	onDelete: (Note) -> Unit,
	onRetry: () -> Unit,
	onDismissError: () -> Unit,
	onOpenNote: (Note) -> Unit,
	onCreate: (Boolean) -> Unit,
	onSettings: () -> Unit,
	onUpdateNote: NoteChange = { _, _ -> },
	onQuickCreate: () -> Unit = { onCreate(false) },
) {
	BackHandler(enabled = state.selectedTab != NotesTab.HOME) { onTabSelected(NotesTab.HOME) }
	var filter by rememberSaveable { mutableStateOf("Todas") }
	var grid by rememberSaveable { mutableStateOf(false) }
	var options by remember { mutableStateOf<Note?>(null) }
	var deleting by remember { mutableStateOf<Note?>(null) }
	var sharing by remember { mutableStateOf<Note?>(null) }
	val snackbar = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	var pendingArchive by remember { mutableStateOf<Pair<Note, Boolean>?>(null) }
	val active = state.allNotes.filterNot { NoteDocument.decode(it.content).archived }
	val notes = state.allNotes.filterNot { it.isReminder }
	val notesTab = state.selectedTab in listOf(NotesTab.NOTES, NotesTab.SEARCH)
	fun archive(note: Note) {
		val archived = NoteDocument.decode(note.content).archived
		pendingArchive = note to archived
		onUpdateNote(note) { it.copy(archived = !archived) }
	}
	LaunchedEffect(state.allNotes, state.error, pendingArchive) {
		val pending = pendingArchive ?: return@LaunchedEffect
		if (state.error != null) {
			pendingArchive = null
			return@LaunchedEffect
		}
		val saved =
			state.allNotes.firstOrNull { it.id == pending.first.id } ?: return@LaunchedEffect
		if (NoteDocument.decode(saved.content).archived == pending.second) return@LaunchedEffect
		pendingArchive = null
		scope.launch {
			val result =
				snackbar.showSnackbar(
					if (pending.second) "Nota restaurada" else "Nota archivada",
					"Deshacer",
				)
			if (result == SnackbarResult.ActionPerformed)
				onUpdateNote(saved) { it.copy(archived = pending.second) }
		}
	}
	DeleteNoteDialog(deleting, { deleting = null }, onDelete)
	sharing?.let { note ->
		ExportMenu(note.title, NoteDocument.decode(note.content)) { sharing = null }
	}
	options?.let { note ->
		val doc = NoteDocument.decode(note.content)
		ModalBottomSheet(
			onDismissRequest = { options = null },
			containerColor = MaterialTheme.colorScheme.surface,
			sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		) {
			Column(
				Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Text(note.title, style = MaterialTheme.typography.titleMedium)
				TextButton(
					onClick = {
						options = null
						sharing = note
					}
				) {
					Icon(MindIcons.Share, null)
					Spacer(Modifier.width(12.dp))
					Text("Compartir nota")
				}
				TextButton(
					onClick = {
						options = null
						onUpdateNote(note) { it.copy(pinned = !it.pinned) }
					}
				) {
					Icon(MindIcons.PushPin, null)
					Spacer(Modifier.width(12.dp))
					Text(if (doc.pinned) "Desfijar" else "Fijar a mano")
				}
				TextButton(
					onClick = {
						options = null
						archive(note)
					}
				) {
					DesignIcon(MindIcons.Archive)
					Spacer(Modifier.width(12.dp))
					Text(if (doc.archived) "Desarchivar" else "Archivar")
				}
				TextButton(
					onClick = {
						options = null
						deleting = note
					}
				) {
					Icon(MindIcons.Delete, null, tint = MaterialTheme.colorScheme.error)
					Spacer(Modifier.width(12.dp))
					Text("Eliminar", color = MaterialTheme.colorScheme.error)
				}
			}
		}
	}
	Scaffold(
		contentWindowInsets = WindowInsets.statusBars,
		snackbarHost = { DesignSnackbarHost(snackbar) },
		topBar = {
			CustomTopBar(
				onSettings,
				title =
					when (state.selectedTab) {
						NotesTab.HOME -> null
						NotesTab.NOTES -> "Notas"
						NotesTab.REMINDERS -> "Recordatorios"
						NotesTab.SEARCH -> "Buscar"
					},
			)
		},
		bottomBar = {
			BottomNavigationBar(
				state.selectedTab,
				{
					onQueryChange("")
					onTabSelected(it)
				},
				onCreate,
			)
		},
	) { padding ->
		Column(Modifier.fillMaxSize().padding(padding)) {
			if (state.error != null)
				Row(
					Modifier.padding(horizontal = 24.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						"No se pudo completar la operación",
						Modifier.weight(1f),
						color = MaterialTheme.colorScheme.error,
					)
					TextButton(
						onClick = if (state.error == NotesError.LOAD) onRetry else onDismissError
					) {
						Text("Reintentar")
					}
				}
			if (state.isLoading)
				Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			else
				when {
					state.selectedTab == NotesTab.HOME ->
						LazyColumn(
							contentPadding =
								PaddingValues(
									start = 24.dp,
									end = 24.dp,
									top = 8.dp,
									bottom = 24.dp,
								),
							verticalArrangement = Arrangement.spacedBy(16.dp),
						) {
							item {
								SearchEntry(
									"",
									{
										onQueryChange(it)
										onTabSelected(NotesTab.SEARCH)
									},
									onClick = { onTabSelected(NotesTab.SEARCH) },
								)
							}
							val days =
								active
									.sortedWith(
										compareByDescending<Note> { it.creationTimestamp() }
											.thenByDescending { it.id }
									)
									.groupBy { DateUtils.dayKey(it.creationTimestamp()) }
							items(days.entries.toList(), key = { "agenda-${it.key}" }) { (_, notesOfDay) ->
								AgendaDayGroup(notesOfDay, onOpenNote) { options = it }
							}
							if (active.isEmpty())
								item {
									EmptyDesignState(
										"Un lugar para tus ideas",
										"Todavía no hay notas. Guarda lo que quieras retomar.",
										"Crear una nota",
									) {
										onQuickCreate()
									}
								}
						}

					notesTab -> {
						val filtered =
							(if (state.selectedTab == NotesTab.SEARCH) state.allNotes else notes)
								.filter { note ->
									val doc = NoteDocument.decode(note.content)
									val matchesFilter =
										when (filter) {
											"Archivadas" -> doc.archived
											"Fijadas" -> doc.pinned && !doc.archived
											"Con imágenes" ->
												doc.images.isNotEmpty() && !doc.archived

											else -> !doc.archived
										}
									matchesFilter &&
											(state.query.isBlank() ||
													note.title.contains(state.query, true) ||
													NoteDocument.plainText(note.content)
														.contains(state.query, true))
								}
						val header: @Composable () -> Unit = {
							Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
								SearchEntry(state.query, onQueryChange)
								Row(
									Modifier.fillMaxWidth()
										.horizontalScroll(rememberScrollState())
										.selectableGroup(),
									horizontalArrangement = Arrangement.spacedBy(4.dp),
								) {
									listOf("Todas", "Fijadas", "Con imágenes", "Archivadas")
										.forEach { label ->
											Surface(
												modifier =
													Modifier.selectable(
														filter == label,
														role = Role.Tab,
														onClick = { filter = label },
													),
												color =
													if (filter == label)
														MaterialTheme.colorScheme.primaryContainer
													else MaterialTheme.colorScheme.background,
												shape = RoundedCornerShape(12.dp),
											) {
												Row(
													Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp),
													verticalAlignment = Alignment.CenterVertically,
													horizontalArrangement = Arrangement.spacedBy(6.dp),
												) {
													if (filter == label) Icon(MindIcons.Check, null, Modifier.size(16.dp))
													Text(label, style = MaterialTheme.typography.bodySmall)
												}
											}
										}
								}
								Row(
									Modifier.fillMaxWidth(),
									verticalAlignment = Alignment.CenterVertically,
								) {
									Text(
										if (state.query.isBlank()) "Tus notas"
										else "Coincidencias en título y contenido",
										Modifier.weight(1f),
										style = MaterialTheme.typography.bodyMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant,
									)
									DesignIconButton(
										if (grid) MindIcons.List else MindIcons.GridView,
										"Cambiar vista",
									) {
										grid = !grid
									}
								}
							}
						}
						LazyVerticalGrid(
							columns = GridCells.Fixed(if (grid) 2 else 1),
							contentPadding =
								PaddingValues(
									start = 24.dp,
									end = 24.dp,
									top = 8.dp,
									bottom = 24.dp,
								),
							verticalArrangement = Arrangement.spacedBy(16.dp),
							horizontalArrangement = Arrangement.spacedBy(16.dp),
						) {
							item(span = { GridItemSpan(maxLineSpan) }) { header() }
							items(filtered, key = { requireNotNull(it.id) }) { note ->
								DesignNoteCard(
									note,
									onClick = { onOpenNote(note) },
									onLongClick = { options = note },
									compact = grid,
								)
							}
							if (filtered.isEmpty())
								item(span = { GridItemSpan(maxLineSpan) }) {
									EmptyDesignState(
										"Sin notas aquí",
										"Prueba otra búsqueda o guarda una nueva idea.",
										"Crear una nota",
									) {
										onCreate(false)
									}
								}
						}
					}

					else ->
						ReminderCollection(active, onSettings, onOpenNote, onCreate, onUpdateNote)
				}
		}
	}
}

@Composable
private fun SectionLabel(value: String) = Text(value, style = MaterialTheme.typography.titleMedium)

@Composable
private fun SearchEntry(value: String, onChange: (String) -> Unit, onClick: (() -> Unit)? = null) {
	Surface(
		shape = RoundedCornerShape(14.dp),
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
		color = MaterialTheme.colorScheme.surface,
	) {
		Row(
			Modifier.fillMaxWidth()
				.heightIn(min = 48.dp)
				.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
				.padding(horizontal = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp),
		) {
			DesignIcon(MindIcons.Search)
			if (onClick != null)
				Text("Buscar en tus notas", style = MaterialTheme.typography.bodyMedium)
			else
				BasicTextField(
					value,
					onChange,
					modifier = Modifier.weight(1f),
					singleLine = true,
					textStyle =
						MaterialTheme.typography.bodyMedium.copy(
							color = MaterialTheme.colorScheme.onSurface
						),
					cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
					decorationBox = { field ->
						Box {
							if (value.isBlank())
								Text(
									"Buscar en tus notas",
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							field()
						}
					},
				)
		}
	}
}

@Composable
private fun EmptyDesignState(title: String, body: String, action: String, onAction: () -> Unit) {
	Column(
		Modifier.fillMaxWidth().padding(vertical = 32.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		Text(title, style = MaterialTheme.typography.titleLarge)
		Text(
			body,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		Button(onClick = onAction, shape = RoundedCornerShape(16.dp)) { Text(action) }
	}
}

@Composable
private fun ReminderCollection(
	notes: List<Note>,
	onSettings: () -> Unit,
	onOpen: (Note) -> Unit,
	onCreate: (Boolean) -> Unit,
	update: NoteChange,
) {
	val scope = rememberCoroutineScope()
	val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
	val reminders =
		notes
			.filter {
				val d = NoteDocument.decode(it.content)
				(it.isReminder || d.reminder != null) && !d.reminderCompleted
			}
			.sortedBy { NoteDocument.decode(it.content).reminder?.at ?: Long.MAX_VALUE }
	LazyColumn(
		contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		item {
			Text(
				"Lo que quieres retomar, a su tiempo.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
		val groups = reminders.groupBy {
			NoteDocument.decode(it.content).reminder?.at?.let(DateUtils::dayKey) ?: "Sin fecha"
		}
		groups.forEach { (_, group) ->
			val at = NoteDocument.decode(group.first().content).reminder?.at
			item {
				SectionLabel(
					if (at == null) "Sin fecha"
					else
						(if (DateUtils.isToday(at)) "Hoy · " else "") +
								SimpleDateFormat("d MMMM", locale).format(Date(at))
				)
			}
			items(group, key = { requireNotNull(it.id) }) { note ->
				val doc = NoteDocument.decode(note.content)
				Surface(
					shape = RoundedCornerShape(22.dp),
					color = MaterialTheme.colorScheme.surface,
				) {
					Column(
						Modifier.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(12.dp),
					) {
						Text(
							note.title,
							style = MaterialTheme.typography.bodyLarge,
							fontWeight = FontWeight.Medium,
						)
						val message =
							doc.reminder?.message?.ifBlank { doc.subtitle } ?: doc.subtitle
						if (message.isNotBlank())
							Text(
								message,
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						doc.reminder?.let {
							Text(
								DateUtils.shortDate(it.at) + ", " + DateUtils.getTime(it.at),
								style = MaterialTheme.typography.bodyMedium,
							)
						}
						DesignRow("Abrir nota", MindIcons.Arrow) { onOpen(note) }
						Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
							Surface(
								onClick = {
									update(note) { latest ->
										latest.copy(
											reminder =
												latest.reminder?.copy(
													at =
														System.currentTimeMillis() +
																(latest.reminder?.snoozeMinutes ?: 10) *
																60_000L
												)
										)
									}
								},
								enabled = doc.reminder != null,
								modifier = Modifier.weight(1f),
								shape = RoundedCornerShape(12.dp),
								color = MaterialTheme.colorScheme.surfaceContainer,
							) {
								Row(
									Modifier.heightIn(min = 48.dp),
									verticalAlignment = Alignment.CenterVertically,
									horizontalArrangement = Arrangement.Center,
								) {
									DesignIcon(MindIcons.Clock)
									Spacer(Modifier.width(8.dp))
									Text("Posponer", style = MaterialTheme.typography.labelMedium)
								}
							}
							Surface(
								onClick = {
									update(note) { latest ->
										latest.completeReminder(System.currentTimeMillis())
									}
								},
								modifier = Modifier.weight(1f),
								shape = RoundedCornerShape(12.dp),
								color = MaterialTheme.colorScheme.surfaceContainer,
							) {
								Row(
									Modifier.heightIn(min = 48.dp),
									verticalAlignment = Alignment.CenterVertically,
									horizontalArrangement = Arrangement.Center,
								) {
									DesignIcon(MindIcons.Check)
									Spacer(Modifier.width(8.dp))
									Text("Completar", style = MaterialTheme.typography.labelMedium)
								}
							}
						}
					}
				}
			}
		}
		if (reminders.isEmpty())
			item {
				EmptyDesignState(
					"Todo al día",
					"Añade un aviso para lo que quieras retomar.",
					"Añadir recordatorio",
				) {
					onCreate(true)
				}
			}
		item {
			Text(
				"Sin avisos de más. Tú eliges cuándo.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
		if (reminders.isNotEmpty())
			item { TextButton(onClick = { onCreate(true) }) { Text("+ Recordatorio") } }
	}
}
