package com.example.cleannotes.editor

import android.app.AlarmManager
import android.app.Application
import com.example.cleannotes.data.export.NoteExport
import com.example.cleannotes.data.reminders.ReminderScheduler
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.ReminderConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ReminderExportTest {
	@Test
	fun repeatSkipsMissedOccurrencesAndOneShotDoesNotRepeat() {
		assertNull(ReminderConfig(100).nextAfter(200))
		assertEquals(180100L, ReminderConfig(100, repeatUnit = "minute").nextAfter(150100))
		assertEquals(
			7200100L,
			ReminderConfig(100, repeatUnit = "hour", interval = 2).nextAfter(200),
		)
		val config = ReminderConfig(12345, "Tomar agua", "day", 2, false, false, false)
		assertEquals(config, NoteDocument.decode(NoteDocument(reminder = config).encode()).reminder)
	}

	@Test
	fun scheduleRestoreAndCancelUseStableNoteIdentity() {
		val context = RuntimeEnvironment.getApplication()
		val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
		val at =
			4_102_444_800_000L // 2100: identity/restore checks must not expire if the host sleeps.
		ReminderScheduler.update(context, 42, "Llamar", ReminderConfig(at))
		assertEquals(1, alarms.scheduledAlarms.size)
		ReminderScheduler.update(context, 42, "Llamar", ReminderConfig(at + 60000))
		assertEquals(1, alarms.scheduledAlarms.size)
		ReminderScheduler.restore(context)
		assertEquals(1, alarms.scheduledAlarms.size)
		ReminderScheduler.cancel(context, 42)
		assertEquals(0, alarms.scheduledAlarms.size)
		ReminderScheduler.restore(context)
		assertEquals(0, alarms.scheduledAlarms.size)
	}

	@Test
	fun txtIsUtf8AndContainsNoSerializationOrFormattingMarkup() {
		val output = java.io.ByteArrayOutputStream()
		NoteExport.write(
			RuntimeEnvironment.getApplication(),
			"Título",
			NoteDocument("Texto ágil", subtitle = "Un resumen"),
			false,
			output,
		)
		assertEquals("Título\n\nUn resumen\n\nTexto ágil", output.toString("UTF-8"))
		assertFalse(NoteExport.filename("../Nota:/", true).contains('/'))
	}

	@Test
	@GraphicsMode(GraphicsMode.Mode.NATIVE)
	fun exportedLayoutPaginatesLongNotesWithoutClipping() {
		val bitmaps = mutableListOf<android.graphics.Bitmap>()
		var completed = 0
		NoteExport.renderPages(
			RuntimeEnvironment.getApplication(),
			"Una nota extensa",
			NoteDocument(
				(1..180).joinToString("\n") {
					"Línea $it. Texto de prueba con acentos: café, canción."
				}
			),
			{ _ ->
				val bitmap =
					android.graphics.Bitmap.createBitmap(
						595,
						842,
						android.graphics.Bitmap.Config.ARGB_8888,
					)
				bitmap.eraseColor(android.graphics.Color.WHITE)
				bitmaps.add(bitmap)
				android.graphics.Canvas(bitmap)
			},
			{ completed++ },
		)
		assertTrue(completed > 1)
		assertEquals(bitmaps.size, completed)
		bitmaps.forEachIndexed { index, bitmap ->
			java.io.File("/tmp/cleannotes-export-page-$index.png").outputStream().use {
				bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
			}
			bitmap.recycle()
		}
	}

	@Test
	fun completingFutureRepeatedReminderAdvancesInsteadOfRepeatingSameOccurrence() {
		val at = 4_102_444_800_000L
		val doc = NoteDocument(reminder = ReminderConfig(at, repeatUnit = "hour"))
		val next = doc.completeReminder(at - 60_000)
		assertEquals(at + 3_600_000, next.reminder!!.at)
		assertFalse(next.reminderCompleted)
		assertTrue(
			doc.copy(reminder = ReminderConfig(at)).completeReminder(at - 60_000).reminderCompleted
		)
	}
}
