package com.example.cleannotes.presentation.ui.edit_note.richtext

import com.example.cleannotes.text.MarkdownRendering
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cleannotes.data.attachments.NoteImages
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.MindIcons
import kotlinx.coroutines.launch

/** UI-only bridge. The document remains owned and persisted by EditNoteViewModel. */
@Stable
class RichEditorController {
	var editor by mutableStateOf<RichNoteEditor?>(null)
	var importing by mutableStateOf(false)
	var error by mutableStateOf<String?>(null)
	var canUndo by mutableStateOf(false)
	var canRedo by mutableStateOf(false)
	var activeFormats by mutableStateOf(setOf<String>())
	var chooseImage: () -> Unit = {}
	var takePhoto: () -> Unit = {}
	var pasteImage: (Uri) -> Unit = {}
}

@Composable
fun rememberRichEditorController(
	document: NoteDocument,
	onChange: (String) -> Unit,
): RichEditorController {
	val controller = remember { RichEditorController() }
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	val currentChange by rememberUpdatedState(onChange)
	val latest by rememberUpdatedState(document)
	var cameraName by rememberSaveable { mutableStateOf<String?>(null) }
	val addImage: (Uri) -> Unit = { uri ->
		scope.launch {
			controller.importing = true
			try {
				val name = NoteImages.import(context, uri)
				currentChange(latest.copy(images = latest.images + name).encode())
			} catch (e: kotlinx.coroutines.CancellationException) {
				throw e
			} catch (_: Exception) {
				controller.error = "No se pudo añadir la imagen. Prueba con otra foto."
			} finally {
				controller.importing = false
			}
		}
	}
	val gallery =
		rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
			uri?.let(addImage)
		}
	val camera =
		rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
			cameraName?.let { name ->
				if (success) {
					scope.launch {
						controller.importing = true
						try {
							val uri =
								androidx.core.content.FileProvider.getUriForFile(
									context,
									"${context.packageName}.images",
									NoteImages.file(context, name),
								)
							val imported = NoteImages.import(context, uri)
							currentChange(latest.copy(images = latest.images + imported).encode())
						} catch (e: kotlinx.coroutines.CancellationException) {
							throw e
						} catch (_: Exception) {
							controller.error = "No se pudo procesar la foto."
						} finally {
							NoteImages.file(context, name).delete()
							controller.importing = false
						}
					}
				} else NoteImages.file(context, name).delete()
			}
			cameraName = null
		}
	SideEffect {
		controller.pasteImage = addImage
		controller.chooseImage = { gallery.launch("image/*") }
		controller.takePhoto = {
			try {
				val (name, uri) = NoteImages.cameraFile(context)
				cameraName = name
				camera.launch(uri)
			} catch (_: Exception) {
				cameraName?.let { NoteImages.file(context, it).delete() }
				cameraName = null
				controller.error = "No hay una cámara disponible."
			}
		}
	}
	DisposableEffect(controller) { onDispose { controller.editor = null } }
	return controller
}

@Composable
fun RichEditorContent(
	document: NoteDocument,
	enabled: Boolean,
	controller: RichEditorController,
	onChange: (String) -> Unit,
	focusMode: Boolean = false,
	visible: Boolean = true,
) {
	val currentChange by rememberUpdatedState(onChange)
	val color = MaterialTheme.colorScheme.onSurface.toArgb()
	val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
	AndroidView(
		factory = { RichNoteEditor(it).also { view -> controller.editor = view } },
		update = { view ->
			view.onDocumentChanged = { currentChange(it.encode()) }
			view.onPasteImage = { controller.pasteImage(it) }
			view.onEditorStateChanged = { undo, redo, formats ->
				controller.canUndo = undo
				controller.canRedo = redo
				controller.activeFormats = formats
			}
			view.textSize = if (focusMode) 18f else 16f
			view.typeface =
				androidx.core.content.res.ResourcesCompat.getFont(
					view.context,
					com.example.cleannotes.R.font.inter_regular,
				)
			view.isEnabled = enabled
			view.setTextColor(color)
			view.setHintTextColor(hintColor)
			view.hint = "Escribe en Markdown…"
			view.bind(document)
		},
		modifier =
			Modifier.fillMaxWidth().then(
				if (visible) Modifier.heightIn(min = 48.dp)
					.semantics { contentDescription = "Cuerpo de la nota" }
				else Modifier.height(0.dp).clearAndSetSemantics { }
			),
	)
}

@Composable
fun NoteImageStrip(document: NoteDocument, enabled: Boolean, onChange: (String) -> Unit) {
	val context = LocalContext.current
	document.images.forEachIndexed { index, name ->
		key(name) {
			var bitmap by remember { mutableStateOf<Bitmap?>(null) }
			var menu by remember { mutableStateOf(false) }
			LaunchedEffect(name) { bitmap = NoteImages.preview(context, name) }
			Box(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
				bitmap?.let {
					Image(
						it.asImageBitmap(),
						"Imagen ${index + 1} de la nota",
						Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(16.dp)),
						contentScale = ContentScale.Crop,
					)
				} ?: Text("Imagen no disponible", Modifier.padding(24.dp))
				Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
					FilledTonalIconButton(
						onClick = { menu = true },
						enabled = enabled,
					) {
						Icon(MindIcons.MoreVert, "Opciones de imagen")
					}
					DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
						if (index > 0)
							DropdownMenuItem(
								text = { Text("Mover arriba") },
								onClick = {
									menu = false
									val images = document.images.toMutableList()
									images.removeAt(index)
									images.add(index - 1, name)
									onChange(document.copy(images = images).encode())
								},
							)
						DropdownMenuItem(
							text = { Text("Quitar imagen") },
							onClick = {
								menu = false
								onChange(
									document
										.copy(images = document.images.filterNot { it == name })
										.encode()
								)
							},
						)
					}
				}
			}
		}
	}
}

/** Read mode uses native selectable text and never loads remote images or HTML. */
@Composable
fun MarkdownPreview(document: NoteDocument) {
	val color = MaterialTheme.colorScheme.onSurface.toArgb()
	val accent = MaterialTheme.colorScheme.primary.toArgb()
	val density = androidx.compose.ui.platform.LocalDensity.current.density
	val rendered = remember(document.text, document.paragraphSpacing, density) {
		MarkdownRendering.render(document.text, (document.paragraphSpacing * density).toInt())
	}
	AndroidView(
		factory = { context ->
			android.widget.TextView(context).apply {
				setTextIsSelectable(true)
				movementMethod = android.text.method.LinkMovementMethod.getInstance()
				textSize = 16f
				setPadding(0, 0, 0, 24)
			}
		},
		update = { view ->
			view.setTextColor(color)
			view.setLinkTextColor(accent)
			view.setLineSpacing(document.extraLineSpacing.toFloat(), document.lineSpacing)
			view.textAlignment = when (document.textAlignment) {
				2 -> android.view.View.TEXT_ALIGNMENT_CENTER
				1 -> android.view.View.TEXT_ALIGNMENT_VIEW_END
				else -> android.view.View.TEXT_ALIGNMENT_VIEW_START
			}
			view.text = rendered
		},
		modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
			.semantics { contentDescription = "Vista previa de Markdown" },
	)
}
