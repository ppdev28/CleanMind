package com.example.cleannotes.data.chatgpt

import com.example.cleannotes.ai.*
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Ephemeral IPv4 loopback listener, never exposed to the LAN. No tokens pass through the browser.
 */
internal class LoopbackCallback : Closeable {
	private val socket =
		ServerSocket(0, 4, InetAddress.getByName("127.0.0.1")).apply { soTimeout = 300_000 }
	val redirectUri = "http://127.0.0.1:${socket.localPort}/auth/callback"

	suspend fun await(state: String): HttpUrl = suspendCancellableCoroutine { continuation ->
		continuation.invokeOnCancellation { close() }
		thread(name = "cleanmind-chatgpt-callback", isDaemon = true) {
			try {
				val deadline = System.nanoTime() + 300_000_000_000L
				while (continuation.isActive) {
					val remaining = (deadline - System.nanoTime()) / 1_000_000
					if (remaining <= 0) throw AiException(AiError.TIMEOUT)
					socket.soTimeout = remaining.coerceAtMost(300_000).toInt().coerceAtLeast(1)
					socket.accept().use { connection ->
						connection.soTimeout = 5_000
						val input = connection.getInputStream()
						val header = StringBuilder()
						while (header.length < 16_384 && !header.endsWith("\r\n\r\n")) {
							val byte = input.read()
							if (byte < 0) break
							header.append(byte.toChar())
						}
						val request =
							header.lineSequence().firstOrNull().orEmpty().trim().split(' ')
						val url =
							request
								.getOrNull(1)
								?.takeIf { it.startsWith("/") }
								?.let {
									"http://127.0.0.1:${socket.localPort}$it".toHttpUrlOrNull()
								}
						val valid =
							request.firstOrNull() == "GET" &&
								url?.encodedPath == "/auth/callback" &&
								url.queryParameterValues("state") == listOf(state)
						val body =
							if (valid)
								"""<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Clean Mind</title><body><h1>Vuelve a Clean Mind</h1><p>La app verificará la conexión con ChatGPT. Puedes volver con el botón Atrás o con este enlace.</p><a href="cleanmind://chatgpt/return">Volver a Clean Mind</a></body></html>"""
							else "Solicitud no válida."
						val bytes = body.toByteArray(Charsets.UTF_8)
						val response =
							"HTTP/1.1 ${if (valid) "200 OK" else "400 Bad Request"}\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nContent-Security-Policy: default-src 'none'; base-uri 'none'; frame-ancestors 'none'\r\nConnection: close\r\n\r\n"
						// Closing the browser response must not discard an already validated
						// callback.
						runCatching {
							connection.getOutputStream().apply {
								write(response.toByteArray(Charsets.US_ASCII))
								write(bytes)
								flush()
							}
						}
						if (valid && continuation.isActive) {
							continuation.resume(url)
							return@thread
						}
					}
				}
			} catch (error: Exception) {
				if (continuation.isActive) continuation.resumeWithException(safeChatGptError(error))
			} finally {
				close()
			}
		}
	}

	override fun close() {
		runCatching { socket.close() }
	}
}
