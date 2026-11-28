package com.example.cleannotes.data.chatgpt

import com.example.cleannotes.ai.*
import com.nimbusds.jose.util.Base64URL
import java.security.MessageDigest
import java.security.SecureRandom
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal data class ChatGptEndpoints(
	val issuer: String = "https://auth.openai.com",
	val authorize: String = "https://auth.openai.com/api/accounts/authorize",
	val token: String = "https://auth.openai.com/api/accounts/oauth/token",
	val discovery: String = "https://auth.openai.com/.well-known/openid-configuration",
	val apiBase: String = "https://api.openai.com/v1",
)

internal class ChatGptAuthorization(
	val redirectUri: String,
	val account: ChatGptAccount?,
	val state: String = randomValue(),
	val nonce: String = randomValue(),
	val verifier: String = randomValue(),
	registrationId: String? = null,
) {
	private val selectedClientId = account?.clientId ?: registrationId

	fun url(hostId: String, endpoints: ChatGptEndpoints): String =
		endpoints.authorize
			.toHttpUrl()
			.newBuilder()
			.addQueryParameter("client_id", selectedClientId ?: "dynamic_agent_client")
			.apply {
				if (selectedClientId == null) addQueryParameter("agent_name_hint", "Clean Mind")
			}
			.apply {
				if (account?.tokens?.permitsInference == false)
					addQueryParameter("prompt", "consent")
			}
			.addQueryParameter("ext_agent_host_id", hostId)
			.addQueryParameter("response_type", "code")
			.addQueryParameter("redirect_uri", redirectUri)
			.addQueryParameter(
				"scope",
				"openid profile email offline_access resource.invoke chatgpt.tokens.use.direct",
			)
			.addQueryParameter("resource", "https://api.openai.com/v1")
			.addQueryParameter("state", state)
			.addQueryParameter("nonce", nonce)
			.addQueryParameter("code_challenge_method", "S256")
			.addQueryParameter(
				"code_challenge",
				Base64URL.encode(
						MessageDigest.getInstance("SHA-256")
							.digest(verifier.toByteArray(Charsets.US_ASCII))
					)
					.toString(),
			)
			.build()
			.toString()

	fun callback(url: HttpUrl): Pair<String, String> {
		fun single(name: String): String? {
			val values = url.queryParameterValues(name)
			if (values.size > 1) throw AiException(AiError.SIGN_IN)
			return values.singleOrNull()
		}
		if (
			url.newBuilder().query(null).build().toString() != redirectUri ||
				single("state") != state
		)
			throw AiException(AiError.SIGN_IN)
		if (single("error") != null) throw AiException(AiError.CONSENT)
		val code = single("code")?.takeIf { it.isNotBlank() } ?: throw AiException(AiError.SIGN_IN)
		val clientId = single("client_id") ?: selectedClientId ?: throw AiException(AiError.SIGN_IN)
		if (
			clientId == "dynamic_agent_client" ||
				!clientId.matches(Regex("[A-Za-z0-9_-]{1,256}")) ||
				(selectedClientId != null && selectedClientId != clientId)
		)
			throw AiException(AiError.SIGN_IN)
		return code to clientId
	}

	override fun toString() = "ChatGptAuthorization([REDACTED])"

	companion object {
		private fun randomValue() =
			Base64URL.encode(ByteArray(32).also { SecureRandom().nextBytes(it) }).toString()
	}
}
