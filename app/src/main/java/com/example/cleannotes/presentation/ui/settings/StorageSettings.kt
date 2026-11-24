package com.example.cleannotes.presentation.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cleannotes.data.export.NoteExport

@Composable
fun StorageSettings(
	viewModel: StorageSettingsViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
) {
	val status by viewModel.status.collectAsState()
	val busy by viewModel.busy.collectAsState()
	val context = LocalContext.current
	var folder by remember { mutableStateOf(NoteExport.folder(context)) }
	var error by remember { mutableStateOf(NoteExport.prefs(context).getString("error", null)) }
	val picker =
		rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
			if (uri != null) {
				try {
					context.contentResolver.takePersistableUriPermission(
						uri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION or
								Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
					)
					val previous = folder
					NoteExport.prefs(context)
						.edit()
						.putString("folder", uri.toString())
						.remove("error")
						.apply()
					folder = uri
					error = null
					viewModel.copyAll()
					if (previous != null && previous != uri)
						runCatching {
							context.contentResolver.releasePersistableUriPermission(
								previous,
								Intent.FLAG_GRANT_READ_URI_PERMISSION or
										Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
							)
						}
				} catch (_: Exception) {
					error = "No se pudo obtener permiso para escribir en esa carpeta."
				}
			}
		}
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text("Carpeta de las notas", style = MaterialTheme.typography.titleMedium)
		Text(
			"Cada guardado crea o actualiza una copia TXT en la carpeta elegida. Los originales y las imágenes se conservan en la app. Puedes exportar PDF desde cada nota.",
			style = MaterialTheme.typography.bodySmall,
		)
		Text(
			folder?.toString()?.let(android.net.Uri::decode) ?: "Sin carpeta adicional",
			style = MaterialTheme.typography.bodySmall,
		)
		error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
		if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
		if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
		if (folder != null)
			TextButton(
				enabled = !busy,
				onClick = viewModel::copyAll,
			) {
				Text("Copiar todas las notas ahora")
			}
		Row {
			TextButton(enabled = !busy, onClick = { picker.launch(folder) }) {
				Text("Elegir carpeta")
			}
			if (folder != null)
				TextButton(
					enabled = !busy,
					onClick = {
						folder?.let { uri ->
							runCatching {
								context.contentResolver.releasePersistableUriPermission(
									uri,
									Intent.FLAG_GRANT_READ_URI_PERMISSION or
											Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
								)
							}
						}
						NoteExport.prefs(context).edit().remove("folder").remove("error").apply()
						folder = null
						error = null
					},
				) {
					Text("Desactivar copia")
				}
		}
	}
}
