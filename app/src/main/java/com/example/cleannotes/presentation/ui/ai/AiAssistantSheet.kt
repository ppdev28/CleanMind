package com.example.cleannotes.presentation.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cleannotes.ai.*
import com.example.cleannotes.presentation.ui.design.AppBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
	state: AiAssistantUiState,
	note: AiNoteSnapshot,
	enabled: Boolean,
	onSelect: (AiAction) -> Unit,
	onInstruction: (String) -> Unit,
	onGenerate: () -> Unit,
	onApply: (AiSuggestion) -> Unit,
	onSettings: () -> Unit,
	onCancel: () -> Unit,
	onDismiss: () -> Unit,
) {
	if (!state.visible) return
	AppBottomSheet(onDismissRequest = onDismiss) {
		Column(
			Modifier.fillMaxWidth()
				.verticalScroll(rememberScrollState())
				.imePadding()
				.padding(horizontal = 24.dp)
				.padding(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			Text("Asistente de IA", style = MaterialTheme.typography.headlineSmall)
			if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
			else if (!state.configured) {
				Text(
					"Añade tu clave de API y elige un modelo en Ajustes para usar la IA en tus notas."
				)
				Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
					Text("Configurar IA")
				}
			} else {
				Text(
					"Se enviará el título y el texto de esta nota a ${state.provider}. No se incluyen imágenes. Revisa la propuesta antes de añadirla; las peticiones pueden tener coste.",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				AiAction.entries.forEach { action ->
					FilterChip(
						selected = state.action == action,
						enabled = !state.generating,
						onClick = { onSelect(action) },
						label = { Text(action.label) },
						modifier = Modifier.fillMaxWidth(),
					)
				}
				if (state.action == AiAction.CUSTOM)
					OutlinedTextField(
						state.instruction,
						onInstruction,
						label = { Text("¿Qué quieres escribir?") },
						placeholder = {
							Text("Por ejemplo: redacta un correo breve con estas ideas")
						},
						enabled = !state.generating,
						modifier = Modifier.fillMaxWidth(),
						minLines = 2,
						supportingText = {
							Text("${state.instruction.length}/${AiRequest.MAX_INSTRUCTION_CHARS}")
						},
						isError = state.instruction.length > AiRequest.MAX_INSTRUCTION_CHARS,
					)
				if (state.generating) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(12.dp),
					) {
						CircularProgressIndicator(Modifier.size(24.dp))
						Text(
							"Generando propuesta…",
							Modifier.semantics { liveRegion = LiveRegionMode.Polite },
						)
					}
					OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
						Text("Cancelar generación")
					}
				} else {
					val valid =
						note.plainText().length <= AiRequest.MAX_NOTE_CHARS &&
							if (state.action == AiAction.CUSTOM)
								state.instruction.isNotBlank() &&
									state.instruction.length <= AiRequest.MAX_INSTRUCTION_CHARS
							else note.plainText().isNotBlank()
					if (note.plainText().length > AiRequest.MAX_NOTE_CHARS)
						Text(
							AiError.INPUT_TOO_LONG.message,
							color = MaterialTheme.colorScheme.error,
						)
					else if (note.plainText().isBlank() && state.action != AiAction.CUSTOM)
						Text(
							"Escribe una nota o elige Instrucción propia para crear texto desde cero.",
							style = MaterialTheme.typography.bodySmall,
						)
					Button(
						onClick = onGenerate,
						enabled = enabled && valid,
						modifier = Modifier.fillMaxWidth(),
					) {
						Text(if (state.suggestion != null) "Generar otra propuesta" else "Generar")
					}
				}
				state.suggestion?.let { suggestion ->
					HorizontalDivider()
					Text("Propuesta", style = MaterialTheme.typography.titleMedium)
					SelectionContainer {
						Text(
							suggestion.text,
							Modifier.semantics { liveRegion = LiveRegionMode.Polite },
						)
					}
					Text(
						"Se añadirá al final de la nota. El contenido original se conserva.",
						style = MaterialTheme.typography.bodySmall,
					)
					if (suggestion.note != note)
						Text(
							"La nota ha cambiado. Genera una nueva propuesta antes de añadirla.",
							color = MaterialTheme.colorScheme.error,
						)
					Button(
						onClick = { onApply(suggestion) },
						enabled = enabled && suggestion.note == note,
						modifier = Modifier.fillMaxWidth(),
					) {
						Text("Añadir a la nota")
					}
				}
			}
			state.error?.let {
				Text(
					it,
					Modifier.semantics { liveRegion = LiveRegionMode.Polite },
					color = MaterialTheme.colorScheme.error,
				)
			}
			TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
		}
	}
}
