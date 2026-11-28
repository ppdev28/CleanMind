package com.example.cleannotes.data.attachments

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.cleannotes.model.NoteFile
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NoteFiles {
	const val MAX_BYTES = 25L * 1024 * 1024
	fun file(context: Context, name: String): File {
		require(name.matches(Regex("[a-zA-Z0-9-]+\\.bin")))
		return File(File(context.filesDir, "note_files").apply { mkdirs() }, name)
	}

	fun uri(context: Context, attachment: NoteFile): Uri = FileProvider.getUriForFile(
		context,
		"${context.packageName}.images",
		file(context, attachment.name)
	)

	suspend fun import(context: Context, source: Uri): NoteFile = withContext(Dispatchers.IO) {
		var title = "Archivo"
		context.contentResolver.query(
			source,
			arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
			null,
			null,
			null
		)?.use { cursor ->
			if (cursor.moveToFirst()) {
				val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				if (name >= 0) title =
					cursor.getString(name).orEmpty().replace(Regex("[\\p{Cntrl}/\\\\]"), "_").take(150)
						.ifBlank { "Archivo" }
				val size = cursor.getColumnIndex(OpenableColumns.SIZE)
				if (size >= 0 && !cursor.isNull(size)) require(cursor.getLong(size) <= MAX_BYTES) { "El archivo supera los 25 MB." }
			}
		}
		val name = "${UUID.randomUUID()}.bin";
		val target = file(context, name)
		try {
			var total = 0L
			context.contentResolver.openInputStream(source)?.use { input ->
				target.outputStream().use { output ->
					val buffer = ByteArray(8192)
					while (true) {
						val count =
							input.read(buffer); if (count < 0) break; total += count; require(total <= MAX_BYTES) { "El archivo supera los 25 MB." }; output.write(
							buffer,
							0,
							count
						)
					}
				}
			} ?: error("No se pudo leer el archivo.")
			NoteFile(
				name,
				title,
				context.contentResolver.getType(source) ?: "application/octet-stream",
				total
			)
		} catch (error: Exception) {
			target.delete(); throw error
		}
	}
}
