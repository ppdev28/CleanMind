package com.example.cleannotes.data.chatgpt

import android.content.Context
import com.example.cleannotes.ai.*
import com.example.cleannotes.data.ai.EncryptedPrivateFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class EncryptedChatGptStore @Inject constructor(@ApplicationContext context: Context) :
	ChatGptStore {
	private val file =
		EncryptedPrivateFile(context, "chatgpt_accounts", "cleanmind_chatgpt_accounts_v1")

	override suspend fun load(): ChatGptState =
		try {
			file.read()?.let { Json.decodeFromString<ChatGptState>(it) } ?: ChatGptState()
		} catch (e: CancellationException) {
			throw e
		} catch (_: Exception) {
			throw AiException(AiError.STORAGE)
		}

	override suspend fun save(state: ChatGptState) = file.write(Json.encodeToString(state))
}
