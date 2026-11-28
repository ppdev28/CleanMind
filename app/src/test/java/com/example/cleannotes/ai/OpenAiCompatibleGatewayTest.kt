package com.example.cleannotes.ai

import com.example.cleannotes.data.ai.OpenAiCompatibleGateway
import com.example.cleannotes.di.AiModule
import com.example.cleannotes.model.NoteDocument
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OpenAiCompatibleGatewayTest {
	private lateinit var server: MockWebServer
	private lateinit var client: OkHttpClient
	private lateinit var gateway: OpenAiCompatibleGateway
	private lateinit var config: AiConfiguration
	private val request = AiRequest(AiNoteSnapshot("Título", "Contenido"), AiAction.SUMMARIZE)

	@Before
	fun setUp() {
		val certificate =
			HeldCertificate.Builder()
				.commonName("localhost")
				.addSubjectAlternativeName("localhost")
				.build()
		val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
		val clientTls =
			HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
		server =
			MockWebServer().apply {
				useHttps(serverTls.sslSocketFactory(), false)
				start()
			}
		client =
			AiModule.httpClient()
				.newBuilder()
				.sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
				.build()
		gateway = OpenAiCompatibleGateway(client)
		config = AiConfiguration(server.url("/v1").toString(), "test-model", "test-secret")
	}

	@After
	fun tearDown() {
		server.shutdown()
		client.connectionPool.evictAll()
		client.dispatcher.executorService.shutdown()
	}

	private fun reply(text: String = "Respuesta") =
		MockResponse()
			.setHeader("Content-Type", "application/json")
			.setBody(
				buildJsonObject {
						putJsonArray("choices") {
							add(
								buildJsonObject {
									put("finish_reason", "stop")
									putJsonObject("message") { put("content", text) }
								}
							)
						}
					}
					.toString()
			)

	@Test
	fun sendsRealHttpsRequestAndParsesTextWithoutSendingAttachments() = runTest {
		server.enqueue(reply("  Resumen de la nota  "))
		val doc = NoteDocument(text = "Contenido visible", images = listOf("secret-image-uri"))
		assertEquals(
			"Resumen de la nota",
			gateway.complete(config, request.copy(note = AiNoteSnapshot("Título", doc.encode()))),
		)
		val sent = server.takeRequest(2, TimeUnit.SECONDS)!!
		assertEquals("/v1/chat/completions", sent.path)
		assertEquals("Bearer test-secret", sent.getHeader("Authorization"))
		val payload = Json.parseToJsonElement(sent.body.readUtf8()).jsonObject
		assertEquals("test-model", payload["model"]!!.jsonPrimitive.content)
		assertFalse(payload["stream"]!!.jsonPrimitive.boolean)
		assertEquals(4096, payload["max_completion_tokens"]!!.jsonPrimitive.int)
		val messages = payload["messages"]!!.jsonArray
		assertEquals("system", messages.first().jsonObject["role"]!!.jsonPrimitive.content)
		val userText = messages.last().jsonObject["content"]!!.jsonPrimitive.content
		assertTrue(userText.contains("Contenido visible"))
		assertFalse(userText.contains("secret-image-uri"))
	}

	@Test
	fun mapsHttpErrorsWithoutExposingProviderMessagesOrSecrets() = runTest {
		mapOf(
				401 to AiError.AUTHENTICATION,
				403 to AiError.AUTHENTICATION,
				404 to AiError.MODEL,
				429 to AiError.RATE_LIMIT,
				503 to AiError.SERVER,
				400 to AiError.RESPONSE,
			)
			.forEach { (code, reason) ->
				server.enqueue(
					MockResponse()
						.setResponseCode(code)
						.setBody("test-secret private-provider-error")
				)
				val error =
					try {
						gateway.complete(config, request)
						error("Expected error")
					} catch (e: AiException) {
						e
					}
				assertEquals(reason, error.reason)
				assertFalse(error.message!!.contains("test-secret"))
				assertFalse(error.message!!.contains("private-provider-error"))
			}
	}

	@Test
	fun rejectsMalformedEmptyRefusedAndTruncatedResults() = runTest {
		listOf(
				"not JSON",
				"{}",
				"{\"choices\":[]}",
				"{\"choices\":[{\"message\":{\"content\":\" \"}}]}",
				"{\"choices\":[{\"message\":{\"content\":\"text\",\"refusal\":\"refused\"}}]}",
				"{\"choices\":[{\"message\":{\"content\":\"text\"},\"finish_reason\":\"content_filter\"}]}",
			)
			.forEach { body ->
				server.enqueue(MockResponse().setBody(body))
				try {
					gateway.complete(config, request)
					fail("Expected invalid response")
				} catch (e: AiException) {
					assertEquals(AiError.RESPONSE, e.reason)
				}
			}
		server.enqueue(
			MockResponse()
				.setBody(
					"{\"choices\":[{\"message\":{\"content\":\"partial\"},\"finish_reason\":\"length\"}]}"
				)
		)
		try {
			gateway.complete(config, request)
			fail("Expected truncated response")
		} catch (e: AiException) {
			assertEquals(AiError.TRUNCATED, e.reason)
		}
	}

	@Test
	fun rejectsOversizedResponses() = runTest {
		server.enqueue(reply("x".repeat(260_000)))
		try {
			gateway.complete(config, request)
			fail("Expected response size limit")
		} catch (e: AiException) {
			assertEquals(AiError.RESPONSE, e.reason)
		}
	}

	@Test
	fun refusesRedirectsInsteadOfForwardingCredentialsOrNoteContent() = runTest {
		server.enqueue(
			MockResponse().setResponseCode(307).setHeader("Location", server.url("/other"))
		)
		try {
			gateway.complete(config, request)
			fail("Expected redirect rejection")
		} catch (e: AiException) {
			assertEquals(AiError.RESPONSE, e.reason)
		}
		assertEquals(1, server.requestCount)
	}

	@Test
	fun connectionTestDoesNotIncludeNoteData() = runTest {
		server.enqueue(reply("OK"))
		gateway.testConnection(config)
		val sent = server.takeRequest(2, TimeUnit.SECONDS)!!.body.readUtf8()
		assertTrue(sent.contains("Prueba de conexión"))
		assertFalse(sent.contains("Título"))
		assertFalse(sent.contains("Contenido"))
	}

	@Test
	fun cancellingRequestCancelsUnderlyingCall() = runTest {
		server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
		val job = launch(Dispatchers.Default) { gateway.complete(config, request) }
		withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(5, TimeUnit.SECONDS)) }
		job.cancelAndJoin()
		assertTrue(job.isCancelled)
	}

	@Test
	fun timesOutWithoutAutomaticRetries() = runTest {
		server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
		val shortGateway =
			OpenAiCompatibleGateway(
				client.newBuilder().callTimeout(500, TimeUnit.MILLISECONDS).build()
			)
		try {
			shortGateway.complete(config, request)
			fail("Expected timeout")
		} catch (e: AiException) {
			assertEquals(AiError.TIMEOUT, e.reason)
		}
		assertEquals(1, server.requestCount)
	}
}
