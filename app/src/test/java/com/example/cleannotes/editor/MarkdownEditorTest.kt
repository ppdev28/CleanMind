package com.example.cleannotes.editor

import android.app.Application
import android.text.style.StyleSpan
import android.text.style.URLSpan
import com.example.cleannotes.data.export.NoteExport
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.edit_note.richtext.MarkdownEditing
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichNoteEditor
import com.example.cleannotes.text.MarkdownRendering
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class MarkdownEditorTest {
	@Test
	fun legacyProjectionPreservesMetadataAndNeverMutatesTheOriginal() {
		val original = NoteDocument(
			text = "Hola mundo",
			marks = listOf(TextMark(0, 4, "bold")),
			images = listOf("a.jpg"),
			subtitle = "Subtítulo",
			createdAt = 123L,
			checklist = listOf(CheckItem("id", "Pendiente"))
		)
		val stored = original.encode()
		val converted = MarkdownText.editable(original)
		assertEquals(stored, original.encode())
		assertTrue(converted.markdown)
		assertTrue(converted.text.contains("<strong>Hola</strong>"))
		assertEquals(original.images, converted.images)
		assertEquals(original.checklist, converted.checklist)
		assertEquals(original.createdAt, converted.createdAt)
		assertEquals(converted, MarkdownText.editable(converted))
		assertEquals("Hola mundo", MarkdownText.plain(converted.text))
		assertTrue(MarkdownText.plain("`<u>`").contains("<u>"))
		val punctuation =
			MarkdownText.editable(NoteDocument(text = "¡Hola! Ciencia (breve). &amp; <u>literal</u>"))
		assertTrue(punctuation.text.contains("Ciencia (breve)."))
		assertEquals(
			"# literal",
			MarkdownText.plain(MarkdownText.editable(NoteDocument(text = "# literal")).text)
		)
		assertEquals(
			"¡Hola! Ciencia (breve). &amp; <u>literal</u>",
			MarkdownText.plain(punctuation.text)
		)
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
	fun enterContinuesNestedListsNumbersTasksAndQuotes() {
		mapOf(
			"- átomo" to "- ",
			"  * átomo" to "  * ",
			"9. átomo" to "10. ",
			"9) átomo" to "10) ",
			"- [x] átomo" to "- [ ] ",
			"> cita" to "> "
		).forEach { (line, prefix) ->
			assertEquals("$line\n$prefix", MarkdownEditing.enter("$line\n", line.length)?.text)
		}
		assertEquals("\n", MarkdownEditing.enter("- [ ] \n", 6)?.text)
		assertNull(MarkdownEditing.enter("```\n- código\n", 12))
	}

	@Test
	fun wrappingAndMultiLineBlocksKeepSelectionAndToggleCleanly() {
		val wrapped = MarkdownEditing.wrap("Hola mundo", 0, 4, "**")
		assertEquals("**Hola** mundo", wrapped.text)
		assertEquals(
			"Hola mundo",
			MarkdownEditing.wrap(wrapped.text, wrapped.start, wrapped.end, "**").text
		)
		assertEquals("1. uno\n2. dos", MarkdownEditing.block("- uno\n- dos", 0, 11, "number").text)
	}

	@Test
	fun markdownCommandsAndUndoPreserveImagesAndReminderSettings() {
		val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
		var saved = NoteDocument(
			text = "átomo",
			markdown = true,
			images = listOf("a.jpg"),
			reminder = ReminderConfig(100L)
		)
		editor.bind(saved)
		editor.onDocumentChanged = { saved = it }
		editor.setSelection(0, 5)
		editor.format("bold")
		assertEquals("**átomo**", saved.text)
		assertTrue(saved.marks.isEmpty())
		editor.undoEdit()
		assertEquals("átomo", saved.text)
		assertEquals(listOf("a.jpg"), saved.images)
		assertEquals(100L, saved.reminder?.at)
		editor.redoEdit()
		assertEquals("**átomo**", saved.text)
		editor.alignment(2)
		assertEquals(2, saved.textAlignment)
		editor.undoEdit()
		assertEquals(0, saved.textAlignment)
		assertEquals("**átomo**", saved.text)
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
