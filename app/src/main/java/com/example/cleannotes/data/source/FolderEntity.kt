package com.example.cleannotes.data.source

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cleannotes.model.NoteFolder

@Entity(tableName = "folder", indices = [Index(value = ["nameKey"], unique = true)])
data class FolderEntity(
	@PrimaryKey(autoGenerate = true) val id: Int = 0,
	val name: String,
	val nameKey: String,
) {
	fun toFolder() = NoteFolder(id, name)
}
