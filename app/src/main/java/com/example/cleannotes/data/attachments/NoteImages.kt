package com.example.cleannotes.data.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NoteImages {
	private fun directory(context: Context) =
		File(context.filesDir, "note_images").apply { mkdirs() }

	fun file(context: Context, name: String): File {
		require(name.matches(Regex("[a-zA-Z0-9-]+\\.jpg")))
		return File(directory(context), name)
	}

	fun cameraFile(context: Context): Pair<String, Uri> {
		val name = "${UUID.randomUUID()}.jpg"
		val file = file(context, name).apply { createNewFile() }
		return name to FileProvider.getUriForFile(context, "${context.packageName}.images", file)
	}

	suspend fun import(context: Context, uri: Uri): String =
		withContext(Dispatchers.IO) {
			val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
			context.contentResolver.openInputStream(uri)?.use {
				BitmapFactory.decodeStream(it, null, options)
			}
			require(options.outWidth > 0 && options.outHeight > 0) { "Imagen no compatible" }
			var sample = 1
			while (maxOf(options.outWidth, options.outHeight) / sample > 2048) sample *= 2
			val bitmap =
				context.contentResolver.openInputStream(uri)?.use {
					BitmapFactory.decodeStream(
						it,
						null,
						BitmapFactory.Options().apply { inSampleSize = sample },
					)
				} ?: error("No se pudo leer la imagen")
			val orientation = runCatching {
				context.contentResolver.openInputStream(uri)?.use {
					android.media
						.ExifInterface(it)
						.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
				} ?: 1
			}
				.getOrDefault(1)
			val matrix =
				android.graphics.Matrix().apply {
					when (orientation) {
						2 -> setScale(-1f, 1f)
						3 -> setRotate(180f)
						4 -> setScale(1f, -1f)
						5 -> {
							setRotate(90f)
							postScale(-1f, 1f)
						}

						6 -> setRotate(90f)
						7 -> {
							setRotate(270f)
							postScale(-1f, 1f)
						}

						8 -> setRotate(270f)
					}
				}
			val oriented =
				if (matrix.isIdentity) bitmap
				else
					Bitmap.createBitmap(
						bitmap,
						0,
						0,
						bitmap.width,
						bitmap.height,
						matrix,
						true,
					)
			val name = "${UUID.randomUUID()}.jpg"
			try {
				file(context, name).outputStream().use {
					check(oriented.compress(Bitmap.CompressFormat.JPEG, 90, it))
				}
			} finally {
				if (oriented !== bitmap) oriented.recycle()
				bitmap.recycle()
			}
			name
		}

	suspend fun preview(context: Context, name: String): Bitmap? =
		withContext(Dispatchers.IO) {
			runCatching {
				val path = file(context, name).absolutePath
				val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
				BitmapFactory.decodeFile(path, bounds)
				var sample = 1
				while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1200) sample *= 2
				BitmapFactory.decodeFile(
					path,
					BitmapFactory.Options().apply { inSampleSize = sample },
				)
			}
				.getOrNull()
		}
}
