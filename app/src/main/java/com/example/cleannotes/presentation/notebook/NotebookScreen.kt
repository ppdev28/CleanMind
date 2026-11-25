package com.example.cleannotes.presentation.notebook

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.creationTimestamp
import com.example.cleannotes.presentation.ui.components.AgendaDayGroup
import com.example.cleannotes.presentation.ui.components.BottomNavigationBar
import com.example.cleannotes.presentation.ui.components.CustomTopBar
import com.example.cleannotes.presentation.ui.components.DeleteNoteDialog
import com.example.cleannotes.presentation.ui.design.*
import com.example.cleannotes.presentation.ui.design.AppBottomSheet
import com.example.cleannotes.presentation.ui.edit_note.ExportMenu
import com.example.cleannotes.presentation.ui.folders.*
import com.example.cleannotes.presentation.ui.motion.AppMotion
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
	onFolderSelected: (Int?) -> Unit = {},
	onSaveFolder: (Int?, String) -> Unit = { _, _ -> },
	onDeleteFolder: (Int) -> Unit = {},
	onMoveNote: (Note, Int?) -> Unit = { _, _ -> },
	onClearFolderError: () -> Unit = {},
	active: Boolean = true,
) {
	val topBarScroll = TopAppBarDefaults.enterAlwaysScrollBehavior()
	LaunchedEffect(state.selectedTab) {
		topBarScroll.state.heightOffset = 0f
		topBarScroll.state.contentOffset = 0f
	}
	BackHandler(enabled = active && state.selectedTab != NotesTab.HOME) {
		onTabSelected(NotesTab.HOME)
	}
	var filter by rememberSaveable { mutableStateOf("Todas") }
	var grid by rememberSaveable { mutableStateOf(false) }
	var options by remember { mutableStateOf<Note?>(null) }
	var deleting by remember { mutableStateOf<Note?>(null) }
	var sharing by remember { mutableStateOf<Note?>(null) }
	var managingFolders by rememberSaveable { mutableStateOf(false) }
	var moving by remember { mutableStateOf<Note?>(null) }
	if (managingFolders)
		FolderManager(
			state.folders,
			state.folderOperation,
			onSaveFolder,
			onDeleteFolder,
			onClearFolderError,
		) {
			managingFolders = false
			onClearFolderError()
		}
	moving?.let { original ->
		val note = state.allNotes.firstOrNull { it.id == original.id } ?: original
		MoveNoteSheet(
			note,
			state.folders,
			state.folderOperation,
			onMove = { onMoveNote(note, it) },
			onManage = {
				moving = null
				managingFolders = true
				onClearFolderError()
			},
			onDismiss = {
				moving = null
				onClearFolderError()
			},
		)
	}
	val snackbar = remember { SnackbarHostState() }
	val tabStates = rememberSaveableStateHolder()
	val scope = rememberCoroutineScope()
	var pendingArchive by remember { mutableStateOf<Pair<Note, Boolean>?>(null) }
	val active = state.allNotes.filterNot { NoteDocument.decode(it.content).archived }
	val notes = state.allNotes.filterNot { it.isReminder }
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
		ExportMenu(note.title, NoteDocument.decode(note.content)) {
			sharing = null
		}
	}
	options?.let { note ->
		val doc = NoteDocument.decode(note.content)
		AppBottomSheet(
			onDismissRequest = { options = null },
			sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		) {
			Column(
				Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
			) {
				Text(note.title, style = MaterialTheme.typography.titleMedium)
				if (!note.isReminder)
					TextButton(
						onClick = {
							options = null
							moving = note
							onClearFolderError()
						}
					) {
						Icon(MindIcons.FolderOpen, null)
						Spacer(Modifier.width(12.dp))
						Text("Mover a carpeta")
					}
				TextButton(
					onClick = {
						options = null
						sharing = note
					}
				) {
					Icon(
						MindIcons.Share,
						null,
					)
					Spacer(Modifier.width(12.dp))
					Text("Compartir nota")
				}
				TextButton(
					onClick = {
						options = null
						onUpdateNote(note) { it.copy(pinned = !it.pinned) }
					}
				) {
					Icon(
						MindIcons.PushPin,
						null,
					)
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
					Icon(
						MindIcons.Delete,
						null,
						tint = MaterialTheme.colorScheme.error,
					)
					Spacer(Modifier.width(12.dp))
					Text(
						"Eliminar",
						color = MaterialTheme.colorScheme.error,
					)
				}
			}
		}
	}
	Scaffold(
		modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
		containerColor = androidx.compose.ui.graphics.Color.Transparent,
		contentWindowInsets = WindowInsets.statusBars,
		snackbarHost = { DesignSnackbarHost(snackbar) },
		topBar = {
			Column {
				Box(
					Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
						.background(MaterialTheme.colorScheme.background)
				)
				CustomTopBar(
					onSettings,
					scrollBehavior = topBarScroll,
					windowInsets = WindowInsets(0.dp),
					title =
						when (state.selectedTab) {
							NotesTab.HOME -> null
							NotesTab.NOTES -> "Notas"
							NotesTab.REMINDERS -> "Recordatorios"
							NotesTab.SEARCH -> "Buscar"
						},
				)
			}
		},
		bottomBar = {
			BottomNavigationBar(
				state.selectedTab,
				{
					onQueryChange("")
					onTabSelected(it)
				},
				onCreate,
				folderName =
					if (state.selectedTab == NotesTab.NOTES)
						state.folders.firstOrNull { it.id == state.selectedFolderId }?.name
					else null,
			)
		},
	) { padding ->
		val contentTopInset = if (state.error == null) padding.calculateTopPadding() else 0.dp
		Column(Modifier.fillMaxSize()) {
			if (state.error != null)
				Row(
					Modifier.padding(top = padding.calculateTopPadding()).padding(horizontal = 24.dp),
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
			AnimatedContent(
				targetState = state.isLoading to state.selectedTab,
				modifier = Modifier.fillMaxSize(),
				transitionSpec = {
					AppMotion.content(targetState.second.ordinal >= initialState.second.ordinal)
				},
				label = "notebookSection",
			) { (loading, selectedTab) ->
				val notesTab = selectedTab in listOf(NotesTab.NOTES, NotesTab.SEARCH)
				if (loading)
					Box(
						Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
						contentAlignment = Alignment.Center,
					) {
						CircularProgressIndicator()
					}
				else
					tabStates.SaveableStateProvider(selectedTab) {
						when {
							selectedTab == NotesTab.HOME ->
								LazyColumn(
									contentPadding =
										PaddingValues(
											start = 24.dp,
											end = 24.dp,
											top = contentTopInset + 8.dp,
											bottom = padding.calculateBottomPadding() + 24.dp,
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

									val agendaNotes = active.filterNot { it.isReminder }
									val days =
										agendaNotes
											.sortedWith(
												compareByDescending<Note> { it.creationTimestamp() }
													.thenByDescending { it.id }
											)
											.groupBy { DateUtils.dayKey(it.creationTimestamp()) }
									items(days.entries.toList(), key = { "agenda-${it.key}" }) { (_, notesOfDay) ->
										Column(Modifier.animateItem()) {
											AgendaDayGroup(notesOfDay, onOpenNote) { options = it }
										}
									}
									if (agendaNotes.isEmpty())
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
								val listLayoutState = rememberLazyGridState()
								val gridLayoutState = rememberLazyGridState()
								val filtered =
									(if (selectedTab == NotesTab.SEARCH) state.allNotes else notes)
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
													(selectedTab != NotesTab.NOTES ||
															state.selectedFolderId == null ||
															(if (state.selectedFolderId == 0)
																note.folderId == null
															else
																note.folderId == state.selectedFolderId)) &&
													(selectedTab != NotesTab.SEARCH ||
															state.query.isBlank() ||
															note.title.contains(
																state.query,
																true,
															) ||
															NoteDocument.plainText(note.content)
																.contains(state.query, true))
										}
								val header: @Composable () -> Unit = {
									Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
										if (selectedTab == NotesTab.SEARCH)
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
																MaterialTheme.colorScheme
																	.primaryContainer
															else
																MaterialTheme.colorScheme
																	.background,
														shape = RoundedCornerShape(12.dp),
													) {
														Row(
															Modifier.heightIn(min = 48.dp)
																.padding(horizontal = 10.dp),
															verticalAlignment =
																Alignment.CenterVertically,
															horizontalArrangement =
																Arrangement.spacedBy(6.dp),
														) {
															if (filter == label)
																Icon(
																	MindIcons.Check,
																	null,
																	Modifier.size(16.dp),
																)
															Text(
																label,
																style =
																	MaterialTheme.typography
																		.bodySmall,
															)
														}
													}
												}
										}
										Row(
											Modifier.fillMaxWidth(),
											verticalAlignment = Alignment.CenterVertically,
										) {
											Text(
												if (selectedTab == NotesTab.SEARCH) {
													if (state.query.isBlank()) "Busca en tu libreta"
													else "Coincidencias en título y contenido"
												} else
													state.folders
														.firstOrNull {
															it.id == state.selectedFolderId
														}
														?.name
														?: if (state.selectedFolderId == 0)
															"Sin carpeta"
														else "Todas las notas",
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
								Crossfade(
									targetState = grid,
									animationSpec = tween(AppMotion.Short),
									label = "noteCollectionLayout",
								) { gridMode ->
									LazyVerticalGrid(
										columns = GridCells.Adaptive(128.dp),
										state = if (gridMode) gridLayoutState else listLayoutState,
										contentPadding =
											PaddingValues(
												start = 24.dp,
												end = 24.dp,
												top = contentTopInset + 8.dp,
												bottom = padding.calculateBottomPadding() + 24.dp,
											),
										verticalArrangement = Arrangement.spacedBy(16.dp),
										horizontalArrangement = Arrangement.spacedBy(16.dp),
									) {
										if (selectedTab == NotesTab.NOTES) {
											item(
												key = "folder-header",
												span = { GridItemSpan(maxLineSpan) },
											) {
												FolderGridHeader {
													managingFolders = true
													onClearFolderError()
												}
											}
											item(key = "folder-all") {
												FolderTile(
													"Todas las notas",
													notes.size,
													state.selectedFolderId == null,
													{ onFolderSelected(null) },
													Modifier.animateItem(),
												)
											}
											item(key = "folder-unfiled") {
												FolderTile(
													"Sin carpeta",
													notes.count { it.folderId == null },
													state.selectedFolderId == 0,
													{ onFolderSelected(0) },
													Modifier.animateItem(),
												)
											}
											items(state.folders, key = { "folder-${it.id}" }) { folder ->
												FolderTile(
													folder.name,
													notes.count { it.folderId == folder.id },
													state.selectedFolderId == folder.id,
													{ onFolderSelected(folder.id) },
													Modifier.animateItem(),
												)
											}
										}
										item(
											key = "collection-tools",
											span = { GridItemSpan(maxLineSpan) },
										) {
											header()
										}

										items(
											filtered,
											key = { requireNotNull(it.id) },
											span = {
												GridItemSpan(if (gridMode) 1 else maxLineSpan)
											},
										) { note ->
											DesignNoteCard(
												note,
												modifier = Modifier.animateItem(),
												onClick = { onOpenNote(note) },
												onLongClick = { options = note },
												compact = gridMode,
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
							}

							else ->
								ReminderCollection(
									active,
									onOpenNote,
									onCreate,
									onUpdateNote,
									bottomInset = padding.calculateBottomPadding(),
									topInset = contentTopInset,
								) {
									deleting = it
								}
						}
					}
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
		modifier = Modifier.appSurface(RoundedCornerShape(14.dp)),
		color = androidx.compose.ui.graphics.Color.Transparent,
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
		Modifier.fillMaxWidth().animateContentSize().padding(vertical = 32.dp),
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
	onOpen: (Note) -> Unit,
	onCreate: (Boolean) -> Unit,
	update: NoteChange,
	bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
	topInset: androidx.compose.ui.unit.Dp = 0.dp,
	onDelete: (Note) -> Unit,
) {
	val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
	var editing by remember { mutableStateOf<Note?>(null) }
	var snoozing by remember { mutableStateOf<Int?>(null) }
	editing?.let { note ->
		com.example.cleannotes.presentation.ui.edit_note.ReminderSheet(
			note.title,
			NoteDocument.decode(note.content),
			onConfirm = { draft ->
				update(note) { it.copy(reminder = draft.reminder, reminderCompleted = false) }
				editing = null
			},
			onDismiss = { editing = null },
		)
	}
	val reminders =
		notes
			.filter {
				val d = NoteDocument.decode(it.content)
				(it.isReminder || d.reminder != null) && !d.reminderCompleted
			}
			.sortedBy { NoteDocument.decode(it.content).reminder?.at ?: Long.MAX_VALUE }
	LazyColumn(
		contentPadding =
			PaddingValues(
				start = 24.dp,
				end = 24.dp,
				top = topInset + 8.dp,
				bottom = bottomInset + 24.dp
			),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			Text(
				"Tus pendientes, en orden.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(bottom = 16.dp),
			)
		}
		val groups = reminders.groupBy {
			NoteDocument.decode(it.content).reminder?.at?.let(DateUtils::dayKey) ?: "Sin fecha"
		}
		groups.forEach { (_, group) ->
			val at = NoteDocument.decode(group.first().content).reminder?.at
			item {
				Column(
					Modifier.padding(top = 12.dp),
					verticalArrangement = Arrangement.spacedBy(8.dp),
				) {
					Text(
						if (at == null) "Sin aviso"
						else SimpleDateFormat("d MMMM", locale).format(Date(at)),
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
				}
			}
			items(group, key = { requireNotNull(it.id) }) { note ->
				val doc = NoteDocument.decode(note.content)
				Row(
					Modifier.fillMaxWidth().heightIn(min = 72.dp).animateItem(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					Checkbox(
						false,
						{ update(note) { it.completeReminder(System.currentTimeMillis()) } },
						modifier =
							Modifier.semantics { contentDescription = "Completar ${note.title}" },
					)
					Column(
						Modifier.weight(1f)
							.combinedClickable(
								onClick = {
									if (note.isReminder) onOpen(note) else editing = note
								},
								onLongClick = { onDelete(note) },
								onLongClickLabel = "Eliminar recordatorio",
							)
							.padding(vertical = 12.dp),
						verticalArrangement = Arrangement.spacedBy(4.dp),
					) {
						Text(note.title, style = MaterialTheme.typography.titleSmall)
						Text(
							doc.reminder?.let {
								DateUtils.getTime(it.at) +
										(if (it.repeatUnit == "none") "" else " · Repite")
							} ?: "Sin notificación",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
					if (doc.reminder != null)
						Box {
							IconButton(onClick = { snoozing = note.id }) {
								Icon(
									MindIcons.Clock,
									"Posponer ${note.title}",
								)
							}
							DropdownMenu(snoozing == note.id, { snoozing = null }) {
								listOf(
									10 to "10 minutos",
									60 to "1 hora",
									1440 to "1 día",
								)
									.forEach { (minutes, label) ->
										DropdownMenuItem(
											text = { Text(label) },
											onClick = {
												snoozing = null
												update(note) { latest ->
													latest.copy(
														reminder =
															latest.reminder?.copy(
																at =
																	System.currentTimeMillis() +
																			minutes * 60_000L
															)
													)
												}
											},
										)
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
	}
}
