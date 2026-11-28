package com.example.cleannotes.chatgpt

import com.example.cleannotes.ai.*
import com.example.cleannotes.data.chatgpt.*
import com.example.cleannotes.di.AiModule
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.*
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatGptApiTest {
	private lateinit var server: MockWebServer
	private lateinit var client: OkHttpClient
	private lateinit var endpoints: ChatGptEndpoints
	private lateinit var api: ChatGptApi
	private val scopes = setOf("resource.invoke", "chatgpt.tokens.use.direct", "offline_access")

	@Before
	fun setup() {
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
		endpoints =
			ChatGptEndpoints(
				server.url("/").toString().trimEnd('/'),
				server.url("/authorize").toString(),
				server.url("/token").toString(),
				server.url("/discovery").toString(),
				server.url("/v1").toString(),
			)
		api = ChatGptApi(client, endpoints)
	}

	@After
	fun cleanup() {
		server.shutdown()
		client.connectionPool.evictAll()
		client.dispatcher.executorService.shutdown()
	}

	private fun tokens(expiry: Long = System.currentTimeMillis() + 3_600_000) =
		ChatGptTokens("access-old", "refresh-old", "id-old", expiry, scopes)

	private fun account(expiry: Long = System.currentTimeMillis() + 3_600_000) =
		ChatGptAccount(
			"oaiapp_test",
			"subject",
			"Cuenta 1",
			tokens = tokens(expiry),
			model = "model-test",
		)

	private fun tokenResponse(idToken: String? = null, scope: String = scopes.joinToString(" ")) =
		MockResponse()
			.setBody(
				buildJsonObject {
						put("access_token", "access-new")
						put("refresh_token", "refresh-new")
						put("token_type", "Bearer")
						put("expires_in", 3600)
						put("scope", scope)
						if (idToken != null) put("id_token", idToken)
					}
					.toString()
			)

	private fun modelsReply() =
		MockResponse()
			.setBody(
				"""{"models":[{"slug":"hidden","display_name":"Hidden","visibility":"hidden"},{"slug":"model-test","display_name":"Modelo disponible","visibility":"list"}]}"""
			)

	private fun completed() =
		MockResponse()
			.setHeader("Content-Type", "text/event-stream")
			.setBody(
				"data: {\"type\":\"response.output_text.delta\",\"delta\":\"Propuesta completa\"}\n\n" +
					"data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}\n\n"
			)

	@Test
	fun listsOnlyModelsAvailableForTheAccountAndSendsPlanCompatibleResponsesRequest() = runTest {
		server.enqueue(modelsReply())
		server.enqueue(completed())
		assertEquals(listOf(ChatGptModel("model-test", "Modelo disponible")), api.models(account()))
		val note =
			com.example.cleannotes.model.NoteDocument(
				text = "Texto",
				images = listOf("private-image"),
			)
		assertEquals(
			"Propuesta completa",
			api.complete(
				account(),
				AiRequest(AiNoteSnapshot("Nota", note.encode()), AiAction.SUMMARIZE),
			),
		)
		assertEquals("/v1/models", server.takeRequest().path)
		val sent = server.takeRequest()
		assertEquals("/v1/responses", sent.path)
		assertEquals("Bearer access-old", sent.getHeader("Authorization"))
		val payload = Json.parseToJsonElement(sent.body.readUtf8()).jsonObject
		assertFalse(payload["store"]!!.jsonPrimitive.boolean)
		assertTrue(payload["stream"]!!.jsonPrimitive.boolean)
		assertTrue(payload["input"] is JsonArray)
		assertFalse(payload.containsKey("max_output_tokens"))
		assertFalse(payload.containsKey("temperature"))
		assertFalse(payload.toString().contains("private-image"))
	}

	@Test
	fun fullDynamicRegistrationValidatesIdentityAndPreservesTheHostAcrossSignOut() = runTest {
		val store = MemoryStore(ChatGptState())
		val service = ChatGptAccountService(store, api, client)
		val key = RSAKeyGenerator(2048).keyID("key").generate()
		service.signIn(null) { authorizationUrl ->
			val url = authorizationUrl.toHttpUrl()
			val jwt =
				SignedJWT(
						JWSHeader.Builder(JWSAlgorithm.RS256).keyID("key").build(),
						JWTClaimsSet.Builder()
							.issuer(endpoints.issuer)
							.audience("oaiapp_new")
							.subject("verified-user")
							.issueTime(Date())
							.expirationTime(Date(System.currentTimeMillis() + 60_000))
							.claim("nonce", url.queryParameter("nonce"))
							.claim("email", "person@example.com")
							.claim("email_verified", true)
							.build(),
					)
					.apply { sign(RSASSASigner(key)) }
					.serialize()
			server.enqueue(tokenResponse(jwt))
			server.enqueue(
				MockResponse()
					.setBody(
						"""{"issuer":"${endpoints.issuer}","jwks_uri":"${server.url("/keys")}"}"""
					)
			)
			server.enqueue(MockResponse().setBody(JWKSet(key.toPublicJWK()).toString()))
			// Emulate the browser's loopback callback; no real account is used.
			Thread {
					val callback =
						url.queryParameter("redirect_uri") +
							"?state=${url.queryParameter("state")}&code=code&client_id=oaiapp_new"
					(URL(callback).openConnection() as HttpURLConnection)
						.apply {
							connectTimeout = 5000
							readTimeout = 5000
						}
						.inputStream
						.close()
				}
				.start()
		}
		assertEquals("verified-user", store.saved.activeAccount?.subject)
		assertEquals("person@example.com", store.saved.activeAccount?.email)
		assertNotNull(store.saved.activeAccount?.tokens)
		val sent = server.takeRequest()
		assertEquals("/token", sent.path)
		val form = sent.body.readUtf8()
		assertTrue(form.contains("client_id=oaiapp_new"))
		assertFalse(form.contains("dynamic_agent_client"))
		assertTrue(form.contains("code_verifier="))
		server.takeRequest()
		server.takeRequest()
		val host = store.saved.hostId
		server.enqueue(
			MockResponse()
				.setBody(
					"""{"issuer":"${endpoints.issuer}","revocation_endpoint":"${server.url("/revoke")}"}"""
				)
		)
		server.enqueue(MockResponse().setResponseCode(200))
		assertTrue(service.signOut("oaiapp_new"))
		assertEquals(host, store.saved.hostId)
		assertEquals("oaiapp_new", store.saved.activeAccount?.clientId)
		assertNull(store.saved.activeAccount?.tokens)
	}

	@Test
	fun concurrentRequestsRefreshOnceAndPersistTheRotatedTokensBeforeInference() = runTest {
		val original = account(System.currentTimeMillis() - 1000)
		val store = MemoryStore(ChatGptState("urn:uuid:host", listOf(original), original.clientId))
		val service = ChatGptAccountService(store, api, client)
		server.enqueue(tokenResponse())
		server.enqueue(completed())
		server.enqueue(completed())
		val request = AiRequest(AiNoteSnapshot("Nota", "Original"), AiAction.SUMMARIZE)
		val results = coroutineScope {
			listOf(async { service.complete(request) }, async { service.complete(request) })
				.awaitAll()
		}
		assertEquals(listOf("Propuesta completa", "Propuesta completa"), results)
		val requests = List(3) { server.takeRequest() }
		assertEquals(1, requests.count { it.path == "/token" })
		assertTrue(
			requests
				.filter { it.path == "/v1/responses" }
				.all { it.getHeader("Authorization") == "Bearer access-new" }
		)
		assertEquals("refresh-new", store.saved.activeAccount?.tokens?.refreshToken)
	}

	@Test
	fun invalidRefreshClearsSessionWithoutLosingRegistrationOrFallingBackToApi() = runTest {
		val original = account(0)
		val store = MemoryStore(ChatGptState("urn:uuid:host", listOf(original), original.clientId))
		val service = ChatGptAccountService(store, api, client)
		server.enqueue(MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\"}"))
		try {
			service.models()
			fail("Expected session error")
		} catch (e: AiException) {
			assertEquals(AiError.SESSION_EXPIRED, e.reason)
		}
		assertEquals(original.clientId, store.saved.activeClientId)
		assertNull(store.saved.activeAccount?.tokens)
		assertEquals(1, server.requestCount)
	}

	@Test
	fun identityOnlyRefreshPreservesTheAccountButCannotRunInference() = runTest {
		val original = account(0)
		val store = MemoryStore(ChatGptState("urn:uuid:host", listOf(original), original.clientId))
		val service = ChatGptAccountService(store, api, client)
		server.enqueue(tokenResponse(scope = "openid profile email offline_access"))
		try {
			service.models()
			fail("Expected consent error")
		} catch (e: AiException) {
			assertEquals(AiError.CONSENT, e.reason)
		}
		assertNotNull(store.saved.activeAccount?.tokens)
		assertFalse(store.saved.activeAccount!!.tokens!!.permitsInference)
		assertEquals(original.clientId, store.saved.activeClientId)
		assertEquals(1, server.requestCount)
	}

	@Test
	fun policyRestrictionPreservesTheSessionInsteadOfLoopingThroughOAuth() = runTest {
		val original = account()
		val store = MemoryStore(ChatGptState("urn:uuid:host", listOf(original), original.clientId))
		val service = ChatGptAccountService(store, api, client)
		server.enqueue(MockResponse().setResponseCode(403))
		try {
			service.models()
			fail("Expected policy restriction")
		} catch (e: AiException) {
			assertEquals(AiError.CHATGPT_UNAVAILABLE, e.reason)
		}
		assertEquals(original.tokens, store.saved.activeAccount?.tokens)
	}

	@Test
	fun logoutClearsLocalTokensEvenWhenRemoteRevocationIsUnavailable() = runTest {
		val original = account()
		val store = MemoryStore(ChatGptState("urn:uuid:host", listOf(original), original.clientId))
		val service = ChatGptAccountService(store, api, client)
		server.enqueue(MockResponse().setResponseCode(503))
		assertFalse(service.signOut(original.clientId))
		assertNull(store.saved.activeAccount?.tokens)
		assertEquals(original.subject, store.saved.activeAccount?.subject)
	}

	@Test
	fun planLimitDoesNotTriggerAnApiRetry() = runTest {
		server.enqueue(
			MockResponse()
				.setResponseCode(429)
				.setHeader("x-request-id", "req_test")
				.setBody(
					"""{"error":{"code":"subscription_sharing_usage_limit_exceeded","message":"secret-provider-error"}}"""
				)
		)
		try {
			api.complete(account(), AiRequest(AiNoteSnapshot("Nota", "Texto"), AiAction.SUMMARIZE))
			fail("Expected limit")
		} catch (e: AiException) {
			assertEquals(AiError.CHATGPT_LIMIT, e.reason)
			assertFalse(e.message!!.contains("secret-provider-error"))
			assertEquals("req_test", e.diagnostic?.requestId)
			assertEquals("subscription_sharing_usage_limit_exceeded", e.diagnostic?.code)
		}
		assertEquals(1, server.requestCount)
	}

	@Test
	fun failedCodeExchangeKeepsTheIssuedRegistrationForRetryWithoutReplacingTheActiveAccount() =
		runTest {
			val old = account()
			val store = MemoryStore(ChatGptState("urn:uuid:host", listOf(old), old.clientId))
			val service = ChatGptAccountService(store, api, client)
			server.enqueue(
				MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\"}")
			)
			try {
				service.signIn(null) { authorization ->
					val url = authorization.toHttpUrl()
					Thread {
							val callback =
								url.queryParameter("redirect_uri") +
									"?state=${url.queryParameter("state")}&code=code&client_id=oaiapp_pending"
							(URL(callback).openConnection() as HttpURLConnection)
								.apply {
									connectTimeout = 5000
									readTimeout = 5000
								}
								.inputStream
								.close()
						}
						.start()
				}
				fail("Expected failed code exchange")
			} catch (e: AiException) {
				assertEquals(AiError.SESSION_EXPIRED, e.reason)
			}
			assertEquals("oaiapp_pending", store.saved.pendingClientId)
			assertEquals(old, store.saved.activeAccount)
			try {
				service.signIn(null) { authorization ->
					assertEquals(
						"oaiapp_pending",
						authorization.toHttpUrl().queryParameter("client_id"),
					)
					throw AiException(AiError.SIGN_IN)
				}
			} catch (_: AiException) {}
		}

	private class MemoryStore(var saved: ChatGptState) : ChatGptStore {
		override suspend fun load() = saved

		override suspend fun save(state: ChatGptState) {
			saved = state
		}
	}
}
