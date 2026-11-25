package com.example.cleannotes.presentation.ui.edit_note

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
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
import com.example.cleannotes.presentation.ui.motion.AppMotion
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
	val document = remember(state.content) { NoteDocument.decode(state.content) }
	val latestDocument by rememberUpdatedState(document)
	val change: (NoteDocument) -> Unit = { onEvent(EditNoteEvent.EnteredContent(it.encode())) }
	val controller =
		rememberRichEditorController(document) { onEvent(EditNoteEvent.EnteredContent(it)) }
	val enabled = !state.isLoading && !state.loadFailed && !state.isClosing
	var focus by rememberSaveable { mutableStateOf(false) }
	var reminder by rememberSaveable { mutableStateOf(state.isReminder && state.content.isEmpty()) }
	var imageMenu by rememberSaveable { mutableStateOf(false) }
	var formatting by rememberSaveable { mutableStateOf(false) }
	var share by rememberSaveable { mutableStateOf(false) }
	var more by remember { mutableStateOf(false) }
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
	BackHandler(enabled = active) { if (focus) focus = false else close() }
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
	fun addItem() {
		change(
			document.copy(
				checklist = document.checklist + CheckItem(java.util.UUID.randomUUID().toString())
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
	if (formatting)
		AppBottomSheet(
			onDismissRequest = { formatting = false },
		) {
			EditorBottomBar(
				document,
				controller,
				enabled,
				status,
				state.saveStatus == SaveStatus.ERROR,
				{
					formatting = false
					share = true
				},
				initialFormatting = true,
			)
		}
	if (imageMenu)
		AppBottomSheet(
			onDismissRequest = { imageMenu = false },
		) {
			Column(
				Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Text("Añadir imagen", style = MaterialTheme.typography.titleLarge)
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
		modifier = Modifier.imePadding(),
		snackbarHost = { DesignSnackbarHost(snackbar) },
		topBar = {
			Row(
				Modifier.fillMaxWidth()
					.statusBarsPadding()
					.padding(horizontal = 16.dp)
					.padding(top = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				DesignIconButton(
					if (focus) MindIcons.Close else MindIcons.ArrowBack,
					if (focus) "Salir de concentración" else "Volver",
				) {
					if (focus) focus = false else close()
				}
				Text(
					if (focus) "Salir de concentración" else status,
					Modifier.weight(1f).padding(start = 8.dp),
					style = MaterialTheme.typography.bodyMedium,
					color =
						if (state.saveStatus == SaveStatus.ERROR) MaterialTheme.colorScheme.error
						else MaterialTheme.colorScheme.onSurfaceVariant,
				)
				if (!focus)
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
			AnimatedVisibility(!focus, enter = AppMotion.reveal(), exit = AppMotion.conceal()) {
				Surface(color = MaterialTheme.colorScheme.background) {
					Column(Modifier.navigationBarsPadding()) {
						HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
						Row(
							Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							CaptureTool(
								"Lista",
								MindIcons.List,
								enabled,
								Modifier.weight(1f),
								::addItem,
							)
							CaptureTool(
								"Imagen",
								MindIcons.Image,
								enabled && !controller.importing,
								Modifier.weight(1f),
							) {
								imageMenu = true
							}
							CaptureTool(
								"Recordar",
								MindIcons.Notifications,
								enabled,
								Modifier.weight(1f),
							) {
								reminder = true
							}
						}
						Row(
							Modifier.fillMaxWidth().padding(horizontal = 16.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							TextButton(
								onClick = { formatting = true },
								enabled = enabled,
								modifier = Modifier.weight(1f),
							) {
								Text(
									"Aa · Selecciona texto para dar formato",
									style = MaterialTheme.typography.bodySmall,
								)
							}
							Box {
								IconButton(onClick = { more = true }) {
									Icon(MindIcons.MoreVert, "Más opciones")
								}
								DropdownMenu(more, { more = false }) {
									DropdownMenuItem(
										text = { Text("Compartir") },
										onClick = {
											more = false
											share = true
										},
									)
									DropdownMenuItem(
										text = { Text("Deshacer") },
										enabled = controller.canUndo,
										onClick = {
											more = false
											controller.editor?.undoEdit()
										},
									)
									DropdownMenuItem(
										text = { Text("Rehacer") },
										enabled = controller.canRedo,
										onClick = {
											more = false
											controller.editor?.redoEdit()
										},
									)
								}
							}
						}
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
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 24.dp)
					.padding(top = 16.dp, bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				if (!focus && !quickCapture) {
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						Surface(
							onClick = { focus = true },
							enabled = enabled,
							modifier = Modifier.weight(1f),
							color = MaterialTheme.colorScheme.surfaceContainer,
							shape = RoundedCornerShape(14.dp),
						) {
							Row(
								Modifier.heightIn(min = 48.dp).padding(12.dp),
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.spacedBy(8.dp),
							) {
								Icon(MindIcons.CenterFocusStrong, null)
								Text(
									"Concentrarme",
									style = MaterialTheme.typography.bodyMedium,
								)
							}
						}
						Surface(
							onClick = {
								val before = document.archived
								change(document.copy(archived = !before))
								scope.launch {
									if (
										snackbar.showSnackbar(
											if (before) "Nota restaurada" else "Nota archivada",
											"Deshacer",
										) == SnackbarResult.ActionPerformed
									)
										change(latestDocument.copy(archived = before))
								}
							},
							enabled = enabled,
							shape = RoundedCornerShape(14.dp),
							color = MaterialTheme.colorScheme.surfaceContainer,
						) {
							Box(
								Modifier.size(48.dp),
								contentAlignment = Alignment.Center,
							) {
								DesignIcon(
									MindIcons.Archive,
									if (document.archived) "Desarchivar nota" else "Archivar nota",
								)
							}
						}
					}
				}
				if (!focus)
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
							fontSize = if (focus) 30.sp else 28.sp,
							lineHeight = if (focus) 39.sp else 36.sp,
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
							if (document.subtitle.isBlank() && !focus)
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
					focus,
				)
				document.checklist.forEach { item ->
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
						}
					}
				}
				NoteImageStrip(document, enabled) { onEvent(EditNoteEvent.EnteredContent(it)) }
				if (document.images.isNotEmpty())
					Text(
						"${document.images.size} ${if (document.images.size == 1) "imagen" else "imágenes"}",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				if (document.reminder != null && !document.reminderCompleted && !focus)
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
	onClick: () -> Unit,
) {
	val stacked = LocalDensity.current.fontScale > 1.1f
	Surface(
		onClick = onClick,
		enabled = enabled,
		modifier = modifier,
		shape = RoundedCornerShape(12.dp),
		color = MaterialTheme.colorScheme.surfaceContainer,
	) {
		if (stacked) {
			Column(
				Modifier.heightIn(min = 76.dp).padding(horizontal = 4.dp, vertical = 4.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
			) {
				DesignIcon(icon)
				Text(
					label,
					style = MaterialTheme.typography.bodyMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}
		} else {
			Row(
				Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.Center,
			) {
				DesignIcon(icon)
				Spacer(Modifier.width(8.dp))
				Text(
					label,
					style = MaterialTheme.typography.bodyMedium,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
				)
			}
		}
	}
}
