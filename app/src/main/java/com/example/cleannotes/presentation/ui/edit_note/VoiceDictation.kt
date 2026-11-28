package com.example.cleannotes.presentation.ui.edit_note

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.example.cleannotes.presentation.ui.edit_note.richtext.RichEditorController
import java.util.Locale

object VoiceDictation {
	fun intent(locale: Locale = Locale.getDefault()): Intent =
		Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
			.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
			.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
			.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta el texto de tu nota")
			.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

	fun result(code: Int, data: Intent?): String? = if (code == Activity.RESULT_OK)
		data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
			?.firstOrNull { it.isNotBlank() } else null
}

@Composable
fun rememberVoiceDictation(controller: RichEditorController): () -> Unit {
	val launcher =
		rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			VoiceDictation.result(result.resultCode, result.data)
				?.let { controller.editor?.insertDictation(it) }
		}
	return {
		try {
			launcher.launch(VoiceDictation.intent())
		} catch (_: ActivityNotFoundException) {
			controller.error = "Instala o activa un servicio de reconocimiento de voz en Android."
		} catch (_: SecurityException) {
			controller.error =
				"El servicio de dictado no está disponible. Revisa sus permisos en Android."
		}
	}
}
