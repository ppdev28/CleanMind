package com.example.cleannotes.presentation.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.design.MindIcons
import java.util.UUID

@Composable
fun ThemePaletteSettings(
	appearance: Appearance,
	onSelect: (String) -> Unit,
	onEdit: (String) -> Unit,
	onDelete: (String) -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Text("Temas", style = MaterialTheme.typography.titleLarge)
		Text("La paleta se adapta al modo claro y oscuro.", style = MaterialTheme.typography.bodySmall)
		FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			Appearance.presets.forEach { palette ->
				FilterChip(
					selected = appearance.selected.id == palette.id,
					onClick = { onSelect(palette.id) },
					label = { Text(palette.name) },
					leadingIcon = {
						Box(
							Modifier.size(18.dp).background(
								palette.accent?.let(::Color) ?: MaterialTheme.colorScheme.onSurface,
								CircleShape
							)
						)
					})
			}
		}
		appearance.customThemes.forEach { theme ->
			Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				FilterChip(
					selected = appearance.selectedId == theme.id,
					onClick = { onSelect(theme.id) },
					modifier = Modifier.weight(1f),
					label = { Text(theme.name) },
					leadingIcon = { Box(Modifier.size(18.dp).background(Color(theme.accent), CircleShape)) })
				IconButton(onClick = { onEdit(theme.id) }) {
					Icon(
						MindIcons.EditNote,
						"Editar tema ${theme.name}"
					)
				}
				IconButton(onClick = { onDelete(theme.id) }) {
					Icon(
						MindIcons.Delete,
						"Eliminar tema ${theme.name}"
					)
				}
			}
		}
		OutlinedButton(onClick = { onEdit("new") }) {
			Icon(
				MindIcons.Add,
				null
			); Spacer(Modifier.width(8.dp)); Text("Crear tema")
		}
	}
}

/** Keep the dialog outside the scrolling settings layout so input focus cannot scroll its parent. */
@Composable
fun ThemeEditorDialog(
	id: String,
	appearance: Appearance,
	onSave: (CustomTheme) -> Unit,
	onDismiss: () -> Unit
) {
	val original = appearance.customThemes.firstOrNull { it.id == id }
	var name by rememberSaveable(id) { mutableStateOf(original?.name.orEmpty()) }
	var hex by rememberSaveable(id) {
		mutableStateOf(original?.accent?.let { "%06X".format(it and 0xFFFFFFL) } ?: "3E7194")
	}
	val color = hex.removePrefix("#").takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) }?.toLongOrNull(16)
		?.or(0xFF000000L)
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text(if (original == null) "Crear tema" else "Editar tema") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				ThemeInput("Nombre del tema", name) { name = it.take(30) }
				ThemeInput("Color de acento · HEX", hex) { hex = it.take(7) }
				if (color == null) Text(
					"Introduce seis caracteres hexadecimales, por ejemplo 3E7194.",
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall
				)
				else Box(Modifier.size(48.dp).background(Color(color), CircleShape))
			}
		},
		confirmButton = {
			TextButton(enabled = name.isNotBlank() && color != null, onClick = {
				onSave(
					CustomTheme(
						original?.id ?: UUID.randomUUID().toString(),
						name.trim(),
						requireNotNull(color)
					)
				); onDismiss()
			}) { Text("Guardar tema") }
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ThemeInput(label: String, value: String, onChange: (String) -> Unit) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text(label, style = MaterialTheme.typography.labelMedium)
		Surface(
			shape = MaterialTheme.shapes.small,
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
		) {
			BasicTextField(
				value,
				onChange,
				singleLine = true,
				modifier = Modifier.fillMaxWidth().padding(14.dp).semantics { contentDescription = label },
				textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
				cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
			)
		}
	}
}
