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

}
