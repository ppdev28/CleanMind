package com.example.cleannotes.presentation

import com.example.cleannotes.data.preferences.UserPreferencesRepository
import com.example.cleannotes.model.ThemeMode
import com.example.cleannotes.presentation.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }
    @Test fun failedWritePreservesTheStoredThemeAndAllowsRetry() = runTest(dispatcher) {
        val repository = object : UserPreferencesRepository {
            override val themeMode = MutableStateFlow(ThemeMode.DARK)
            var fail = true
            override suspend fun setThemeMode(mode: ThemeMode) {
                if (fail) error("Write failed")
                themeMode.value = mode
            }
        }
        val viewModel = SettingsViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        runCurrent()
        viewModel.setThemeMode(ThemeMode.LIGHT)
        runCurrent()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertTrue(viewModel.uiState.value.hasError)
        repository.fail = false
        viewModel.setThemeMode(ThemeMode.LIGHT)
        runCurrent()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.hasError)
    }
}
