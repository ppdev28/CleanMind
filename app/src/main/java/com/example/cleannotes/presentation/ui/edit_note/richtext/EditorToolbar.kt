package com.example.cleannotes.presentation.ui.edit_note.richtext

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleannotes.model.NoteDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorBottomBar(
	document: NoteDocument,
	controller: RichEditorController,
	enabled: Boolean,
	status: String,
	statusError: Boolean,
	onShare: () -> Unit,
	initialFormatting: Boolean = false,
) {
	var formatting by rememberSaveable { mutableStateOf(initialFormatting) }
	var sheet by rememberSaveable { mutableStateOf<String?>(null) }
	var more by remember { mutableStateOf(false) }
	val editor = controller.editor
	val active = controller.activeFormats
	Column(Modifier.navigationBarsPadding()) {
		if (formatting) {
			Row(
				Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				FormatButton(
					"Título grande",
					enabled,
					"size:160" in active,
					Modifier.weight(1f),
					{ editor?.format("size", 160) },
				) {
					Text("H₁", fontSize = 19.sp)
				}
				FormatButton(
					"Título pequeño",
					enabled,
					"size:125" in active,
					Modifier.weight(1f),
					{ editor?.format("size", 125) },
				) {
					Text("H₂", fontSize = 19.sp)
				}
				FormatButton(
					"Texto normal",
					enabled,
					active.none { it.startsWith("size:") } || "size:100" in active,
					Modifier.weight(1f),
					{ editor?.format("size", 100) },
				) {
					Text("Aa", fontSize = 18.sp)
				}
				FormatButton(
					"Negrita",
					enabled,
					"bold" in active,
					Modifier.weight(1f),
					{ editor?.format("bold") },
				) {
					Icon(Icons.Outlined.FormatBold, null)
				}
				FormatButton(
					"Cursiva",
					enabled,
					"italic" in active,
					Modifier.weight(1f),
					{ editor?.format("italic") },
				) {
					Icon(Icons.Outlined.FormatItalic, null)
				}
				FormatButton(
					"Subrayado",
					enabled,
					"underline" in active,
					Modifier.weight(1f),
					{ editor?.format("underline") },
				) {
					Icon(Icons.Outlined.FormatUnderlined, null)
				}
				FormatButton(
					"Borrar formato",
					enabled,
					false,
					Modifier.weight(1f),
					{ editor?.format("clear") },
				) {
					Icon(Icons.Outlined.FormatClear, null)
				}
				IconButton(onClick = { formatting = false }, modifier = Modifier.weight(1f)) {
					Icon(Icons.Outlined.Close, "Cerrar formato")
				}
			}
		} else {
			Row(
				Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(
					onClick = { sheet = "insert" },
					enabled = enabled && !controller.importing,
				) {
					BottomActionIcon(Icons.Outlined.AddBox, "Añadir a la nota")
				}
				IconButton(onClick = { formatting = true }, enabled = enabled) {
					BottomActionIcon(Icons.Outlined.TextFormat, "Formato de texto")
				}
				Spacer(Modifier.weight(1f))
				Box {
					IconButton(onClick = { more = true }, enabled = enabled) {
						BottomActionIcon(Icons.Outlined.MoreVert, "Más opciones")
					}
					DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
						if (status.isNotEmpty())
							Text(
								status,
								Modifier.widthIn(max = 280.dp)
									.padding(horizontal = 16.dp, vertical = 12.dp),
								fontSize = 12.sp,
								color =
									if (statusError) MaterialTheme.colorScheme.error
									else MaterialTheme.colorScheme.onSurfaceVariant,
							)
						DropdownMenuItem(
							text = { Text("Enviar") },
							leadingIcon = { Icon(Icons.Outlined.Share, null) },
							onClick = {
								more = false
								onShare()
							},
						)
						DropdownMenuItem(
							text = { Text("Formato y espaciado") },
							leadingIcon = { Icon(Icons.Outlined.FormatLineSpacing, null) },
							onClick = {
								more = false
								sheet = "advanced"
							},
						)
					}
				}
			}
		}
	}
	if (sheet != null) {
		ModalBottomSheet(
			onDismissRequest = { sheet = null },
			containerColor = MaterialTheme.colorScheme.surface,
			sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
		) {
			when (sheet) {
				"insert" ->
					Column(Modifier.padding(bottom = 24.dp)) {
						InsertAction(Icons.Outlined.PhotoCamera, "Hacer foto") {
							sheet = null
							controller.takePhoto()
						}
						InsertAction(Icons.Outlined.Image, "Añadir imagen") {
							sheet = null
							controller.chooseImage()
						}
						InsertAction(Icons.Outlined.ContentPaste, "Pegar imagen") {
							sheet = null
							if (editor?.pasteImage() != true)
								controller.error =
									"Copia una imagen al portapapeles para pegarla aquí."
						}
						InsertAction(Icons.Outlined.CheckBox, "Casillas de verificación") {
							sheet = null
							editor?.checkItem()
						}
						InsertAction(
							Icons.AutoMirrored.Outlined.FormatListBulleted,
							"Lista con viñetas",
						) {
							sheet = null
							editor?.list("• ")
						}
						InsertAction(Icons.Outlined.FormatListNumbered, "Lista numerada") {
							sheet = null
							editor?.list("number")
						}
					}

				"advanced" -> AdvancedFormatting(document, controller)
			}
		}
	}
}

@Composable
private fun BottomActionIcon(icon: ImageVector, label: String) {
	Box(
		Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
		contentAlignment = Alignment.Center,
	) {
		Icon(icon, label, Modifier.size(22.dp))
	}
}

@Composable
private fun FormatButton(
	label: String,
	enabled: Boolean,
	selected: Boolean,
	modifier: Modifier,
	onClick: () -> Unit,
	content: @Composable () -> Unit,
) {
	IconButton(
		onClick = onClick,
		enabled = enabled,
		modifier =
			modifier.semantics {
				contentDescription = label
				this.selected = selected
			},
	) {
		Box(
			Modifier.size(32.dp)
				.background(
					if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
					RoundedCornerShape(4.dp),
				),
			contentAlignment = Alignment.Center,
		) {
			content()
		}
	}
}

@Composable
private fun InsertAction(icon: ImageVector, label: String, onClick: () -> Unit) {
	Surface(onClick = onClick, color = Color.Transparent) {
		Row(
			Modifier.fillMaxWidth()
				.heightIn(min = 52.dp)
				.padding(horizontal = 24.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(24.dp),
		) {
			Icon(icon, null, Modifier.size(24.dp))
			Text(label, fontSize = 16.sp)
		}
	}
}

@Composable
private fun AdvancedFormatting(document: NoteDocument, controller: RichEditorController) {
	val editor = controller.editor
	Column(
		Modifier.fillMaxWidth()
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 20.dp)
			.padding(bottom = 32.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Text("Formato y espaciado", fontSize = 20.sp, fontWeight = FontWeight.Medium)
		Text(
			"Se aplica a la selección o al párrafo actual.",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		Row(Modifier.horizontalScroll(rememberScrollState())) {
			IconButton(onClick = { editor?.format("strike") }) {
				Icon(Icons.Outlined.StrikethroughS, "Tachado")
			}
			IconButton(onClick = { editor?.format("align", 0) }) {
				Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, "Alinear a la izquierda")
			}
			IconButton(onClick = { editor?.format("align", 2) }) {
				Icon(Icons.Outlined.FormatAlignCenter, "Centrar")
			}
			IconButton(onClick = { editor?.format("align", 1) }) {
				Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, "Alinear a la derecha")
			}
			IconButton(onClick = { editor?.format("indent", 48) }) {
				Icon(Icons.AutoMirrored.Outlined.FormatIndentIncrease, "Aumentar sangría")
			}
			IconButton(onClick = { editor?.format("indent", 0) }) {
				Icon(Icons.AutoMirrored.Outlined.FormatIndentDecrease, "Quitar sangría")
			}
		}
		SpacingOptions("Interlineado", listOf(1f, 1.3f, 1.5f, 2f), document.lineSpacing) {
			editor?.spacing(it, document.extraLineSpacing)
		}
		SpacingOptions("Espacio entre párrafos", listOf(0, 8, 16, 24), document.paragraphSpacing) {
			editor?.paragraphSpacing(it)
		}
		SpacingOptions(
			"Espacio extra entre líneas",
			listOf(0, 4, 8, 16),
			document.extraLineSpacing,
		) {
			editor?.spacing(document.lineSpacing, it)
		}
	}
}

@Composable
private fun <T> SpacingOptions(label: String, values: List<T>, selected: T, onSelect: (T) -> Unit) {
	Text(label, style = MaterialTheme.typography.labelLarge)
	Row(
		Modifier.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		values.forEach { value ->
			FilterChip(
				selected = value == selected,
				onClick = { onSelect(value) },
				label = { Text(value.toString()) },
			)
		}
	}
}
