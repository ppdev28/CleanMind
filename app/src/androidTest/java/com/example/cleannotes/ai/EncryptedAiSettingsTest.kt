package com.example.cleannotes.ai

import androidx.test.platform.app.InstrumentationRegistry
import com.example.cleannotes.data.ai.EncryptedAiSettingsRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Runs against a real Android Keystore on an emulator/device. */
class EncryptedAiSettingsTest {
	@Test
	fun persistsEncryptedConfigurationAcrossInstancesAndDeletesIt() = runBlocking {
		val target = InstrumentationRegistry.getInstrumentation().targetContext
		val directory =
			File(target.cacheDir, "ai-test-${java.util.UUID.randomUUID()}").apply { mkdirs() }
		val context =
			object : android.content.ContextWrapper(target) {
				override fun getNoBackupFilesDir(): File = directory
			}
		val repository = EncryptedAiSettingsRepository(context)
		repository.clear()
		try {
			val configuration = AiConfiguration(apiKey = "instrumentation-test-secret")
			repository.save(configuration)
			val bytes = File(context.noBackupFilesDir, "ai_configuration").readBytes()
			assertFalse(bytes.decodeToString().contains(configuration.apiKey))
			assertFalse(bytes.decodeToString().contains(configuration.baseUrl))
			assertEquals(configuration, EncryptedAiSettingsRepository(context).load())
			repository.save(configuration)
			val file = File(context.noBackupFilesDir, "ai_configuration")
			assertFalse(bytes.contentEquals(file.readBytes()))
			file.writeBytes(byteArrayOf(1, 2, 3))
			try {
				repository.load()
				fail("Expected corrupt configuration to be reported")
			} catch (e: AiException) {
				assertEquals(AiError.STORAGE, e.reason)
			}
			repository.clear()
			assertEquals(AiConfiguration(), repository.load())
		} finally {
			repository.clear()
			directory.deleteRecursively()
		}
	}
	@Test
	fun persistsChatGptAccountsAndRotatingTokensInAnEncryptedIsolatedFile() = runBlocking {
		val target = InstrumentationRegistry.getInstrumentation().targetContext
		val directory = File(target.cacheDir, "chatgpt-test-${java.util.UUID.randomUUID()}").apply { mkdirs() }
		val context = object : android.content.ContextWrapper(target) {
			override fun getNoBackupFilesDir(): File = directory
		}
		try {
			val store = com.example.cleannotes.data.chatgpt.EncryptedChatGptStore(context)
			val tokens = ChatGptTokens("test-access-secret", "test-refresh-secret", "test-id-secret", 12345,
				setOf("resource.invoke", "chatgpt.tokens.use.direct"))
			val account = ChatGptAccount("oaiapp_test", "subject", "Cuenta 1", "person@example.com", tokens, "model")
			val state = ChatGptState("urn:uuid:test", listOf(account), account.clientId)
			store.save(state)
			val file = File(directory, "chatgpt_accounts")
			val bytes = file.readBytes()
			assertFalse(bytes.decodeToString().contains("test-refresh-secret"))
			assertFalse(bytes.decodeToString().contains("person@example.com"))
			assertEquals(state, com.example.cleannotes.data.chatgpt.EncryptedChatGptStore(context).load())
			val rotated = state.copy(accounts = listOf(account.copy(tokens = tokens.copy(refreshToken = "rotated-secret"))))
			store.save(rotated)
			assertFalse(bytes.contentEquals(file.readBytes()))
			assertEquals("rotated-secret", store.load().activeAccount?.tokens?.refreshToken)
			store.save(rotated.copy(accounts = listOf(account.copy(tokens = null))))
			assertNull(store.load().activeAccount?.tokens)
		} finally { directory.deleteRecursively() }
	}

}
