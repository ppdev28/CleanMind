package com.example.cleannotes.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TextMark(val start: Int, val end: Int, val kind: String, val value: Int = 0)

@Serializable
data class NoteDocument(
	val text: String = "",
	val marks: List<TextMark> = emptyList(),
	val images: List<String> = emptyList(),
	val lineSpacing: Float = 1.3f,
	val extraLineSpacing: Int = 8,
	val paragraphSpacing: Int = 0,
	val backgroundColor: Int = 0,
	val reminder: ReminderConfig? = null,
	val subtitle: String = "",
	val pinned: Boolean = false,
	val archived: Boolean = false,
	val reminderCompleted: Boolean = false,
	val checklist: List<CheckItem> = emptyList(),
	val createdAt: Long? = null,
) {
	fun completeReminder(now: Long): NoteDocument {
		val next = reminder?.let { it.nextAfter(maxOf(now, it.at)) }
		return copy(
			reminderCompleted = next == null,
			reminder = if (next == null) reminder else reminder?.copy(at = next),
		)
	}

	fun encode(): String = PREFIX + Json.encodeToString(this)

	companion object {
		private const val PREFIX = "\u001eCleanMind:document:1\n"
		private const val LEGACY_PREFIX = "\u001eCleanNotes:document:1\n"

		fun decode(content: String): NoteDocument {
			val payload =
				when {
					content.startsWith(PREFIX) -> content.removePrefix(PREFIX)
					content.startsWith(LEGACY_PREFIX) -> content.removePrefix(LEGACY_PREFIX)
					else -> return NoteDocument(text = content, extraLineSpacing = 0)
				}
			return runCatching { Json.decodeFromString<NoteDocument>(payload) }
				.getOrElse {
					NoteDocument(text = content)
				}
		}

		fun plainText(content: String): String =
			decode(content).let {
				listOf(
					it.subtitle,
					it.text,
					it.checklist.joinToString("\n") { item ->
						(if (item.done) "☑ " else "☐ ") + item.text
					},
				)
					.filter(String::isNotBlank)
					.joinToString("\n\n")
			}
	}
}

@Serializable
data class CheckItem(val id: String, val text: String = "", val done: Boolean = false)

@Serializable
data class ReminderConfig(
	val at: Long,
	val message: String = "",
	val repeatUnit: String = "none",
	val interval: Int = 1,
	val sound: Boolean = true,
	val vibrate: Boolean = true,
	val highPriority: Boolean = true,
	val snoozeMinutes: Int = 10,
) {
	fun nextAfter(now: Long): Long? {
		if (repeatUnit == "none") return null
		if (at > now) return at
		if (repeatUnit == "minute" || repeatUnit == "hour") {
			val step =
				(if (repeatUnit == "minute") 60_000L else 3_600_000L) * interval.coerceIn(1, 365)
			return at + ((now - at) / step + 1) * step
		}
		val field =
			when (repeatUnit) {
				"minute" -> java.util.Calendar.MINUTE
				"hour" -> java.util.Calendar.HOUR_OF_DAY
				"day" -> java.util.Calendar.DAY_OF_YEAR
				"week" -> java.util.Calendar.WEEK_OF_YEAR
				"month" -> java.util.Calendar.MONTH
				else -> return null
			}
		val anchor = java.util.Calendar.getInstance().apply { timeInMillis = at }
		var count = interval.coerceIn(1, 365)
		var calendar = anchor.clone() as java.util.Calendar
		while (calendar.timeInMillis <= now) {
			calendar = (anchor.clone() as java.util.Calendar).apply { add(field, count) }
			count += interval.coerceIn(1, 365)
		}
		return calendar.timeInMillis
	}
}
