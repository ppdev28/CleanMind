package com.example.cleannotes.ai

import com.example.cleannotes.model.CheckItem
import com.example.cleannotes.model.NoteDocument
import org.junit.Assert.*
import org.junit.Test

class AiModelsTest {
	@Test
	fun normalizesConfigurationAndBuildsEndpointWithoutDroppingProviderPath() {
		val config =
			AiConfiguration(" https://example.com/proxy/v1/ ", " model ", " secret ").normalized()
		config.validate()
		assertEquals("https://example.com/proxy/v1/chat/completions", config.endpoint().toString())
		assertEquals("model", config.model)
		assertEquals("secret", config.apiKey)
		assertFalse(config.toString().contains("secret"))
	}

	@Test
	fun rejectsUnsafeOrAmbiguousEndpoints() {
		listOf(
				"http://example.com/v1",
				"ftp://example.com",
				"https://user:pass@example.com",
				"https://example.com?key=secret",
				"https://example.com/#fragment",
				"invalid",
			)
			.forEach { url ->
				val error =
					assertThrows(AiException::class.java) {
						AiConfiguration(url, apiKey = "secret").validate()
					}
				assertEquals(AiError.CONFIGURATION, error.reason)
			}
	}

	@Test
	fun rejectsEmptyCredentialsAndHeaderInjection() {
		listOf("", " ", "secret\nInjected: header", "á").forEach { key ->
			assertThrows(AiException::class.java) { AiConfiguration(apiKey = key).validate() }
		}
		assertThrows(AiException::class.java) {
			AiConfiguration(model = "", apiKey = "secret").validate()
		}
	}

	@Test
	fun snapshotIncludesOnlyReadableText() {
		val doc =
			NoteDocument(
				text = "Cuerpo",
				subtitle = "Subtítulo",
				images = listOf("file:///private/image"),
				checklist = listOf(CheckItem("1", "Llamar")),
			)
		val text = AiNoteSnapshot("Título", doc.encode()).plainText()
		assertTrue(text.contains("Título"))
		assertTrue(text.contains("Subtítulo"))
		assertTrue(text.contains("Cuerpo"))
		assertTrue(text.contains("Llamar"))
		assertFalse(text.contains("private/image"))
		assertFalse(text.contains("CleanMind:document"))
	}

	@Test
	fun validatesEmptyNotesAndInputLimitsWithoutSilentlyTruncating() {
		val empty = AiNoteSnapshot("", "")
		assertThrows(AiException::class.java) { AiRequest(empty, AiAction.SUMMARIZE).validate() }
		AiRequest(empty, AiAction.CUSTOM, "Redacta un correo").validate()
		assertThrows(AiException::class.java) { AiRequest(empty, AiAction.CUSTOM).validate() }
		assertEquals(
			AiError.INPUT_TOO_LONG,
			assertThrows(AiException::class.java) {
					AiRequest(AiNoteSnapshot("", "a".repeat(24_001)), AiAction.IMPROVE).validate()
				}
				.reason,
		)
		assertThrows(AiException::class.java) {
			AiRequest(empty, AiAction.CUSTOM, "a".repeat(2_001)).validate()
		}
	}
}
