package com.example.cleannotes.data.chatgpt

import com.example.cleannotes.ai.*
import java.util.UUID
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Singleton
class ChatGptAccountService
internal constructor(
	private val store: ChatGptStore,
	private val api: ChatGptApi,
	private val client: OkHttpClient,
) : ChatGptAccess {
	private val mutex = Mutex()

	override suspend fun state(): ChatGptState =
		mutex.withLock {
			val state = store.load()
			if (state.hostId.isNotBlank()) state
			else state.copy(hostId = "urn:uuid:${UUID.randomUUID()}").also { store.save(it) }
		}

	override suspend fun signIn(clientId: String?, onAuthorization: (String) -> Unit) {
		val before = state()
		val account =
			clientId?.let { id ->
				before.accounts.firstOrNull { it.clientId == id }
					?: throw AiException(AiError.SIGN_IN)
			}
		val result =
			withContext(Dispatchers.IO) { LoopbackCallback() }
				.use { listener ->
					val authorization =
						ChatGptAuthorization(
							listener.redirectUri,
							account,
							registrationId = if (account == null) before.pendingClientId else null,
						)
					onAuthorization(authorization.url(before.hostId, api.endpoints))
					val (code, issuedId) =
						authorization.callback(listener.await(authorization.state))
					if (account == null)
						mutex.withLock { store.save(store.load().copy(pendingClientId = issuedId)) }
					issuedId to api.exchange(authorization, code, issuedId)
				}
		mutex.withLock {
			val current = store.load()
			val id = result.first
			val (identity, tokens) = result.second
			val existing = current.accounts.firstOrNull { it.clientId == id }
			if (existing != null && existing.subject != identity.subject)
				throw AiException(AiError.SIGN_IN)
			val signedIn =
				(existing
						?: ChatGptAccount(
							id,
							identity.subject,
							"Cuenta ${current.accounts.size + 1}",
						))
					.copy(email = identity.email, tokens = tokens)
			cancelRequests(current.activeClientId)
			store.save(
				current.copy(
					accounts = current.accounts.filterNot { it.clientId == id } + signedIn,
					activeClientId = id,
					pendingClientId = current.pendingClientId?.takeUnless { it == id },
				)
			)
		}
	}

	override suspend fun select(clientId: String?) =
		mutex.withLock {
			val current = store.load()
			if (clientId != null && current.accounts.none { it.clientId == clientId })
				throw AiException(AiError.SIGN_IN)
			cancelRequests(current.activeClientId)
			store.save(current.copy(activeClientId = clientId))
		}

	override suspend fun models(): List<ChatGptModel> =
		mutex.withLock {
			val account = authorizedAccount()
			val models =
				try {
					api.models(account)
				} catch (error: AiException) {
					if (error.reason == AiError.SESSION_EXPIRED)
						replace(account.copy(tokens = null))
					throw error
				}
			if (models.none { it.id == account.model })
				replace(account.copy(model = models.first().id))
			models
		}

	override suspend fun selectModel(model: String) =
		mutex.withLock {
			val account = authorizedAccount()
			if (model.isBlank()) throw AiException(AiError.NO_MODELS)
			replace(account.copy(model = model))
		}

	override suspend fun acknowledgeWelcome() =
		mutex.withLock {
			val current = store.load()
			val account = current.activeAccount ?: return@withLock
			replace(account.copy(welcomed = true))
		}

	override suspend fun signOut(clientId: String): Boolean {
		cancelRequests(clientId)
		return mutex.withLock {
			val current = store.load()
			val account =
				current.accounts.firstOrNull { it.clientId == clientId } ?: return@withLock true
			var revoked = false
			try {
				api.revoke(account)
				revoked = true
			} catch (e: CancellationException) {
				throw e
			} catch (_: Exception) {
				/* Local sign-out still completes. */
			} finally {
				withContext(NonCancellable) { replace(account.copy(tokens = null)) }
			}
			revoked
		}
	}

	override suspend fun complete(request: AiRequest): String {
		val account = mutex.withLock { authorizedAccount() }
		return try {
			api.complete(account, request)
		} catch (error: AiException) {
			if (error.reason == AiError.SESSION_EXPIRED)
				mutex.withLock {
					val current =
						store.load().accounts.firstOrNull { it.clientId == account.clientId }
					if (current?.tokens?.accessToken == account.tokens?.accessToken)
						replace(account.copy(tokens = null))
				}
			throw error
		}
	}

	/** The mutex serializes rotating refresh tokens and account mutations. */
	private suspend fun authorizedAccount(): ChatGptAccount {
		val account = store.load().activeAccount ?: throw AiException(AiError.SESSION_EXPIRED)
		val tokens = account.tokens ?: throw AiException(AiError.SESSION_EXPIRED)
		if (!tokens.permitsInference) throw AiException(AiError.CONSENT)
		if (tokens.expiresAt > System.currentTimeMillis() + 60_000) return account
		if (tokens.refreshToken.isBlank()) {
			replace(account.copy(tokens = null))
			throw AiException(AiError.SESSION_EXPIRED)
		}
		try {
			val renewed = account.copy(tokens = api.refresh(account))
			replace(renewed)
			if (renewed.tokens?.permitsInference != true) throw AiException(AiError.CONSENT)
			return renewed
		} catch (error: AiException) {
			if (error.reason == AiError.SESSION_EXPIRED) replace(account.copy(tokens = null))
			throw error
		}
	}

	private suspend fun replace(account: ChatGptAccount) {
		val current = store.load()
		store.save(
			current.copy(
				accounts =
					current.accounts.map { if (it.clientId == account.clientId) account else it }
			)
		)
	}

	private fun cancelRequests(clientId: String?) {
		if (clientId == null) return
		(client.dispatcher.queuedCalls() + client.dispatcher.runningCalls())
			.filter { it.request().tag(ChatGptRequestTag::class.java)?.clientId == clientId }
			.forEach { it.cancel() }
	}
}
