package com.example.cleannotes.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AiSettings(viewModel: AiSettingsViewModel = hiltViewModel()) {
	val state by viewModel.state.collectAsStateWithLifecycle()
	AiSettingsContent(
		state,
		viewModel::edit,
		viewModel::save,
		viewModel::testConnection,
		viewModel::clear,
		viewModel::reload,
		viewModel::cancel,
	)
}

@Composable
fun AiSettingsContent(
	state: AiSettingsUiState,
	onEdit: (com.example.cleannotes.ai.AiConfiguration) -> Unit,
	onSave: () -> Unit,
	onTest: () -> Unit,
	onClear: () -> Unit,
	onReload: () -> Unit,
	onCancel: () -> Unit,
) {
	val config = state.configuration
	val enabled = !state.loading && !state.busy
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Text("Inteligencia artificial", style = MaterialTheme.typography.titleLarge)
		Text(
			"Conecta tu API de OpenAI o un proveedor compatible con Chat Completions. La clave se guarda cifrada en este dispositivo.",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		OutlinedTextField(
			config.baseUrl,
			{ onEdit(config.copy(baseUrl = it)) },
			label = { Text("URL base de la API") },
			placeholder = { Text("https://api.openai.com/v1") },
			enabled = enabled,
			singleLine = true,
			modifier = Modifier.fillMaxWidth(),
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
			supportingText = { Text("Incluye /v1 si tu proveedor lo requiere. Solo HTTPS.") },
		)
		OutlinedTextField(
			config.model,
			{ onEdit(config.copy(model = it)) },
			label = { Text("Modelo de IA") },
			enabled = enabled,
			singleLine = true,
			modifier = Modifier.fillMaxWidth(),
		)
		OutlinedTextField(
			config.apiKey,
			{ onEdit(config.copy(apiKey = it)) },
			label = { Text("Clave de API") },
			enabled = enabled,
			singleLine = true,
			modifier = Modifier.fillMaxWidth(),
			visualTransformation = PasswordVisualTransformation(),
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
		)
		Text(
			"Solo se envía el texto de la nota abierta cuando pulsas Generar. No se envían imágenes ni otras notas. La prueba de conexión envía un mensaje de prueba y puede tener coste según tu proveedor.",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		state.error?.let {
			Text(
				it,
				color = MaterialTheme.colorScheme.error,
				style = MaterialTheme.typography.bodyMedium,
			)
		}
		state.status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
		if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
		Button(
			onClick = onSave,
			enabled = enabled && state.dirty,
			modifier = Modifier.fillMaxWidth(),
		) {
			Text("Guardar configuración de IA")
		}
		OutlinedButton(onClick = onTest, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
			Text("Probar conexión")
		}
		if (state.busy) TextButton(onClick = onCancel) { Text("Cancelar") }
		Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
			TextButton(onClick = onClear, enabled = enabled) {
				Text("Eliminar clave y configuración")
			}
		}
		if (state.error != null)
			TextButton(onClick = onReload, enabled = enabled) { Text("Recargar configuración") }
	}
}
