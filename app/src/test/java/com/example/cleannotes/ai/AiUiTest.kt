package com.example.cleannotes.ai

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.example.cleannotes.presentation.ui.ai.AiAssistantSheet
import com.example.cleannotes.presentation.ui.ai.AiAssistantUiState
import com.example.cleannotes.presentation.ui.settings.AiSettingsContent
import com.example.cleannotes.presentation.ui.settings.AiSettingsUiState
import com.example.cleannotes.presentation.ui.theme.CleanMindTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AiUiTest {
	@get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

	@Test
	fun unconfiguredAssistantOffersSettingsWithoutGeneration() {
		var opened = false
		compose.setContent {
			CleanMindTheme {
				AiAssistantSheet(
					AiAssistantUiState(visible = true),
					AiNoteSnapshot("", ""),
					true,
					{},
					{},
					{},
					{},
					{ opened = true },
					{},
					{},
				)
			}
		}
		compose.onNodeWithText("Generar").assertDoesNotExist()
		compose.onNodeWithText("Configurar IA").performClick()
		assertTrue(opened)
	}

	@Test
	fun resultIsOnlyAppliedWhenExplicitlyAccepted() {
		val note = AiNoteSnapshot("Nota", "Texto original")
		val suggestion = AiSuggestion(note, "Propuesta generada")
		var applied: AiSuggestion? = null
		compose.setContent {
			CleanMindTheme {
				AiAssistantSheet(
					AiAssistantUiState(
						visible = true,
						configured = true,
						provider = "api.example.com",
						suggestion = suggestion,
					),
					note,
					true,
					{},
					{},
					{},
					{ applied = it },
					{},
					{},
					{},
				)
			}
		}
		assertNull(applied)
		compose.onNodeWithText("Añadir a la nota").performScrollTo().performClick()
		assertEquals(suggestion, applied)
	}

	@Test
	fun staleResultCannotBeApplied() {
		val note = AiNoteSnapshot("Nota", "Original")
		compose.setContent {
			CleanMindTheme {
				AiAssistantSheet(
					AiAssistantUiState(
						visible = true,
						configured = true,
						suggestion = AiSuggestion(note, "Propuesta"),
					),
					note.copy(content = "Modificado"),
					true,
					{},
					{},
					{},
					{},
					{},
					{},
					{},
				)
			}
		}
		compose.onNodeWithText("Añadir a la nota").performScrollTo().assertIsNotEnabled()
	}

	@Test
	fun settingsMaskApiKeyAndExposeSaveAndConnectionTest() {
		var saved = false
		compose.setContent {
			CleanMindTheme {
				androidx.compose.foundation.layout.Column(
					androidx.compose.ui.Modifier.verticalScroll(
						androidx.compose.foundation.rememberScrollState()
					)
				) {
					AiSettingsContent(
						AiSettingsUiState(
							configuration = AiConfiguration(apiKey = "private-key"),
							loading = false,
							dirty = true,
						),
						{},
						{ saved = true },
						{},
						{},
						{},
						{},
					)
				}
			}
		}
		compose
			.onNodeWithText("private-key")
			.assert(
				SemanticsMatcher.keyIsDefined(
					androidx.compose.ui.semantics.SemanticsProperties.Password
				)
			)
		compose.onNodeWithText("Probar conexión").assertExists()
		compose.onNodeWithText("Guardar configuración de IA").performScrollTo().performClick()
		assertTrue(saved)
	}
}
