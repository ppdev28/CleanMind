package com.example.cleannotes.data.source

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
	@Query("SELECT * FROM note ORDER BY timestamp DESC, id DESC")
	fun observeNotes(): Flow<List<NoteEntity>>

	@Query("SELECT * FROM note WHERE id = :id")
	suspend fun getNoteById(id: Int): NoteEntity?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertNote(note: NoteEntity): Long

	@Query("DELETE FROM note WHERE id IN (:ids)")
	suspend fun deleteNoteIds(ids: List<Int>)

	/** One transaction even when the selection exceeds SQLite's bind limit. */
	@Transaction
	suspend fun deleteNotes(ids: List<Int>) {
		ids.distinct().chunked(900).forEach { deleteNoteIds(it) }
	}

	@Query("SELECT * FROM folder ORDER BY nameKey, id")
	fun observeFolders(): Flow<List<FolderEntity>>

	@Query("SELECT * FROM folder WHERE id = :id")
	suspend fun getFolder(id: Int): FolderEntity?

	@Insert
	suspend fun insertFolder(folder: FolderEntity): Long

	@Query("UPDATE folder SET name = :name, nameKey = :key WHERE id = :id")
	suspend fun renameFolder(id: Int, name: String, key: String): Int

	@Query("DELETE FROM folder WHERE id = :id")
	suspend fun deleteFolder(id: Int)

	@Query("UPDATE note SET folderId = :folderId WHERE id = :id AND isReminder = 0")
	suspend fun moveNote(id: Int, folderId: Int?): Int

	/** Membership belongs to the database: a stale editor must not undo a later move. */
	@Transaction
	suspend fun savePreservingFolder(note: NoteEntity): Long {
		val previous = note.id?.let { getNoteById(it) }
		val selected = if (previous != null) previous.folderId else note.folderId
		val valid = selected?.takeIf { getFolder(it) != null }
		return insertNote(note.copy(folderId = if (note.isReminder) null else valid))
	}
}
