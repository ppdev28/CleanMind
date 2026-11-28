package com.example.cleannotes.chatgpt

import com.example.cleannotes.data.chatgpt.LoopbackCallback
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LoopbackCallbackTest {
	@Test
	fun listensOnLoopbackRejectsWrongStateAndAcceptsTheMatchingCallback() = runBlocking {
		LoopbackCallback().use { listener ->
			val pending = async { listener.await("expected-state") }
			yield()
			fun request(url: String): Pair<Int, String> {
				val connection = URL(url).openConnection() as HttpURLConnection
				connection.connectTimeout = 2000
				connection.readTimeout = 2000
				return try {
					val code = connection.responseCode
					code to
						(if (code == 200) connection.inputStream.bufferedReader().readText()
						else "")
				} finally {
					connection.disconnect()
				}
			}
			val wrong =
				withContext(Dispatchers.IO) {
					request(listener.redirectUri + "?state=wrong&code=secret-code")
				}
			assertEquals(400, wrong.first)
			val correct =
				withContext(Dispatchers.IO) {
					request(
						listener.redirectUri +
							"?state=expected-state&code=secret-code&client_id=oaiapp_test"
					)
				}
			assertEquals(200, correct.first)
			assertFalse(correct.second.contains("secret-code"))
			assertTrue(correct.second.contains("cleanmind://chatgpt/return"))
			assertEquals(
				"secret-code",
				withTimeout(2000) { pending.await() }.queryParameter("code"),
			)
		}
	}

	@Test
	fun cancellationClosesTheListener() = runBlocking {
		val listener = LoopbackCallback()
		val job = launch { listener.await("state") }
		yield()
		job.cancelAndJoin()
		val failed =
			withContext(Dispatchers.IO) {
				runCatching {
						(URL(listener.redirectUri).openConnection() as HttpURLConnection)
							.apply { connectTimeout = 500 }
							.inputStream
							.close()
					}
					.isFailure
			}
		assertTrue(failed)
	}
}
