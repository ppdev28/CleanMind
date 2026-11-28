package com.example.cleannotes.ai

import com.example.cleannotes.model.NoteDocument
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class AiConfiguration(
	val baseUrl: String = "https://api.openai.com/v1",
	val model: String = "gpt-4.1-mini",
	val apiKey: String = "",
) {
	fun normalized() =
		copy(baseUrl = baseUrl.trim().trimEnd('/'), model = model.trim(), apiKey = apiKey.trim())

	fun endpoint(): HttpUrl {
		val url = baseUrl.toHttpUrlOrNull() ?: throw AiException(AiError.CONFIGURATION)
		if (
			url.scheme != "https" ||
				url.username.isNotEmpty() ||
				url.password.isNotEmpty() ||
				url.query != null ||
				url.fragment != null
		)
			throw AiException(AiError.CONFIGURATION)
		return url.newBuilder()
			.encodedPath(url.encodedPath.trimEnd('/') + "/chat/completions")
			.build()
	}

	fun validate() {
		endpoint()
		if (
			model.isBlank() ||
				model.length > 200 ||
				apiKey.isBlank() ||
				apiKey.length > 4096 ||
				apiKey.any { it.code !in 33..126 }
		)
			throw AiException(AiError.CONFIGURATION)
	}

	// Credentials must never appear in diagnostic output.
	override fun toString() = "AiConfiguration(model=$model, apiKey=[REDACTED])"
}

enum class AiAction(val label: String, val instruction: String) {
	SUMMARIZE(
		"Resumir",
		"Resume las ideas principales de la nota de forma breve y fiel. No inventes datos.",
	),
	IMPROVE(
		"Mejorar redacción",
		"Mejora la claridad, la ortografía y la estructura de la nota sin cambiar su significado ni inventar datos.",
	),
	TASKS(
		"Extraer tareas",
		"Extrae las tareas concretas de la nota, una por línea con un guion. No inventes tareas ni fechas. Si no hay tareas, indícalo.",
	),
	CUSTOM(
		"Instrucción propia",
		"Sigue la instrucción del usuario usando la nota como contexto cuando exista.",
	),
}

data class AiNoteSnapshot(val title: String, val content: String) {
	fun plainText() =
		listOf(title, NoteDocument.plainText(content))
			.filter(String::isNotBlank)
			.joinToString("\n\n")
}

data class AiRequest(val note: AiNoteSnapshot, val action: AiAction, val instruction: String = "") {
	fun validate() {
		if (note.plainText().length > MAX_NOTE_CHARS || instruction.length > MAX_INSTRUCTION_CHARS)
			throw AiException(AiError.INPUT_TOO_LONG)
		if (
			action == AiAction.CUSTOM && instruction.isBlank() ||
				action != AiAction.CUSTOM && note.plainText().isBlank()
		)
			throw AiException(AiError.EMPTY_INPUT)
	}

	companion object {
		const val MAX_NOTE_CHARS = 24_000
		const val MAX_INSTRUCTION_CHARS = 2_000
	}
}

data class AiSuggestion(val note: AiNoteSnapshot, val text: String)

enum class AiError(val message: String) {
	CONFIGURATION("Revisa la URL HTTPS, el modelo y la clave de API en Ajustes."),
	STORAGE(
		"No se pudo leer o guardar la configuración de IA. Reintenta o elimina la configuración y vuelve a introducirla."
	),
	AUTHENTICATION("La API ha rechazado la clave. Revisa la clave y los permisos en Ajustes."),
	RATE_LIMIT(
		"Has alcanzado el límite de la API o no tienes saldo disponible. Revisa tu cuenta y vuelve a intentarlo."
	),
	MODEL("La API no encuentra el modelo o la ruta. Revisa el modelo y la URL en Ajustes."),
	NETWORK("No se pudo conectar con la API. Revisa la conexión e inténtalo de nuevo."),
	TIMEOUT("La API ha tardado demasiado. Vuelve a intentarlo."),
	SERVER("El proveedor de IA no está disponible. Inténtalo más tarde."),
	RESPONSE(
		"La API no devolvió una respuesta de texto válida. Revisa el modelo o vuelve a intentarlo."
	),
	TRUNCATED("La respuesta superó el límite de generación. Pide un resultado más breve."),
	EMPTY_INPUT("Escribe una nota o introduce una instrucción propia para comenzar."),
	INPUT_TOO_LONG(
		"La nota admite hasta 24.000 caracteres y la instrucción hasta 2.000. Reduce el texto para usar IA."
	),
}

class AiException(val reason: AiError) : Exception(reason.message)

fun Throwable.aiMessage(): String =
	(this as? AiException)?.reason?.message ?: AiError.SERVER.message

interface AiSettingsRepository {
	suspend fun load(): AiConfiguration

	suspend fun save(configuration: AiConfiguration)

	suspend fun clear()
}

interface AiGateway {
	suspend fun complete(configuration: AiConfiguration, request: AiRequest): String

	suspend fun testConnection(configuration: AiConfiguration)
}
