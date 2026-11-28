package com.example.cleannotes.presentation.ui.chatgpt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleannotes.ai.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatGptAccountUi(
	val clientId: String,
	val label: String,
	val email: String,
	val connected: Boolean,
	val planEnabled: Boolean,
	val model: String,
	val welcomed: Boolean,
)

data class ChatGptSettingsUiState(
	val accounts: List<ChatGptAccountUi> = emptyList(),
	val activeClientId: String? = null,
	val models: List<ChatGptModel> = emptyList(),
	val loading: Boolean = true,
	val busy: Boolean = false,
	val signingIn: Boolean = false,
	val authorizationUrl: String? = null,
	val error: String? = null,
	val status: String? = null,
) {
	val account
		get() = accounts.firstOrNull { it.clientId == activeClientId }

	val welcome
		get() = account?.let { it.connected && it.planEnabled && !it.welcomed } == true

	// OAuth credentials remain inside the access service, never in presentation state.
	override fun toString() = "ChatGptSettingsUiState(busy=$busy, [REDACTED])"
}

@HiltViewModel
class ChatGptSettingsViewModel @Inject constructor(private val access: ChatGptAccess) :
	ViewModel() {
	private val _state = MutableStateFlow(ChatGptSettingsUiState())
	val state = _state.asStateFlow()
	private var job: Job? = null
	private var operationId = 0

	init {
		reload()
	}

	fun reload() = operation {
		refreshState()
		if (state.value.account?.connected == true) {
			val models = access.models()
			refreshState()
			_state.update { it.copy(models = models) }
		}
	}

	fun signIn(clientId: String? = null) = operation {
		_state.update { it.copy(signingIn = true) }
		access.signIn(clientId) { url -> _state.update { it.copy(authorizationUrl = url) } }
		refreshState()
		val models = access.models()
		refreshState()
		_state.update { it.copy(models = models, status = "Cuenta de ChatGPT conectada.") }
	}

	fun select(clientId: String?) = operation {
		access.select(clientId)
		refreshState()
		if (state.value.account?.connected == true) {
			val models = access.models()
			refreshState()
			_state.update { it.copy(models = models) }
		}
	}

	fun selectModel(model: String) = operation {
		access.selectModel(model)
		refreshState()
	}

	fun welcomeAccepted() = operation {
		access.acknowledgeWelcome()
		refreshState()
	}

	fun signOut(clientId: String) = operation {
		val revoked = access.signOut(clientId)
		refreshState()
		_state.update {
			it.copy(
				status =
					if (revoked) "Sesión de ChatGPT cerrada."
					else
						"Sesión cerrada en este dispositivo. No se pudo confirmar la revocación remota; desconecta Clean Mind desde los ajustes de ChatGPT."
			)
		}
	}

	fun browserOpened() {
		_state.update { it.copy(authorizationUrl = null) }
	}

	fun browserUnavailable() {
		cancel()
		_state.update {
			it.copy(
				error =
					"No se pudo abrir el navegador. Instala o activa un navegador para iniciar sesión."
			)
		}
	}

	fun cancel() {
		operationId++
		job?.cancel()
		_state.update { it.copy(busy = false, signingIn = false, authorizationUrl = null) }
	}

	private suspend fun refreshState() {
		val saved = access.state()
		// Strip credentials from the UI; presence of a session is represented separately below.
		_state.update { previous ->
			previous.copy(
				accounts =
					saved.accounts.map {
						ChatGptAccountUi(
							it.clientId,
							it.label,
							it.email,
							it.tokens != null,
							it.tokens?.permitsInference == true,
							it.model,
							it.welcomed,
						)
					},
				activeClientId = saved.activeClientId,
				models =
					if (
						previous.activeClientId == saved.activeClientId &&
							saved.activeAccount?.tokens != null
					)
						previous.models
					else emptyList(),
			)
		}
	}

	private fun operation(block: suspend () -> Unit) {
		if (state.value.busy) return
		val operation = ++operationId
		_state.update { it.copy(busy = true, error = null, status = null) }
		job =
			viewModelScope.launch {
				try {
					block()
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					try {
						refreshState()
					} catch (cancelled: CancellationException) {
						throw cancelled
					} catch (_: Exception) {}
					_state.update { it.copy(error = e.aiMessage()) }
				} finally {
					if (operation == operationId)
						_state.update {
							it.copy(
								loading = false,
								busy = false,
								signingIn = false,
								authorizationUrl = null,
							)
						}
				}
			}
	}
}
