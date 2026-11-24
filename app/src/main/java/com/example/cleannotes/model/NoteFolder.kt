package com.example.cleannotes.model

import java.text.Normalizer
import java.util.Locale

data class NoteFolder(val id: Int, val name: String)

/** Keep validation identical for creation and renaming, including accented names. */
fun normalizedFolderName(value: String): String =
	Normalizer.normalize(value.trim().replace(Regex("\\s+"), " "), Normalizer.Form.NFC).also {
		require(it.isNotBlank() && it.length <= 40) { "Usa un nombre de 1 a 40 caracteres." }
	}

fun folderNameKey(value: String): String = normalizedFolderName(value).lowercase(Locale.ROOT)
