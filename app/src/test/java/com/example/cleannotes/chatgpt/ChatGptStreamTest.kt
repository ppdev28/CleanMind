package com.example.cleannotes.chatgpt

import com.example.cleannotes.ai.*
import com.example.cleannotes.data.chatgpt.ChatGptResponseStream
import kotlinx.serialization.json.*
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class ChatGptStreamTest {
	private fun event(type: String, text: String? = null): String =
		"data: " +
			buildJsonObject {
					put("type", type)
					if (text != null) put("delta", text)
					if (type == "response.completed")
						putJsonObject("response") { put("status", "completed") }
				}
				.toString() +
			"\n\n"

	@Test
	fun accumulatesDeltasAndRequiresCompletedInference() {
		val stream =
			event("response.created") +
				event("response.output_text.delta", "Hola ") +
				event("response.output_text.delta", "mundo") +
				event("response.completed")
		assertEquals("Hola mundo", ChatGptResponseStream.read(Buffer().writeUtf8(stream)))
	}

	@Test
	fun neverOffersPartialFailedRefusedOrEmptyOutput() {
		val partial = event("response.output_text.delta", "Parcial")
		listOf(
				partial,
				partial + event("response.failed"),
				partial + event("response.incomplete"),
				event("error"),
				event("response.completed"),
				"data: [DONE]\n\n",
			)
			.forEach {
				assertThrows(AiException::class.java) {
					ChatGptResponseStream.read(Buffer().writeUtf8(it))
				}
			}
	}

	@Test
	fun boundsOutputBeforeItCanBecomeAnAcceptedSuggestion() {
		assertEquals(
			AiError.TRUNCATED,
			assertThrows(AiException::class.java) {
					ChatGptResponseStream.read(
						Buffer().writeUtf8(event("response.output_text.delta", "x".repeat(32_001)))
					)
				}
				.reason,
		)
	}
}
