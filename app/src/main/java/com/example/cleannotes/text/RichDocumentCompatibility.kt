package com.example.cleannotes.text

import android.text.SpannableStringBuilder
import android.text.style.*
import android.graphics.Typeface
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.TextMark

/** Read compatibility only: existing Markdown opens as normal text without a database rewrite. */
object RichDocumentCompatibility {
	fun editable(document: NoteDocument): NoteDocument {
		if (!document.markdown) return document
		val rendered = SpannableStringBuilder(MarkdownRendering.render(document.text))
		// The normal editor has no hyperlink field. Keep each destination visible in the text.
		rendered.getSpans(0, rendered.length, URLSpan::class.java)
			.sortedByDescending { rendered.getSpanEnd(it) }.forEach { link ->
				val start = rendered.getSpanStart(link)
				val end = rendered.getSpanEnd(link)
				if (rendered.substring(start, end) != link.url) rendered.insert(end, " (${link.url})")
				rendered.removeSpan(link)
			}
		val marks = rendered.getSpans(0, rendered.length, Any::class.java).flatMap { span ->
			val start = rendered.getSpanStart(span)
			val end = rendered.getSpanEnd(span)
			when (span) {
				is StyleSpan -> buildList {
					if (span.style and Typeface.BOLD != 0) add(TextMark(start, end, "bold"))
					if (span.style and Typeface.ITALIC != 0) add(TextMark(start, end, "italic"))
				}

				is UnderlineSpan -> listOf(TextMark(start, end, "underline"))
				is StrikethroughSpan -> listOf(TextMark(start, end, "strike"))
				is RelativeSizeSpan -> listOf(TextMark(start, end, "size", (span.sizeChange * 100).toInt()))
				is LeadingMarginSpan.Standard -> listOf(
					TextMark(
						start,
						end,
						"indent",
						span.getLeadingMargin(true)
					)
				)

				else -> emptyList()
			}
		}.filter { it.start >= 0 && it.end > it.start && it.end <= rendered.length }
		return document.copy(text = rendered.toString(), marks = marks, markdown = false)
	}
}
