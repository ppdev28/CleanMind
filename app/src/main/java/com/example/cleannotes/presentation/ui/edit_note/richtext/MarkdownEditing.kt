package com.example.cleannotes.presentation.ui.edit_note.richtext

/** Source edits preserve offsets and are committed as a single undo operation. */
object MarkdownEditing {
	data class Edit(val text: String, val start: Int, val end: Int = start)

	fun wrap(text: String, start: Int, end: Int, open: String, close: String = open): Edit {
		val a = minOf(start, end).coerceIn(0, text.length)
		val b = maxOf(start, end).coerceIn(a, text.length)
		if (a >= open.length && text.substring(a - open.length, a) == open && text.startsWith(close, b))
			return Edit(
				text.removeRange(b, b + close.length).removeRange(a - open.length, a),
				a - open.length,
				b - open.length
			)
		if (b - a >= open.length + close.length && text.startsWith(open, a) && text.substring(a, b)
				.endsWith(close)
		)
			return Edit(
				text.removeRange(b - close.length, b).removeRange(a, a + open.length),
				a,
				b - open.length - close.length
			)
		return Edit(
			text.substring(0, a) + open + text.substring(a, b) + close + text.substring(b),
			a + open.length,
			b + open.length
		)
	}

	fun block(text: String, start: Int, end: Int, prefix: String): Edit {
		val a = text.lastIndexOf('\n', (minOf(start, end) - 1).coerceAtLeast(-1)) + 1
		val b = text.indexOf('\n', maxOf(start, end)).let { if (it < 0) text.length else it }
		val selected = text.substring(a, b).lines()
		val marker = Regex("^(#{1,6} |[-+*] (?:\\[[ xX]] )?|\\d+[.)] |[☐☑•] |>[ ]?)")
		val result = selected.mapIndexed { index, line ->
			when (prefix) {
				"indent" -> "    $line"
				"outdent" -> line.removePrefix("    ").removePrefix("\t")
				"number" -> "${index + 1}. " + line.replace(marker, "")
				else -> prefix + line.replace(marker, "")
			}
		}.joinToString("\n")
		return Edit(text.replaceRange(a, b, result), a, a + result.length)
	}

	/** Enter continues lists/quotes, resets checked tasks and exits an empty list item. */
	fun enter(text: String, newline: Int): Edit? {
		val a = text.lastIndexOf('\n', newline - 1) + 1
		val line = text.substring(a, newline)
		val before = text.substring(0, a)
		if (Regex("(?m)^ {0,3}(`{3,}|~{3,})").findAll(before).count() % 2 != 0) return null
		val match = Regex("^(\\s*)([-+*] (?:\\[([ xX])] )?|(\\d+)([.)]) |> ?)(.*)$").matchEntire(line)
			?: return null
		if (match.groupValues[6].isBlank()) return Edit(text.removeRange(a, newline), a + 1)
		val marker = when {
			match.groupValues[4].isNotEmpty() -> "${(match.groupValues[4].toLongOrNull() ?: return null) + 1}${match.groupValues[5]} "
			match.groupValues[3].isNotEmpty() -> match.groupValues[2].take(2) + "[ ] "
			else -> match.groupValues[2]
		}
		val prefix = match.groupValues[1] + marker
		return Edit(
			text.substring(0, newline + 1) + prefix + text.substring(newline + 1),
			newline + 1 + prefix.length
		)
	}
}
