package com.example.cleannotes.model

/** A note's type is independent of the text the user writes. */
data class Note(
	val id: Int? = null,
	val title: String,
	val content: String,
	val timestamp: Long,
	val isReminder: Boolean = false,
	val folderId: Int? = null,
) {
	/** Immutable note instances share one decoded document across list, search and editor consumers. */
	val document: NoteDocument by lazy(LazyThreadSafetyMode.PUBLICATION) { NoteDocument.decode(content) }
	val searchableText: String by lazy(LazyThreadSafetyMode.PUBLICATION) { document.plainText() }
}

/** Older notes use their last known timestamp until their first update pins that reference. */
fun Note.creationTimestamp(): Long = document.createdAt ?: timestamp
