package com.example.cleannotes.presentation.ui.edit_note.richtext

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.presentation.ui.design.AppBottomSheet
import com.example.cleannotes.presentation.ui.design.MindIcons

/** The new visible toolbar and single formatting panel operate on normal rich text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichFormatBar(
	document: NoteDocument,
	controller: RichEditorController,
	enabled: Boolean,
	modifier: Modifier = Modifier
) {
	var expanded by rememberSaveable { mutableStateOf(false) }
	val keyboard = LocalSoftwareKeyboardController.current
	val editor = controller.editor
	val active = controller.activeFormats
	Row(modifier) {
		Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
			FormatAction(
				MindIcons.FormatBold,
				"Negrita",
				enabled,
				"bold" in active
			) { editor?.format("bold") }
			FormatAction(MindIcons.FormatItalic, "Cursiva", enabled, "italic" in active) {
				editor?.format(
					"italic"
				)
			}
			FormatAction(
				MindIcons.FormatUnderlined,
				"Subrayado",
				enabled,
				"underline" in active
			) { editor?.format("underline") }
			FormatAction(
				MindIcons.FormatListBulleted,
				"Lista con viñetas",
				enabled
			) { editor?.list("• ") }
			FormatAction(
				MindIcons.FormatListNumbered,
				"Lista numerada",
				enabled
			) { editor?.list("number") }
			FormatAction(
				MindIcons.StrikethroughS,
				"Tachado",
				enabled,
				"strike" in active
			) { editor?.format("strike") }
		}
		IconButton(onClick = { keyboard?.hide(); expanded = true }, enabled = enabled) {
			Icon(
				MindIcons.Tune,
				"Más formato y espaciado"
			)
		}
	}
	if (expanded) AppBottomSheet(
		onDismissRequest = { expanded = false },
		sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	) {
		Column(
			Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
				.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
		) {
			Text("Formato de texto", style = MaterialTheme.typography.titleLarge)
			Text(
				"Se aplica a la selección o al párrafo actual.",
				style = MaterialTheme.typography.bodySmall
			)
			Text("Tamaño de texto", style = MaterialTheme.typography.labelLarge)
			FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				listOf(180, 155, 135, 120, 110, 100).forEachIndexed { index, size ->
					FilterChip(
						selected = "size:$size" in active,
						onClick = { editor?.format("size", size) },
						label = { Text("H${index + 1}") })
				}
				AssistChip(onClick = { editor?.format("size", 100) }, label = { Text("Normal") })
			}
			Text("Estilo y listas", style = MaterialTheme.typography.labelLarge)
			FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				listOf<Pair<String, () -> Unit>>(
					"Negrita" to { editor?.format("bold") }, "Cursiva" to { editor?.format("italic") },
					"Subrayado" to { editor?.format("underline") }, "Tachado" to { editor?.format("strike") },
					"Viñetas" to { editor?.list("• ") }, "Numeración" to { editor?.list("number") },
					"Borrar formato" to { editor?.format("clear") },
				).forEach { (label, action) -> AssistChip(onClick = action, label = { Text(label) }) }
			}
			Text("Alineación y sangría", style = MaterialTheme.typography.labelLarge)
			Row(Modifier.horizontalScroll(rememberScrollState())) {
				FormatAction(MindIcons.FormatAlignLeft, "Alinear a la izquierda", enabled) {
					editor?.format(
						"align",
						0
					)
				}
				FormatAction(MindIcons.FormatAlignCenter, "Centrar", enabled) { editor?.format("align", 2) }
				FormatAction(
					MindIcons.FormatAlignRight,
					"Alinear a la derecha",
					enabled
				) { editor?.format("align", 1) }
				FormatAction(
					MindIcons.FormatIndentIncrease,
					"Aumentar sangría",
					enabled
				) { editor?.format("indent", 48) }
				FormatAction(
					MindIcons.FormatIndentDecrease,
					"Quitar sangría",
					enabled
				) { editor?.format("indent", 0) }
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
