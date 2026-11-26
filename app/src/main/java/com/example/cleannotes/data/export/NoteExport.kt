package com.example.cleannotes.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.DocumentsContract
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.cleannotes.model.NoteDocument
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object NoteExport {
	private val folderWrites = Mutex()

	fun prefs(context: Context) = context.getSharedPreferences("note_export", Context.MODE_PRIVATE)

	fun folder(context: Context): Uri? = prefs(context).getString("folder", null)?.let(Uri::parse)

	fun plainText(title: String, document: NoteDocument) =
		listOf(
			title,
			document.subtitle,
			if (document.markdown) com.example.cleannotes.model.MarkdownText.plain(document.text) else document.text,
			document.checklist.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text },
		)
			.filter { it.isNotBlank() }
			.joinToString("\n\n")

	fun filename(title: String, pdf: Boolean) =
		title.replace(Regex("[^\\p{L}\\p{N} ._-]"), "_").take(80).ifBlank { "Nota" } +
				if (pdf) ".pdf" else ".txt"

	fun write(
		context: Context,
		title: String,
		document: NoteDocument,
		pdf: Boolean,
		output: OutputStream,
	) {
		if (!pdf) {
			output.write(plainText(title, document).toByteArray(Charsets.UTF_8))
			return
		}
		val result = PdfDocument()
		try {
			var page: PdfDocument.Page? = null
			renderPages(
				context,
				title,
				document,
				{ number ->
					result
						.startPage(PdfDocument.PageInfo.Builder(595, 842, number).create())
						.also { page = it }
						.canvas
				},
				{ result.finishPage(requireNotNull(page)) },
			)
			result.writeTo(output)
		} finally {
			result.close()
		}
	}

	internal fun renderPages(
		context: Context,
		title: String,
		document: NoteDocument,
		startPage: (Int) -> android.graphics.Canvas,
		finishPage: () -> Unit,
	) {
		var number = 1
		var canvas = startPage(number)
		var y = 48f
		fun nextPage() {
			finishPage()
			canvas = startPage(++number)
			y = 48f
		}

		fun text(value: CharSequence, size: Float, bold: Boolean) {
			val paint =
				TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
					color = Color.BLACK
					textSize = size
					typeface =
						android.graphics.Typeface.create(
							"sans-serif",
							if (bold) android.graphics.Typeface.BOLD
							else android.graphics.Typeface.NORMAL,
						)
				}
			val layout =
				StaticLayout.Builder.obtain(value, 0, value.length, paint, 499)
					.setAlignment(Layout.Alignment.ALIGN_NORMAL)
					.setLineSpacing(3f, 1f)
					.setIncludePad(false)
					.build()
			for (line in 0 until layout.lineCount) {
				val top = layout.getLineTop(line)
				val bottom = layout.getLineBottom(line)
				val height = bottom - top
				if (y + height > 794) nextPage()
				canvas.save()
				canvas.clipRect(48f, y, 547f, y + height)
				canvas.translate(48f, y - top)
				layout.draw(canvas)
				canvas.restore()
				y += height
			}
			y += 16
		}
		text(title, 22f, true)
		if (document.subtitle.isNotBlank()) text(document.subtitle, 14f, false)
		val body = android.text.SpannableStringBuilder(document.text)
		document.marks.forEach { mark ->
			if (mark.start < 0 || mark.end > body.length || mark.start >= mark.end) return@forEach
			val span =
				when (mark.kind) {
					"bold" -> android.text.style.StyleSpan(android.graphics.Typeface.BOLD)
					"italic" -> android.text.style.StyleSpan(android.graphics.Typeface.ITALIC)
					"underline" -> android.text.style.UnderlineSpan()
					"strike" -> android.text.style.StrikethroughSpan()
					"size" -> android.text.style.RelativeSizeSpan(mark.value / 100f)
					else -> return@forEach
				}
			body.setSpan(span, mark.start, mark.end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
		text(
			if (document.markdown) com.example.cleannotes.text.MarkdownRendering.render(document.text) else body,
			12f,
			false
		)
		if (document.checklist.isNotEmpty())
			text(
				document.checklist.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text },
				12f,
				false,
			)
		document.images.forEach { name ->
			val bitmap =
				android.graphics.BitmapFactory.decodeFile(
					com.example.cleannotes.data.attachments.NoteImages.file(
						context,
						name,
					)
						.path
				) ?: return@forEach
			try {
				val width = minOf(499f, bitmap.width.toFloat())
				val scale = minOf(width / bitmap.width, 650f / bitmap.height)
				val w = bitmap.width * scale
				val h = bitmap.height * scale
				if (y + h > 794) nextPage()
				canvas.drawBitmap(
					bitmap,
					null,
					android.graphics.RectF(48f, y, 48f + w, y + h),
					null,
				)
				y += h + 16
			} finally {
				bitmap.recycle()
			}
		}
		finishPage()
	}

	suspend fun shareFile(
		context: Context,
		title: String,
		document: NoteDocument,
		pdf: Boolean,
	): Uri =
		withContext(Dispatchers.IO) {
			val directory = File(context.cacheDir, "exports").apply { mkdirs() }
			val file = File(directory, "${java.util.UUID.randomUUID()}-${filename(title, pdf)}")
			file.outputStream().use { write(context, title, document, pdf, it) }
			FileProvider.getUriForFile(context, "${context.packageName}.images", file)
		}

	suspend fun saveToFolder(
		context: Context,
		title: String,
		document: NoteDocument,
		pdf: Boolean,
		name: String = filename(title, pdf),
	) =
		withContext(Dispatchers.IO) {
			folderWrites.withLock {
				val tree = folder(context) ?: error("Elige una carpeta en Ajustes")
				val parent =
					DocumentsContract.buildDocumentUriUsingTree(
						tree,
						DocumentsContract.getTreeDocumentId(tree),
					)
				val children =
					DocumentsContract.buildChildDocumentsUriUsingTree(
						tree,
						DocumentsContract.getTreeDocumentId(tree),
					)
				var target: Uri? = null
				context.contentResolver
					.query(
						children,
						arrayOf(
							DocumentsContract.Document.COLUMN_DOCUMENT_ID,
							DocumentsContract.Document.COLUMN_DISPLAY_NAME,
						),
						null,
						null,
						null,
					)
					?.use { cursor ->
						while (cursor.moveToNext()) if (cursor.getString(1) == name) {
							target =
								DocumentsContract.buildDocumentUriUsingTree(
									tree,
									cursor.getString(0),
								)
							break
						}
					}
				val uri =
					target
						?: DocumentsContract.createDocument(
							context.contentResolver,
							parent,
							if (pdf) "application/pdf" else "text/plain",
							name,
						)
						?: error("No se pudo crear el archivo")
				context.contentResolver.openOutputStream(uri, "wt")?.use {
					write(context, title, document, pdf, it)
				} ?: error("No se pudo abrir el archivo")
			}
		}
}
