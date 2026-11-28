package com.example.cleannotes.model

import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Node
import org.commonmark.node.HtmlInline
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

/** Read compatibility for documents stored by the previous Markdown editor. */
object MarkdownText {
	private val extensions = listOf(
		TablesExtension.create(), StrikethroughExtension.create(), TaskListItemsExtension.create(),
	)
	private val parser = Parser.builder().extensions(extensions).build()
	private val plainRenderer = TextContentRenderer.builder().extensions(extensions).build()

	fun parse(source: String): Node = parser.parse(source)
	private val styleTag = Regex("</?(?:strong|b|em|i|u|s|del|big)>", RegexOption.IGNORE_CASE)
	private val lineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

	fun plain(source: String): String {
		val root = parse(source)
		fun stripTags(node: Node) {
			var child = node.firstChild
			while (child != null) {
				val current = child
				child = current.next
				when {
					current is HtmlInline && styleTag.matches(current.literal) -> current.unlink()
					current is HtmlInline && lineBreak.matches(current.literal) -> {
						current.insertBefore(Text("\n")); current.unlink()
					}

					else -> stripTags(current)
				}
			}
		}
		stripTags(root)
		return plainRenderer.render(root).trimEnd()
	}
}
