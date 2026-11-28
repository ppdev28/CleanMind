package com.example.cleannotes.chatgpt

import com.example.cleannotes.ai.*
import com.example.cleannotes.presentation.ui.ai.AiAssistantViewModel
import com.example.cleannotes.presentation.ui.chatgpt.ChatGptSettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatGptPresentationTest {
	private val dispatcher = StandardTestDispatcher()
	private val access = FakeAccess()

	@Before
	fun setup() {
		Dispatchers.setMain(dispatcher)
	}

	@After
	fun cleanup() {
		Dispatchers.resetMain()
	}

	@Test
	fun assistantUsesChatGptWhenSelectedWithoutReadingOrChargingTheApiConfiguration() =
		runTest(dispatcher) {
			val vm = assistant()
			vm.open()
			advanceUntilIdle()
			assertTrue(vm.state.value.configured)
			assertTrue(vm.state.value.usesChatGpt)
			val note = AiNoteSnapshot("Nota", "Texto original")
			vm.generate(note)
			advanceUntilIdle()
			assertEquals(1, access.requests.size)
			assertEquals(note, vm.state.value.suggestion?.note)
			assertEquals("Resultado del plan", vm.state.value.suggestion?.text)
		}

	@Test
	fun revokedSessionAndUsageLimitStayOnTheChatGptSource() =
		runTest(dispatcher) {
			val vm = assistant()
			vm.open()
			advanceUntilIdle()
			access.failure = AiException(AiError.CHATGPT_LIMIT)
			vm.generate(AiNoteSnapshot("Nota", "Texto"))
			advanceUntilIdle()
			assertEquals(AiError.CHATGPT_LIMIT.message, vm.state.value.error)
			assertNull(vm.state.value.suggestion)
			access.saved =
				access.saved.copy(accounts = access.saved.accounts.map { it.copy(tokens = null) })
			vm.open()
			advanceUntilIdle()
			assertFalse(vm.state.value.configured)
			assertTrue(vm.state.value.usesChatGpt)
			assertEquals(AiError.SESSION_EXPIRED.message, vm.state.value.error)
		}

	@Test
	fun settingsKeepCredentialsOutOfUiAndRetainModelChoicesAfterSelectionAndWelcome() =
		runTest(dispatcher) {
			val vm = ChatGptSettingsViewModel(access)
			advanceUntilIdle()
			assertEquals("person@example.com", vm.state.value.account?.email)
			assertTrue(vm.state.value.welcome)
			assertFalse(vm.state.value.toString().contains("access-secret"))
			vm.selectModel("model-two")
			advanceUntilIdle()
			assertEquals(2, vm.state.value.models.size)
			assertEquals("model-two", vm.state.value.account?.model)
			vm.welcomeAccepted()
			advanceUntilIdle()
			assertFalse(vm.state.value.welcome)
			assertEquals(2, vm.state.value.models.size)
		}

	@Test
	fun cancellingBrowserLoginClearsPendingUrlAndDoesNotReplaceTheActiveAccount() =
		runTest(dispatcher) {
			val vm = ChatGptSettingsViewModel(access)
			advanceUntilIdle()
			vm.signIn()
			runCurrent()
			assertTrue(vm.state.value.signingIn)
			assertNotNull(vm.state.value.authorizationUrl)
			vm.browserOpened()
			assertNull(vm.state.value.authorizationUrl)
			vm.cancel()
			advanceUntilIdle()
			assertFalse(vm.state.value.busy)
			assertEquals("oaiapp_saved", vm.state.value.activeClientId)
		}

	private fun assistant() =
		AiAssistantViewModel(
			object : AiSettingsRepository {
				override suspend fun load(): AiConfiguration =
					error("Must not read API configuration")

				override suspend fun save(configuration: AiConfiguration) = Unit

				override suspend fun clear() = Unit
			},
			object : AiGateway {
				override suspend fun complete(
					configuration: AiConfiguration,
					request: AiRequest,
				): String = error("Must not charge API")

				override suspend fun testConnection(configuration: AiConfiguration) = Unit
			},
			access,
		)

	private class FakeAccess : ChatGptAccess {
		var saved =
			ChatGptState(
				"host",
				listOf(
					ChatGptAccount(
						"oaiapp_saved",
						"subject",
						"Cuenta 1",
						"person@example.com",
						ChatGptTokens(
							"access-secret",
							"refresh-secret",
							"id-secret",
							Long.MAX_VALUE,
							setOf("resource.invoke", "chatgpt.tokens.use.direct"),
						),
						"model-one",
					)
				),
				"oaiapp_saved",
			)
		val requests = mutableListOf<AiRequest>()
		var failure: Exception? = null

		override suspend fun state() = saved

		override suspend fun signIn(clientId: String?, onAuthorization: (String) -> Unit) {
			onAuthorization("https://auth.openai.com/authorize")
			awaitCancellation()
		}

		override suspend fun select(clientId: String?) {
			saved = saved.copy(activeClientId = clientId)
		}

		override suspend fun models() =
			listOf(ChatGptModel("model-one", "Modelo uno"), ChatGptModel("model-two", "Modelo dos"))

		override suspend fun selectModel(model: String) {
			saved = saved.copy(accounts = saved.accounts.map { it.copy(model = model) })
		}

		override suspend fun acknowledgeWelcome() {
			saved = saved.copy(accounts = saved.accounts.map { it.copy(welcomed = true) })
		}

		override suspend fun signOut(clientId: String): Boolean {
			saved = saved.copy(accounts = saved.accounts.map { it.copy(tokens = null) })
			return true
		}

		override suspend fun complete(request: AiRequest): String {
			requests += request
			failure?.let { throw it }
			return "Resultado del plan"
		}
	}
}
