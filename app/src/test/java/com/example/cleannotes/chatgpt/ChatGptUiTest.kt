package com.example.cleannotes.chatgpt

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.example.cleannotes.presentation.ui.chatgpt.*
import com.example.cleannotes.presentation.ui.theme.CleanMindTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ChatGptUiTest {
	@get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

	@Test
	fun offersOfficialChatGptSignInAndExplainsTheGoogleAccount() {
		var started = false
		compose.setContent {
			CleanMindTheme {
				Column(Modifier.verticalScroll(rememberScrollState())) {
					ChatGptSettingsContent(
						ChatGptSettingsUiState(loading = false),
						{ started = true },
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
		}
		compose.onNodeWithText("cuenta de Google", substring = true).assertExists()
		compose.onNodeWithText("Continuar con ChatGPT").performScrollTo().performClick()
		assertTrue(started)
	}

	@Test
	fun connectedAccountShowsPlanUsageAndExplicitApiSwitch() {
		var usage = false
		var apiSelected = false
		val account =
			ChatGptAccountUi(
				"oaiapp_test",
				"Cuenta 1",
				"person@example.com",
				true,
				true,
				"model",
				true,
			)
		compose.setContent {
			CleanMindTheme {
				Column(Modifier.verticalScroll(rememberScrollState())) {
					ChatGptSettingsContent(
						ChatGptSettingsUiState(
							accounts = listOf(account),
							activeClientId = account.clientId,
							loading = false,
						),
						{},
						{ apiSelected = it == null },
						{},
						{},
						{},
						{},
						{},
						{ usage = true },
					)
				}
			}
		}
		compose.onNodeWithText("Usando tu plan de ChatGPT").assertExists()
		compose.onNodeWithText("Gestionar uso en ChatGPT").performScrollTo().performClick()
		assertTrue(usage)
		compose.onNodeWithText("Usar configuración de API").performScrollTo().performClick()
		assertTrue(apiSelected)
	}
}
