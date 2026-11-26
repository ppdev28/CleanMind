package com.example.cleannotes.model

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Node
import org.commonmark.node.HtmlInline
import org.commonmark.node.Text
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

/** One CommonMark dialect for editing, search, previews and exports. */
object MarkdownText {
	private val extensions = listOf(
		TablesExtension.create(), StrikethroughExtension.create(), TaskListItemsExtension.create(),
	)
	private val parser = Parser.builder().extensions(extensions)
		.includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES).build()
	private val plainRenderer = TextContentRenderer.builder().extensions(extensions).build()

	fun parse(source: String): Node = parser.parse(source)
	fun plain(source: String): String {
		val root = parse(source)
		fun stripTags(node: Node) {
			generateSequence(node.firstChild) { it.next }.toList().forEach { child ->
				if (child is HtmlInline && Regex(
						"</?(?:strong|b|em|i|u|s|del|big)>",
						RegexOption.IGNORE_CASE
					).matches(child.literal)
				) child.unlink()
				else if (child is HtmlInline && Regex(
						"<br\\s*/?>",
						RegexOption.IGNORE_CASE
					).matches(child.literal)
				) {
					child.insertBefore(Text("\n")); child.unlink()
				} else stripTags(child)
			}
		}
		stripTags(root)
		return plainRenderer.render(root).trimEnd()
	}

	/** Projection only: opening an old note never rewrites its stored document. */
	fun editable(document: NoteDocument): NoteDocument {
		if (document.markdown) return document
		val marks =
			document.marks.filter { it.start >= 0 && it.end <= document.text.length && it.start < it.end }
		val boundaries =
			(listOf(0, document.text.length) + marks.flatMap { listOf(it.start, it.end) }).distinct()
				.sorted()
		val converted = buildString {
			boundaries.zipWithNext().forEach { (start, end) ->
				val tags = marks.filter { it.start <= start && it.end >= end }.mapNotNull {
					when (it.kind) {
						"bold" -> "strong"
						"italic" -> "em"
						"underline" -> "u"
						"strike" -> "del"
						"size" -> if (it.value > 100) "big" else null
						else -> null
					}
				}.distinct()
				tags.forEach { append("<$it>") }
				append(
					document.text.substring(start, end)
						.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
						.replace(Regex("([\\\\`*_\\[\\]~])"), "\\\\$1")
				)
				tags.asReversed().forEach { append("</$it>") }
			}
		}.replace(Regex("(?m)^(#{1,6})(?= )"), "\\\\$1")
			.replace(Regex("(?m)^• "), "- ")
			.replace(Regex("(?m)^☐ "), "- [ ] ").replace(Regex("(?m)^☑ "), "- [x] ")
		return document.copy(text = converted, marks = emptyList(), markdown = true)
	}
}
