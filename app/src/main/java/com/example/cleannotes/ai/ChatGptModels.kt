package com.example.cleannotes.ai

import kotlinx.serialization.Serializable

@Serializable
data class ChatGptTokens(
	val accessToken: String,
	val refreshToken: String,
	val idToken: String,
	val expiresAt: Long,
	val scopes: Set<String>,
) {
	override fun toString() = "ChatGptTokens([REDACTED])"

	val permitsInference
		get() = scopes.containsAll(setOf("resource.invoke", "chatgpt.tokens.use.direct"))
}

@Serializable
data class ChatGptAccount(
	val clientId: String,
	val subject: String,
	val label: String,
	val email: String = "",
	val tokens: ChatGptTokens? = null,
	val model: String = "",
	val welcomed: Boolean = false,
)

@Serializable
data class ChatGptState(
	val hostId: String = "",
	val accounts: List<ChatGptAccount> = emptyList(),
	val activeClientId: String? = null,
	val pendingClientId: String? = null,
) {
	val activeAccount
		get() = accounts.firstOrNull { it.clientId == activeClientId }
}

data class ChatGptModel(val id: String, val name: String)

data class ChatGptIdentity(val subject: String, val email: String)

interface ChatGptStore {
	suspend fun load(): ChatGptState

	suspend fun save(state: ChatGptState)
}

interface ChatGptAccess {
	suspend fun state(): ChatGptState

	suspend fun signIn(clientId: String?, onAuthorization: (String) -> Unit)

	suspend fun select(clientId: String?)

	suspend fun models(): List<ChatGptModel>

	suspend fun selectModel(model: String)

	suspend fun acknowledgeWelcome()

	/**
	 * Returns false when remote revocation could not be confirmed; local tokens are always cleared.
	 */
	suspend fun signOut(clientId: String): Boolean

	suspend fun complete(request: AiRequest): String
}
