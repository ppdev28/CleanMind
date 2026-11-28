package com.example.cleannotes.editor

import android.app.Application
import androidx.core.content.FileProvider
import com.example.cleannotes.data.attachments.NoteFiles
import com.example.cleannotes.data.links.LinkPreviews
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichNoteEditor
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AttachmentsAndLinksTest {
	@Test
	fun attachedFilesRoundTripAndTextUndoKeepsThem() {
		val file = NoteFile("test.bin", "Informe.pdf", "application/pdf", 100)
		var saved = NoteDocument(text = "Ciencia", files = listOf(file))
		assertEquals(saved, NoteDocument.decode(saved.encode()))
		assertTrue(NoteDocument.plainText(saved.encode()).contains("Informe.pdf"))
		val editor =
			RichNoteEditor(RuntimeEnvironment.getApplication()); editor.bind(saved); editor.onDocumentChanged =
			{ saved = it }
		editor.setSelection(0, 7); editor.format("bold"); editor.undoEdit(); assertEquals(
			listOf(file),
			saved.files
		)
	}

	@Test
	fun importsAnIndependentPrivateCopyAndRejectsPathTraversal() = runBlocking {
		val context = RuntimeEnvironment.getApplication()
		val source = File(
			context.cacheDir,
			"exports/sample.txt"
		).apply { parentFile!!.mkdirs(); writeText("Ciencia") }
		val uri = FileProvider.getUriForFile(context, "${context.packageName}.images", source)
		val saved = NoteFiles.import(context, uri)
		source.delete(); assertEquals("Ciencia", NoteFiles.file(context, saved.name).readText())
		assertEquals("sample.txt", saved.displayName)
		assertThrows(IllegalArgumentException::class.java) { NoteFiles.file(context, "../private.bin") }
		Unit
	}

	@Test
	fun linksAreDeduplicatedAndMetadataIsParsedWithoutExecutingHtml() {
		val urls =
			LinkPreviews.urls("Mira https://example.com/a. y www.example.org y https://example.com/a. javascript:bad")
		assertEquals(listOf("https://example.com/a", "https://www.example.org"), urls)
		val preview = LinkPreviews.parse(
			urls.first(),
			"<title>Base</title><meta property='og:title' content='Ciencia &amp; luz'><meta name='description' content='Una idea breve'><script>bad()</script>"
		)
		assertEquals("Ciencia & luz", preview.title); assertEquals(
			"Una idea breve",
			preview.description
		)
	}
}
