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
}
