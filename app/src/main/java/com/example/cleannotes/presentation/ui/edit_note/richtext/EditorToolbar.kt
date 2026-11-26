package com.example.cleannotes.presentation.ui.edit_note.richtext

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.AppBottomSheet
import com.example.cleannotes.presentation.ui.design.MindIcons

/** Frequent tools stay visible above the keyboard; the rest need one tap, without nested menus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownFormatBar(
	document: NoteDocument,
	controller: RichEditorController,
	enabled: Boolean,
	modifier: Modifier = Modifier,
) {
	var expanded by rememberSaveable { mutableStateOf(false) }
	var linkDialog by rememberSaveable { mutableStateOf(false) }
	var linkLabel by rememberSaveable { mutableStateOf("") }
	var linkUrl by rememberSaveable { mutableStateOf("") }
	val editor = controller.editor
	Row(modifier) {
		Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
			FormatAction(
				MindIcons.FormatBold,
				"Negrita",
				enabled,
				"bold" in controller.activeFormats
			) { editor?.format("bold") }
			FormatAction(
				MindIcons.FormatItalic,
				"Cursiva",
				enabled,
				"italic" in controller.activeFormats
			) { editor?.format("italic") }
			FormatAction(
				MindIcons.FormatUnderlined,
				"Subrayado",
				enabled,
				"underline" in controller.activeFormats
			) { editor?.format("underline") }
			FormatAction(
				MindIcons.FormatListBulleted,
				"Lista con viñetas",
				enabled
			) { editor?.list("- ") }
			FormatAction(
				MindIcons.FormatListNumbered,
				"Lista numerada",
				enabled
			) { editor?.list("number") }
			FormatAction(MindIcons.CheckBox, "Lista de tareas Markdown", enabled) { editor?.checkItem() }
			FormatAction(MindIcons.StrikethroughS, "Tachado", enabled) { editor?.format("strike") }
		}
		IconButton(onClick = { expanded = true }, enabled = enabled) {
			Icon(
				MindIcons.Tune,
				"Más formato y espaciado"
			)
		}
	}
	if (expanded) AppBottomSheet(
		onDismissRequest = { expanded = false },
		sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
	) {
		Column(
			Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
				.padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text("Formato Markdown", style = MaterialTheme.typography.titleLarge)
			Text(
				"Se aplica al texto seleccionado o al párrafo actual.",
				style = MaterialTheme.typography.bodySmall
			)
			Text("Encabezados", style = MaterialTheme.typography.labelLarge)
			FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				(1..6).forEach { level ->
					AssistChip(
						onClick = { editor?.markdownBlock("#".repeat(level) + " ") },
						label = { Text("H$level") })
				}
				AssistChip(onClick = { editor?.markdownBlock("") }, label = { Text("Normal") })
			}
			Text("Insertar y organizar", style = MaterialTheme.typography.labelLarge)
			FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				listOf<Pair<String, () -> Unit>>(
					"Código" to { editor?.markdownWrap("`") },
					"Bloque de código" to { editor?.markdownWrap("\n```\n", "\n```\n") },
					"Cita" to { editor?.markdownBlock("> ") },
					"Enlace" to {
						linkLabel = editor?.let { view ->
							view.text?.substring(
								minOf(view.selectionStart, view.selectionEnd).coerceAtLeast(0),
								maxOf(view.selectionStart, view.selectionEnd).coerceAtLeast(0)
							)
						}.orEmpty()
						linkUrl = ""
						expanded = false
						linkDialog = true
					},
					"Tabla" to { editor?.markdownInsert("\n\n| Columna 1 | Columna 2 |\n| --- | --- |\n| Texto | Texto |\n\n") },
					"Separador" to { editor?.markdownInsert("\n\n---\n\n") },
					"Sangrar" to { editor?.markdownBlock("indent") },
					"Quitar sangría" to { editor?.markdownBlock("outdent") },
					"Borrar formato" to { editor?.format("clear") },
				).forEach { (label, action) -> AssistChip(onClick = action, label = { Text(label) }) }
			}
			Text("Alineación de la nota", style = MaterialTheme.typography.labelLarge)
			Row {
				FormatAction(
					MindIcons.FormatAlignLeft,
					"Alinear a la izquierda",
					enabled,
					document.textAlignment == 0
				) { editor?.alignment(0) }
				FormatAction(
					MindIcons.FormatAlignCenter,
					"Centrar",
					enabled,
					document.textAlignment == 2
				) { editor?.alignment(2) }
				FormatAction(
					MindIcons.FormatAlignRight,
					"Alinear a la derecha",
					enabled,
					document.textAlignment == 1
				) { editor?.alignment(1) }
			}
			SpacingOptions(
				"Interlineado",
				listOf(1f, 1.3f, 1.5f, 2f),
				document.lineSpacing
			) { editor?.spacing(it, document.extraLineSpacing) }
			SpacingOptions(
				"Espacio entre párrafos",
				listOf(0, 8, 16, 24),
				document.paragraphSpacing
			) { editor?.paragraphSpacing(it) }
			SpacingOptions(
				"Espacio extra entre líneas",
				listOf(0, 4, 8, 16),
				document.extraLineSpacing
			) { editor?.spacing(document.lineSpacing, it) }
		}
	}
	if (linkDialog) {
		val address = linkUrl.trim().let { if (it.contains(":")) it else "https://$it" }
		val uri = android.net.Uri.parse(address)
		val valid = linkUrl.isNotBlank() && ((uri.scheme in listOf(
			"https",
			"http"
		) && !uri.host.isNullOrBlank()) || (uri.scheme == "mailto" && uri.schemeSpecificPart.contains("@")))
		AlertDialog(
			onDismissRequest = { linkDialog = false },
			title = { Text("Insertar enlace") },
			text = {
				Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
					OutlinedTextField(
						linkLabel,
						{ linkLabel = it },
						label = { Text("Texto del enlace") },
						singleLine = true
					)
					OutlinedTextField(
						linkUrl,
						{ linkUrl = it },
						label = { Text("Dirección") },
						singleLine = true
					)
				}
			},
			confirmButton = {
				TextButton(enabled = valid, onClick = {
					val label = linkLabel.ifBlank { address }.replace("\\", "\\\\").replace("[", "\\[")
						.replace("]", "\\]")
					editor?.markdownInsert("[$label](${address.replace(")", "%29").replace(" ", "%20")})")
					linkDialog = false
				}) { Text("Insertar") }
			},
			dismissButton = { TextButton(onClick = { linkDialog = false }) { Text("Cancelar") } },
		)
	}

}

@Composable
private fun FormatAction(
	icon: ImageVector,
	label: String,
	enabled: Boolean,
	selected: Boolean = false,
	onClick: () -> Unit
) {
	IconToggleButton(checked = selected, onCheckedChange = { onClick() }, enabled = enabled) {
		Icon(
			icon,
			label
		)
	}
}

@Composable
private fun <T> SpacingOptions(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit) {
	Text(label, style = MaterialTheme.typography.labelLarge)
	Row(
		Modifier.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		values.forEach { value ->
			FilterChip(
				selected = value == selected,
				onClick = { onSelect(value) },
				label = { Text(value.toString()) })
		}
	}
}
