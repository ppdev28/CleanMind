package com.example.cleannotes.data.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cleannotes.model.Note

@Entity(tableName = "note")
data class NoteEntity(
	@PrimaryKey val id: Int? = null,
	val title: String,
	val content: String,
	val timestamp: Long,
	@ColumnInfo(defaultValue = "0") val isReminder: Boolean = false,
) {
	fun toNote() = Note(id, title, content, timestamp, isReminder)
}

fun Note.toNoteEntity() = NoteEntity(id, title, content, timestamp, isReminder)
