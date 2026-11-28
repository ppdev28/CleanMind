package com.example.cleannotes.presentation.ui.ai

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

data class AiAssistantUiState(
	val visible: Boolean = false,
	val loading: Boolean = false,
	val configured: Boolean = false,
	val provider: String = "",
	val action: AiAction = AiAction.SUMMARIZE,
	val instruction: String = "",
	val generating: Boolean = false,
	val suggestion: AiSuggestion? = null,
	val error: String? = null,
)

@HiltViewModel
class AiAssistantViewModel
@Inject
constructor(private val settings: AiSettingsRepository, private val gateway: AiGateway) :
	ViewModel() {
	private val _state = MutableStateFlow(AiAssistantUiState())
	val state = _state.asStateFlow()
	private var configuration: AiConfiguration? = null
	private var job: Job? = null
	private var operationId = 0

	fun open() {
		val operation = ++operationId
		job?.cancel()
		configuration = null
		_state.value = AiAssistantUiState(visible = true, loading = true)
		job =
			viewModelScope.launch {
				try {
					val loaded = settings.load()
					if (loaded.apiKey.isNotBlank()) {
						loaded.validate()
						configuration = loaded
						_state.update {
							it.copy(configured = true, provider = loaded.endpoint().host)
						}
					}
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					_state.update { it.copy(error = e.aiMessage()) }
				} finally {
					if (operation == operationId) _state.update { it.copy(loading = false) }
				}
			}
	}

	fun select(action: AiAction) {
		if (!state.value.generating)
			_state.update { it.copy(action = action, suggestion = null, error = null) }
	}

	fun instruct(text: String) {
		if (!state.value.generating)
			_state.update { it.copy(instruction = text, suggestion = null, error = null) }
	}

	fun generate(note: AiNoteSnapshot) {
		val config = configuration ?: return
		if (!state.value.visible || state.value.loading || state.value.generating) return
		val request = AiRequest(
			note,
			state.value.action,
			if (state.value.action == AiAction.CUSTOM) state.value.instruction.trim() else "",
		)
		val operation = ++operationId
		_state.update { it.copy(generating = true, suggestion = null, error = null) }
		job =
			viewModelScope.launch {
				try {
					request.validate()
					val text = gateway.complete(config, request)
					_state.update { it.copy(suggestion = AiSuggestion(note, text)) }
				} catch (e: CancellationException) {
					throw e
				} catch (e: Exception) {
					_state.update { it.copy(error = e.aiMessage()) }
				} finally {
					if (operation == operationId) _state.update { it.copy(generating = false) }
				}
			}
	}

	fun cancelGeneration() {
		operationId++
		job?.cancel()
		_state.update { it.copy(generating = false) }
	}

	fun dismiss() {
		operationId++
		job?.cancel()
		configuration = null
		_state.value = AiAssistantUiState()
	}
}
