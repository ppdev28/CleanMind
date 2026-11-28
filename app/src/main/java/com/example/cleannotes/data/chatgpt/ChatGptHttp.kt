package com.example.cleannotes.data.chatgpt

import com.example.cleannotes.ai.*
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*

internal data class ChatGptRequestTag(val clientId: String)

internal suspend fun <T> OkHttpClient.chatGptRequest(request: Request, read: (Response) -> T): T =
	suspendCancellableCoroutine { continuation ->
		val call = newCall(request)
		continuation.invokeOnCancellation { call.cancel() }
		call.enqueue(
			object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					if (continuation.isActive) continuation.resumeWithException(safeChatGptError(e))
				}

				override fun onResponse(call: Call, response: Response) {
					try {
						val result = response.use { read(it) }
						if (continuation.isActive) continuation.resume(result)
					} catch (e: Exception) {
						if (continuation.isActive)
							continuation.resumeWithException(safeChatGptError(e))
					}
				}
			}
		)
	}

internal fun safeChatGptError(error: Exception): AiException =
	when (error) {
		is AiException -> error
		is InterruptedIOException -> AiException(AiError.TIMEOUT)
		is IOException -> AiException(AiError.NETWORK)
		else -> AiException(AiError.RESPONSE)
	}

internal fun Response.boundedBody(): String {
	val source = body?.source() ?: throw AiException(AiError.RESPONSE)
	if (source.request(256_001)) throw AiException(AiError.RESPONSE)
	return source.readUtf8()
}

internal fun Response.checkChatGptStatus() {
	if (isSuccessful) return
	val body = runCatching { Json.parseToJsonElement(boundedBody()).jsonObject }.getOrNull()
	fun safeCode(value: String?): String? =
		value?.takeIf { it.matches(Regex("[A-Za-z0-9_.:-]{1,128}")) }
	val errorCode =
		safeCode((body?.get("error") as? JsonObject)?.get("code")?.jsonPrimitive?.contentOrNull)
	val requestId = safeCode(header("x-request-id") ?: header("openai-request-id"))
	val shape =
		when {
			body == null -> "unparseable"
			"error" in body -> "error"
			"detail" in body -> "detail"
			else -> "other"
		}
	throw AiException(
		when (code) {
			401 -> AiError.SIGN_IN
			403 -> AiError.CHATGPT_UNAVAILABLE
			429 -> AiError.CHATGPT_LIMIT
			in 500..599 -> AiError.SERVER
			else -> AiError.RESPONSE
		},
		AiDiagnostic(code, errorCode, requestId, shape),
	)
}
