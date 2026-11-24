package com.example.cleannotes.data.source

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cleannotes.model.Note

@Entity(
	tableName = "note",
	foreignKeys =
		[
			ForeignKey(
				entity = FolderEntity::class,
				parentColumns = ["id"],
				childColumns = ["folderId"],
				onDelete = ForeignKey.SET_NULL,
			)
		],
	indices = [Index("folderId")],
)
data class NoteEntity(
	@PrimaryKey val id: Int? = null,
	val title: String,
	val content: String,
	val timestamp: Long,
	@ColumnInfo(defaultValue = "0") val isReminder: Boolean = false,
	val folderId: Int? = null,
) {
	fun toNote() = Note(id, title, content, timestamp, isReminder, folderId)
}

fun Note.toNoteEntity() = NoteEntity(id, title, content, timestamp, isReminder, folderId)
