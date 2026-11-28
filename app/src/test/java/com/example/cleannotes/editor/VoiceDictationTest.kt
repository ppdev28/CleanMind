package com.example.cleannotes.editor

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.speech.RecognizerIntent
import com.example.cleannotes.model.*
import com.example.cleannotes.presentation.ui.edit_note.VoiceDictation
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichNoteEditor
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class VoiceDictationTest {
	@Test
	fun requestsSpeechInTheCurrentLanguageAndIgnoresCancellation() {
		val intent = VoiceDictation.intent(Locale.forLanguageTag("es-ES"))
		assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
		assertEquals("es-ES", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
		val result =
			Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("La luz viaja"))
		assertNull(VoiceDictation.result(Activity.RESULT_CANCELED, result))
		assertEquals("La luz viaja", VoiceDictation.result(Activity.RESULT_OK, result))
	}

	@Test
	fun dictationReplacesTheSelectionAndCanBeUndoneWithoutLosingAttachments() {
		val original = NoteDocument(
			text = "Hola mundo",
			marks = listOf(TextMark(0, 4, "bold")),
			images = listOf("test.jpg")
		)
		val editor = RichNoteEditor(RuntimeEnvironment.getApplication());
		var saved = original
		editor.bind(original); editor.onDocumentChanged = { saved = it }; editor.setSelection(5, 10)
		editor.insertDictation("ciencia")
		assertEquals("Hola ciencia", saved.text); assertEquals(original.images, saved.images)
		editor.undoEdit(); assertEquals(original, saved)
	}
}
