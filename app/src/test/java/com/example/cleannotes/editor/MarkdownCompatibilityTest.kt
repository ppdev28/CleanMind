package com.example.cleannotes.editor

import android.app.Application
import android.text.style.StyleSpan
import android.text.style.URLSpan
import com.example.cleannotes.data.export.NoteExport
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichNoteEditor
import com.example.cleannotes.text.MarkdownRendering
import com.example.cleannotes.text.RichDocumentCompatibility
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MarkdownCompatibilityTest {
	@Test
	fun normalDocumentsAreReturnedWithoutChanges() {
		val original = NoteDocument(
			text = "Hola *literal*",
			marks = listOf(TextMark(0, 4, "bold")),
			images = listOf("a.jpg")
		)
		assertSame(original, RichDocumentCompatibility.editable(original))
	}

	@Test
	fun existingMarkdownOpensAsNormalRichTextAndPreservesItsMetadata() {
		val original = NoteDocument(
			text = "# Ciencia\n\n**Masa** y [energía](https://example.com)\n\n- [x] Medir",
			markdown = true,
			images = listOf("a.jpg"),
			subtitle = "Subtítulo",
			createdAt = 123L,
			pinned = true,
			archived = true,
			checklist = listOf(CheckItem("id", "Pendiente")),
			reminder = ReminderConfig(456L),
			textAlignment = 2
		)
		val stored = original.encode()
		val converted = RichDocumentCompatibility.editable(original)
		assertEquals(stored, original.encode())
		assertFalse(converted.markdown)
		assertFalse(converted.text.contains("**"))
		assertTrue(converted.text.contains("https://example.com"))
		assertTrue(converted.text.contains("☑ Medir"))
		assertTrue(converted.marks.any {
			it.kind == "bold" && converted.text.substring(it.start, it.end).contains("Masa")
		})
		assertEquals(
			original.copy(text = converted.text, marks = converted.marks, markdown = false),
			converted
		)
		val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
		var saved = converted
		editor.bind(converted)
		editor.onDocumentChanged = { saved = it }
		editor.setSelection(0, 7)
		editor.format("italic")
		assertFalse(saved.markdown)
		assertEquals(converted.text, saved.text)
		editor.undoEdit()
		assertEquals(converted, saved)
	}

	@Test
	fun commonMarkRenderingHandlesNestedFormattingTablesTasksAndCode() {
		val source =
			"# Ciencia\n\n**Masa *y energía***, ~~error~~ y `E=mc²`.\n\n- [x] Medir\n- [ ] Repetir\n\n| Magnitud | Unidad |\n| --- | --- |\n| Masa | kg |\n\n> Una cita\n\n```kotlin\nval x = 1\n```"
		val rendered = MarkdownRendering.render(source)
		assertTrue(rendered.contains("Ciencia"))
		assertFalse(rendered.contains("**"))
		assertTrue(rendered.contains("kg"))
		assertTrue(rendered.contains("☑ Medir"))
		assertTrue(rendered.contains("☐ Repetir"))
		assertTrue(rendered.getSpans(0, rendered.length, StyleSpan::class.java).size >= 3)
	}

	@Test
	fun linksAllowNormalUrlsAndNeverExecuteRawHtml() {
		val rendered =
			MarkdownRendering.render("[web](https://example.com) [bad](javascript:alert) <script>alert(1)</script>")
		assertEquals(
			listOf("https://example.com"),
			rendered.getSpans(0, rendered.length, URLSpan::class.java).map { it.url })
		assertTrue(rendered.contains("<script>"))
	}

	@Test
	fun plainSharingAndSearchUseReadableMarkdownContent() {
		val document =
			NoteDocument(text = "# Ciencia\n\n**Masa** y [energía](https://example.com)", markdown = true)
		val exported = NoteExport.plainText("Título", document)
		assertTrue(exported.contains("Masa"))
		assertFalse(exported.contains("**"))
		assertFalse(NoteDocument.plainText(document.encode()).contains("# Ciencia"))
		assertEquals(document, NoteDocument.decode(document.encode()))
	}
}
