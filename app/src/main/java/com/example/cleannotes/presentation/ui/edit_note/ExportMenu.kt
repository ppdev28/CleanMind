package com.example.cleannotes.presentation.ui.edit_note

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cleannotes.data.export.NoteExport
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.GlassBottomSheet
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.theme.CleanMindTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportMenu(title: String, document: NoteDocument, onDismiss: () -> Unit) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	var pdf by rememberSaveable { mutableStateOf(false) }
	var busy by remember { mutableStateOf(false) }
	var message by remember { mutableStateOf<String?>(null) }
	var failed by remember { mutableStateOf(false) }
	var pendingTitle by rememberSaveable { mutableStateOf("") }
	var pendingContent by rememberSaveable { mutableStateOf("") }
	var pendingPdf by rememberSaveable { mutableStateOf(false) }
	var awaitingDestination by rememberSaveable { mutableStateOf(false) }
	val enabled = !busy && !awaitingDestination
	fun execute(success: String, action: suspend () -> Unit) {
		if (busy) return
		scope.launch {
			busy = true
			message = null
			failed = false
			try {
				action()
				message = success
			} catch (e: kotlinx.coroutines.CancellationException) {
				throw e
			} catch (_: Exception) {
				failed = true
				message =
					"No se pudo completar la acción. Comprueba el destino y vuelve a intentarlo."
			} finally {
				busy = false
			}
		}
	}

	val picker =
		rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
			->
			awaitingDestination = false
			if (result.resultCode == android.app.Activity.RESULT_OK)
				result.data?.data?.let { uri ->
					execute("Archivo guardado") {
						withContext(Dispatchers.IO) {
							context.contentResolver.openOutputStream(uri, "wt")?.use {
								NoteExport.write(
									context,
									pendingTitle,
									NoteDocument.decode(pendingContent),
									pendingPdf,
									it,
								)
							} ?: error("Archivo no disponible")
						}
					}
				}
		}
	GlassBottomSheet(
		onDismissRequest = { if (!busy) onDismiss() },
		sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
	) {
		ExportSheetContent(
			title = title,
			document = document,
			pdf = pdf,
			enabled = enabled,
			busy = busy,
			hasFolder = NoteExport.folder(context) != null,
			message = message,
			failed = failed,
			onFormatChange = {
				pdf = it
				message = null
				failed = false
			},
			onClose = onDismiss,
			onShare = {
				val selectedPdf = pdf
				execute("Elige una aplicación para enviar la nota") {
					val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_SUBJECT, title)
					if (selectedPdf) {
						val uri = NoteExport.shareFile(context, title, document, true)
						intent.type = "application/pdf"
						intent.putExtra(Intent.EXTRA_STREAM, uri)
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
						intent.clipData = ClipData.newUri(context.contentResolver, "Nota", uri)
					} else {
						intent.type = "text/plain"
						intent.putExtra(Intent.EXTRA_TEXT, NoteExport.plainText(title, document))
					}
					context.startActivity(Intent.createChooser(intent, "Compartir nota"))
				}
			},
			onSave = {
				pendingTitle = title
				pendingContent = document.encode()
				pendingPdf = pdf
				awaitingDestination = true
				message = null
				try {
					picker.launch(
						Intent(Intent.ACTION_CREATE_DOCUMENT)
							.addCategory(Intent.CATEGORY_OPENABLE)
							.setType(if (pdf) "application/pdf" else "text/plain")
							.putExtra(Intent.EXTRA_TITLE, NoteExport.filename(title, pdf))
					)
				} catch (_: Exception) {
					awaitingDestination = false
					failed = true
					message = "No se pudo abrir el selector de archivos."
				}
			},
			onSaveToFolder = {
				execute("Archivo guardado en tu carpeta") {
					NoteExport.saveToFolder(
						context,
						title,
						document,
						pdf,
					)
				}
			},
			onCopy = {
				(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
					.setPrimaryClip(
						ClipData.newPlainText(title, NoteExport.plainText(title, document))
					)
				failed = false
				message = "Texto copiado"
			},
		)
	}
}

@Composable
private fun ExportSheetContent(
	title: String,
	document: NoteDocument,
	pdf: Boolean,
	enabled: Boolean,
	busy: Boolean,
	hasFolder: Boolean,
	message: String?,
	failed: Boolean,
	onFormatChange: (Boolean) -> Unit,
	onClose: () -> Unit,
	onShare: () -> Unit,
	onSave: () -> Unit,
	onSaveToFolder: () -> Unit,
	onCopy: () -> Unit,
) {
	Column(
		Modifier.fillMaxWidth()
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 24.dp)
			.padding(bottom = 24.dp),
		verticalArrangement = Arrangement.spacedBy(20.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Column(Modifier.weight(1f)) {
				Text("Comparte tu nota", style = MaterialTheme.typography.titleLarge)
				Text(
					"Elige cómo quieres enviarla",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			IconButton(onClick = onClose, enabled = enabled) { Icon(MindIcons.Close, "Cerrar") }
		}
		Surface(
			shape = RoundedCornerShape(16.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
		) {
			Row(
				Modifier.fillMaxWidth().padding(16.dp),
				horizontalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Icon(
					MindIcons.InsertDriveFile,
					null,
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(top = 2.dp),
				)
				Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
					Text(
						title.ifBlank { "Sin título" },
						style = MaterialTheme.typography.titleSmall,
						maxLines = 2,
						overflow = TextOverflow.Ellipsis,
					)
					if (document.subtitle.isNotBlank())
						Text(
							document.subtitle,
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
						)
					if (document.text.isNotBlank())
						Text(
							document.text,
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							maxLines = 2,
							overflow = TextOverflow.Ellipsis,
						)
					if (document.images.isNotEmpty())
						Text(
							"${document.images.size} ${if (document.images.size == 1) "imagen incluida en PDF" else "imágenes incluidas en PDF"}",
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
				}
			}
		}
		Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
			Text(
				"Formato",
				style = MaterialTheme.typography.labelLarge,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
			Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				FormatOption(
					"Texto",
					"TXT · Sin formato",
					MindIcons.Notes,
					!pdf,
					enabled,
					Modifier.weight(1f),
				) {
					onFormatChange(false)
				}
				FormatOption(
					"PDF",
					"Diseño e imágenes",
					MindIcons.PictureAsPdf,
					pdf,
					enabled,
					Modifier.weight(1f),
				) {
					onFormatChange(true)
				}
			}
			Text(
				if (pdf) "Conserva los formatos básicos y las imágenes en un documento paginado."
				else
					"Envía el título y el contenido como texto. Al guardar, se crea un archivo .txt sin imágenes.",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
		Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
			Button(
				onClick = onShare,
				enabled = enabled,
				modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
				shape = RoundedCornerShape(14.dp),
			) {
				Icon(MindIcons.Share, null, Modifier.size(20.dp))
				Spacer(Modifier.width(10.dp))
				Text(if (pdf) "Compartir PDF" else "Compartir texto")
			}
			OutlinedButton(
				onClick = onSave,
				enabled = enabled,
				modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
				shape = RoundedCornerShape(14.dp),
			) {
				Icon(MindIcons.FileDownload, null, Modifier.size(20.dp))
				Spacer(Modifier.width(10.dp))
				Text("Guardar archivo…")
			}
			if (hasFolder)
				ExportAction(
					MindIcons.FolderOpen,
					"Guardar en mi carpeta",
					enabled,
					onSaveToFolder,
				)
			ExportAction(MindIcons.ContentCopy, "Copiar texto", enabled, onCopy)
		}
		if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
		message?.let {
			Surface(
				color =
					if (failed) MaterialTheme.colorScheme.errorContainer
					else MaterialTheme.colorScheme.surfaceContainerHigh,
				shape = RoundedCornerShape(12.dp),
			) {
				Text(
					it,
					Modifier.fillMaxWidth().padding(14.dp).semantics {
						liveRegion = LiveRegionMode.Polite
					},
					style = MaterialTheme.typography.bodySmall,
					color =
						if (failed) MaterialTheme.colorScheme.onErrorContainer
						else MaterialTheme.colorScheme.onSurface,
				)
			}
		}
	}
}

@Composable
private fun FormatOption(
	title: String,
	description: String,
	icon: ImageVector,
	selected: Boolean,
	enabled: Boolean,
	modifier: Modifier,
	onClick: () -> Unit,
) {
	Surface(
		modifier =
			modifier.selectable(
				selected = selected,
				enabled = enabled,
				role = Role.RadioButton,
				onClick = onClick,
			),
		shape = RoundedCornerShape(16.dp),
		color =
			if (selected) MaterialTheme.colorScheme.primaryContainer
			else MaterialTheme.colorScheme.surfaceContainer,
		border =
			BorderStroke(
				if (selected) 1.5.dp else 1.dp,
				if (selected) MaterialTheme.colorScheme.primary
				else MaterialTheme.colorScheme.outlineVariant,
			),
	) {
		Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				Icon(
					icon,
					null,
					tint =
						if (selected) MaterialTheme.colorScheme.primary
						else MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Spacer(Modifier.weight(1f))
				if (selected)
					Icon(
						MindIcons.CheckCircle,
						null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(18.dp),
					)
			}
			Text(
				title,
				style = MaterialTheme.typography.titleMedium,
				color =
					if (selected) MaterialTheme.colorScheme.onPrimaryContainer
					else MaterialTheme.colorScheme.onSurface,
			)
			Text(
				description,
				style = MaterialTheme.typography.bodySmall,
				color =
					if (selected) MaterialTheme.colorScheme.onPrimaryContainer
					else MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun ExportAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
	TextButton(
		onClick = onClick,
		enabled = enabled,
		modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
		contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
	) {
		Icon(icon, null, Modifier.size(20.dp))
		Spacer(Modifier.width(12.dp))
		Text(label, modifier = Modifier.weight(1f))
	}
}

@Preview(name = "Compartir · claro", widthDp = 393, showBackground = true, locale = "es")
@Composable
private fun ShareLightPreview() = SharePreview(false)

@Preview(name = "Compartir · oscuro", widthDp = 393, showBackground = true, locale = "es")
@Composable
private fun ShareDarkPreview() = SharePreview(true)

@Preview(
	name = "Compartir · texto grande",
	widthDp = 320,
	heightDp = 740,
	fontScale = 1.5f,
	showBackground = true,
	locale = "es",
)
@Composable
private fun ShareLargeTextPreview() = SharePreview(false)

@Composable
private fun SharePreview(dark: Boolean) {
	CleanMindTheme(darkTheme = dark) {
		Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
			ExportSheetContent(
				"Ideas para el fin de semana",
				NoteDocument(
					"Un paseo, un libro y una cafetería nueva.",
					images = listOf("example.jpg"),
				),
				pdf = dark,
				enabled = true,
				busy = false,
				hasFolder = true,
				message = null,
				failed = false,
				onFormatChange = {},
				onClose = {},
				onShare = {},
				onSave = {},
				onSaveToFolder = {},
				onCopy = {},
			)
		}
	}
}
