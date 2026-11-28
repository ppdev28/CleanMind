package com.example.cleannotes.presentation.ui.edit_note

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.cleannotes.data.attachments.NoteFiles
import com.example.cleannotes.data.links.LinkPreviews
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.MindIcons
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichEditorController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberFileAttachments(
	document: NoteDocument,
	controller: RichEditorController,
	onChange: (String) -> Unit
): () -> Unit {
	val context = LocalContext.current;
	val scope = rememberCoroutineScope()
	val latest by rememberUpdatedState(document);
	val change by rememberUpdatedState(onChange)
	val launcher =
		rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
			if (uris.isNotEmpty()) scope.launch {
				controller.importing = true
				try {
					val imported = uris.map { NoteFiles.import(context, it) }
					change(latest.copy(files = latest.files + imported).encode())
				} catch (error: CancellationException) {
					throw error
				} catch (error: Exception) {
					controller.error = error.message ?: "No se pudo adjuntar el archivo."
				} finally {
					controller.importing = false
				}
			}
		}
	return { launcher.launch(arrayOf("*/*")) }
}

@Composable
fun NoteFileList(
	document: NoteDocument,
	enabled: Boolean,
	controller: RichEditorController,
	onChange: (String) -> Unit
) {
	val context = LocalContext.current
	document.files.forEach { attachment ->
		key(attachment.name) {
			Surface(onClick = {
				try {
					val uri = NoteFiles.uri(context, attachment)
					val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, attachment.mimeType)
						.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
						.apply { clipData = android.content.ClipData.newRawUri(attachment.displayName, uri) }
					context.startActivity(Intent.createChooser(intent, "Abrir archivo"))
				} catch (_: Exception) {
					controller.error = "No se pudo abrir el archivo. Instala una aplicación compatible."
				}
			}, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
				Row(
					Modifier.fillMaxWidth().padding(start = 16.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(12.dp)
				) {
					Icon(MindIcons.AttachFile, null)
					Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
						Text(attachment.displayName, style = MaterialTheme.typography.bodyMedium)
						Text(
							android.text.format.Formatter.formatShortFileSize(context, attachment.size),
							style = MaterialTheme.typography.bodySmall
						)
					}
					IconButton(
						enabled = enabled,
						onClick = {
							onChange(
								document.copy(files = document.files.filterNot { it.name == attachment.name })
									.encode()
							)
						}) { Icon(MindIcons.Close, "Quitar archivo ${attachment.displayName}") }
				}
			}
		}
	}
}

@Composable
fun NoteLinkPreviews(title: String, document: NoteDocument, controller: RichEditorController) {
	val context = LocalContext.current;
	val uriHandler = LocalUriHandler.current
	val source = listOf(
		title,
		document.subtitle,
		document.text,
		document.checklist.joinToString("\n") { it.text }).joinToString("\n")
	val urls = remember(source) { LinkPreviews.urls(source) }
	urls.forEach { url ->
		key(url) {
			var preview by remember(url) { mutableStateOf(LinkPreviews.fallback(url)) }
			var loading by remember(url) { mutableStateOf(true) }
			LaunchedEffect(url) { delay(700); preview = LinkPreviews.load(context, url); loading = false }
			Surface(onClick = {
				try {
					uriHandler.openUri(url)
				} catch (_: Exception) {
					controller.error = "No hay una aplicación disponible para abrir el enlace."
				}
			}, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
				Row(
					Modifier.fillMaxWidth().padding(16.dp),
					horizontalArrangement = Arrangement.spacedBy(12.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(MindIcons.Link, null)
					Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
						Text(preview.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
						if (preview.description.isNotBlank()) Text(
							preview.description,
							style = MaterialTheme.typography.bodySmall,
							maxLines = 3
						)
						if (preview.title != preview.host) Text(
							preview.host,
							style = MaterialTheme.typography.labelSmall
						)
					}
					if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
				}
			}
		}
	}
}
