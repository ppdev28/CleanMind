package com.example.cleannotes.presentation.ui.settings

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

data class AiSettingsUiState(
	val configuration: AiConfiguration = AiConfiguration(),
	val loading: Boolean = true,
	val busy: Boolean = false,
	val dirty: Boolean = false,
	val error: String? = null,
	val status: String? = null,
)

@HiltViewModel
class AiSettingsViewModel
@Inject
constructor(private val repository: AiSettingsRepository, private val gateway: AiGateway) :
	ViewModel() {
	private val _state = MutableStateFlow(AiSettingsUiState())
	val state = _state.asStateFlow()
	private var job: Job? = null
	private var operationId = 0

	init {
		reload()
	}

	fun reload() {
		if (state.value.busy) return
		val operation = ++operationId
		job?.cancel()
		job =
			viewModelScope.launch {
				_state.update { it.copy(loading = true, error = null, status = null) }
				try {
					_state.value =
						AiSettingsUiState(configuration = repository.load(), loading = false)
				} catch (e: CancellationException) {
					throw e
				} catch (_: Exception) {
					_state.update { it.copy(error = AiError.STORAGE.message) }
				} finally {
					if (operation == operationId) _state.update { it.copy(loading = false) }
				}
			}
	}

	fun edit(configuration: AiConfiguration) {
		if (state.value.loading || state.value.busy) return
		_state.update {
			it.copy(configuration = configuration, dirty = true, error = null, status = null)
		}
	}

	fun save() = operation {
		val configuration = state.value.configuration.normalized()
		configuration.validate()
		repository.save(configuration)
		_state.update {
			it.copy(
				configuration = configuration,
				dirty = false,
				status = "Configuración de IA guardada.",
			)
		}
	}

	fun testConnection() = operation {
		val configuration = state.value.configuration.normalized()
		configuration.validate()
		gateway.testConnection(configuration)
		_state.update {
			it.copy(
				status =
					if (it.dirty) "Conexión correcta. Guarda los cambios para usarlos en las notas."
					else "Conexión correcta."
			)
		}
	}

	fun clear() = operation {
		repository.clear()
		_state.value =
			AiSettingsUiState(
				loading = false,
				busy = true,
				status = "Configuración y clave de IA eliminadas.",
			)
	}

	fun cancel() {
		operationId++
		job?.cancel()
		_state.update { it.copy(busy = false) }
	}

	private fun operation(block: suspend () -> Unit) {
		if (state.value.loading || state.value.busy) return
		val operation = ++operationId
		_state.update { it.copy(busy = true, error = null, status = null) }
		job =
			viewModelScope.launch {
				try {
					block()
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					_state.update { it.copy(error = e.aiMessage()) }
				} finally {
					if (operation == operationId) _state.update { it.copy(busy = false) }
				}
			}
	}
}
