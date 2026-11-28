package com.example.cleannotes.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleannotes.data.preferences.UserPreferencesRepository
import com.example.cleannotes.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
	val themeMode: ThemeMode = ThemeMode.SYSTEM,
	val isLoading: Boolean = true,
	val hasError: Boolean = false,
	val appearance: Appearance = Appearance(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: UserPreferencesRepository) :
	ViewModel() {
	private val reload = MutableStateFlow(0)
	private val writeFailed = MutableStateFlow(false)
	private val preferences = reload.flatMapLatest {
		combine(repository.themeMode, repository.appearance) { mode, appearance ->
			SettingsUiState(themeMode = mode, appearance = appearance, isLoading = false)
		}
			.catch { error ->
				if (error is CancellationException) throw error
				emit(SettingsUiState(isLoading = false, hasError = true))
			}
	}
	val uiState =
		combine(
			preferences,
			writeFailed,
		) { state, failed ->
			state.copy(hasError = state.hasError || failed)
		}
			.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

	fun setThemeMode(mode: ThemeMode) {
		viewModelScope.launch {
			try {
				repository.setThemeMode(mode)
				writeFailed.value = false
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				writeFailed.value = true
			}
		}
	}

	private fun appearanceWrite(action: suspend () -> Unit) {
		viewModelScope.launch {
			try {
				action(); writeFailed.value = false
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				writeFailed.value = true
			}
		}
	}

	fun selectPalette(id: String) = appearanceWrite { repository.selectPalette(id) }
	fun saveCustomTheme(theme: CustomTheme) = appearanceWrite { repository.saveCustomTheme(theme) }
	fun deleteCustomTheme(id: String) = appearanceWrite { repository.deleteCustomTheme(id) }

	fun retry() {
		writeFailed.value = false
		reload.update { it + 1 }
	}
}
