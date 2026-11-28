package com.example.cleannotes.data.links

import android.content.Context
import java.io.File
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

@Serializable
data class LinkPreview(
	val url: String,
	val title: String,
	val description: String = "",
	val resolved: Boolean = false
) {
	val host: String
		get() = runCatching {
			URI(url).host.orEmpty().removePrefix("www.")
		}.getOrDefault(url)
}

object LinkPreviews {
	private val permits = Semaphore(2)
	fun urls(text: String): List<String> = Regex("(?i)\\b(?:https?://|www\\.)[^\\s<>]+")
		.findAll(text).map { it.value.trimEnd('.', ',', ')', ']', '}', '!', '?', ';', ':', '"', '\'') }
		.map { if (it.startsWith("www.", true)) "https://$it" else it }
		.filter { valid(it) }.distinct().take(5).toList()

	private fun valid(url: String) = runCatching {
		URI(url).let {
			it.scheme.lowercase() in listOf(
				"http",
				"https"
			) && !it.host.isNullOrBlank() && it.userInfo == null
		}
	}.getOrDefault(false)

	fun fallback(url: String) =
		LinkPreview(url, runCatching { URI(url).host.orEmpty() }.getOrDefault(url))

	fun parse(url: String, html: String): LinkPreview {
		val doc = Jsoup.parse(html, url)
		val title =
			doc.selectFirst("meta[property=og:title]")?.attr("content")?.takeIf { it.isNotBlank() }
				?: doc.title()
		val description =
			doc.selectFirst("meta[property=og:description], meta[name=description]")?.attr("content")
				.orEmpty()
		return LinkPreview(
			url,
			title.trim().take(200).ifBlank { fallback(url).title },
			description.trim().take(400),
			true
		)
	}

	suspend fun load(context: Context, original: String): LinkPreview = withContext(Dispatchers.IO) {
		if (!valid(original)) return@withContext fallback(original)
		val key = MessageDigest.getInstance("SHA-256").digest(original.toByteArray())
			.joinToString("") { "%02x".format(it) }
		val cache = File(File(context.cacheDir, "link_previews").apply { mkdirs() }, "$key.json")
		runCatching { Json.decodeFromString<LinkPreview>(cache.readText()) }.getOrNull()?.let {
			val ttl = if (it.resolved) 86_400_000L else 300_000L
			if (System.currentTimeMillis() - cache.lastModified() < ttl) return@withContext it
		}
		val preview = permits.withPermit {
			runCatching {
				var url = original
				for (attempt in 0..3) {
					val response = Jsoup.connect(url).timeout(6000).maxBodySize(512 * 1024)
						.userAgent("CleanMind/1.0 (link preview)")
						.followRedirects(false).ignoreHttpErrors(true).execute()
					if (response.statusCode() in 300..399) {
						url =
							URI(url).resolve(response.header("Location") ?: error("Missing redirect")).toString()
						require(valid(url)); continue
					}
					check(
						response.statusCode() in 200..299 && response.contentType().orEmpty().contains("html")
					)
					return@runCatching parse(original, response.body())
				}
				error("Too many redirects")
			}.getOrElse { fallback(original) }
		}
		runCatching { cache.writeText(Json.encodeToString(preview)) }
		preview
	}
}
