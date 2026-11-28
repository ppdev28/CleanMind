package com.example.cleannotes.presentation.ui.edit_note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleannotes.model.CheckItem
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.*
import com.example.cleannotes.presentation.ui.edit_note.richtext.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
	state: EditNoteUiState,
	onEvent: (EditNoteEvent) -> Unit,
	onSaveAndClose: () -> Unit,
	onBack: () -> Unit,
	quickCapture: Boolean = false,
	active: Boolean = true,
) {
	val document = remember(state.content) {
		com.example.cleannotes.text.RichDocumentCompatibility.editable(NoteDocument.decode(state.content))
	}
	val latestDocument by rememberUpdatedState(document)
	val change: (NoteDocument) -> Unit = { onEvent(EditNoteEvent.EnteredContent(it.encode())) }
	val controller =
		rememberRichEditorController(document) { onEvent(EditNoteEvent.EnteredContent(it)) }
	val dictate = rememberVoiceDictation(controller)
	val attachFiles =
		rememberFileAttachments(document, controller) { onEvent(EditNoteEvent.EnteredContent(it)) }
	val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
	val enabled = !state.isLoading && !state.loadFailed && !state.isClosing
	var reminder by rememberSaveable { mutableStateOf(state.isReminder && state.content.isEmpty()) }
	var imageMenu by rememberSaveable { mutableStateOf(false) }
	var share by rememberSaveable { mutableStateOf(false) }
	var discard by remember { mutableStateOf(false) }
	val snackbar = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()
	val context = androidx.compose.ui.platform.LocalContext.current
	val notifications =
		androidx.activity.compose.rememberLauncherForActivityResult(
			androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
		) { allowed ->
			if (!allowed)
				scope.launch {
					snackbar.showSnackbar(
						"Activa las notificaciones en Ajustes para recibir el aviso."
					)
				}
		}
	val close: () -> Unit = {
		if (state.loadFailed) onBack()
		else if (state.saveStatus == SaveStatus.ERROR && state.hasChanges) discard = true
		else onSaveAndClose()
	}
	BackHandler(enabled = active) { close() }
	val status =
		when {
			state.loadFailed -> "No se pudo abrir"
			state.saveStatus == SaveStatus.ERROR -> "No se pudo guardar"
			state.saveStatus == SaveStatus.SAVING -> "Guardando…"
			state.saveStatus == SaveStatus.SAVED -> "Guardado"
			else -> "Borrador"
		}
	LaunchedEffect(controller.error) {
		controller.error?.let {
			snackbar.showSnackbar(it)
			controller.error = null
		}
	}
	LaunchedEffect(quickCapture, controller.editor) {
		if (quickCapture)
			controller.editor?.let { view ->
				view.requestFocus()
				view.post {
					(view.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
							as android.view.inputmethod.InputMethodManager)
						.showSoftInput(
							view,
							android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT,
						)
				}
			}
	}
	var removalJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
	fun removeItem(item: CheckItem, index: Int) {
		change(latestDocument.copy(checklist = latestDocument.checklist.filterNot { it.id == item.id }))
		removalJob?.cancel()
		removalJob = scope.launch {
			if (snackbar.showSnackbar(
					"Casilla eliminada",
					"Deshacer"
				) == SnackbarResult.ActionPerformed
			) {
				val current = latestDocument
				if (current.checklist.none { it.id == item.id }) {
					val items =
						current.checklist.toMutableList().apply { add(index.coerceAtMost(size), item) }
					change(current.copy(checklist = items))
				}
			}
		}
	}

	fun addItem() {
		change(
			latestDocument.copy(
				checklist = latestDocument.checklist + CheckItem(java.util.UUID.randomUUID().toString())
			)
		)
	}
	if (discard)
		AlertDialog(
			onDismissRequest = { discard = false },
			title = { Text("Hay cambios sin guardar") },
			text = { Text("Puedes seguir editando o descartar los cambios pendientes.") },
			confirmButton = {
				TextButton(
					onClick = {
						discard = false
						onBack()
					}
				) {
					Text("Descartar")
				}
			},
			dismissButton = {
				TextButton(onClick = { discard = false }) { Text("Seguir editando") }
			},
		)
	if (share) ExportMenu(state.title, document) { share = false }
	if (reminder)
		ReminderSheet(
			state.title,
			document,
			onConfirm = {
				change(it)
				reminder = false
				if (
					it.reminder != null &&
					android.os.Build.VERSION.SDK_INT >= 33 &&
					context.checkSelfPermission(
						android.Manifest.permission.POST_NOTIFICATIONS
					) != android.content.pm.PackageManager.PERMISSION_GRANTED
				)
					notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
			},
			onDismiss = { reminder = false },
		)
	if (imageMenu)
		AppBottomSheet(
			onDismissRequest = { imageMenu = false },
		) {
			Column(
				Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Text("Imágenes y archivos", style = MaterialTheme.typography.titleLarge)
				DesignRow("Adjuntar archivo", MindIcons.AttachFile) { imageMenu = false; attachFiles() }
				DesignRow("Elegir imagen", MindIcons.Image) {
					imageMenu = false
					controller.chooseImage()
				}
				TextButton(
					onClick = {
						imageMenu = false
						controller.takePhoto()
					}
				) {
					Text("Hacer foto")
				}
				TextButton(
					onClick = {
						imageMenu = false
						if (controller.editor?.pasteImage() != true)
							controller.error = "Copia una imagen para pegarla aquí."
					}
				) {
					Text("Pegar imagen")
				}
			}
		}
	Scaffold(
		containerColor = androidx.compose.ui.graphics.Color.Transparent,
		contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
		snackbarHost = { DesignSnackbarHost(snackbar) },
		topBar = {
			Row(
				Modifier.fillMaxWidth()
					.background(MaterialTheme.colorScheme.background)
					.statusBarsPadding()
					.padding(horizontal = 16.dp)
					.padding(top = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				DesignIconButton(
					MindIcons.ArrowBack,
					"Volver",
				) {
					close()
				}
				Text(
					status,
					Modifier.weight(1f).padding(start = 8.dp),
					style = MaterialTheme.typography.bodyMedium,
					color =
						if (state.saveStatus == SaveStatus.ERROR) MaterialTheme.colorScheme.error
						else MaterialTheme.colorScheme.onSurfaceVariant,
				)
				DesignIconButton(MindIcons.Share, "Compartir nota", enabled) { share = true }
				IconToggleButton(
					checked = document.pinned,
					enabled = enabled,
					onCheckedChange = { change(document.copy(pinned = it)) },
					modifier = Modifier.size(48.dp),
				) {
					Icon(
						MindIcons.PushPin,
						if (document.pinned) "Desfijar nota" else "Fijar nota",
						tint =
							if (document.pinned) MaterialTheme.colorScheme.primary
							else MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
		},
		bottomBar = {

			Surface(color = MaterialTheme.colorScheme.background) {
				Column(Modifier.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))) {
					HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

					FlowRow(
						Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						verticalArrangement = Arrangement.spacedBy(8.dp),
						maxItemsInEachRow = if (!keyboardVisible && LocalDensity.current.fontScale > 1.15f) 2 else 4,
					) {
						CaptureTool(
							"Dictar",
							MindIcons.Mic,
							enabled,
							Modifier.weight(1f),
							compact = keyboardVisible,
							onClick = dictate
						)
						CaptureTool(
							"Lista",
							MindIcons.List,
							enabled,
							Modifier.weight(1f),
							compact = keyboardVisible,
							onClick = ::addItem,
						)
						CaptureTool(
							"Imagen",
							MindIcons.Image,
							enabled && !controller.importing,
							Modifier.weight(1f),
							compact = keyboardVisible,
						) {
							imageMenu = true
						}
						CaptureTool(
							"Recordar",
							MindIcons.Notifications,
							enabled,
							Modifier.weight(1f),
							compact = keyboardVisible,
						) {
							reminder = true
						}
					}

					Row(
						Modifier.fillMaxWidth().padding(horizontal = 16.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						RichFormatBar(document, controller, enabled, Modifier.weight(1f))

					}
				}
			}

		},
	) { padding ->
		if (state.isLoading)
			Box(
				Modifier.fillMaxSize().padding(padding),
				contentAlignment = Alignment.Center,
			) {
				CircularProgressIndicator()
			}
		else
			Column(
				Modifier.fillMaxSize()
					.padding(padding)
					.consumeWindowInsets(padding)
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 24.dp)
					.padding(top = 16.dp, bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {

				Text(
					"Título opcional",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				BasicTextField(
					state.title,
					{ onEvent(EditNoteEvent.EnteredTitle(it)) },
					enabled = enabled,
					modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Título" },
					textStyle =
						MaterialTheme.typography.headlineLarge.copy(
							color = MaterialTheme.colorScheme.onSurface,
							fontSize = 28.sp,
							lineHeight = 36.sp,
						),
					cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
					decorationBox = { field ->
						Box {
							if (state.title.isBlank())
								Text(
									"Sin título",
									style = MaterialTheme.typography.headlineLarge,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							field()
						}
					},
				)
				BasicTextField(
					document.subtitle,
					{ change(document.copy(subtitle = it)) },
					enabled = enabled,
					modifier =
						Modifier.fillMaxWidth().semantics { contentDescription = "Subtítulo" },
					textStyle =
						MaterialTheme.typography.bodyMedium.copy(
							color = MaterialTheme.colorScheme.onSurfaceVariant
						),
					cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
					decorationBox = { field ->
						Box {
							if (document.subtitle.isBlank())
								Text(
									"Subtítulo (opcional)",
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							field()
						}
					},
				)
				RichEditorContent(
					document,
					enabled,
					controller,
					{ onEvent(EditNoteEvent.EnteredContent(it)) },
				)
				document.checklist.forEachIndexed { index, item ->
					key(item.id) {
						Row(
							Modifier.heightIn(min = 48.dp),
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							Checkbox(
								item.done,
								{ checked ->
									change(
										document.copy(
											checklist =
												document.checklist.map {
													if (it.id == item.id) it.copy(done = checked)
													else it
												}
										)
									)
								},
								enabled = enabled,
							)
							BasicTextField(
								item.text,
								{ text ->
									change(
										document.copy(
											checklist =
												document.checklist.map {
													if (it.id == item.id) it.copy(text = text)
													else it
												}
										)
									)
								},
								enabled = enabled,
								modifier =
									Modifier.weight(1f).semantics {
										contentDescription = "Elemento de lista"
									},
								singleLine = true,
								keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
								keyboardActions = KeyboardActions(onNext = { addItem() }),
								textStyle =
									MaterialTheme.typography.bodyMedium.copy(
										color = MaterialTheme.colorScheme.onSurface,
										textDecoration =
											if (item.done) TextDecoration.LineThrough else null,
									),
								decorationBox = { field ->
									Box {
										if (item.text.isEmpty())
											Text(
												"Elemento de lista",
												style = MaterialTheme.typography.bodyMedium,
												color = MaterialTheme.colorScheme.onSurfaceVariant,
											)
										field()
									}
								},
							)
							IconButton(onClick = { removeItem(item, index) }, enabled = enabled) {
								Icon(MindIcons.Close, "Eliminar casilla ${index + 1}")
							}
						}
					}
				}
				NoteImageStrip(document, enabled) { onEvent(EditNoteEvent.EnteredContent(it)) }
				NoteFileList(document, enabled, controller) { onEvent(EditNoteEvent.EnteredContent(it)) }
				NoteLinkPreviews(state.title, document, controller)
				if (document.images.isNotEmpty())
					Text(
						"${document.images.size} ${if (document.images.size == 1) "imagen" else "imágenes"}",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				if (document.reminder != null && !document.reminderCompleted)
					DesignRow(
						"Recordatorio",
						MindIcons.Notifications,
						java.text.DateFormat.getDateTimeInstance(
							java.text.DateFormat.SHORT,
							java.text.DateFormat.SHORT,
						)
							.format(document.reminder.at),
					) {
						reminder = true
					}
				if (quickCapture)
					Text(
						"Puedes organizarlo después.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				if (controller.importing) LinearProgressIndicator(Modifier.fillMaxWidth())
			}
	}
}

@Composable
private fun CaptureTool(
	label: String,
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	enabled: Boolean,
	modifier: Modifier,
	compact: Boolean,
	onClick: () -> Unit,
) {
	Surface(
		onClick = onClick,
		enabled = enabled,
		modifier = modifier.semantics {
			contentDescription = if (label == "Dictar") "Dictar por voz" else label
		},
		shape = RoundedCornerShape(12.dp),
		color = MaterialTheme.colorScheme.surfaceContainer,
	) {
		Column(
			Modifier.heightIn(min = if (compact) 48.dp else 68.dp)
				.padding(horizontal = 4.dp, vertical = 8.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
		) {
			DesignIcon(icon)
			if (!compact) Text(
				label,
				style = MaterialTheme.typography.labelMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)
		}
	}
}
