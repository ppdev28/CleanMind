package com.example.cleannotes.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cleannotes.MainActivity
import com.example.cleannotes.data.attachments.NoteImages
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichNoteEditor
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises Android's real clipboard and FileProvider without editing the user's notes. */
@RunWith(AndroidJUnit4::class)
class RichImagePasteTest {
	@Test
	fun contextMenuPastePassesAnImageUriToTheImporter() {
		ActivityScenario.launch(MainActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertTrue(
					"The app must be foreground before reading the clipboard",
					activity.hasWindowFocus(),
				)
				val clipboard = activity.getSystemService(ClipboardManager::class.java)
				val previous = clipboard.primaryClip
				val file = NoteImages.file(activity, "${UUID.randomUUID()}.jpg")
				try {
					val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
					try {
						file.outputStream().use {
							bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
						}
					} finally {
						bitmap.recycle()
					}
					val uri =
						FileProvider.getUriForFile(activity, "${activity.packageName}.images", file)
					clipboard.setPrimaryClip(
						ClipData.newUri(activity.contentResolver, "QA image", uri)
					)
					var pasted: Uri? = null
					val editor = RichNoteEditor(activity).apply { onPasteImage = { pasted = it } }
					assertTrue(editor.onTextContextMenuItem(android.R.id.paste))
					assertEquals(uri, pasted)
				} finally {
					if (previous != null) clipboard.setPrimaryClip(previous)
					else if (android.os.Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip()
					else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
					file.delete()
				}
			}
		}
	}
}
