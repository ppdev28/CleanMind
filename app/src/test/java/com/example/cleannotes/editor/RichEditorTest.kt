package com.example.cleannotes.editor

import android.app.Application
import android.graphics.Typeface
import android.text.style.StyleSpan
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.TextMark
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichNoteEditor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RichEditorTest {
    @Test fun legacyTextIsNotTreatedAsMarkup() {
        assertEquals("<b>hola</b>", NoteDocument.decode("<b>hola</b>").text)
    }
    @Test fun documentRoundTripPreservesFormattingAndAttachments() {
        val doc = NoteDocument("Hola mundo", listOf(TextMark(0, 4, "bold")), listOf("test.jpg"), 1.5f, 16)
        assertEquals(doc, NoteDocument.decode(doc.encode()))
        assertEquals("Hola mundo", NoteDocument.plainText(doc.encode()))
    }
    @Test fun boldSelectionSurvivesReopeningAndCanBeUndone() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        var saved = NoteDocument("Hola mundo")
        editor.bind(saved)
        editor.onDocumentChanged = { saved = it }
        editor.setSelection(0, 4)
        editor.format("bold")
        assertEquals(listOf(TextMark(0, 4, "bold")), saved.marks)
        val reopened = RichNoteEditor(RuntimeEnvironment.getApplication())
        reopened.bind(NoteDocument.decode(saved.encode()))
        assertEquals(Typeface.BOLD, reopened.text!!.getSpans(0, 4, StyleSpan::class.java).single().style)
        editor.undoEdit()
        assertTrue(saved.marks.isEmpty())
        editor.redoEdit()
        assertEquals("bold", saved.marks.single().kind)
    }
    @Test fun clearingPartOfAStylePreservesTheRest() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        editor.bind(NoteDocument("abcdef", listOf(TextMark(0, 6, "bold"))))
        var result = NoteDocument()
        editor.onDocumentChanged = { result = it }
        editor.setSelection(2, 4)
        editor.format("clear")
        assertEquals(listOf(TextMark(0, 2, "bold"), TextMark(4, 6, "bold")), result.marks)
    }
    @Test fun listsAndCheckboxesModifyTheSelectedParagraph() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        editor.bind(NoteDocument("Uno\nDos"))
        var result = NoteDocument()
        editor.onDocumentChanged = { result = it }
        editor.setSelection(0, 7)
        editor.list("number")
        assertEquals("1. Uno\n2. Dos", result.text)
        editor.setSelection(0)
        editor.checkItem()
        assertTrue(result.text.startsWith("☐ "))
        editor.setSelection(0)
        editor.checkItem()
        assertTrue(result.text.startsWith("☑ "))
    }
    @Test fun paragraphAndLineSpacingSurviveReopening() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        editor.bind(NoteDocument("Uno\nDos"))
        var saved = NoteDocument()
        editor.onDocumentChanged = { saved = it }
        editor.spacing(2f, 4)
        editor.paragraphSpacing(24)
        val restored = NoteDocument.decode(saved.encode())
        assertEquals(2f, restored.lineSpacing)
        assertEquals(4, restored.extraLineSpacing)
        assertEquals(24, restored.paragraphSpacing)
    }

    @Test fun changingNoteColorPreservesSelectionAndFormatting() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        val original = NoteDocument("Hola mundo", listOf(TextMark(0, 4, "bold")))
        editor.bind(original)
        editor.setSelection(0, 4)
        val colored = NoteDocument.decode(original.copy(backgroundColor = 0xFFFFF8B8.toInt()).encode())
        editor.bind(colored)
        assertEquals(0, editor.selectionStart)
        assertEquals(4, editor.selectionEnd)
        assertEquals(Typeface.BOLD, editor.text!!.getSpans(0, 4, StyleSpan::class.java).single().style)
        assertEquals(0xFFFFF8B8.toInt(), colored.backgroundColor)
        assertEquals(0, NoteDocument.decode(original.encode()).backgroundColor)
    }

    @Test fun toolbarTracksSelectionAndUndoAvailability() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        editor.bind(NoteDocument("Hola mundo"))
        var canUndo = false
        var canRedo = false
        var active = emptySet<String>()
        editor.onEditorStateChanged = { undo, redo, formats -> canUndo = undo; canRedo = redo; active = formats }
        editor.setSelection(0, 4)
        editor.format("bold")
        assertTrue(canUndo)
        assertFalse(canRedo)
        assertTrue("bold" in active)
        editor.setSelection(5, 10)
        assertFalse("bold" in active)
        editor.undoEdit()
        assertFalse(canUndo)
        assertTrue(canRedo)
        editor.redoEdit()
        assertTrue(canUndo)
        assertFalse(canRedo)
    }

    @Test fun enterContinuesBulletAndNumberedListsAndEmptyItemEndsList() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        var saved = NoteDocument()
        editor.onDocumentChanged = { saved = it }
        editor.bind(NoteDocument("• Uno"))
        editor.setSelection(editor.length())
        editor.text!!.append("\n")
        assertEquals("• Uno\n• ", saved.text)
        editor.text!!.append("\n")
        assertEquals("• Uno\n\n", saved.text)
        editor.bind(NoteDocument("9. Nueve"))
        editor.setSelection(editor.length())
        editor.text!!.append("\n")
        assertEquals("9. Nueve\n10. ", saved.text)
        editor.undoEdit()
        assertEquals("9. Nueve", saved.text)
        editor.redoEdit()
        assertEquals("9. Nueve\n10. ", saved.text)
    }

    @Test fun subtitleRoundTripsAndOlderDocumentsRemainReadable() {
        val old = NoteDocument.decode("\u001eCleanNotes:document:1\n{\"text\":\"Cuerpo antiguo\"}")
        assertEquals("", old.subtitle)
        assertEquals("Cuerpo antiguo", old.text)
        val next = old.copy(subtitle = "Resumen propio")
        assertEquals(next, NoteDocument.decode(next.encode()))
        assertEquals("Resumen propio\n\nCuerpo antiguo", NoteDocument.plainText(next.encode()))
    }

    @Test fun undoOfBodyPreservesSubtitleEditedSeparately() {
        val editor = RichNoteEditor(RuntimeEnvironment.getApplication())
        editor.bind(NoteDocument("Texto"))
        var saved = NoteDocument()
        editor.onDocumentChanged = { saved = it }
        editor.setSelection(0, 5)
        editor.format("bold")
        editor.bind(saved.copy(subtitle = "Subtítulo nuevo"))
        editor.undoEdit()
        assertEquals("Subtítulo nuevo", saved.subtitle)
        assertTrue(saved.marks.isEmpty())
        editor.redoEdit()
        assertEquals("Subtítulo nuevo", saved.subtitle)
        assertEquals("bold", saved.marks.single().kind)
    }

}
