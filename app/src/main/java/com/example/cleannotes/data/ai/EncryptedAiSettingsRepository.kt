package com.example.cleannotes.data.ai

import android.content.Context
import com.example.cleannotes.ai.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class EncryptedAiSettingsRepository @Inject constructor(@ApplicationContext context: Context) : AiSettingsRepository {
	private val file = EncryptedPrivateFile(context, "ai_configuration", "cleanmind_ai_configuration_v1")
	override suspend fun load(): AiConfiguration = try {
		file.read()?.let { Json.decodeFromString<AiConfiguration>(it) } ?: AiConfiguration()
	} catch (e: kotlinx.coroutines.CancellationException) { throw e }
	catch (_: Exception) { throw AiException(AiError.STORAGE) }

	override suspend fun save(configuration: AiConfiguration) {
		val normalized = configuration.normalized()
		normalized.validate()
		file.write(Json.encodeToString(normalized))
	}

	override suspend fun clear() = file.delete()
}
