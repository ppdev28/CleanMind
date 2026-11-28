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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.Note
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.components.HomeAgenda
import com.example.cleannotes.presentation.ui.components.NotebookBottomBar
import com.example.cleannotes.presentation.ui.components.CustomTopBar
import com.example.cleannotes.presentation.ui.components.NoteCreationSheet
import com.example.cleannotes.presentation.ui.design.*
import com.example.cleannotes.presentation.ui.design.AppBottomSheet
import com.example.cleannotes.presentation.ui.edit_note.ExportMenu
import com.example.cleannotes.presentation.ui.folders.*
import com.example.cleannotes.presentation.ui.motion.AppMotion
import com.example.cleannotes.presentation.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date

typealias NoteChange = (Note, (NoteDocument) -> NoteDocument) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
	state: NotesUiState,
	onTabSelected: (NotesTab) -> Unit,
	onQueryChange: (String) -> Unit,
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
	onDeleteSelection: (Set<Int>) -> Unit = {},
	onClearDeletionError: () -> Unit = {},
) {
	var searchFocusRequest by rememberSaveable { mutableIntStateOf(0) }
	val topBarScroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
		canScroll = { active && !state.isLoading }
	)
	LaunchedEffect(state.selectedTab, searchFocusRequest) {
		topBarScroll.state.heightOffset = 0f
		topBarScroll.state.contentOffset = 0f
	}
	BackHandler(enabled = active && state.selectedTab != NotesTab.HOME) {
		onTabSelected(NotesTab.HOME)
	}
	var filter by rememberSaveable { mutableStateOf("Todas") }
	var grid by rememberSaveable { mutableStateOf(false) }
	var homeGrid by rememberSaveable { mutableStateOf(true) }
	var selectedIds by rememberSaveable { mutableStateOf(listOf<Int>()) }
	var confirmSelectionDelete by rememberSaveable { mutableStateOf(false) }
	var deletionRevision by rememberSaveable { mutableIntStateOf(state.deletion.revision) }
	val latestState by rememberUpdatedState(state)
	val selecting = selectedIds.isNotEmpty()
	val visibleNotes = remember(
		state.allNotes,
		state.selectedTab,
		state.selectedFolderId,
		state.query,
		filter
	) { visibleNotes(state, filter) }
	val selectedNote =
		if (selectedIds.size == 1) state.allNotes.firstOrNull { it.id == selectedIds.first() } else null

	fun toggleSelection(note: Note) {
		if (latestState.deletion.busy) return
		val id = note.id ?: return
		selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
	}

	fun openOrSelect(note: Note) {
		if (selectedIds.isNotEmpty()) toggleSelection(note) else onOpenNote(note)
	}
	LaunchedEffect(state.deletion.revision) {
		if (deletionRevision != state.deletion.revision) {
			selectedIds = emptyList(); confirmSelectionDelete = false; deletionRevision =
				state.deletion.revision
		}
	}
	LaunchedEffect(state.allNotes, state.isLoading, state.deletion.busy) {
		if (!state.isLoading && !state.deletion.busy) selectedIds =
			selectedIds.filter { id -> state.allNotes.any { it.id == id } }
		if (selectedIds.isEmpty()) confirmSelectionDelete = false
	}
	BackHandler(enabled = active && selecting) {
		if (!state.deletion.busy) {
			selectedIds = emptyList(); confirmSelectionDelete = false
		}
	}
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
	val notes = state.allNotes.filterNot { it.isReminder }
	if (confirmSelectionDelete && selecting) BatchDeleteDialog(
		selectedIds.size,
		state.deletion,
		onDismiss = { confirmSelectionDelete = false },
		onConfirm = { onDeleteSelection(selectedIds.toSet()) })
	sharing?.let { note ->
		ExportMenu(note.title, NoteDocument.decode(note.content)) {
			sharing = null
		}
	}
	var creating by rememberSaveable { mutableStateOf(false) }
	if (creating) NoteCreationSheet(
		onCreate = { reminder -> creating = false; onCreate(reminder) },
		onDismiss = { creating = false })
	Scaffold(
		modifier = Modifier.nestedScroll(topBarScroll.nestedScrollConnection),
		containerColor = androidx.compose.ui.graphics.Color.Transparent,
		contentWindowInsets = WindowInsets.safeDrawing,
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
					onToggleLayout = when (state.selectedTab) {
						NotesTab.HOME -> ({ homeGrid = !homeGrid })
						NotesTab.NOTES, NotesTab.SEARCH -> ({ grid = !grid })
						NotesTab.REMINDERS -> null
					},
					gridLayout = if (state.selectedTab == NotesTab.HOME) homeGrid else grid,
					layoutLabel = if (state.selectedTab == NotesTab.HOME) {
						if (homeGrid) "Ver en agenda" else "Ver en cuadrícula"
					} else "Cambiar vista",
					title =
						when (state.selectedTab) {
							NotesTab.HOME -> null
							NotesTab.NOTES -> "Notas"
							NotesTab.REMINDERS -> "Recordatorios"
							NotesTab.SEARCH -> "Buscar"
						},
				)
				if (selecting) NoteSelectionBar(
					selectedIds.size, !state.deletion.busy,
					onClose = { selectedIds = emptyList() },
					onSelectAll = {
						selectedIds = (selectedIds + visibleNotes.mapNotNull { it.id }).distinct()
					},
					onDelete = { onClearDeletionError(); confirmSelectionDelete = true },
					onShare = selectedNote?.let { note -> ({ sharing = note }) },
					onMove = selectedNote?.takeUnless { it.isReminder }
						?.let { note -> ({ moving = note; onClearFolderError() }) },
					onPin = selectedNote?.let { note -> ({ onUpdateNote(note) { it.copy(pinned = !it.pinned) } }) },
					pinned = selectedNote?.let { NoteDocument.decode(it.content).pinned } ?: false)
			}
		},
		bottomBar = {
			if (WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) == 0) NotebookBottomBar(
				selectedTab = state.selectedTab,
				onTabSelected = { tab ->
					onQueryChange(""); onTabSelected(tab)
					if (tab == NotesTab.SEARCH) searchFocusRequest++
				},
				onCreateNote = { creating = true },
				enabled = !selecting && active && !state.deletion.busy,
				folderName = if (state.selectedTab == NotesTab.NOTES) state.folders.firstOrNull { it.id == state.selectedFolderId }?.name else null,
			)
		},
	) { padding ->
		Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
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
						Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center,
					) {
						CircularProgressIndicator()
					}
				else
					tabStates.SaveableStateProvider(selectedTab) {
						when {
							selectedTab == NotesTab.HOME -> HomeAgenda(
								notes = notes,
								grid = homeGrid,
								onOpen = ::openOrSelect,
								onSelect = { toggleSelection(it); onClearDeletionError() },
								onCreate = onQuickCreate,
								selectedIds = selectedIds.toSet(),
							)

							notesTab -> {
								val listLayoutState = rememberLazyGridState()
								val gridLayoutState = rememberLazyGridState()
								LaunchedEffect(searchFocusRequest) {
									if (selectedTab == NotesTab.SEARCH && searchFocusRequest > 0) {
										listLayoutState.scrollToItem(0)
										gridLayoutState.scrollToItem(0)
									}
								}
								val filtered = visibleNotes
								val header: @Composable () -> Unit = {
									Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
										if (selectedTab == NotesTab.SEARCH)
											SearchEntry(state.query, onQueryChange, focusRequest = searchFocusRequest)
										Row(
											Modifier.fillMaxWidth()
												.horizontalScroll(rememberScrollState())
												.selectableGroup(),
											horizontalArrangement = Arrangement.spacedBy(4.dp),
										) {
											listOf("Todas", "Fijadas", "Con imágenes")
												.forEach { label ->
													Surface(
														modifier =
															Modifier.clip(RoundedCornerShape(12.dp)).selectable(
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
												top = 8.dp,
												bottom = 24.dp,
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
												onClick = { openOrSelect(note) },
												onLongClick = { toggleSelection(note); onClearDeletionError() },
												selectionMode = selecting, selected = note.id in selectedIds,
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
									state.allNotes, ::openOrSelect, onCreate, onUpdateNote,
									selectedIds = selectedIds.toSet(), onToggleSelection = ::toggleSelection,
									onSelect = { toggleSelection(it); onClearDeletionError() }
								)
						}
					}
			}
		}
	}
}

@Composable
private fun SearchEntry(
	value: String,
	onChange: (String) -> Unit,
	onClick: (() -> Unit)? = null,
	focusRequest: Int = 0
) {
	val requester = remember { FocusRequester() }
	val keyboard = LocalSoftwareKeyboardController.current
	LaunchedEffect(focusRequest) {
		if (onClick == null && focusRequest > 0) {
			requester.requestFocus()
			keyboard?.show()
		}
	}
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
					modifier = Modifier.weight(1f).focusRequester(requester),
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
	selectedIds: Set<Int>,
	onToggleSelection: (Note) -> Unit,
	onSelect: (Note) -> Unit,
) {
	val selecting = selectedIds.isNotEmpty()
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
				top = 8.dp,
				bottom = 24.dp
			),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		item {
			Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				Text(
					"Tus pendientes, en orden.",
					Modifier.weight(1f),
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				FilledTonalIconButton(
					onClick = { onCreate(true) },
					enabled = !selecting
				) { Icon(MindIcons.Add, "Nuevo recordatorio") }
			}
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
						if (selecting) note.id in selectedIds else false,
						{ if (selecting) onToggleSelection(note) else update(note) { it.completeReminder(System.currentTimeMillis()) } },
						modifier =
							Modifier.semantics {
								contentDescription =
									if (selecting) "Seleccionar ${note.title}" else "Completar ${note.title}"
							},
					)
					Column(
						Modifier.weight(1f)
							.clip(RoundedCornerShape(12.dp))
							.combinedClickable(
								onClick = {
									if (selecting || note.isReminder) onOpen(note) else editing = note
								},
								onLongClick = { onSelect(note) },
								onLongClickLabel = "Seleccionar recordatorio",
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
					if (doc.reminder != null && !selecting)
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
