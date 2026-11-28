package com.example.cleannotes.presentation.ui.chatgpt

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ChatGptSettings(viewModel: ChatGptSettingsViewModel = hiltViewModel()) {
	val state by viewModel.state.collectAsStateWithLifecycle()
	val context = LocalContext.current
	LaunchedEffect(state.authorizationUrl) {
		state.authorizationUrl?.let { url ->
			try {
				context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
				viewModel.browserOpened()
			} catch (_: Exception) {
				viewModel.browserUnavailable()
			}
		}
	}
	ChatGptSettingsContent(
		state,
		viewModel::signIn,
		viewModel::select,
		viewModel::selectModel,
		viewModel::signOut,
		viewModel::reload,
		viewModel::cancel,
		viewModel::welcomeAccepted,
		{
			context.startActivity(
				Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/settings/usage"))
			)
		},
	)
}

@Composable
fun ChatGptSettingsContent(
	state: ChatGptSettingsUiState,
	onSignIn: (String?) -> Unit,
	onSelect: (String?) -> Unit,
	onModel: (String) -> Unit,
	onSignOut: (String) -> Unit,
	onReload: () -> Unit,
	onCancel: () -> Unit,
	onWelcome: () -> Unit,
	onUsage: () -> Unit,
) {
	var expanded by remember { mutableStateOf(false) }
	val account = state.account
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		Text("Usar mi cuenta de ChatGPT", style = MaterialTheme.typography.titleLarge)
		Text(
			"Conecta tu cuenta y autoriza el uso de tu plan en Clean Mind. En el navegador de OpenAI puedes entrar con tu cuenta de Google habitual. No necesitas una clave de API.",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
		)
		state.accounts.forEach { saved ->
			FilterChip(
				selected = state.activeClientId == saved.clientId,
				enabled = !state.busy,
				onClick = { onSelect(saved.clientId) },
				label = {
					Text(
						"${saved.label} · ${saved.email.ifBlank { "ChatGPT" }}${if (saved.connected) "" else " · Sesión cerrada"}"
					)
				},
				modifier = Modifier.fillMaxWidth(),
			)
		}
		if (account != null) {
			Text(
				if (account.planEnabled && account.connected) "Usando tu plan de ChatGPT"
				else if (account.connected) "Cuenta conectada. Falta autorizar el uso de tu plan."
				else "Vuelve a iniciar sesión para usar esta cuenta.",
				style = MaterialTheme.typography.titleMedium,
			)
			if (account.connected && account.planEnabled) {
				Box {
					OutlinedButton(
						onClick = { expanded = true },
						enabled = !state.busy && state.models.isNotEmpty(),
						modifier = Modifier.fillMaxWidth(),
					) {
						Text(
							state.models.firstOrNull { it.id == account.model }?.name
								?: "Elegir modelo de ChatGPT"
						)
					}
					DropdownMenu(expanded, { expanded = false }) {
						state.models.forEach { model ->
							DropdownMenuItem(
								text = { Text(model.name) },
								onClick = {
									expanded = false
									onModel(model.id)
								},
							)
						}
					}
				}
				TextButton(onClick = onReload, enabled = !state.busy) { Text("Actualizar modelos") }
				TextButton(onClick = { onSignIn(account.clientId) }, enabled = !state.busy) {
					Text("Volver a conectar esta cuenta")
				}
			} else
				Button(
					onClick = { onSignIn(account.clientId) },
					enabled = !state.busy,
					modifier = Modifier.fillMaxWidth(),
				) {
					Text(
						if (account.connected) "Autorizar uso de mi plan"
						else "Continuar con ChatGPT"
					)
				}
			if (account.connected)
				TextButton(onClick = { onSignOut(account.clientId) }, enabled = !state.busy) {
					Text("Cerrar sesión de ChatGPT")
				}
		}
		if (account == null || account.connected)
			OutlinedButton(
				onClick = { onSignIn(null) },
				enabled = !state.busy,
				modifier = Modifier.fillMaxWidth(),
			) {
				Text(
					if (state.accounts.isEmpty()) "Continuar con ChatGPT"
					else "Añadir otra cuenta de ChatGPT"
				)
			}
		if (state.activeClientId != null)
			TextButton(onClick = { onSelect(null) }, enabled = !state.busy) {
				Text("Usar configuración de API")
			}
		else
			Text(
				"Fuente actual de IA: configuración de API.",
				style = MaterialTheme.typography.bodySmall,
			)
		Text(
			"El uso depende de los permisos y límites de tu plan. La app solo envía el texto de la nota al generar; no accede a tu historial de ChatGPT.",
			style = MaterialTheme.typography.bodySmall,
		)
		TextButton(onClick = onUsage) { Text("Gestionar uso en ChatGPT") }
		if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
		if (state.signingIn) {
			Text(
				"Completa el acceso en el navegador y vuelve a Clean Mind. El intento caduca en 5 minutos.",
				style = MaterialTheme.typography.bodySmall,
			)
			TextButton(onClick = onCancel) { Text("Cancelar inicio de sesión") }
		}
		state.error?.let {
			Text(it, color = MaterialTheme.colorScheme.error)
			TextButton(onClick = onReload, enabled = !state.busy) {
				Text("Reintentar conexión de ChatGPT")
			}
		}
		state.status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
	}
	if (state.welcome && !state.busy)
		AlertDialog(
			onDismissRequest = onWelcome,
			title = { Text("Estás usando tu plan de ChatGPT") },
			text = {
				Text(
					"Las peticiones de IA de Clean Mind usarán el acceso incluido en tu plan y sus límites. Puedes revisar y gestionar el uso desde los ajustes de ChatGPT."
				)
			},
			confirmButton = { TextButton(onClick = onWelcome) { Text("Entendido") } },
		)
}
