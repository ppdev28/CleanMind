package com.example.cleannotes.chatgpt

import com.example.cleannotes.ai.*
import com.example.cleannotes.data.chatgpt.*
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.Date
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class ChatGptAuthorizationTest {
	private val callback = "http://127.0.0.1:43210/auth/callback"
	private val endpoints = ChatGptEndpoints()

	@Test
	fun firstSignInUsesPkceHostIdentityAndOfficialPlanScopes() {
		val attempt =
			ChatGptAuthorization(
				callback,
				null,
				"state",
				"nonce",
				"dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
			)
		val url = attempt.url("urn:uuid:host", endpoints).toHttpUrl()
		assertEquals(
			"https://auth.openai.com/api/accounts/authorize",
			url.newBuilder().query(null).build().toString(),
		)
		assertEquals("dynamic_agent_client", url.queryParameter("client_id"))
		assertEquals("Clean Mind", url.queryParameter("agent_name_hint"))
		assertEquals("urn:uuid:host", url.queryParameter("ext_agent_host_id"))
		assertEquals(callback, url.queryParameter("redirect_uri"))
		assertEquals("S256", url.queryParameter("code_challenge_method"))
		assertEquals(
			"E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
			url.queryParameter("code_challenge"),
		)
		assertTrue(url.queryParameter("scope")!!.contains("chatgpt.tokens.use.direct"))
		assertFalse(url.toString().contains("client_secret"))
	}

	@Test
	fun reauthorizationReusesIssuedClientAndOmitsNewRegistrationName() {
		val account = ChatGptAccount("oaiapp_saved", "subject", "Cuenta 1")
		val attempt = ChatGptAuthorization(callback, account)
		val url = attempt.url("urn:uuid:host", endpoints).toHttpUrl()
		assertEquals(account.clientId, url.queryParameter("client_id"))
		assertNull(url.queryParameter("agent_name_hint"))
		val returned = "$callback?state=${attempt.state}&code=code".toHttpUrl()
		assertEquals("code" to account.clientId, attempt.callback(returned))
	}

	@Test
	fun retryReusesAPendingIssuedClientAndExplicitPlanConsentUsesTheSavedRegistration() {
		val pending = ChatGptAuthorization(callback, null, registrationId = "oaiapp_pending")
		val url = pending.url("urn:uuid:host", endpoints).toHttpUrl()
		assertEquals("oaiapp_pending", url.queryParameter("client_id"))
		assertNull(url.queryParameter("agent_name_hint"))
		val identityOnly =
			ChatGptAccount(
				"oaiapp_saved",
				"subject",
				"Cuenta",
				tokens = ChatGptTokens("", "", "id", 0, setOf("openid")),
			)
		assertEquals(
			"consent",
			ChatGptAuthorization(callback, identityOnly)
				.url("host", endpoints)
				.toHttpUrl()
				.queryParameter("prompt"),
		)
	}

	@Test
	fun callbackRejectsMissingIssuedClientWrongStateDuplicatesAndClientReplacement() {
		val attempt = ChatGptAuthorization(callback, null, "expected")
		val valid = "$callback?state=expected&code=code&client_id=oaiapp_new"
		assertEquals("code" to "oaiapp_new", attempt.callback(valid.toHttpUrl()))
		listOf(
				valid.replace("state=expected", "state=other"),
				valid + "&state=expected",
				valid.replace("&client_id=oaiapp_new", ""),
				valid.replace("oaiapp_new", "dynamic_agent_client"),
				valid.replace("/auth/callback", "/callback"),
				valid + "&code=another",
			)
			.forEach { invalid ->
				assertThrows(AiException::class.java) { attempt.callback(invalid.toHttpUrl()) }
			}
		val returning =
			ChatGptAuthorization(
				callback,
				ChatGptAccount("oaiapp_saved", "subject", "Cuenta"),
				"expected",
			)
		assertThrows(AiException::class.java) { returning.callback(valid.toHttpUrl()) }
	}

	@Test
	fun deniedConsentIsOnlyHandledAfterStateValidation() {
		val attempt = ChatGptAuthorization(callback, null, "expected")
		assertEquals(
			AiError.CONSENT,
			assertThrows(AiException::class.java) {
					attempt.callback("$callback?state=expected&error=access_denied".toHttpUrl())
				}
				.reason,
		)
		assertEquals(
			AiError.SIGN_IN,
			assertThrows(AiException::class.java) {
					attempt.callback("$callback?state=wrong&error=access_denied".toHttpUrl())
				}
				.reason,
		)
	}

	@Test
	fun verifiesSignatureIdentityAudienceNonceAndLifetime() {
		val key = RSAKeyGenerator(2048).keyID("signing-key").generate()
		val now = System.currentTimeMillis()
		fun jwt(
			issuer: String = endpoints.issuer,
			audience: String = "oaiapp_app",
			nonce: String = "nonce",
			exp: Long = now + 60_000,
		): String {
			val claims =
				JWTClaimsSet.Builder()
					.issuer(issuer)
					.audience(audience)
					.subject("verified-subject")
					.issueTime(Date(now))
					.expirationTime(Date(exp))
					.claim("nonce", nonce)
					.claim("email", "person@example.com")
					.claim("email_verified", true)
					.build()
			return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
				.apply { sign(RSASSASigner(key)) }
				.serialize()
		}
		val keys = JWKSet(key.toPublicJWK()).toString()
		val identity =
			ChatGptIdentityVerifier.verify(
				jwt(),
				keys,
				endpoints.issuer,
				"oaiapp_app",
				"nonce",
				now,
			)
		assertEquals(ChatGptIdentity("verified-subject", "person@example.com"), identity)
		listOf(
				jwt(issuer = "https://attacker.example"),
				jwt(audience = "different-client"),
				jwt(nonce = "other"),
				jwt(exp = now - 60_000),
			)
			.forEach {
				assertThrows(AiException::class.java) {
					ChatGptIdentityVerifier.verify(
						it,
						keys,
						endpoints.issuer,
						"oaiapp_app",
						"nonce",
						now,
					)
				}
			}
		val otherKey = RSAKeyGenerator(2048).keyID(key.keyID).generate()
		assertThrows(AiException::class.java) {
			ChatGptIdentityVerifier.verify(
				jwt(),
				JWKSet(otherKey.toPublicJWK()).toString(),
				endpoints.issuer,
				"oaiapp_app",
				"nonce",
				now,
			)
		}
	}

	@Test
	fun tokensAndPendingAuthorizationDoNotExposeSecretsInDiagnosticStrings() {
		assertFalse(
			ChatGptTokens("access-secret", "refresh-secret", "id-secret", 0, emptySet())
				.toString()
				.contains("secret")
		)
		assertFalse(
			ChatGptAuthorization(callback, null, verifier = "private-verifier")
				.toString()
				.contains("private-verifier")
		)
	}
}
