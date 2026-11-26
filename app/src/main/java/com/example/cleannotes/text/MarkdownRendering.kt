package com.example.cleannotes.text

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.*
import com.example.cleannotes.model.MarkdownText
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.*
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.node.*

/** Native spans also feed PDF export. Raw HTML is never executed, and images stay offline. */
object MarkdownRendering {
	fun render(source: String): Spanned {
		val out = SpannableStringBuilder()
		val html = ArrayDeque<Pair<String, Int>>()
		fun span(value: Any, start: Int, end: Int = out.length) {
			if (start < end) out.setSpan(value, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		}

		fun gap() {
			if (out.isNotEmpty() && out.last() != '\n') out.append('\n')
		}

		fun children(node: Node): List<Node> = generateSequence(node.firstChild) { it.next }.toList()
		fun htmlTag(literal: String) {
			val tag = literal.lowercase().trim()
			if (tag in listOf("<br>", "<br/>", "<br />")) {
				out.append('\n'); return
			}
			val name = tag.removePrefix("</").removePrefix("<").removeSuffix(">")
			if (name !in setOf("strong", "b", "em", "i", "u", "s", "del", "big")) {
				out.append(literal); return
			}
			if (!tag.startsWith("</")) html.addLast(name to out.length)
			else if (html.lastOrNull()?.first == name) {
				val start = html.removeLast().second
				span(
					when (name) {
						"strong", "b" -> StyleSpan(Typeface.BOLD)
						"em", "i" -> StyleSpan(Typeface.ITALIC)
						"u" -> UnderlineSpan()
						"big" -> RelativeSizeSpan(1.35f)
						else -> StrikethroughSpan()
					}, start
				)
			}
		}

		fun visit(node: Node, depth: Int = 0) {
			val start = out.length
			when (node) {
				is Text -> out.append(node.literal)
				is Code -> {
					out.append(node.literal); span(TypefaceSpan("monospace"), start)
				}

				is SoftLineBreak, is HardLineBreak -> out.append('\n')
				is HtmlInline -> htmlTag(node.literal)
				is HtmlBlock -> {
					gap(); out.append(node.literal); gap()
				}

				is ThematicBreak -> {
					gap(); out.append("────────────\n\n")
				}

				is FencedCodeBlock -> {
					gap();
					val a = out.length; out.append(node.literal); span(
						TypefaceSpan("monospace"),
						a
					); gap(); out.append('\n')
				}

				is IndentedCodeBlock -> {
					gap();
					val a = out.length; out.append(node.literal); span(
						TypefaceSpan("monospace"),
						a
					); gap(); out.append('\n')
				}

				is BulletList, is OrderedList -> {
					gap()
					children(node).forEachIndexed { index, item ->
						out.append("  ".repeat(depth))
						val task = children(item).filterIsInstance<TaskListItemMarker>().firstOrNull()
						out.append(
							when {
								task != null -> if (task.isChecked) "☑ " else "☐ "
								node is OrderedList -> "${node.markerStartNumber + index}. "
								else -> "• "
							}
						)
						children(item).filterNot { it is TaskListItemMarker }.forEach { visit(it, depth + 1) }
						gap()
					}
					gap()
				}

				is TaskListItemMarker -> Unit
				is TableBlock -> {
					gap()
					val rows = children(node).flatMap { children(it) }
						.map { row -> children(row).map { MarkdownText.plainCell(it) } }
					val count = rows.maxOfOrNull { it.size } ?: 0
					val widths =
						List(count) { col -> rows.maxOf { it.getOrElse(col) { "" }.length }.coerceAtMost(30) }
					val a = out.length
					rows.forEachIndexed { index, row ->
						out.append(row.mapIndexed { col, cell -> cell.padEnd(widths[col]) }.joinToString(" | "))
						out.append('\n')
						if (index == 0) out.append(widths.joinToString("-+-") { "─".repeat(it) }).append('\n')
					}
					span(TypefaceSpan("monospace"), a); gap()
				}

				else -> {
					children(node).forEach { visit(it, depth) }
					when (node) {
						is StrongEmphasis -> span(StyleSpan(Typeface.BOLD), start)
						is Emphasis -> span(StyleSpan(Typeface.ITALIC), start)
						is Strikethrough -> span(StrikethroughSpan(), start)
						is Heading -> {
							span(StyleSpan(Typeface.BOLD), start)
							span(
								RelativeSizeSpan(listOf(1.8f, 1.55f, 1.35f, 1.2f, 1.1f, 1f)[node.level - 1]),
								start
							)
							gap(); out.append('\n')
						}

						is Link -> if (Regex("(?i)^(https?://|mailto:)").containsMatchIn(node.destination)) span(
							URLSpan(node.destination),
							start
						)

						is Image -> {
							if (out.length == start) out.append("Imagen"); out.append(" (${node.destination})")
						}

						is BlockQuote -> {
							span(LeadingMarginSpan.Standard(28), start); gap(); out.append('\n')
						}

						is Paragraph -> {
							gap(); if (node.parent !is ListItem) out.append('\n')
						}
					}
				}
			}
		}
		visit(MarkdownText.parse(source))
		while (out.isNotEmpty() && out.last() == '\n') out.delete(out.length - 1, out.length)
		return out
	}

	private fun MarkdownText.plainCell(node: Node): String = buildString {
		fun collect(n: Node) {
			when (n) {
				is Text -> append(n.literal); is Code -> append(n.literal); else -> generateSequence(n.firstChild) { it.next }.forEach(
				::collect
			)
			}
		}
		collect(node)
	}
}
