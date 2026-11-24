package com.example.cleannotes.presentation.ui.edit_note.richtext

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.text.*
import android.text.style.*
import android.view.Gravity
import android.widget.EditText
import com.example.cleannotes.model.NoteDocument
import com.example.cleannotes.model.TextMark

/** Native editable text preserves selection, IME composition and Android's copy/paste menu. */
class RichNoteEditor(context: Context) : EditText(context) {
	var onDocumentChanged: (NoteDocument) -> Unit = {}
	var onPasteImage: (Uri) -> Unit = {}
	var onEditorStateChanged: (Boolean, Boolean, Set<String>) -> Unit = { _, _, _ -> }
	private var ready = false
	private var insertedNewline = -1
	private var document = NoteDocument()
	private var changing = false
	private val undo = ArrayDeque<NoteDocument>()
	private val redo = ArrayDeque<NoteDocument>()

	init {
		importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
		gravity = Gravity.TOP or Gravity.START
		background = null
		setPadding(0, 0, 0, 24)
		includeFontPadding = false
		textSize = 16f
		minLines = 2
		inputType =
			android.text.InputType.TYPE_CLASS_TEXT or
					android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
					android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
		addTextChangedListener(
			object : TextWatcher {
				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int,
				) {
				}

				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					insertedNewline =
						if (!changing && before == 0 && count == 1 && s?.get(start) == '\n') start
						else -1
				}

				override fun afterTextChanged(s: Editable?) {
					if (changing) return
					val newline = insertedNewline
					insertedNewline = -1
					if (s != null && newline >= 0) {
						val start = s.toString().lastIndexOf('\n', newline - 1) + 1
						val line = s.substring(start, newline)
						val match = Regex("^(\\s*)(• |☐ |☑ |([0-9]+)\\. )(.*)$").matchEntire(line)
						if (match != null) {
							changing = true
							if (match.groupValues[4].isBlank()) {
								s.delete(start, newline)
							} else {
								val number = match.groupValues[3].toLongOrNull()
								val prefix =
									match.groupValues[1] +
											when {
												number != null -> "${number + 1}. "
												match.groupValues[2].startsWith("☑") -> "☐ "
												else -> match.groupValues[2]
											}
								s.insert(newline + 1, prefix)
								setSelection(newline + 1 + prefix.length)
							}
							changing = false
						}
					}
					publish()
				}
			}
		)
		ready = true
	}

	override fun onSelectionChanged(selStart: Int, selEnd: Int) {
		super.onSelectionChanged(selStart, selEnd)
		if (ready && !changing) notifyEditorState()
	}

	private fun notifyEditorState() {
		val start = minOf(selectionStart, selectionEnd).coerceAtLeast(0)
		val end = maxOf(selectionStart, selectionEnd).coerceAtLeast(0)
		val formats =
			document.marks
				.filter { mark -> mark.start <= start && mark.end >= end }
				.map { if (it.kind == "size") "size:${it.value}" else it.kind }
				.toSet()
		onEditorStateChanged(undo.isNotEmpty(), redo.isNotEmpty(), formats)
	}

	fun bind(value: NoteDocument) {
		if (value == document) {
			notifyEditorState()
			return
		}
		document = value
		render()
	}

	private fun render() {
		changing = true
		val cursor = selectionStart.coerceIn(0, document.text.length)
		val selectionEndBeforeRender = selectionEnd.coerceIn(0, document.text.length)
		val value = SpannableStringBuilder(document.text)
		document.marks.forEach { mark ->
			if (mark.start < 0 || mark.end > value.length || mark.start >= mark.end) return@forEach
			val span: Any =
				when (mark.kind) {
					"bold" -> StyleSpan(Typeface.BOLD)
					"italic" -> StyleSpan(Typeface.ITALIC)
					"underline" -> UnderlineSpan()
					"strike" -> StrikethroughSpan()
					"highlight" -> return@forEach
					"color" -> return@forEach
					"size" -> RelativeSizeSpan(mark.value / 100f)
					"indent" -> LeadingMarginSpan.Standard(mark.value)
					"align" ->
						AlignmentSpan.Standard(Layout.Alignment.values()[mark.value.coerceIn(0, 2)])

					else -> return@forEach
				}
			value.setSpan(span, mark.start, mark.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		}
		applyParagraphSpacing(value)
		setText(value)
		setSelection(cursor, selectionEndBeforeRender)
		setLineSpacing(document.extraLineSpacing.toFloat(), document.lineSpacing)
		changing = false
		notifyEditorState()
	}

	private fun snapshot(): NoteDocument {
		val value = text ?: return document
		val marks =
			value
				.getSpans(0, value.length, Any::class.java)
				.mapNotNull { span ->
					val type =
						when (span) {
							is StyleSpan -> if (span.style == Typeface.BOLD) "bold" else "italic"
							is UnderlineSpan -> "underline"
							is StrikethroughSpan -> "strike"
							is BackgroundColorSpan -> "highlight"
							is ForegroundColorSpan -> "color"
							is RelativeSizeSpan -> "size"
							is LeadingMarginSpan.Standard -> "indent"
							is AlignmentSpan.Standard -> "align"
							else -> return@mapNotNull null
						}
					val amount =
						when (span) {
							is ForegroundColorSpan -> span.foregroundColor
							is RelativeSizeSpan -> (span.sizeChange * 100).toInt()
							is LeadingMarginSpan.Standard -> span.getLeadingMargin(true)
							is AlignmentSpan.Standard -> span.alignment.ordinal
							else -> 0
						}
					TextMark(value.getSpanStart(span), value.getSpanEnd(span), type, amount)
				}
				.filter { it.start < it.end }
		return document.copy(text = value.toString(), marks = marks)
	}

	private fun publish() {
		text?.let { applyParagraphSpacing(it) }
		val next = snapshot()
		if (next == document) return
		undo.addLast(document)
		if (undo.size > 80) undo.removeFirst()
		redo.clear()
		document = next
		onDocumentChanged(next)
		notifyEditorState()
	}

	fun format(kind: String, amount: Int = 0) {
		val value = text ?: return
		var start = minOf(selectionStart, selectionEnd).coerceAtLeast(0)
		var end = maxOf(selectionStart, selectionEnd).coerceAtLeast(0)
		// Without a selection, format the current paragraph.
		if (start == end || kind in listOf("align", "indent")) {
			start = value.toString().lastIndexOf('\n', start - 1).let { if (it < 0) 0 else it + 1 }
			end = value.toString().indexOf('\n', end).let { if (it < 0) value.length else it }
		}
		if (start >= end) return
		val current = snapshot()
		val existing = current.marks.filter { it.kind == kind && it.start < end && it.end > start }
		val kept =
			current.marks.flatMap { mark ->
				if (
					(mark.kind == kind || kind == "clear") && mark.start < end && mark.end > start
				) {
					buildList {
						if (mark.start < start) add(mark.copy(end = start))
						if (mark.end > end) add(mark.copy(start = end))
					}
				} else listOf(mark)
			}
		val toggleOff = existing.any { it.start <= start && it.end >= end && it.value == amount }
		val next =
			current.copy(
				marks =
					kept +
							if (kind != "clear" && !toggleOff)
								listOf(
									TextMark(
										start,
										end,
										kind,
										amount,
									)
								)
							else emptyList()
			)
		commit(next)
		setSelection(start, end)
	}

	fun list(prefix: String) {
		val value = text ?: return
		val start =
			value.toString().lastIndexOf('\n', selectionStart - 1).let { if (it < 0) 0 else it + 1 }
		val end =
			value.toString().indexOf('\n', selectionEnd.coerceAtLeast(start)).let {
				if (it < 0) value.length else it
			}
		val selected = value.substring(start, end)
		val lines =
			selected
				.lines()
				.mapIndexed { index, line ->
					val clean = line.replace(Regex("^(• |☐ |☑ |\\d+\\. )"), "")
					(if (prefix == "number") "${index + 1}. " else prefix) + clean
				}
				.joinToString("\n")
		value.replace(start, end, lines)
	}

	fun checkItem() {
		val value = text ?: return
		val start = value.toString().lastIndexOf('\n', selectionStart - 1) + 1
		when {
			value.toString().startsWith("☐ ", start) -> value.replace(start, start + 1, "☑")
			value.toString().startsWith("☑ ", start) -> value.replace(start, start + 1, "☐")
			else -> value.insert(start, "☐ ")
		}
	}

	private fun applyParagraphSpacing(value: Spannable) {
		value.getSpans(0, value.length, ParagraphGap::class.java).forEach { value.removeSpan(it) }
		if (document.paragraphSpacing == 0) return
		value.toString().forEachIndexed { index, char ->
			if (char == '\n')
				value.setSpan(
					ParagraphGap(
						(document.paragraphSpacing * resources.displayMetrics.density).toInt()
					),
					index,
					index + 1,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
				)
		}
	}

	private class ParagraphGap(private val gap: Int) : LineHeightSpan {
		override fun chooseHeight(
			text: CharSequence,
			start: Int,
			end: Int,
			spanstartv: Int,
			v: Int,
			fm: android.graphics.Paint.FontMetricsInt,
		) {
			fm.descent += gap
			fm.bottom += gap
		}
	}

	fun paragraphSpacing(gap: Int) {
		commit(snapshot().copy(paragraphSpacing = gap))
	}

	fun spacing(multiplier: Float, extra: Int) {
		commit(snapshot().copy(lineSpacing = multiplier, extraLineSpacing = extra))
	}

	private fun commit(next: NoteDocument) {
		if (next == document) return
		undo.addLast(document)
		if (undo.size > 80) undo.removeFirst()
		redo.clear()
		document = next
		render()
		onDocumentChanged(next)
		notifyEditorState()
	}

	// Subtitle, attachments and reminder settings are edited outside the native text history.
	private fun restoreTextHistory(value: NoteDocument) =
		value.copy(
			subtitle = document.subtitle,
			images = document.images,
			reminder = document.reminder,
			backgroundColor = document.backgroundColor,
			pinned = document.pinned,
			archived = document.archived,
			reminderCompleted = document.reminderCompleted,
			checklist = document.checklist,
			createdAt = document.createdAt,
		)

	fun undoEdit() {
		if (undo.isNotEmpty()) {
			redo.addLast(document)
			document = restoreTextHistory(undo.removeLast())
			render()
			onDocumentChanged(document)
		}
	}

	fun redoEdit() {
		if (redo.isNotEmpty()) {
			undo.addLast(document)
			document = restoreTextHistory(redo.removeLast())
			render()
			onDocumentChanged(document)
		}
	}

	fun pasteImage(): Boolean {
		val clip =
			(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
				?: return false
		var consumed = false
		for (index in 0 until clip.itemCount) {
			val uri = clip.getItemAt(index).uri ?: continue
			if (
				runCatching {
					context.contentResolver.getType(uri)?.startsWith("image/") == true
				}
					.getOrDefault(false)
			) {
				onPasteImage(uri)
				consumed = true
			}
		}
		return consumed
	}

	override fun onTextContextMenuItem(id: Int): Boolean {
		if (id == android.R.id.paste && pasteImage()) return true
		return super.onTextContextMenuItem(id)
	}
}
